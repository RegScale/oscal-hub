'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  BarChart, Bar, CartesianGrid, Cell, Legend,
  LineChart, Line, PieChart, Pie,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Award, BarChart3, Building2, Download, Eye, Library, Search, Star,
  TrendingUp, Users,
} from 'lucide-react';
import { HelpButton } from '@/components/HelpButton';
import {
  publicCatalogApi,
  type PublicCatalogAnalytics,
  type PublicCatalogPage,
  type PublicCatalogTopContributors,
  type PublicItemSummary,
} from '@/lib/api/public-catalog';

const TYPES = ['catalog', 'profile', 'ssp', 'ap', 'ar', 'poam', 'component-definition'];

// Shared palette across the analytics tab so a given OSCAL type keeps the
// same color across the bar/pie charts.
const CHART_COLORS = [
  '#3B82F6', '#10B981', '#F59E0B', '#EF4444',
  '#8B5CF6', '#EC4899', '#06B6D4', '#84CC16',
];

export default function PublicCatalogPage() {
  const [tab, setTab] = useState('browse');

  return (
    <div className="container mx-auto px-4 py-8">
      <header className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Library className="h-6 w-6 text-primary" />
          <h1 className="text-2xl font-bold">OSCAL Data Products</h1>
          <HelpButton slug="public-catalog" />
        </div>
        <p className="text-sm text-muted-foreground">
          Browse OSCAL content shared by the community. Login to download or rate.
        </p>
      </header>

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList className="flex-wrap h-auto gap-1">
          <TabsTrigger value="browse" className="gap-2">
            <Search className="h-4 w-4" /> Browse
          </TabsTrigger>
          <TabsTrigger value="rated" className="gap-2">
            <Star className="h-4 w-4" /> Highest Rated
          </TabsTrigger>
          <TabsTrigger value="downloaded" className="gap-2">
            <Download className="h-4 w-4" /> Most Downloaded
          </TabsTrigger>
          <TabsTrigger value="contributors" className="gap-2">
            <Award className="h-4 w-4" /> Top Contributors
          </TabsTrigger>
          <TabsTrigger value="analytics" className="gap-2">
            <BarChart3 className="h-4 w-4" /> Analytics
          </TabsTrigger>
        </TabsList>

        <TabsContent value="browse"><BrowseTab /></TabsContent>
        <TabsContent value="rated"><RankedTab kind="rated" /></TabsContent>
        <TabsContent value="downloaded"><RankedTab kind="downloaded" /></TabsContent>
        <TabsContent value="contributors"><TopContributorsTab /></TabsContent>
        <TabsContent value="analytics"><AnalyticsTab /></TabsContent>
      </Tabs>
    </div>
  );
}

// ----------------------------------------------------------------------------
// Browse tab — search + filters + paginated grid (the original /catalog UI).
// ----------------------------------------------------------------------------

function BrowseTab() {
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
    <div className="pt-4">
      <div className="flex flex-col md:flex-row gap-3 items-start md:items-center mb-6">
        <div className="relative w-full md:w-72">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search title or description"
            value={q}
            onChange={(e) => { setPage(0); setQ(e.target.value); }}
            className="pl-9"
          />
        </div>
        <select
          className="bg-background border border-input rounded-md h-9 px-3 text-sm"
          value={type}
          onChange={(e) => { setPage(0); setType(e.target.value); }}
        >
          <option value="">All types</option>
          {TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
        <Input
          placeholder="Tag filter"
          value={tag}
          onChange={(e) => { setPage(0); setTag(e.target.value); }}
          className="w-full md:w-40"
        />
        <select
          className="bg-background border border-input rounded-md h-9 px-3 text-sm"
          value={sort}
          onChange={(e) => { setPage(0); setSort(e.target.value as typeof sort); }}
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

      <ItemGrid items={items} />

      {total > size && (
        <div className="flex justify-center items-center gap-2 mt-6">
          <Button variant="outline" size="sm" disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}>
            Previous
          </Button>
          <span className="px-3 py-1 text-sm text-muted-foreground">
            Page {page + 1} of {Math.ceil(total / size)}
          </span>
          <Button variant="outline" size="sm"
                  disabled={(page + 1) * size >= total}
                  onClick={() => setPage((p) => p + 1)}>
            Next
          </Button>
        </div>
      )}
    </div>
  );
}

// ----------------------------------------------------------------------------
// Highest Rated / Most Downloaded — share rendering, differ only in source.
// ----------------------------------------------------------------------------

function RankedTab({ kind }: { kind: 'rated' | 'downloaded' }) {
  const [items, setItems] = useState<PublicItemSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    const fetcher = kind === 'rated'
      ? publicCatalogApi.topRated(20)
      : publicCatalogApi.mostDownloaded(20);
    fetcher
      .then((data) => { if (!cancelled) { setItems(data); setError(null); } })
      .catch((e) => { if (!cancelled) setError(String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [kind]);

  const empty = kind === 'rated'
    ? 'No rated items yet — be the first to leave a star.'
    : 'No downloads yet — once items get traction they\'ll appear here.';

  return (
    <div className="pt-4">
      <p className="text-sm text-muted-foreground mb-4">
        {kind === 'rated'
          ? 'Top 20 PUBLIC items by average rating, ranked. Items with at least one rating only.'
          : 'Top 20 PUBLIC items by download count, ranked.'}
      </p>
      {loading && <p className="text-sm text-muted-foreground">Loading…</p>}
      {error && <p className="text-sm text-destructive">Error: {error}</p>}
      {!loading && !error && items.length === 0 && (
        <Card>
          <CardContent className="py-12 text-center text-sm text-muted-foreground">
            {empty}
          </CardContent>
        </Card>
      )}
      <ItemList items={items} primaryMetric={kind === 'rated' ? 'rating' : 'downloads'} />
    </div>
  );
}

// ----------------------------------------------------------------------------
// Top Contributors — two leaderboards side by side.
// ----------------------------------------------------------------------------

function TopContributorsTab() {
  const [data, setData] = useState<PublicCatalogTopContributors | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    publicCatalogApi.topContributors(15)
      .then((d) => { if (!cancelled) { setData(d); setError(null); } })
      .catch((e) => { if (!cancelled) setError(String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  if (loading) return <p className="text-sm text-muted-foreground pt-4">Loading…</p>;
  if (error) return <p className="text-sm text-destructive pt-4">Error: {error}</p>;
  if (!data) return null;

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4">
      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center gap-2">
            <Users className="h-5 w-5 text-primary" />
            <h2 className="font-semibold">Top Users</h2>
          </div>
          <p className="text-xs text-muted-foreground">
            Users with the most PUBLIC items. Ties broken by total downloads.
          </p>
        </CardHeader>
        <CardContent>
          {data.users.length === 0 ? (
            <p className="text-sm text-muted-foreground">No public contributors yet.</p>
          ) : (
            <ol className="space-y-2">
              {data.users.map((u, idx) => (
                <li key={u.userId ?? u.username}
                    className="flex items-center gap-3 rounded-md border border-border/60 px-3 py-2">
                  <span className="w-6 text-sm font-mono text-muted-foreground">{idx + 1}.</span>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-medium truncate">{u.displayName}</div>
                    <div className="text-xs text-muted-foreground truncate">@{u.username}</div>
                  </div>
                  <div className="text-right text-xs">
                    <div className="font-semibold">{u.uploadCount}</div>
                    <div className="text-muted-foreground">items</div>
                  </div>
                  <div className="text-right text-xs">
                    <div className="font-semibold">{u.totalDownloads.toLocaleString()}</div>
                    <div className="text-muted-foreground">downloads</div>
                  </div>
                </li>
              ))}
            </ol>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center gap-2">
            <Building2 className="h-5 w-5 text-primary" />
            <h2 className="font-semibold">Top Organizations</h2>
          </div>
          <p className="text-xs text-muted-foreground">
            Organizations whose members have published the most PUBLIC items.
          </p>
        </CardHeader>
        <CardContent>
          {data.organizations.length === 0 ? (
            <p className="text-sm text-muted-foreground">No organizations have published yet.</p>
          ) : (
            <ol className="space-y-2">
              {data.organizations.map((o, idx) => (
                <li key={o.organizationId ?? o.name}
                    className="flex items-center gap-3 rounded-md border border-border/60 px-3 py-2">
                  <span className="w-6 text-sm font-mono text-muted-foreground">{idx + 1}.</span>
                  {o.logoUrl ? (
                    <img src={o.logoUrl} alt={o.name}
                         className="h-7 w-7 rounded object-contain border border-border/60 bg-muted" />
                  ) : (
                    <div className="h-7 w-7 rounded bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center">
                      <span className="text-[10px] font-bold text-white">
                        {o.name.charAt(0).toUpperCase()}
                      </span>
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-medium truncate">{o.name}</div>
                  </div>
                  <div className="text-right text-xs">
                    <div className="font-semibold">{o.uploadCount}</div>
                    <div className="text-muted-foreground">items</div>
                  </div>
                  <div className="text-right text-xs">
                    <div className="font-semibold">{o.totalDownloads.toLocaleString()}</div>
                    <div className="text-muted-foreground">downloads</div>
                  </div>
                </li>
              ))}
            </ol>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

// ----------------------------------------------------------------------------
// Analytics — totals + four charts.
// ----------------------------------------------------------------------------

function AnalyticsTab() {
  const [data, setData] = useState<PublicCatalogAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    publicCatalogApi.analytics(26) // last 26 weeks (~6 months)
      .then((d) => { if (!cancelled) { setData(d); setError(null); } })
      .catch((e) => { if (!cancelled) setError(String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  if (loading) return <p className="text-sm text-muted-foreground pt-4">Loading…</p>;
  if (error) return <p className="text-sm text-destructive pt-4">Error: {error}</p>;
  if (!data) return null;

  const byTypeForBar = data.byType.map((t) => ({
    name: t.oscalType,
    items: t.itemCount,
    avgDownloads: Number(t.avgDownloads.toFixed(1)),
    avgRating: Number(t.avgRating.toFixed(2)),
  }));

  const formatWeek = (iso: string) => iso.slice(5); // "MM-DD"

  return (
    <div className="space-y-6 pt-4">
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <StatTile label="Public items" value={data.totals.totalItems} icon={<Library className="h-4 w-4" />} />
        <StatTile label="Total downloads" value={data.totals.totalDownloads} icon={<Download className="h-4 w-4" />} />
        <StatTile label="Contributors" value={data.totals.contributorCount} icon={<Users className="h-4 w-4" />} />
        <StatTile label="Organizations" value={data.totals.organizationCount} icon={<Building2 className="h-4 w-4" />} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader className="pb-2">
            <h3 className="font-semibold flex items-center gap-2">
              <Library className="h-4 w-4 text-primary" /> Items by content type
            </h3>
          </CardHeader>
          <CardContent>
            {data.byType.length === 0 ? (
              <EmptyChart>No public items yet.</EmptyChart>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie
                    data={data.byType}
                    dataKey="itemCount"
                    nameKey="oscalType"
                    cx="50%" cy="50%"
                    outerRadius={90}
                    label={(e: any) => `${e.oscalType}: ${e.itemCount}`}
                  >
                    {data.byType.map((_, i) => (
                      <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <h3 className="font-semibold flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-primary" /> Downloads over time
            </h3>
            <p className="text-xs text-muted-foreground">
              Weekly download events from the audit log. Tracking starts when audit
              logging was enabled — earlier weeks will read as zero.
            </p>
          </CardHeader>
          <CardContent>
            {data.downloadsOverTime.length === 0 ? (
              <EmptyChart>No download events recorded yet.</EmptyChart>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={data.downloadsOverTime.map((b) => ({
                  ...b, week: formatWeek(b.weekStart),
                }))}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="week" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Line type="monotone" dataKey="count" stroke={CHART_COLORS[0]} strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <h3 className="font-semibold flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-primary" /> Uploads over time
            </h3>
            <p className="text-xs text-muted-foreground">
              New PUBLIC items per week (last 26 weeks).
            </p>
          </CardHeader>
          <CardContent>
            {data.uploadsOverTime.length === 0 ? (
              <EmptyChart>No public uploads in the last 26 weeks.</EmptyChart>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={data.uploadsOverTime.map((b) => ({
                  ...b, week: formatWeek(b.weekStart),
                }))}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="week" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Line type="monotone" dataKey="count" stroke={CHART_COLORS[1]} strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <h3 className="font-semibold flex items-center gap-2">
              <Star className="h-4 w-4 text-primary" /> Avg rating &amp; downloads by type
            </h3>
          </CardHeader>
          <CardContent>
            {byTypeForBar.length === 0 ? (
              <EmptyChart>No public items yet.</EmptyChart>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={byTypeForBar}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis yAxisId="left" allowDecimals={false} />
                  <YAxis yAxisId="right" orientation="right" domain={[0, 5]} />
                  <Tooltip />
                  <Legend />
                  <Bar yAxisId="left" dataKey="avgDownloads" name="Avg downloads" fill={CHART_COLORS[2]} />
                  <Bar yAxisId="right" dataKey="avgRating" name="Avg rating (0–5)" fill={CHART_COLORS[4]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

// ----------------------------------------------------------------------------
// Shared bits
// ----------------------------------------------------------------------------

function ItemList({
  items,
  primaryMetric,
}: {
  items: PublicItemSummary[];
  /** Which metric is doing the sorting — gets emphasized at the right edge. */
  primaryMetric: 'rating' | 'downloads';
}) {
  if (items.length === 0) return null;
  return (
    <div className="rounded-md border border-border/60 divide-y divide-border/60 overflow-hidden">
      {items.map((it, idx) => {
        const rating = it.averageRating ?? 0;
        const ratingCount = it.totalRatings ?? 0;
        const downloads = it.downloadCount ?? 0;
        return (
          <Link
            key={it.itemId}
            href={`/catalog/${it.itemId}`}
            className="group flex items-center gap-4 px-4 py-3 transition-colors hover:bg-accent/50"
          >
            <span className="w-8 shrink-0 text-right text-sm font-mono text-muted-foreground">
              {idx + 1}.
            </span>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-medium text-sm group-hover:text-primary transition-colors line-clamp-1">
                  {it.title}
                </span>
                <Badge variant="outline" className="text-[10px] uppercase tracking-wide">
                  {it.oscalType}
                </Badge>
                {it.tags.slice(0, 2).map((t) => (
                  <Badge key={t} variant="secondary" className="text-[10px]">{t}</Badge>
                ))}
                {it.tags.length > 2 && (
                  <Badge variant="outline" className="text-[10px]">+{it.tags.length - 2}</Badge>
                )}
              </div>
              {it.description && (
                <p className="mt-0.5 text-xs text-muted-foreground line-clamp-1">
                  {it.description}
                </p>
              )}
            </div>
            {primaryMetric === 'rating' ? (
              <div className="hidden sm:flex flex-col items-end text-xs text-muted-foreground shrink-0 w-20">
                <span className="inline-flex items-center gap-1">
                  <Download className="h-3 w-3" />
                  {downloads.toLocaleString()}
                </span>
                <span>v{it.currentVersionNumber ?? '—'}</span>
              </div>
            ) : (
              <div className="hidden sm:flex flex-col items-end text-xs text-muted-foreground shrink-0 w-20">
                {ratingCount > 0 ? (
                  <span className="inline-flex items-center gap-1">
                    <Star className="h-3 w-3 fill-yellow-500 text-yellow-500" />
                    {rating.toFixed(1)} ({ratingCount})
                  </span>
                ) : (
                  <span className="text-muted-foreground/60">unrated</span>
                )}
                <span>v{it.currentVersionNumber ?? '—'}</span>
              </div>
            )}
            <div className="flex flex-col items-end shrink-0 w-24">
              {primaryMetric === 'rating' ? (
                <>
                  <div className="flex items-center gap-1 text-base font-semibold">
                    <Star className="h-4 w-4 fill-yellow-500 text-yellow-500" />
                    {rating > 0 ? rating.toFixed(2) : '—'}
                  </div>
                  <span className="text-[11px] text-muted-foreground">
                    {ratingCount} rating{ratingCount === 1 ? '' : 's'}
                  </span>
                </>
              ) : (
                <>
                  <div className="flex items-center gap-1 text-base font-semibold">
                    <Download className="h-4 w-4 text-primary" />
                    {downloads.toLocaleString()}
                  </div>
                  <span className="text-[11px] text-muted-foreground">downloads</span>
                </>
              )}
            </div>
          </Link>
        );
      })}
    </div>
  );
}

function ItemGrid({ items }: { items: PublicItemSummary[] }) {
  return (
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
                    <Badge key={t} variant="secondary" className="text-xs">{t}</Badge>
                  ))}
                  {it.tags.length > 4 && (
                    <Badge variant="outline" className="text-xs">+{it.tags.length - 4}</Badge>
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
  );
}

function StatTile({ label, value, icon }: { label: string; value: number; icon: React.ReactNode }) {
  return (
    <Card>
      <CardContent className="py-4">
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          {icon}
          <span>{label}</span>
        </div>
        <div className="text-2xl font-semibold mt-1">{value.toLocaleString()}</div>
      </CardContent>
    </Card>
  );
}

function EmptyChart({ children }: { children: React.ReactNode }) {
  return (
    <div className="h-[260px] flex items-center justify-center text-sm text-muted-foreground">
      {children}
    </div>
  );
}
