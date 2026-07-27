import type { TourStorageState } from './types';

const KEY_PREFIX = 'oscal-hub.tours.v1.';
const SESSION_DEFER_KEY = 'oscal-hub.tours.prompt-deferred';

export const MAX_WELCOME_DEFERRALS = 3;

function storageKey(userId: number): string {
  return `${KEY_PREFIX}${userId}`;
}

export function defaultTourState(): TourStorageState {
  return { welcomePrompt: { seen: false, deferrals: 0 }, tours: {} };
}

export function loadTourState(userId: number): TourStorageState {
  if (typeof window === 'undefined') return defaultTourState();
  try {
    const raw = window.localStorage.getItem(storageKey(userId));
    if (!raw) return defaultTourState();
    const parsed = JSON.parse(raw);
    return {
      welcomePrompt: {
        seen: Boolean(parsed?.welcomePrompt?.seen),
        deferrals: Number(parsed?.welcomePrompt?.deferrals) || 0,
      },
      tours: parsed?.tours && typeof parsed.tours === 'object' ? parsed.tours : {},
    };
  } catch {
    return defaultTourState();
  }
}

export function saveTourState(userId: number, state: TourStorageState): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(storageKey(userId), JSON.stringify(state));
  } catch {
    // Quota exceeded / private mode: tour state is a nicety, never fatal.
  }
}

export function markTourCompleted(userId: number, tourId: string, version: number): TourStorageState {
  const state = loadTourState(userId);
  // Completion supersedes any earlier dismissal.
  state.tours[tourId] = { completedVersion: version };
  saveTourState(userId, state);
  return state;
}

export function markTourDismissed(
  userId: number,
  tourId: string,
  version: number,
  stepIndex: number,
): TourStorageState {
  const state = loadTourState(userId);
  const existing = state.tours[tourId] ?? {};
  state.tours[tourId] = { ...existing, dismissedVersion: version, dismissedAtStep: stepIndex };
  saveTourState(userId, state);
  return state;
}

export function shouldShowWelcomePrompt(state: TourStorageState, getStartedTourId: string): boolean {
  if (state.welcomePrompt.seen) return false;
  if (state.welcomePrompt.deferrals >= MAX_WELCOME_DEFERRALS) return false;
  const record = state.tours[getStartedTourId];
  if (record && (record.completedVersion !== undefined || record.dismissedAtStep !== undefined)) {
    return false;
  }
  return true;
}

export function recordWelcomeDeferral(userId: number): TourStorageState {
  const state = loadTourState(userId);
  state.welcomePrompt.deferrals += 1;
  saveTourState(userId, state);
  try {
    window.sessionStorage.setItem(SESSION_DEFER_KEY, '1');
  } catch {
    // Non-fatal.
  }
  return state;
}

/** "Maybe later" suppresses the prompt for the rest of the browser session. */
export function wasDeferredThisSession(): boolean {
  if (typeof window === 'undefined') return false;
  try {
    return window.sessionStorage.getItem(SESSION_DEFER_KEY) === '1';
  } catch {
    return false;
  }
}

export function dismissWelcomePrompt(userId: number): TourStorageState {
  const state = loadTourState(userId);
  state.welcomePrompt.seen = true;
  saveTourState(userId, state);
  return state;
}
