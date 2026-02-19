'use client';

import dynamic from 'next/dynamic';
import { Loader2 } from 'lucide-react';

/**
 * Loading skeleton for Monaco Editor.
 * Displays while the editor is being lazy-loaded.
 */
function EditorSkeleton({ height = '400px' }: { height?: string }) {
  return (
    <div
      className="flex items-center justify-center bg-muted/50 border border-border rounded-md animate-pulse"
      style={{ height }}
    >
      <div className="flex flex-col items-center gap-2 text-muted-foreground">
        <Loader2 className="h-8 w-8 animate-spin" />
        <span className="text-sm">Loading editor...</span>
      </div>
    </div>
  );
}

/**
 * Lazy-loaded Monaco Editor component.
 *
 * This component uses Next.js dynamic imports to code-split Monaco Editor,
 * which is approximately 10MB uncompressed. The editor is only loaded when
 * this component is rendered, significantly reducing initial bundle size.
 *
 * Usage:
 * ```tsx
 * import { LazyMonacoEditor } from '@/components/lazy/LazyMonacoEditor';
 *
 * <LazyMonacoEditor
 *   height="400px"
 *   defaultLanguage="json"
 *   defaultValue={content}
 *   onChange={(value) => setContent(value || '')}
 * />
 * ```
 */
export const LazyMonacoEditor = dynamic(
  () => import('@monaco-editor/react').then((mod) => mod.default),
  {
    loading: () => <EditorSkeleton />,
    ssr: false, // Monaco doesn't support SSR
  }
);

/**
 * Lazy-loaded Monaco Diff Editor component.
 */
export const LazyMonacoDiffEditor = dynamic(
  () => import('@monaco-editor/react').then((mod) => mod.DiffEditor),
  {
    loading: () => <EditorSkeleton height="500px" />,
    ssr: false,
  }
);

/**
 * Preload Monaco Editor.
 * Call this function to start loading Monaco in the background
 * before the user navigates to a page that needs it.
 *
 * Usage:
 * ```tsx
 * // In a parent component or on hover
 * import { preloadMonacoEditor } from '@/components/lazy/LazyMonacoEditor';
 *
 * <button onMouseEnter={preloadMonacoEditor}>
 *   Edit Code
 * </button>
 * ```
 */
export function preloadMonacoEditor() {
  // This triggers the dynamic import
  import('@monaco-editor/react');
}

export default LazyMonacoEditor;
