'use client';

import { useState, useEffect, useRef } from 'react';
import Editor, { OnMount } from '@monaco-editor/react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { MarkdownPreview } from './markdown-preview';
import { RefreshCw } from 'lucide-react';
import type { editor } from 'monaco-editor';

interface TemplateEditorProps {
  initialName?: string;
  initialContent?: string;
  onSave: (name: string, content: string) => void;
  onCancel?: () => void;
  isSaving?: boolean;
}

export function TemplateEditor({
  initialName = '',
  initialContent = '',
  onSave,
  onCancel,
  isSaving = false,
}: TemplateEditorProps) {
  const [name, setName] = useState(initialName);
  const [content, setContent] = useState(initialContent);
  const [variables, setVariables] = useState<string[]>([]);

  const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null);

  // Extract variables from content
  // Pattern matches {{ anything }} - allows any content inside braces
  const extractVariables = (text: string) => {
    const pattern = /\{\{\s*([^}]+?)\s*\}\}/g;
    const matches = text.matchAll(pattern);
    const extractedVars = new Set<string>();

    for (const match of matches) {
      extractedVars.add(match[1].trim());
    }

    setVariables(Array.from(extractedVars));
  };

  useEffect(() => {
    extractVariables(content);
  }, [content]);

  const handleRefreshVariables = () => {
    extractVariables(content);
  };

  const handleEditorDidMount: OnMount = (editor, monaco) => {
    editorRef.current = editor;

    // Register custom language for variable highlighting
    monaco.languages.register({ id: 'markdown-template' });

    // Define tokenization rules for variables
    monaco.languages.setMonarchTokensProvider('markdown-template', {
      tokenizer: {
        root: [
          [/\{\{[^}]*\}\}/, 'variable'],
        ],
      },
    });

    // Configure dark theme for editor with purple variables
    monaco.editor.defineTheme('markdown-dark', {
      base: 'vs-dark',
      inherit: true,
      rules: [
        { token: 'variable', foreground: 'a78bfa', fontStyle: 'bold' },
      ],
      colors: {
        'editor.background': '#1e1e1e',
        'editor.foreground': '#d4d4d4',
        'editor.lineHighlightBackground': '#2a2a2a',
        'editorLineNumber.foreground': '#858585',
        'editor.selectionBackground': '#264f78',
        'editor.inactiveSelectionBackground': '#3a3d41',
      },
    });
    monaco.editor.setTheme('markdown-dark');
  };

  const handleSave = () => {
    if (name.trim() && content.trim()) {
      onSave(name, content);
    }
  };

  const insertVariable = () => {
    if (editorRef.current) {
      const position = editorRef.current.getPosition();
      if (position) {
        const variableName = prompt('Enter variable name:');
        if (variableName) {
          editorRef.current.executeEdits('', [
            {
              range: {
                startLineNumber: position.lineNumber,
                startColumn: position.column,
                endLineNumber: position.lineNumber,
                endColumn: position.column,
              },
              text: `{{ ${variableName} }}`,
            },
          ]);
          editorRef.current.focus();
        }
      }
    }
  };

  return (
    <div className="space-y-4">
      {/* Template Name Input */}
      <div className="space-y-2">
        <Label htmlFor="template-name">Template Name</Label>
        <Input
          id="template-name"
          type="text"
          placeholder="Enter template name..."
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="max-w-md"
        />
      </div>

      {/* Variables Status */}
      <Card className="p-4 bg-slate-800 border-slate-700">
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              <Label className="text-slate-200">Detected Variables</Label>
              <Badge variant="secondary" className="bg-slate-700 text-slate-200 border-slate-600">
                {variables.length} {variables.length === 1 ? 'variable' : 'variables'}
              </Badge>
            </div>

            {variables.length > 0 ? (
              <div className="flex flex-wrap gap-2">
                {variables.map((variable) => (
                  <Badge key={variable} variant="outline" className="bg-purple-500/10 border-purple-500/30 text-purple-400 font-mono">
                    {variable}
                  </Badge>
                ))}
              </div>
            ) : (
              <p className="text-sm text-slate-400">
                No variables detected. Use <code className="px-1 py-0.5 bg-slate-900 rounded text-xs text-purple-400">{`{{ variable name }}`}</code> to add variables.
              </p>
            )}
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleRefreshVariables}
            className="ml-4"
          >
            <RefreshCw className="h-4 w-4 mr-2" />
            Refresh
          </Button>
        </div>
      </Card>

      {/* Split View: Editor and Preview */}
      <div className="grid grid-cols-2 gap-4 items-start">
        {/* Editor */}
        <div className="space-y-2">
          <div className="flex items-center justify-between h-9">
            <Label>Template Content (Markdown)</Label>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={insertVariable}
            >
              Insert Variable
            </Button>
          </div>
          <Card className="overflow-hidden" style={{ height: '600px' }}>
            <Editor
              height="600px"
              language="markdown-template"
              value={content}
              onChange={(value) => setContent(value || '')}
              onMount={handleEditorDidMount}
              theme="markdown-dark"
              options={{
                readOnly: false,
                minimap: { enabled: false },
                fontSize: 14,
                lineNumbers: 'on',
                scrollBeyondLastLine: false,
                automaticLayout: true,
                wordWrap: 'on',
                folding: true,
                lineDecorationsWidth: 10,
                lineNumbersMinChars: 3,
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
        </div>

        {/* Preview */}
        <div className="space-y-2">
          <div className="flex items-center justify-between h-9">
            <Label>Preview</Label>
          </div>
          <MarkdownPreview content={content} height="600px" />
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-2 justify-end">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel} disabled={isSaving}>
            Cancel
          </Button>
        )}
        <Button type="button" onClick={handleSave} disabled={!name.trim() || !content.trim() || isSaving}>
          {isSaving ? 'Saving...' : 'Save Template'}
        </Button>
      </div>
    </div>
  );
}
