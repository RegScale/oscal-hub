'use client';
import { useMemo, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { ChevronDown, ChevronRight, Trash2, Plus } from 'lucide-react';

interface Prop {
  name?: string;
  ns?: string;
  value?: string;
}

interface PoamItem {
  uuid?: string;
  title?: string;
  description?: string;
  props?: Prop[];
  [k: string]: unknown;
}

interface Body {
  'poam-items'?: PoamItem[];
  [k: string]: unknown;
}

interface Props {
  body: unknown;
  onChange: (next: Record<string, unknown>) => void;
}

type Severity = 'high' | 'moderate' | 'low';
type Status = 'open' | 'ongoing' | 'risk-accepted' | 'closed';

const SEVERITIES: Severity[] = ['high', 'moderate', 'low'];
const STATUSES: Status[] = ['open', 'ongoing', 'risk-accepted', 'closed'];

const SEVERITY_TONE: Record<Severity, string> = {
  high: 'bg-red-500/15 text-red-300 border-red-700/40',
  moderate: 'bg-amber-500/15 text-amber-300 border-amber-700/40',
  low: 'bg-emerald-500/15 text-emerald-300 border-emerald-700/40',
};

const STATUS_TONE: Record<Status, string> = {
  open: 'bg-sky-500/15 text-sky-300 border-sky-700/40',
  ongoing: 'bg-indigo-500/15 text-indigo-300 border-indigo-700/40',
  'risk-accepted': 'bg-violet-500/15 text-violet-300 border-violet-700/40',
  closed: 'bg-emerald-500/15 text-emerald-300 border-emerald-700/40',
};

function isObject(v: unknown): v is Record<string, unknown> {
  return v != null && typeof v === 'object' && !Array.isArray(v);
}

function asBody(b: unknown): Body {
  return isObject(b) ? (b as Body) : {};
}

function readProp(item: PoamItem, name: string): string | undefined {
  return (item.props ?? []).find((p) => p?.name === name)?.value;
}

function setProp(item: PoamItem, name: string, value: string, ns?: string): PoamItem {
  const props = [...(item.props ?? [])];
  const idx = props.findIndex((p) => p?.name === name && p?.ns === ns);
  const next: Prop = ns ? { name, ns, value } : { name, value };
  if (idx >= 0) props[idx] = { ...props[idx], ...next };
  else props.push(next);
  return { ...item, props };
}

function uuidv4(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  const buf = new Uint8Array(16);
  crypto.getRandomValues(buf);
  buf[6] = (buf[6] & 0x0f) | 0x40;
  buf[8] = (buf[8] & 0x3f) | 0x80;
  const hex = [...buf].map((b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export function PoamItemsEditor({ body, onChange }: Props) {
  const data = asBody(body);
  const items: PoamItem[] = Array.isArray(data['poam-items']) ? (data['poam-items'] as PoamItem[]) : [];

  const [filter, setFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState<'' | Status>('');
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});

  const filteredIndexes = useMemo(() => {
    const q = filter.trim().toLowerCase();
    return items
      .map((item, i) => ({ item, i }))
      .filter(({ item }) => {
        if (statusFilter && readProp(item, 'status') !== statusFilter) return false;
        if (!q) return true;
        const title = (item.title ?? '').toLowerCase();
        const desc = (item.description ?? '').toLowerCase();
        const poamId = (readProp(item, 'poam-id') ?? '').toLowerCase();
        return title.includes(q) || desc.includes(q) || poamId.includes(q);
      })
      .map(({ i }) => i);
  }, [items, filter, statusFilter]);

  const writeItems = (next: PoamItem[]) => {
    onChange({ ...data, 'poam-items': next });
  };

  const updateItemAt = (i: number, patch: (item: PoamItem) => PoamItem) => {
    const next = items.map((it, idx) => (idx === i ? patch(it) : it));
    writeItems(next);
  };

  const deleteItemAt = (i: number) => {
    const label = items[i].title ?? readProp(items[i], 'poam-id') ?? `item ${i + 1}`;
    if (!confirm(`Delete POA&M item "${label}"?`)) return;
    writeItems(items.filter((_, idx) => idx !== i));
  };

  const addItem = () => {
    const fresh: PoamItem = {
      uuid: uuidv4(),
      title: 'New finding',
      description: '',
      props: [
        { name: 'poam-id', value: `P-${String(items.length + 1).padStart(3, '0')}` },
        { name: 'severity', value: 'moderate' },
        { name: 'status', value: 'open' },
        { name: 'scheduled-completion-date', value: '' },
      ],
    };
    writeItems([...items, fresh]);
    setExpanded((e) => ({ ...e, [fresh.uuid as string]: true }));
  };

  if (items.length === 0) {
    return (
      <div className="rounded-md border bg-muted/20 p-6 text-center text-sm text-muted-foreground">
        <p className="mb-3">
          No POA&M items yet. Add one to start tracking a weakness or finding.
        </p>
        <Button size="sm" variant="outline" onClick={addItem}>
          <Plus className="h-4 w-4 mr-1" />
          Add item
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
          placeholder="Filter by ID, title, or text…"
          className="max-w-xs"
        />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as '' | Status)}
          className="h-9 rounded border bg-background px-2 text-sm"
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <span className="text-sm text-muted-foreground ml-auto">
          {filteredIndexes.length} of {items.length} items
        </span>
        <Button size="sm" variant="outline" onClick={addItem}>
          <Plus className="h-4 w-4 mr-1" />
          Add item
        </Button>
      </div>

      <div className="space-y-2 max-h-[60vh] overflow-y-auto pr-1">
        {filteredIndexes.length === 0 && (
          <div className="rounded-md border bg-muted/20 p-4 text-center text-sm text-muted-foreground">
            No items match the current filter.
          </div>
        )}
        {filteredIndexes.map((i) => {
          const item = items[i];
          const poamId = readProp(item, 'poam-id') ?? '(no id)';
          const severity = (readProp(item, 'severity') as Severity | undefined) ?? undefined;
          const status = (readProp(item, 'status') as Status | undefined) ?? undefined;
          const dueDate = readProp(item, 'scheduled-completion-date') ?? '';
          const key = (item.uuid as string) ?? `item-${i}`;
          const isExpanded = !!expanded[key];

          return (
            <div key={key} className="rounded-md border bg-card overflow-hidden">
              <button
                type="button"
                onClick={() => setExpanded((e) => ({ ...e, [key]: !isExpanded }))}
                className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-muted/40 transition-colors"
              >
                {isExpanded ? (
                  <ChevronDown className="h-4 w-4 text-muted-foreground shrink-0" />
                ) : (
                  <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0" />
                )}
                <code className="text-sm font-mono font-semibold">{poamId}</code>
                {severity && (
                  <Badge variant="outline" className={SEVERITY_TONE[severity]}>
                    {severity}
                  </Badge>
                )}
                {status && (
                  <Badge variant="outline" className={STATUS_TONE[status]}>
                    {status}
                  </Badge>
                )}
                <span className="text-sm font-medium truncate flex-1">
                  {item.title || <em>(no title)</em>}
                </span>
                {dueDate && (
                  <span className="text-xs text-muted-foreground shrink-0">due {dueDate}</span>
                )}
              </button>

              {isExpanded && (
                <div className="px-3 pb-3 pt-1 space-y-3 border-t bg-background/50">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <div className="space-y-1">
                      <Label htmlFor={`title-${i}`}>Title</Label>
                      <Input
                        id={`title-${i}`}
                        value={item.title ?? ''}
                        onChange={(e) =>
                          updateItemAt(i, (it) => ({ ...it, title: e.target.value }))
                        }
                      />
                    </div>
                    <div className="space-y-1">
                      <Label htmlFor={`poamid-${i}`}>POA&M ID</Label>
                      <Input
                        id={`poamid-${i}`}
                        value={poamId === '(no id)' ? '' : poamId}
                        onChange={(e) =>
                          updateItemAt(i, (it) => setProp(it, 'poam-id', e.target.value))
                        }
                      />
                    </div>
                    <div className="space-y-1">
                      <Label htmlFor={`sev-${i}`}>Severity</Label>
                      <select
                        id={`sev-${i}`}
                        value={severity ?? ''}
                        onChange={(e) =>
                          updateItemAt(i, (it) => setProp(it, 'severity', e.target.value))
                        }
                        className="h-9 w-full rounded border bg-background px-2 text-sm"
                      >
                        <option value="">— not set —</option>
                        {SEVERITIES.map((s) => (
                          <option key={s} value={s}>
                            {s}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="space-y-1">
                      <Label htmlFor={`stat-${i}`}>Status</Label>
                      <select
                        id={`stat-${i}`}
                        value={status ?? ''}
                        onChange={(e) =>
                          updateItemAt(i, (it) => setProp(it, 'status', e.target.value))
                        }
                        className="h-9 w-full rounded border bg-background px-2 text-sm"
                      >
                        <option value="">— not set —</option>
                        {STATUSES.map((s) => (
                          <option key={s} value={s}>
                            {s}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="space-y-1 md:col-span-2">
                      <Label htmlFor={`due-${i}`}>Scheduled completion date</Label>
                      <Input
                        id={`due-${i}`}
                        type="date"
                        value={dueDate}
                        onChange={(e) =>
                          updateItemAt(i, (it) =>
                            setProp(it, 'scheduled-completion-date', e.target.value),
                          )
                        }
                      />
                    </div>
                  </div>

                  <div className="space-y-1">
                    <Label htmlFor={`desc-${i}`}>Weakness / remediation narrative</Label>
                    <Textarea
                      id={`desc-${i}`}
                      rows={6}
                      value={item.description ?? ''}
                      onChange={(e) =>
                        updateItemAt(i, (it) => ({ ...it, description: e.target.value }))
                      }
                      placeholder="Describe the weakness, impact, and planned remediation."
                    />
                  </div>

                  <div className="flex">
                    <Button
                      size="sm"
                      variant="ghost"
                      className="ml-auto text-destructive hover:text-destructive"
                      onClick={() => deleteItemAt(i)}
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
