const PUBLIC_BASE = "/api/public/catalog";

export interface PublicItemSummary {
  itemId: string;
  title: string;
  description: string | null;
  oscalType: string;
  tags: string[];
  currentVersionNumber: number | null;
  publishedAt: string | null;
  lastPublishedAt: string | null;
  downloadCount: number | null;
  averageRating: number | null;
  totalRatings: number | null;
  // Publishing-org attribution. Null for legacy items with no org membership.
  organizationName: string | null;
  organizationLogoUrl: string | null;
}

export interface PublicCatalogPage {
  content: PublicItemSummary[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface UserContributor {
  userId: number | null;
  username: string;
  displayName: string;
  uploadCount: number;
  totalDownloads: number;
}

export interface OrgContributor {
  organizationId: number | null;
  name: string;
  logoUrl: string | null;
  uploadCount: number;
  totalDownloads: number;
}

export interface PublicCatalogTopContributors {
  users: UserContributor[];
  organizations: OrgContributor[];
}

export interface TypeStat {
  oscalType: string;
  itemCount: number;
  avgDownloads: number;
  avgRating: number;
}

export interface TimeBucket {
  /** ISO date string for the start of the week (Monday). */
  weekStart: string;
  count: number;
}

export interface PublicCatalogAnalytics {
  totals: {
    totalItems: number;
    totalDownloads: number;
    contributorCount: number;
    organizationCount: number;
  };
  byType: TypeStat[];
  uploadsOverTime: TimeBucket[];
  downloadsOverTime: TimeBucket[];
}

async function fetchPublicJson(url: string): Promise<any> {
  const r = await fetch(url, { headers: { Accept: "application/json" } });
  if (!r.ok) {
    throw new Error(`public catalog request failed: ${r.status}`);
  }
  return r.json();
}

export const publicCatalogApi = {
  list: (params: {
    q?: string;
    type?: string;
    tag?: string;
    sort?: "newest" | "downloads" | "rating";
    page?: number;
    size?: number;
  } = {}): Promise<PublicCatalogPage> => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== "" && v !== null) qs.set(k, String(v));
    });
    const path = qs.toString() ? `${PUBLIC_BASE}/items?${qs}` : `${PUBLIC_BASE}/items`;
    return fetchPublicJson(path);
  },
  get: (itemId: string): Promise<PublicItemSummary> =>
    fetchPublicJson(`${PUBLIC_BASE}/items/${encodeURIComponent(itemId)}`),

  /**
   * Download the latest version of a public item. Requires a JWT — anonymous
   * downloads are gated server-side. Triggers a browser download via Blob URL
   * so the Authorization header rides along (a plain <a href> can't carry it).
   */
  download: async (itemId: string, suggestedFilename?: string): Promise<void> => {
    const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;
    if (!token) throw new Error("not authenticated");
    const r = await fetch(`${PUBLIC_BASE}/items/${encodeURIComponent(itemId)}/content`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!r.ok) throw new Error(`download failed: ${r.status}`);
    const blob = await r.blob();
    const filename = parseContentDispositionFilename(r.headers.get("Content-Disposition"))
      ?? suggestedFilename ?? `${itemId}.json`;
    triggerBlobDownload(blob, filename);
  },

  /** Top-N PUBLIC items by download count. */
  mostDownloaded: (limit = 10): Promise<PublicItemSummary[]> =>
    fetchPublicJson(`${PUBLIC_BASE}/most-downloaded?limit=${limit}`),

  /** Top-N PUBLIC items by average rating (with a minimum-rating-count floor). */
  topRated: (limit = 10, minRatings = 1): Promise<PublicItemSummary[]> =>
    fetchPublicJson(`${PUBLIC_BASE}/top-rated?limit=${limit}&minRatings=${minRatings}`),

  topContributors: (limit = 10): Promise<PublicCatalogTopContributors> =>
    fetchPublicJson(`${PUBLIC_BASE}/top-contributors?limit=${limit}`),

  analytics: (weeksBack = 26): Promise<PublicCatalogAnalytics> =>
    fetchPublicJson(`${PUBLIC_BASE}/analytics?weeks=${weeksBack}`),

  /** Same as download() but for a specific version. */
  downloadVersion: async (itemId: string, versionId: string, suggestedFilename?: string): Promise<void> => {
    const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;
    if (!token) throw new Error("not authenticated");
    const r = await fetch(
      `${PUBLIC_BASE}/items/${encodeURIComponent(itemId)}/versions/${encodeURIComponent(versionId)}/content`,
      { headers: { Authorization: `Bearer ${token}` } });
    if (!r.ok) throw new Error(`download failed: ${r.status}`);
    const blob = await r.blob();
    const filename = parseContentDispositionFilename(r.headers.get("Content-Disposition"))
      ?? suggestedFilename ?? `${itemId}-${versionId}.json`;
    triggerBlobDownload(blob, filename);
  },
};

function triggerBlobDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function parseContentDispositionFilename(header: string | null): string | undefined {
  if (!header) return undefined;
  const match = /filename\*?=(?:UTF-8'')?["']?([^"';]+)["']?/i.exec(header);
  return match?.[1];
}
