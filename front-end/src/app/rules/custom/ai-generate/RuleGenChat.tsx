'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import type { ChatEntry } from '@/types/rule-gen';

interface Props {
  entries: ChatEntry[];
  loading: boolean;
  disabled: boolean;
  placeholder?: string;
  onSend: (text: string) => void;
}

export function RuleGenChat({ entries, loading, disabled, placeholder, onSend }: Props) {
  const [draft, setDraft] = useState('');
  const submit = () => {
    const t = draft.trim();
    if (!t) return;
    onSend(t);
    setDraft('');
  };
  return (
    <div className="flex flex-col h-full border rounded-md">
      <div className="flex-1 overflow-auto p-3 space-y-2">
        {entries.map((e, i) => (
          <div
            key={i}
            className={
              e.role === 'user'
                ? 'self-end max-w-[85%] ml-auto bg-primary text-primary-foreground rounded-md px-3 py-2'
                : 'self-start max-w-[85%] mr-auto bg-muted rounded-md px-3 py-2'
            }
          >
            <p className="text-sm whitespace-pre-wrap">{e.text}</p>
          </div>
        ))}
        {loading && (
          <div className="text-xs text-muted-foreground italic">Thinking…</div>
        )}
      </div>
      <div className="border-t p-2 flex gap-2">
        <Textarea
          value={draft}
          disabled={disabled || loading}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
              e.preventDefault();
              submit();
            }
          }}
          placeholder={placeholder ?? 'Describe the rule…'}
          className="min-h-[60px]"
        />
        <Button onClick={submit} disabled={disabled || loading || !draft.trim()}>
          Send
        </Button>
      </div>
    </div>
  );
}
