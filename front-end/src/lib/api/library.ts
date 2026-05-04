/**
 * Library publish + visibility API client.
 *
 * Wraps the backend save-to-library endpoints (one per OSCAL builder type)
 * and the visibility-change endpoint on the library item itself.
 *
 * Backend endpoints (Phase 1):
 *   POST  /api/build/catalogs/{catalogId}/save-to-library
 *   POST  /api/build/profiles/{profileId}/save-to-library
 *   POST  /api/build/components/{componentId}/save-to-library
 *   POST  /api/build/oscal-documents/{id}/save-to-library
 *   PATCH /api/library/{itemId}/visibility
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

export type Visibility = 'PRIVATE' | 'ORGANIZATION' | 'PUBLIC';

export interface SaveToLibraryRequest {
  title: string;
  description?: string;
  tags?: string[];
  visibility: Visibility;
  organizationId?: number;
}

export interface VisibilityChangeRequest {
  visibility: Visibility;
  organizationId?: number;
  reason?: string;
}

/**
 * Response from save-to-library + visibility-change endpoints.
 *
 * Mirrors backend `LibraryItemResponse`. Kept local to this module so the
 * library-publish surface is self-contained; the broader `LibraryItem`
 * type in `@/types/oscal` carries additional UI-only convenience fields
 * (e.g. `name` alias) that the new endpoints don't populate.
 */
export interface LibraryItemResponse {
  itemId: string;
  title: string;
  description?: string;
  oscalType: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  tags: string[];
  currentVersion?: {
    versionId: string;
    versionNumber: number;
    fileName: string;
    format: string;
    fileSize: number;
    uploadedBy: string;
    uploadedAt: string;
    changeDescription?: string;
  };
  downloadCount: number;
  viewCount: number;
  versionCount: number;
  averageRating: number | null;
  totalRatings: number | null;
  commentCount: number | null;
}

function buildAuthHeaders(): Record<string, string> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

async function fetchJson<T>(url: string, init: RequestInit, timeoutMs = 15000): Promise<T> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, { ...init, signal: controller.signal });
    if (response.status === 401 && typeof window !== 'undefined') {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
    if (!response.ok) {
      throw new Error(`Request failed (${response.status}): ${response.statusText}`);
    }
    if (response.status === 204) return undefined as unknown as T;
    return (await response.json()) as T;
  } finally {
    clearTimeout(timer);
  }
}

export const libraryPublishApi = {
  /** Save a draft catalog (build/catalogs/{id}) to the library. */
  async saveCatalogToLibrary(
    catalogId: number,
    request: SaveToLibraryRequest,
  ): Promise<LibraryItemResponse> {
    return fetchJson<LibraryItemResponse>(
      `${API_BASE_URL}/build/catalogs/${catalogId}/save-to-library`,
      {
        method: 'POST',
        headers: buildAuthHeaders(),
        body: JSON.stringify(request),
      },
    );
  },

  /** Save a draft profile (build/profiles/{id}) to the library. */
  async saveProfileToLibrary(
    profileId: number,
    request: SaveToLibraryRequest,
  ): Promise<LibraryItemResponse> {
    return fetchJson<LibraryItemResponse>(
      `${API_BASE_URL}/build/profiles/${profileId}/save-to-library`,
      {
        method: 'POST',
        headers: buildAuthHeaders(),
        body: JSON.stringify(request),
      },
    );
  },

  /**
   * Save a draft component definition to the library.
   *
   * Backend path is `/build/components/{id}/save-to-library` (NOT
   * `component-definitions`) — matches `ComponentDefinitionController`.
   */
  async saveComponentToLibrary(
    componentId: number,
    request: SaveToLibraryRequest,
  ): Promise<LibraryItemResponse> {
    return fetchJson<LibraryItemResponse>(
      `${API_BASE_URL}/build/components/${componentId}/save-to-library`,
      {
        method: 'POST',
        headers: buildAuthHeaders(),
        body: JSON.stringify(request),
      },
    );
  },

  /**
   * Save a generic OSCAL document (SSP / AP / AR / POA&M) to the library.
   *
   * `id` is the database row id from `/build/oscal-documents`, not the
   * OSCAL document UUID.
   */
  async saveOscalDocumentToLibrary(
    id: number,
    request: SaveToLibraryRequest,
  ): Promise<LibraryItemResponse> {
    return fetchJson<LibraryItemResponse>(
      `${API_BASE_URL}/build/oscal-documents/${id}/save-to-library`,
      {
        method: 'POST',
        headers: buildAuthHeaders(),
        body: JSON.stringify(request),
      },
    );
  },

  /** Change the visibility of an existing library item. */
  async changeVisibility(
    itemId: string,
    request: VisibilityChangeRequest,
  ): Promise<LibraryItemResponse> {
    return fetchJson<LibraryItemResponse>(
      `${API_BASE_URL}/library/${itemId}/visibility`,
      {
        method: 'PATCH',
        headers: buildAuthHeaders(),
        body: JSON.stringify(request),
      },
    );
  },
};
