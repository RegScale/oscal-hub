'use client';

import { useEffect, useRef } from 'react';
import Editor, { OnMount } from '@monaco-editor/react';
import { Card } from '@/components/ui/card';
import type { OscalFormat } from '@/types/oscal';

interface CodeEditorProps {
  content: string;
  format: OscalFormat;
  readOnly?: boolean;
  onContentChange?: (content: string) => void;
  highlightLine?: number;
  height?: string;
}

export function CodeEditor({
  content,
  format,
  readOnly = true,
  onContentChange,
  highlightLine,
  height = '600px',
}: CodeEditorProps) {
  const editorRef = useRef<any>(null);
  const monacoRef = useRef<any>(null);

  const handleEditorDidMount: OnMount = (editor, monaco) => {
    editorRef.current = editor;
    monacoRef.current = monaco;

    // Configure editor theme
    monaco.editor.defineTheme('oscal-dark', {
      base: 'vs-dark',
      inherit: true,
      rules: [],
      colors: {
        'editor.background': '#0a0a0a',
        'editor.foreground': '#e5e7eb',
        'editor.lineHighlightBackground': '#1e293b',
        'editorLineNumber.foreground': '#64748b',
        'editor.selectionBackground': '#334155',
        'editor.inactiveSelectionBackground': '#1e293b',
      },
    });
    monaco.editor.setTheme('oscal-dark');
  };

  useEffect(() => {
    if (editorRef.current && monacoRef.current && highlightLine !== undefined) {
      const editor = editorRef.current;
      const monaco = monacoRef.current;

      // Reveal the line
      editor.revealLineInCenter(highlightLine);

      // Set cursor position
      editor.setPosition({ lineNumber: highlightLine, column: 1 });

      // Highlight the line
      editor.deltaDecorations(
        [],
        [
          {
            range: new monaco.Range(highlightLine, 1, highlightLine, 1),
            options: {
              isWholeLine: true,
              className: 'highlight-line',
              glyphMarginClassName: 'highlight-glyph',
            },
          },
        ]
      );
    }
  }, [highlightLine]);

  const getLanguage = (format: OscalFormat): string => {
    switch (format) {
      case 'xml':
        return 'xml';
      case 'json':
        return 'json';
      case 'yaml':
        return 'yaml';
      default:
        return 'plaintext';
    }
  };

  return (
    <Card className="overflow-hidden">
      <style jsx global>{`
        .highlight-line {
          background: rgba(239, 68, 68, 0.1);
          border-left: 3px solid rgb(239, 68, 68);
        }
        .highlight-glyph {
          background: rgb(239, 68, 68);
          width: 3px !important;
          margin-left: 3px;
        }
      `}</style>
      <Editor
        height={height}
        language={getLanguage(format)}
        value={content}
        onChange={(value) => onContentChange?.(value || '')}
        onMount={handleEditorDidMount}
        theme="oscal-dark"
        options={{
          readOnly,
          minimap: { enabled: true },
          fontSize: 13,
          lineNumbers: 'on',
          scrollBeyondLastLine: false,
          automaticLayout: true,
          wordWrap: 'on',
          folding: true,
          lineDecorationsWidth: 10,
          lineNumbersMinChars: 4,
          renderLineHighlight: 'line',
          contextmenu: true,
          selectOnLineNumbers: true,
          roundedSelection: false,
          cursorStyle: 'line',
          formatOnPaste: true,
          formatOnType: true,
        }}
      />
    </Card>
  );
}
