'use client';

import { useState } from 'react';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Eye, Pencil } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface MarkdownFieldProps {
  value: string;
  onChange: (next: string) => void;
  label?: string;
  placeholder?: string;
  rows?: number;
  /** When true, line breaks are not allowed (markup-line). */
  singleLine?: boolean;
  className?: string;
}

export function MarkdownField({
  value,
  onChange,
  label,
  placeholder,
  rows = 4,
  singleLine = false,
  className,
}: MarkdownFieldProps) {
  const [mode, setMode] = useState<'edit' | 'preview'>('edit');

  const handleChange = (raw: string) => {
    if (singleLine) {
      onChange(raw.replace(/\r?\n/g, ' '));
    } else {
      onChange(raw);
    }
  };

  return (
    <div className={`space-y-1 ${className ?? ''}`}>
      <div className="flex items-center justify-between">
        {label && <Label className="text-xs">{label}</Label>}
        <div className="ml-auto inline-flex rounded-md border bg-muted/30 p-0.5">
          <Button
            type="button"
            size="sm"
            variant={mode === 'edit' ? 'secondary' : 'ghost'}
            onClick={() => setMode('edit')}
            className="h-6 px-2 text-xs"
          >
            <Pencil className="h-3 w-3 mr-1" />
            Edit
          </Button>
          <Button
            type="button"
            size="sm"
            variant={mode === 'preview' ? 'secondary' : 'ghost'}
            onClick={() => setMode('preview')}
            className="h-6 px-2 text-xs"
          >
            <Eye className="h-3 w-3 mr-1" />
            Preview
          </Button>
        </div>
      </div>

      {mode === 'edit' ? (
        <Textarea
          value={value}
          onChange={(e) => handleChange(e.target.value)}
          placeholder={placeholder}
          rows={singleLine ? 1 : rows}
          className="font-mono text-xs"
        />
      ) : (
        <div
          className="min-h-[5rem] rounded-md border bg-muted/20 px-3 py-2 text-sm prose prose-sm max-w-none dark:prose-invert prose-headings:my-2 prose-p:my-1 prose-li:my-0"
          data-testid="markdown-preview"
        >
          {value ? (
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{value}</ReactMarkdown>
          ) : (
            <span className="italic text-muted-foreground">Nothing to preview.</span>
          )}
        </div>
      )}
    </div>
  );
}
