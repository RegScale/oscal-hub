'use client';

import { Card } from '@/components/ui/card';

interface MarkdownPreviewProps {
  content: string;
  height?: string;
}

export function MarkdownPreview({ content, height = '600px' }: MarkdownPreviewProps) {
  // Process markdown content and highlight variables
  const processContent = (markdown: string): string => {
    let processed = markdown;

    // Replace {{ variable }} with highlighted spans
    const variablePattern = /\{\{\s*([^}]+)\s*\}\}/g;
    processed = processed.replace(variablePattern, (match, varName) => {
      return `<span class="variable-highlight">{{ ${varName.trim()} }}</span>`;
    });

    // Basic markdown to HTML conversion
    // Headers
    processed = processed.replace(/^### (.*$)/gim, '<h3>$1</h3>');
    processed = processed.replace(/^## (.*$)/gim, '<h2>$1</h2>');
    processed = processed.replace(/^# (.*$)/gim, '<h1>$1</h1>');

    // Bold
    processed = processed.replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>');

    // Italic
    processed = processed.replace(/\*(.*?)\*/gim, '<em>$1</em>');

    // Lists
    processed = processed.replace(/^\* (.*$)/gim, '<li>$1</li>');
    processed = processed.replace(/(<li>[\s\S]*?<\/li>)/, '<ul>$1</ul>');

    // Line breaks
    processed = processed.replace(/\n\n/g, '</p><p>');
    processed = `<p>${processed}</p>`;

    return processed;
  };

  return (
    <Card className="overflow-hidden bg-slate-900 border-slate-700" style={{ height }}>
      <style jsx global>{`
        .markdown-preview h1 {
          font-size: 2rem;
          font-weight: bold;
          margin-bottom: 1rem;
          margin-top: 1.5rem;
          border-bottom: 2px solid #475569;
          padding-bottom: 0.5rem;
          color: #e2e8f0;
        }
        .markdown-preview h2 {
          font-size: 1.5rem;
          font-weight: bold;
          margin-bottom: 0.75rem;
          margin-top: 1.25rem;
          border-bottom: 1px solid #475569;
          padding-bottom: 0.25rem;
          color: #e2e8f0;
        }
        .markdown-preview h3 {
          font-size: 1.25rem;
          font-weight: bold;
          margin-bottom: 0.5rem;
          margin-top: 1rem;
          color: #e2e8f0;
        }
        .markdown-preview p {
          margin-bottom: 1rem;
          line-height: 1.6;
          color: #cbd5e1;
        }
        .markdown-preview ul {
          margin-bottom: 1rem;
          margin-left: 1.5rem;
          list-style-type: disc;
          color: #cbd5e1;
        }
        .markdown-preview li {
          margin-bottom: 0.25rem;
        }
        .markdown-preview strong {
          font-weight: 600;
          color: #f1f5f9;
        }
        .markdown-preview em {
          font-style: italic;
        }
        .variable-highlight {
          color: #a78bfa;
          padding: 0.125rem 0.375rem;
          border-radius: 0.25rem;
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
          font-size: 0.875rem;
          font-weight: 500;
        }
      `}</style>
      <div className="h-full overflow-auto p-6">
        <div
          className="markdown-preview prose prose-slate max-w-none"
          dangerouslySetInnerHTML={{ __html: processContent(content) }}
        />
      </div>
    </Card>
  );
}
