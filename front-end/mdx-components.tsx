import type { MDXComponents } from 'mdx/types';
import { ReactNode } from 'react';
import { Callout } from '@/components/guide/Callout';
import { Steps } from '@/components/guide/Steps';
import { Step } from '@/components/guide/Step';
import { Screenshot } from '@/components/guide/Screenshot';

export function useMDXComponents(components: MDXComponents): MDXComponents {
  return {
    h1: ({ children }) => <h1 className="text-4xl font-bold tracking-tight mb-3">{children}</h1>,
    h2: ({ children, id }) => <h2 id={id} className="text-2xl font-semibold tracking-tight mt-12 mb-4 scroll-mt-20 group">{children}</h2>,
    h3: ({ children, id }) => <h3 id={id} className="text-xl font-semibold mt-8 mb-3 scroll-mt-20">{children}</h3>,
    h4: ({ children }) => <h4 className="text-lg font-semibold mt-6 mb-2">{children}</h4>,
    p: ({ children }) => <p className="text-muted-foreground leading-7 my-4">{children}</p>,
    ul: ({ children }) => <ul className="list-disc list-inside my-4 space-y-1 text-muted-foreground">{children}</ul>,
    ol: ({ children }) => <ol className="list-decimal list-inside my-4 space-y-1 text-muted-foreground">{children}</ol>,
    li: ({ children }) => <li className="leading-7">{children}</li>,
    code: ({ children }) => <code className="rounded bg-muted px-1.5 py-0.5 text-sm font-mono">{children}</code>,
    pre: ({ children }) => <pre className="my-4 overflow-x-auto rounded-lg border border-border bg-muted p-4 text-sm font-mono">{children}</pre>,
    a: ({ href, children }) => <a href={href} className="text-primary underline-offset-4 hover:underline">{children}</a>,
    blockquote: ({ children }: { children?: ReactNode }) => <blockquote className="my-4 border-l-4 border-border pl-4 italic text-muted-foreground">{children}</blockquote>,
    hr: () => <hr className="my-8 border-border" />,
    table: ({ children }) => (
      <div className="my-6 overflow-x-auto rounded-lg border border-border">
        <table className="w-full text-sm border-collapse">{children}</table>
      </div>
    ),
    thead: ({ children }) => <thead className="bg-muted/60">{children}</thead>,
    tbody: ({ children }) => <tbody className="divide-y divide-border">{children}</tbody>,
    tr: ({ children }) => <tr>{children}</tr>,
    th: ({ children }) => <th className="px-4 py-2 text-left font-semibold text-foreground">{children}</th>,
    td: ({ children }) => <td className="px-4 py-2 text-muted-foreground align-top">{children}</td>,
    Callout,
    Steps,
    Step,
    Screenshot,
    ...components,
  };
}
