'use client';

import { useState } from 'react';
import { Loader2, Sparkles } from 'lucide-react';
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
          <div className="self-start max-w-[85%] mr-auto rounded-md px-3 py-2 bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-200 dark:border-indigo-800">
            <div className="flex items-center gap-2 text-sm text-indigo-700 dark:text-indigo-300">
              <Sparkles className="h-4 w-4 animate-pulse" />
              <span>Generating</span>
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
              <span className="inline-flex gap-0.5 ml-0.5" aria-hidden="true">
                <span className="w-1 h-1 rounded-full bg-indigo-500 dark:bg-indigo-400 animate-bounce [animation-delay:-0.3s]" />
                <span className="w-1 h-1 rounded-full bg-indigo-500 dark:bg-indigo-400 animate-bounce [animation-delay:-0.15s]" />
                <span className="w-1 h-1 rounded-full bg-indigo-500 dark:bg-indigo-400 animate-bounce" />
              </span>
            </div>
          </div>
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
