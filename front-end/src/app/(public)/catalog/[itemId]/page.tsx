"use client";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { publicCatalogApi, type PublicItemSummary } from "@/lib/api/public-catalog";

export default function PublicCatalogDetailPage() {
  const params = useParams<{ itemId: string }>();
  const itemId = params.itemId;
  const [item, setItem] = useState<PublicItemSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [downloading, setDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);

  // Auth state — read directly from localStorage rather than useAuth() because
  // this page lives in a (public) route group with no auth context provider.
  const [hasToken, setHasToken] = useState(false);
  useEffect(() => {
    if (typeof window !== "undefined") {
      setHasToken(!!localStorage.getItem("token"));
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    publicCatalogApi.get(itemId)
      .then(data => { if (!cancelled) setItem(data); })
      .catch(e => { if (!cancelled) setError(String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [itemId]);

  if (loading) return <p className="text-sm text-slate-500">Loading…</p>;
  if (error || !item) return (
    <div>
      <p className="text-sm text-red-600">Item not found.</p>
      <Link href="/catalog" className="text-sm text-blue-600 hover:underline">← Back to catalog</Link>
    </div>
  );

  return (
    <div className="max-w-3xl">
      <Link href="/catalog" className="text-sm text-blue-600 hover:underline">← Back to catalog</Link>

      <div className="flex items-center gap-2 mt-4 mb-2">
        <span className="text-xs uppercase tracking-wide bg-slate-100 px-2 py-0.5 rounded">{item.oscalType}</span>
        <span className="text-xs text-slate-500">v{item.currentVersionNumber ?? "—"}</span>
      </div>

      <h1 className="text-2xl font-semibold mb-3">{item.title}</h1>

      {item.description && (
        <p className="text-sm text-slate-700 mb-4 whitespace-pre-line">{item.description}</p>
      )}

      <div className="flex flex-wrap gap-1 mb-4">
        {item.tags.map(t => (
          <span key={t} className="text-xs bg-blue-50 text-blue-700 px-1.5 py-0.5 rounded">{t}</span>
        ))}
      </div>

      <dl className="grid grid-cols-2 gap-2 text-sm mb-6">
        <dt className="text-slate-500">Last published</dt>
        <dd>{item.lastPublishedAt ? new Date(item.lastPublishedAt).toLocaleString() : "—"}</dd>
        <dt className="text-slate-500">Downloads</dt>
        <dd>{item.downloadCount ?? 0}</dd>
        {item.totalRatings != null && item.totalRatings > 0 && (
          <>
            <dt className="text-slate-500">Average rating</dt>
            <dd>★ {item.averageRating?.toFixed(1)} ({item.totalRatings} ratings)</dd>
          </>
        )}
      </dl>

      <div className="flex gap-2">
        {hasToken ? (
          <button
            type="button"
            disabled={downloading}
            onClick={async () => {
              setDownloading(true);
              setDownloadError(null);
              try {
                await publicCatalogApi.download(item.itemId,
                  `${item.title.replace(/[^a-zA-Z0-9._-]/g, "_")}.${item.oscalType}.json`);
              } catch (e) {
                setDownloadError(String(e));
              } finally {
                setDownloading(false);
              }
            }}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
            {downloading ? "Downloading…" : "Download"}
          </button>
        ) : (
          <Link href="/login"
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
            Sign in to download
          </Link>
        )}
        {hasToken ? (
          <Link href={`/library/${item.itemId}`}
                className="px-4 py-2 border rounded hover:bg-slate-50">
            Rate / comment
          </Link>
        ) : (
          <Link href="/login"
                className="px-4 py-2 border rounded hover:bg-slate-50">
            Sign in to rate
          </Link>
        )}
      </div>
      {downloadError && (
        <p className="mt-3 text-sm text-red-600">Download failed: {downloadError}</p>
      )}
    </div>
  );
}
