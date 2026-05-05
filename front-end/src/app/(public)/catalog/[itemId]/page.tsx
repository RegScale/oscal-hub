'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Download, Star, Loader2 } from 'lucide-react';
import { HelpButton } from '@/components/HelpButton';
import { publicCatalogApi, type PublicItemSummary } from '@/lib/api/public-catalog';

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
    if (typeof window !== 'undefined') {
      setHasToken(!!localStorage.getItem('token'));
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    publicCatalogApi
      .get(itemId)
      .then((data) => {
        if (!cancelled) setItem(data);
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
  }, [itemId]);

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="h-4 w-4 animate-spin" />
          Loading…
        </div>
      </div>
    );
  }

  if (error || !item) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="space-y-3">
          <p className="text-sm text-destructive">Item not found.</p>
          <Link
            href="/catalog"
            className="inline-flex items-center gap-1 text-sm text-primary hover:underline"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Data Products
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-3xl">
      <Link
        href="/catalog"
        className="inline-flex items-center gap-1 text-sm text-primary hover:underline mb-4"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to Data Products
      </Link>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-2 mb-3">
            <Badge variant="outline" className="text-xs uppercase tracking-wide">
              {item.oscalType}
            </Badge>
            <Badge variant="secondary" className="text-xs">
              v{item.currentVersionNumber ?? '—'}
            </Badge>
            {item.totalRatings != null && item.totalRatings > 0 && (
              <Badge variant="secondary" className="text-xs">
                <Star className="h-3 w-3 fill-yellow-500 text-yellow-500 mr-1" />
                {item.averageRating?.toFixed(1)} ({item.totalRatings})
              </Badge>
            )}
          </div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold leading-tight">{item.title}</h1>
            <HelpButton slug="public-catalog" />
          </div>
          {item.description && (
            <p className="text-sm text-muted-foreground whitespace-pre-line mt-2">
              {item.description}
            </p>
          )}
        </CardHeader>

        <CardContent className="space-y-6">
          {item.tags.length > 0 && (
            <div className="flex flex-wrap gap-1">
              {item.tags.map((t) => (
                <Badge key={t} variant="secondary" className="text-xs">
                  {t}
                </Badge>
              ))}
            </div>
          )}

          <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-2 text-sm">
            <dt className="text-muted-foreground">Last published</dt>
            <dd>
              {item.lastPublishedAt
                ? new Date(item.lastPublishedAt).toLocaleString()
                : '—'}
            </dd>
            <dt className="text-muted-foreground">Downloads</dt>
            <dd>{item.downloadCount ?? 0}</dd>
            {item.totalRatings != null && item.totalRatings > 0 && (
              <>
                <dt className="text-muted-foreground">Average rating</dt>
                <dd>
                  ★ {item.averageRating?.toFixed(1)} ({item.totalRatings} ratings)
                </dd>
              </>
            )}
          </dl>

          <div className="flex flex-wrap gap-2 pt-2">
            {hasToken ? (
              <Button
                disabled={downloading}
                onClick={async () => {
                  setDownloading(true);
                  setDownloadError(null);
                  try {
                    await publicCatalogApi.download(
                      item.itemId,
                      `${item.title.replace(/[^a-zA-Z0-9._-]/g, '_')}.${item.oscalType}.json`,
                    );
                  } catch (e) {
                    setDownloadError(String(e));
                  } finally {
                    setDownloading(false);
                  }
                }}
              >
                {downloading ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Downloading…
                  </>
                ) : (
                  <>
                    <Download className="h-4 w-4 mr-2" />
                    Download
                  </>
                )}
              </Button>
            ) : (
              <Button asChild>
                <Link href="/login">Login to download</Link>
              </Button>
            )}
            {hasToken ? (
              <Button variant="outline" asChild>
                <Link href={`/library/${item.itemId}`}>Rate / comment</Link>
              </Button>
            ) : (
              <Button variant="outline" asChild>
                <Link href="/login">Login to rate</Link>
              </Button>
            )}
          </div>

          {downloadError && (
            <p className="text-sm text-destructive">Download failed: {downloadError}</p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
