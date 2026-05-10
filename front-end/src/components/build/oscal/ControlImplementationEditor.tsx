'use client';
import { useMemo, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { ChevronDown, ChevronRight, Trash2, Plus, AlertTriangle } from 'lucide-react';

type Confidence = 'high' | 'medium' | 'low';

interface Prop {
  name?: string;
  ns?: string;
  value?: string;
}

interface ImplementedRequirement {
  uuid?: string;
  'control-id'?: string;
  description?: string;
  props?: Prop[];
  [k: string]: unknown;
}

interface Body {
  'control-implementation'?: {
    description?: string;
    'implemented-requirements'?: ImplementedRequirement[];
    [k: string]: unknown;
  };
  [k: string]: unknown;
}

interface Props {
  body: unknown;
  onChange: (next: Record<string, unknown>) => void;
}

const CONFIDENCE_NS = 'https://oscal-hub.io/ns';
const CONFIDENCE_NAME = 'ai-confidence';

function readConfidence(req: ImplementedRequirement): Confidence | undefined {
  const prop = (req.props ?? []).find(
    (p) => p?.name === CONFIDENCE_NAME && p?.ns === CONFIDENCE_NS,
  );
  const v = prop?.value;
  return v === 'high' || v === 'medium' || v === 'low' ? v : undefined;
}

function setConfidence(req: ImplementedRequirement, value: Confidence): ImplementedRequirement {
  const props = [...(req.props ?? [])];
  const idx = props.findIndex((p) => p?.name === CONFIDENCE_NAME && p?.ns === CONFIDENCE_NS);
  const prop: Prop = { name: CONFIDENCE_NAME, ns: CONFIDENCE_NS, value };
  if (idx >= 0) props[idx] = { ...props[idx], ...prop };
  else props.push(prop);
  return { ...req, props };
}

function isObject(v: unknown): v is Record<string, unknown> {
  return v != null && typeof v === 'object' && !Array.isArray(v);
}

function asBody(b: unknown): Body {
  return isObject(b) ? (b as Body) : {};
}

const CONFIDENCE_TONE: Record<Confidence, string> = {
  high: 'bg-emerald-500/15 text-emerald-300 border-emerald-700/40',
  medium: 'bg-sky-500/15 text-sky-300 border-sky-700/40',
  low: 'bg-amber-500/15 text-amber-300 border-amber-700/40',
};

function uuidv4(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  // Fallback — rfc4122 v4 from getRandomValues
  const buf = new Uint8Array(16);
  crypto.getRandomValues(buf);
  buf[6] = (buf[6] & 0x0f) | 0x40;
  buf[8] = (buf[8] & 0x3f) | 0x80;
  const hex = [...buf].map((b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export function ControlImplementationEditor({ body, onChange }: Props) {
  const data = asBody(body);
  const ctrlImpl = data['control-implementation'] ?? {};
  const reqs: ImplementedRequirement[] = Array.isArray(ctrlImpl['implemented-requirements'])
    ? (ctrlImpl['implemented-requirements'] as ImplementedRequirement[])
    : [];

  const [filter, setFilter] = useState('');
  const [showLowOnly, setShowLowOnly] = useState(false);
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});

  const filteredIndexes = useMemo(() => {
    const q = filter.trim().toLowerCase();
    return reqs
      .map((req, i) => ({ req, i }))
      .filter(({ req }) => {
        if (showLowOnly && readConfidence(req) !== 'low') return false;
        if (!q) return true;
        const id = (req['control-id'] ?? '').toLowerCase();
        const desc = (req.description ?? '').toLowerCase();
        return id.includes(q) || desc.includes(q);
      })
      .map(({ i }) => i);
  }, [reqs, filter, showLowOnly]);

  const writeReqs = (next: ImplementedRequirement[]) => {
    onChange({
      ...data,
      'control-implementation': {
        ...ctrlImpl,
        'implemented-requirements': next,
      },
    });
  };

  const updateReqAt = (i: number, patch: (req: ImplementedRequirement) => ImplementedRequirement) => {
    const next = reqs.map((r, idx) => (idx === i ? patch(r) : r));
    writeReqs(next);
  };

  const deleteReqAt = (i: number) => {
    if (!confirm(`Delete narrative for control ${reqs[i]['control-id'] ?? ''}?`)) return;
    writeReqs(reqs.filter((_, idx) => idx !== i));
  };

  const addReq = () => {
    const id = prompt('Control ID (e.g. ac-1):');
    if (!id) return;
    const trimmed = id.trim().toLowerCase();
    if (reqs.some((r) => (r['control-id'] ?? '').toLowerCase() === trimmed)) {
      alert(`A narrative for ${trimmed} already exists.`);
      return;
    }
    const fresh: ImplementedRequirement = {
      uuid: uuidv4(),
      'control-id': trimmed,
      description: '',
      props: [{ name: CONFIDENCE_NAME, ns: CONFIDENCE_NS, value: 'low' }],
    };
    writeReqs([...reqs, fresh]);
    setExpanded((e) => ({ ...e, [trimmed]: true }));
  };

  if (reqs.length === 0) {
    return (
      <div className="rounded-md border bg-muted/20 p-6 text-center text-sm text-muted-foreground">
        <p className="mb-3">
          No control implementations yet. Add one to start describing how the system addresses a control.
        </p>
        <Button size="sm" variant="outline" onClick={addReq}>
          <Plus className="h-4 w-4 mr-1" />
          Add control
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <Input
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          placeholder="Filter by control ID or text…"
          className="max-w-xs"
        />
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={showLowOnly}
            onChange={(e) => setShowLowOnly(e.target.checked)}
          />
          Low confidence only
        </label>
        <span className="text-sm text-muted-foreground ml-auto">
          {filteredIndexes.length} of {reqs.length} controls
        </span>
        <Button size="sm" variant="outline" onClick={addReq}>
          <Plus className="h-4 w-4 mr-1" />
          Add control
        </Button>
      </div>

      <div className="space-y-2 max-h-[60vh] overflow-y-auto pr-1">
        {filteredIndexes.length === 0 && (
          <div className="rounded-md border bg-muted/20 p-4 text-center text-sm text-muted-foreground">
            No controls match the current filter.
          </div>
        )}
        {filteredIndexes.map((i) => {
          const req = reqs[i];
          const id = req['control-id'] ?? '(unnamed)';
          const conf = readConfidence(req);
          const isExpanded = !!expanded[id];
          return (
            <div key={req.uuid ?? id ?? i} className="rounded-md border bg-card overflow-hidden">
              <button
                type="button"
                onClick={() => setExpanded((e) => ({ ...e, [id]: !isExpanded }))}
                className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-muted/40 transition-colors"
              >
                {isExpanded ? (
                  <ChevronDown className="h-4 w-4 text-muted-foreground shrink-0" />
                ) : (
                  <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0" />
                )}
                <code className="text-sm font-mono font-semibold uppercase">{id}</code>
                {conf && (
                  <Badge variant="outline" className={CONFIDENCE_TONE[conf]}>
                    {conf}
                  </Badge>
                )}
                {!conf && (
                  <Badge variant="outline" className="text-muted-foreground">
                    no confidence
                  </Badge>
                )}
                <span className="text-sm text-muted-foreground truncate flex-1">
                  {(req.description ?? '').slice(0, 120) || <em>(no narrative yet)</em>}
                </span>
                {conf === 'low' && (
                  <AlertTriangle className="h-4 w-4 text-amber-400 shrink-0" />
                )}
              </button>

              {isExpanded && (
                <div className="px-3 pb-3 pt-1 space-y-3 border-t bg-background/50">
                  <div className="space-y-2">
                    <Label htmlFor={`desc-${i}`}>Implementation narrative</Label>
                    <Textarea
                      id={`desc-${i}`}
                      rows={6}
                      value={req.description ?? ''}
                      onChange={(e) =>
                        updateReqAt(i, (r) => ({ ...r, description: e.target.value }))
                      }
                      placeholder="Describe how the system implements this control."
                    />
                  </div>

                  <div className="flex items-center gap-3">
                    <Label htmlFor={`conf-${i}`} className="text-sm">
                      AI confidence
                    </Label>
                    <select
                      id={`conf-${i}`}
                      value={conf ?? ''}
                      onChange={(e) =>
                        updateReqAt(i, (r) => setConfidence(r, e.target.value as Confidence))
                      }
                      className="h-8 rounded border bg-background px-2 text-sm"
                    >
                      <option value="">— not set —</option>
                      <option value="high">high</option>
                      <option value="medium">medium</option>
                      <option value="low">low</option>
                    </select>

                    <Button
                      size="sm"
                      variant="ghost"
                      className="ml-auto text-destructive hover:text-destructive"
                      onClick={() => deleteReqAt(i)}
                    >
                      <Trash2 className="h-4 w-4 mr-1" />
                      Delete
                    </Button>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
