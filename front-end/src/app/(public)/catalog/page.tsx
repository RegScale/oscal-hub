"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { publicCatalogApi, type PublicItemSummary, type PublicCatalogPage } from "@/lib/api/public-catalog";

export default function PublicCatalogPage() {
  const [items, setItems] = useState<PublicItemSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [q, setQ] = useState("");
  const [type, setType] = useState("");
  const [tag, setTag] = useState("");
  const [sort, setSort] = useState<"newest" | "downloads" | "rating">("newest");
  const [page, setPage] = useState(0);
  const size = 24;

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    publicCatalogApi.list({ q, type, tag, sort, page, size })
      .then((data: PublicCatalogPage) => {
        if (cancelled) return;
        setItems(data.content);
        setTotal(data.totalElements);
        setError(null);
      })
      .catch((e) => { if (!cancelled) setError(String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [q, type, tag, sort, page]);

  const types = ["catalog", "profile", "ssp", "ap", "ar", "poam", "component-definition"];

  return (
    <div>
      <div className="flex flex-col md:flex-row gap-3 items-start md:items-center mb-6">
        <input
          className="border rounded px-3 py-2 w-full md:w-72"
          placeholder="Search title or description"
          value={q}
          onChange={(e) => { setPage(0); setQ(e.target.value); }}
        />
        <select className="border rounded px-3 py-2" value={type}
                onChange={(e) => { setPage(0); setType(e.target.value); }}>
          <option value="">All types</option>
          {types.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
        <input
          className="border rounded px-3 py-2 w-full md:w-40"
          placeholder="Tag filter"
          value={tag}
          onChange={(e) => { setPage(0); setTag(e.target.value); }}
        />
        <select className="border rounded px-3 py-2" value={sort}
                onChange={(e) => { setPage(0); setSort(e.target.value as typeof sort); }}>
          <option value="newest">Newest</option>
          <option value="downloads">Most downloaded</option>
          <option value="rating">Top rated</option>
        </select>
      </div>

      {loading && <p className="text-sm text-slate-500">Loading…</p>}
      {error && <p className="text-sm text-red-600">Error: {error}</p>}
      {!loading && !error && items.length === 0 && (
        <p className="text-sm text-slate-500">No public items match these filters.</p>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {items.map(it => (
          <Link key={it.itemId} href={`/catalog/${it.itemId}`}
                className="block border rounded-lg p-4 hover:shadow transition">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs uppercase tracking-wide bg-slate-100 px-2 py-0.5 rounded">{it.oscalType}</span>
              {it.averageRating != null && it.totalRatings != null && it.totalRatings > 0 && (
                <span className="text-xs text-slate-600">★ {it.averageRating.toFixed(1)} ({it.totalRatings})</span>
              )}
            </div>
            <h3 className="font-semibold text-base mb-1 line-clamp-2">{it.title}</h3>
            {it.description && (
              <p className="text-sm text-slate-600 line-clamp-3 mb-2">{it.description}</p>
            )}
            <div className="flex flex-wrap gap-1 mb-2">
              {it.tags.slice(0, 4).map(t => (
                <span key={t} className="text-xs bg-blue-50 text-blue-700 px-1.5 py-0.5 rounded">{t}</span>
              ))}
            </div>
            <div className="text-xs text-slate-500">
              {it.downloadCount ?? 0} downloads · v{it.currentVersionNumber ?? "—"}
            </div>
          </Link>
        ))}
      </div>

      {total > size && (
        <div className="flex justify-center gap-2 mt-6">
          <button disabled={page === 0}
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  className="px-3 py-1 border rounded disabled:opacity-50">Previous</button>
          <span className="px-3 py-1 text-sm">Page {page + 1} / {Math.ceil(total / size)}</span>
          <button disabled={(page + 1) * size >= total}
                  onClick={() => setPage(p => p + 1)}
                  className="px-3 py-1 border rounded disabled:opacity-50">Next</button>
        </div>
      )}
    </div>
  );
}
