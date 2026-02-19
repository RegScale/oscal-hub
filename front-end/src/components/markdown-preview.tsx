'use client';

import { Card } from '@/components/ui/card';

interface MarkdownPreviewProps {
  content: string;
  height?: string;
}

export function MarkdownPreview({ content, height = '600px' }: MarkdownPreviewProps) {
  // Process markdown content and highlight variables
  const processContent = (markdown: string): string => {
    if (!markdown) return '';

    // Split into lines for processing
    const lines = markdown.split('\n');
    const htmlLines: string[] = [];
    let inList = false;
    let inCodeBlock = false;

    for (let i = 0; i < lines.length; i++) {
      let line = lines[i];

      // Handle code blocks
      if (line.startsWith('```')) {
        if (inCodeBlock) {
          htmlLines.push('</code></pre>');
          inCodeBlock = false;
        } else {
          htmlLines.push('<pre class="code-block"><code>');
          inCodeBlock = true;
        }
        continue;
      }

      if (inCodeBlock) {
        htmlLines.push(escapeHtml(line));
        continue;
      }

      // Replace {{ variable }} with highlighted spans
      line = line.replace(/\{\{\s*([^}]+)\s*\}\}/g, (match, varName) => {
        return `<span class="variable-highlight">{{ ${varName.trim()} }}</span>`;
      });

      // Headers
      if (line.startsWith('### ')) {
        if (inList) { htmlLines.push('</ul>'); inList = false; }
        htmlLines.push(`<h3>${line.slice(4)}</h3>`);
        continue;
      }
      if (line.startsWith('## ')) {
        if (inList) { htmlLines.push('</ul>'); inList = false; }
        htmlLines.push(`<h2>${line.slice(3)}</h2>`);
        continue;
      }
      if (line.startsWith('# ')) {
        if (inList) { htmlLines.push('</ul>'); inList = false; }
        htmlLines.push(`<h1>${line.slice(2)}</h1>`);
        continue;
      }

      // Horizontal rule
      if (line.match(/^(-{3,}|\*{3,}|_{3,})$/)) {
        if (inList) { htmlLines.push('</ul>'); inList = false; }
        htmlLines.push('<hr />');
        continue;
      }

      // Lists (unordered)
      if (line.match(/^[\*\-] /)) {
        if (!inList) {
          htmlLines.push('<ul>');
          inList = true;
        }
        let content = line.slice(2);
        content = processInlineFormatting(content);
        htmlLines.push(`<li>${content}</li>`);
        continue;
      }

      // Lists (ordered)
      if (line.match(/^\d+\. /)) {
        // Close unordered list if open
        if (inList) { htmlLines.push('</ul>'); inList = false; }
        let content = line.replace(/^\d+\. /, '');
        content = processInlineFormatting(content);
        htmlLines.push(`<li>${content}</li>`);
        continue;
      }

      // Close list if we hit a non-list line
      if (inList && line.trim() !== '') {
        htmlLines.push('</ul>');
        inList = false;
      }

      // Empty line = paragraph break
      if (line.trim() === '') {
        htmlLines.push('<br />');
        continue;
      }

      // Regular paragraph
      line = processInlineFormatting(line);
      htmlLines.push(`<p>${line}</p>`);
    }

    // Close any open list
    if (inList) {
      htmlLines.push('</ul>');
    }
    if (inCodeBlock) {
      htmlLines.push('</code></pre>');
    }

    return htmlLines.join('\n');
  };

  // Process inline formatting (bold, italic, code, links)
  const processInlineFormatting = (text: string): string => {
    // Inline code
    text = text.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');
    // Bold
    text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    text = text.replace(/__([^_]+)__/g, '<strong>$1</strong>');
    // Italic
    text = text.replace(/\*([^*]+)\*/g, '<em>$1</em>');
    text = text.replace(/_([^_]+)_/g, '<em>$1</em>');
    // Links
    text = text.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" class="md-link">$1</a>');
    return text;
  };

  // Escape HTML to prevent XSS in code blocks
  const escapeHtml = (text: string): string => {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
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
        .markdown-preview .code-block {
          background-color: #1e293b;
          border: 1px solid #334155;
          border-radius: 0.375rem;
          padding: 1rem;
          margin: 1rem 0;
          overflow-x: auto;
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
          font-size: 0.875rem;
          color: #e2e8f0;
          white-space: pre;
        }
        .markdown-preview .inline-code {
          background-color: #334155;
          padding: 0.125rem 0.375rem;
          border-radius: 0.25rem;
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
          font-size: 0.875em;
          color: #f472b6;
        }
        .markdown-preview .md-link {
          color: #60a5fa;
          text-decoration: underline;
        }
        .markdown-preview .md-link:hover {
          color: #93c5fd;
        }
        .markdown-preview hr {
          border: none;
          border-top: 1px solid #475569;
          margin: 1.5rem 0;
        }
        .markdown-preview br {
          display: block;
          content: "";
          margin-top: 0.5rem;
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
