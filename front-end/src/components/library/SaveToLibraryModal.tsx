'use client';

import { useState } from 'react';
import type { SaveToLibraryRequest, Visibility } from '@/lib/api/library';

interface SaveToLibraryModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (req: SaveToLibraryRequest) => Promise<void> | void;
  defaultTitle: string;
  defaultDescription?: string;
  /**
   * Caller's current organization id (or `null` / `undefined` if the user
   * has no org). When absent, the Organization radio is disabled because
   * the backend requires `organizationId` for ORGANIZATION visibility.
   */
  userOrganizationId?: number | null;
}

/**
 * Modal for publishing a draft OSCAL document to the library.
 *
 * Collects title (required), description, tags, and visibility scope,
 * then calls `onSubmit` with a `SaveToLibraryRequest` matching the
 * backend `SaveToLibraryRequest` DTO. Does not own the network call —
 * the caller wires `onSubmit` to the appropriate `libraryPublishApi`
 * method (catalog, profile, component, or oscal-document).
 */
export function SaveToLibraryModal({
  open,
  onClose,
  onSubmit,
  defaultTitle,
  defaultDescription,
  userOrganizationId,
}: SaveToLibraryModalProps) {
  const [title, setTitle] = useState(defaultTitle);
  const [description, setDescription] = useState(defaultDescription ?? '');
  const [tagInput, setTagInput] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [visibility, setVisibility] = useState<Visibility>('PRIVATE');
  const [submitting, setSubmitting] = useState(false);

  if (!open) return null;

  const hasOrg = userOrganizationId != null;
  const valid =
    title.trim().length > 0 && (visibility !== 'ORGANIZATION' || hasOrg);

  const submit = async () => {
    setSubmitting(true);
    try {
      await onSubmit({
        title: title.trim(),
        description: description.trim() || undefined,
        tags: tags.length ? tags : undefined,
        visibility,
        organizationId:
          visibility === 'ORGANIZATION' ? userOrganizationId ?? undefined : undefined,
      });
      onClose();
    } finally {
      setSubmitting(false);
    }
  };

  const addTag = () => {
    const trimmed = tagInput.trim();
    if (trimmed && !tags.includes(trimmed)) {
      setTags([...tags, trimmed]);
    }
    setTagInput('');
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Save to Library"
      className="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
    >
      <div className="bg-white rounded-lg shadow-lg max-w-md w-full p-6">
        <h2 className="text-lg font-semibold mb-4">Save to Library</h2>

        <label className="block text-sm font-medium mb-1" htmlFor="stl-title">
          Title
        </label>
        <input
          id="stl-title"
          className="w-full border rounded px-2 py-1 mb-3"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />

        <label className="block text-sm font-medium mb-1" htmlFor="stl-desc">
          Description
        </label>
        <textarea
          id="stl-desc"
          className="w-full border rounded px-2 py-1 mb-3"
          rows={3}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />

        <label className="block text-sm font-medium mb-1" htmlFor="stl-tag">
          Tags
        </label>
        <div className="flex flex-wrap gap-1 mb-2">
          {tags.map((t) => (
            <span
              key={t}
              className="bg-slate-100 px-2 py-0.5 rounded text-xs inline-flex items-center"
            >
              {t}
              <button
                type="button"
                className="ml-1"
                onClick={() => setTags(tags.filter((x) => x !== t))}
                aria-label={`Remove ${t}`}
              >
                ×
              </button>
            </span>
          ))}
        </div>
        <input
          id="stl-tag"
          className="w-full border rounded px-2 py-1 mb-3"
          placeholder="Add tag and press Enter"
          value={tagInput}
          onChange={(e) => setTagInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && tagInput.trim()) {
              e.preventDefault();
              addTag();
            }
          }}
        />

        <fieldset className="mb-4">
          <legend className="block text-sm font-medium mb-1">Visibility</legend>
          <label className="block">
            <input
              type="radio"
              name="vis"
              value="PRIVATE"
              checked={visibility === 'PRIVATE'}
              onChange={() => setVisibility('PRIVATE')}
            />
            {' '}Private — only me
          </label>
          <label className="block">
            <input
              type="radio"
              name="vis"
              value="ORGANIZATION"
              checked={visibility === 'ORGANIZATION'}
              disabled={!hasOrg}
              onChange={() => setVisibility('ORGANIZATION')}
            />
            {' '}Organization — my team
          </label>
          <label className="block">
            <input
              type="radio"
              name="vis"
              value="PUBLIC"
              checked={visibility === 'PUBLIC'}
              onChange={() => setVisibility('PUBLIC')}
            />
            {' '}Public — visible at /catalog
          </label>
        </fieldset>

        <div className="flex justify-end gap-2">
          <button
            type="button"
            className="px-3 py-1 border rounded"
            onClick={onClose}
            disabled={submitting}
          >
            Cancel
          </button>
          <button
            type="button"
            className="px-3 py-1 bg-blue-600 text-white rounded disabled:opacity-50"
            disabled={!valid || submitting}
            onClick={submit}
          >
            Save to Library
          </button>
        </div>
      </div>
    </div>
  );
}
