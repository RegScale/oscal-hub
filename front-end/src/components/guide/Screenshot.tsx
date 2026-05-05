export function Screenshot({ src, alt, caption }: { src?: string; alt: string; caption?: string }) {
  if (!src) return null;
  return (
    <figure className="my-6 rounded-lg border border-border overflow-hidden">
      <img src={src} alt={alt} className="block w-full" />
      {caption && <figcaption className="px-4 py-2 text-sm text-muted-foreground bg-muted">{caption}</figcaption>}
    </figure>
  );
}
