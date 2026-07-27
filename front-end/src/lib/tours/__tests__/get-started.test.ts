import { describe, expect, it } from 'vitest';
import { getStartedTour } from '@/lib/tours/get-started';
import { TOURS, getTour, GET_STARTED_TOUR_ID } from '@/lib/tours/registry';
import type { User } from '@/types/auth';

const regularUser: User = {
  userId: 1,
  username: 'u',
  email: 'u@example.com',
  organizationId: 1,
};

describe('get-started tour', () => {
  it('is registered under the expected id', () => {
    expect(getStartedTour.id).toBe(GET_STARTED_TOUR_ID);
    expect(TOURS).toContain(getStartedTour);
    expect(getTour(GET_STARTED_TOUR_ID)).toBe(getStartedTour);
  });

  it('has unique step ids and starts/ends with modal steps', () => {
    const ids = getStartedTour.steps.map((s) => s.id);
    expect(new Set(ids).size).toBe(ids.length);
    expect(getStartedTour.steps[0].target).toBeUndefined();
    expect(getStartedTour.steps[getStartedTour.steps.length - 1].target).toBeUndefined();
  });

  it('is eligible only for regular users with an organization', () => {
    expect(getStartedTour.eligible(regularUser)).toBe(true);
    expect(getStartedTour.eligible(null)).toBe(false);
    expect(getStartedTour.eligible({ ...regularUser, globalRole: 'SUPER_ADMIN' })).toBe(false);
    expect(getStartedTour.eligible({ ...regularUser, organizationId: undefined })).toBe(false);
  });

  it('requires the sm viewport because it anchors to sm:-hidden nav links', () => {
    expect(getStartedTour.minViewportWidth).toBe(640);
  });
});
