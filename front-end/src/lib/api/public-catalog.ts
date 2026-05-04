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
}

export interface PublicCatalogPage {
  content: PublicItemSummary[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
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

  contentUrl: (itemId: string): string =>
    `${PUBLIC_BASE}/items/${encodeURIComponent(itemId)}/content`,

  versionContentUrl: (itemId: string, versionId: string): string =>
    `${PUBLIC_BASE}/items/${encodeURIComponent(itemId)}/versions/${encodeURIComponent(versionId)}/content`,
};
