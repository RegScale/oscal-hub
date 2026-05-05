'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Download, Eye, Star, Library, Search } from 'lucide-react';
import {
  publicCatalogApi,
  type PublicItemSummary,
  type PublicCatalogPage,
} from '@/lib/api/public-catalog';

const TYPES = ['catalog', 'profile', 'ssp', 'ap', 'ar', 'poam', 'component-definition'];

export default function PublicCatalogPage() {
  const [items, setItems] = useState<PublicItemSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [q, setQ] = useState('');
  const [type, setType] = useState('');
  const [tag, setTag] = useState('');
  const [sort, setSort] = useState<'newest' | 'downloads' | 'rating'>('newest');
  const [page, setPage] = useState(0);
  const size = 24;

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    publicCatalogApi
      .list({ q, type, tag, sort, page, size })
      .then((data: PublicCatalogPage) => {
        if (cancelled) return;
        setItems(data.content);
        setTotal(data.totalElements);
        setError(null);
      })
      .catch((e) => {
        if (!cancelled) setError(String(e));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [q, type, tag, sort, page]);

  return (
    <div>
      <header className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Library className="h-6 w-6 text-primary" />
          <h1 className="text-2xl font-bold">Public Catalog</h1>
        </div>
        <p className="text-sm text-muted-foreground">
          Browse OSCAL content shared by the community. Sign in to download or rate.
        </p>
      </header>

      {/* Filters */}
      <div className="flex flex-col md:flex-row gap-3 items-start md:items-center mb-6">
        <div className="relative w-full md:w-72">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search title or description"
            value={q}
            onChange={(e) => {
              setPage(0);
              setQ(e.target.value);
            }}
            className="pl-9"
          />
        </div>
        <select
          className="bg-background border border-input rounded-md h-9 px-3 text-sm"
          value={type}
          onChange={(e) => {
            setPage(0);
            setType(e.target.value);
          }}
        >
          <option value="">All types</option>
          {TYPES.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
        <Input
          placeholder="Tag filter"
          value={tag}
          onChange={(e) => {
            setPage(0);
            setTag(e.target.value);
          }}
          className="w-full md:w-40"
        />
        <select
          className="bg-background border border-input rounded-md h-9 px-3 text-sm"
          value={sort}
          onChange={(e) => {
            setPage(0);
            setSort(e.target.value as typeof sort);
          }}
        >
          <option value="newest">Newest</option>
          <option value="downloads">Most downloaded</option>
          <option value="rating">Top rated</option>
        </select>
      </div>

      {loading && <p className="text-sm text-muted-foreground">Loading…</p>}
      {error && <p className="text-sm text-destructive">Error: {error}</p>}
      {!loading && !error && items.length === 0 && (
        <Card>
          <CardContent className="py-12 text-center text-sm text-muted-foreground">
            No public items match these filters.
          </CardContent>
        </Card>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {items.map((it) => (
          <Link key={it.itemId} href={`/catalog/${it.itemId}`} className="group">
            <Card className="h-full flex flex-col transition-all group-hover:border-primary/40 group-hover:shadow-md cursor-pointer">
              <CardHeader className="pb-3">
                <div className="flex items-start justify-between gap-2">
                  <Badge variant="outline" className="text-xs uppercase tracking-wide">
                    {it.oscalType}
                  </Badge>
                  {it.averageRating != null && it.totalRatings != null && it.totalRatings > 0 && (
                    <div className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                      <Star className="h-3 w-3 fill-yellow-500 text-yellow-500" />
                      {it.averageRating.toFixed(1)} ({it.totalRatings})
                    </div>
                  )}
                </div>
                <h3 className="font-semibold text-base leading-tight line-clamp-2 mt-3">
                  {it.title}
                </h3>
                {it.description && (
                  <p className="text-sm text-muted-foreground line-clamp-3 mt-1">
                    {it.description}
                  </p>
                )}
              </CardHeader>
              <CardContent className="flex-1 flex flex-col justify-end pt-0 pb-4 space-y-2">
                {it.tags.length > 0 && (
                  <div className="flex flex-wrap gap-1">
                    {it.tags.slice(0, 4).map((t) => (
                      <Badge key={t} variant="secondary" className="text-xs">
                        {t}
                      </Badge>
                    ))}
                    {it.tags.length > 4 && (
                      <Badge variant="outline" className="text-xs">
                        +{it.tags.length - 4}
                      </Badge>
                    )}
                  </div>
                )}
                <div className="flex items-center gap-4 text-xs text-muted-foreground pt-1">
                  <span className="inline-flex items-center gap-1">
                    <Download className="h-3 w-3" />
                    {it.downloadCount ?? 0}
                  </span>
                  <span className="inline-flex items-center gap-1">
                    <Eye className="h-3 w-3" />v{it.currentVersionNumber ?? '—'}
                  </span>
                </div>
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>

      {total > size && (
        <div className="flex justify-center items-center gap-2 mt-6">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Previous
          </Button>
          <span className="px-3 py-1 text-sm text-muted-foreground">
            Page {page + 1} of {Math.ceil(total / size)}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={(page + 1) * size >= total}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  );
}
