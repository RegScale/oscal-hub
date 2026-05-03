'use client';

import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Copy, Check, Download } from 'lucide-react';

interface JsonPreviewProps {
  value: unknown;
  filename?: string;
  maxHeight?: string;
}

export function JsonPreview({ value, filename = 'oscal.json', maxHeight = '600px' }: JsonPreviewProps) {
  const [copied, setCopied] = useState(false);
  const json = useMemo(() => {
    try {
      return JSON.stringify(value, null, 2);
    } catch {
      return '/* Unable to serialize value */';
    }
  }, [value]);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(json);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Copy failed', err);
    }
  };

  const handleDownload = () => {
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="rounded-md border bg-muted/40">
      <div className="flex items-center justify-between border-b px-3 py-2">
        <span className="text-xs font-medium text-muted-foreground">Live OSCAL JSON</span>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={handleCopy}>
            {copied ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
            <span className="ml-1.5 text-xs">{copied ? 'Copied' : 'Copy'}</span>
          </Button>
          <Button variant="outline" size="sm" onClick={handleDownload}>
            <Download className="h-3.5 w-3.5" />
            <span className="ml-1.5 text-xs">Download</span>
          </Button>
        </div>
      </div>
      <pre
        className="overflow-auto p-3 text-xs leading-relaxed"
        style={{ maxHeight }}
      >
        <code>{json}</code>
      </pre>
    </div>
  );
}
