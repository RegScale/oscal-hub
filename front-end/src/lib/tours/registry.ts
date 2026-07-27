import type { TourDefinition } from './types';

export const GET_STARTED_TOUR_ID = 'get-started';

/** All registered tours. Populated as tour definitions land. */
export const TOURS: TourDefinition[] = [];

export function getTour(id: string): TourDefinition | undefined {
  return TOURS.find((tour) => tour.id === id);
}
