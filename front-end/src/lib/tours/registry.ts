import type { TourDefinition } from './types';
import { getStartedTour } from './get-started';

export const GET_STARTED_TOUR_ID = 'get-started';

/** All registered tours, in launcher display order. */
export const TOURS: TourDefinition[] = [getStartedTour];

export function getTour(id: string): TourDefinition | undefined {
  return TOURS.find((tour) => tour.id === id);
}
