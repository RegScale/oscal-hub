import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PackageCompletenessCard } from '../package-completeness-card';

describe('PackageCompletenessCard', () => {
  it('shows loading state', () => {
    render(<PackageCompletenessCard completeness={null} loading={true} />);
    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('renders satisfied and missing items', () => {
    render(<PackageCompletenessCard
      completeness={{ coreDocuments: [
        { documentType: 'SSP', presentCount: 1, satisfied: true },
        { documentType: 'SAR', presentCount: 0, satisfied: false },
      ]}}
      loading={false}
    />);
    expect(screen.getByText('System Security Plan')).toBeInTheDocument();
    expect(screen.getByText('Security Assessment Report')).toBeInTheDocument();
  });

  it('shows count badge for >1 documents', () => {
    render(<PackageCompletenessCard
      completeness={{ coreDocuments: [
        { documentType: 'SSP', presentCount: 3, satisfied: true },
      ]}}
      loading={false}
    />);
    expect(screen.getByText('\xd73')).toBeInTheDocument();
  });
});
