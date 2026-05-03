import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BuiltDocList } from './BuiltDocList';

const listMock = vi.fn();

vi.mock('@/lib/api-client', () => ({
  catalogBuilderApi: {
    list: (...args: unknown[]) => listMock(...args),
    getContent: vi.fn(),
    remove: vi.fn(),
  },
  profileBuilderApi: {
    list: vi.fn(),
    getContent: vi.fn(),
    remove: vi.fn(),
  },
}));

const baseDoc = {
  id: 1,
  oscalUuid: 'u',
  title: 'Sample',
  oscalVersion: '1.1.3',
  storagePath: '',
  filename: 'f.json',
  fileSize: 100,
  createdBy: 'me',
  createdAt: '2026-05-01T00:00:00Z',
  updatedAt: '2026-05-02T00:00:00Z',
  groupCount: 1,
  controlCount: 5,
  paramCount: 2,
};

describe('BuiltDocList — draft badge', () => {
  beforeEach(() => {
    listMock.mockReset();
  });

  it('renders a Draft badge for catalogs marked as draft', async () => {
    listMock.mockResolvedValue([
      { ...baseDoc, id: 1, title: 'Final Catalog', draft: false },
      { ...baseDoc, id: 2, title: 'WIP Catalog', draft: true },
    ]);
    render(<BuiltDocList docType="catalog" onCreateNew={() => {}} />);
    await waitFor(() => {
      expect(screen.getByText('Final Catalog')).toBeInTheDocument();
      expect(screen.getByText('WIP Catalog')).toBeInTheDocument();
    });
    // Only the WIP catalog should show the Draft badge
    const badges = screen.getAllByText('Draft');
    expect(badges).toHaveLength(1);
  });

  it('omits the Draft badge when no documents are drafts', async () => {
    listMock.mockResolvedValue([{ ...baseDoc, draft: false }]);
    render(<BuiltDocList docType="catalog" onCreateNew={() => {}} />);
    await waitFor(() => {
      expect(screen.getByText('Sample')).toBeInTheDocument();
    });
    expect(screen.queryByText('Draft')).toBeNull();
  });
});
