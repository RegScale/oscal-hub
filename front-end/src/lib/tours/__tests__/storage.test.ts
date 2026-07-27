import { beforeEach, describe, expect, it } from 'vitest';
import {
  loadTourState,
  saveTourState,
  markTourCompleted,
  markTourDismissed,
  shouldShowWelcomePrompt,
  recordWelcomeDeferral,
  dismissWelcomePrompt,
  wasDeferredThisSession,
  defaultTourState,
  MAX_WELCOME_DEFERRALS,
} from '@/lib/tours/storage';

const USER_ID = 42;
const KEY = `oscal-hub.tours.v1.${USER_ID}`;

describe('tour storage', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('returns the default state when nothing is stored', () => {
    const state = loadTourState(USER_ID);
    expect(state).toEqual({ welcomePrompt: { seen: false, deferrals: 0 }, tours: {} });
  });

  it('round-trips state through localStorage under a per-user key', () => {
    const state = defaultTourState();
    state.tours['get-started'] = { completedVersion: 1 };
    saveTourState(USER_ID, state);
    expect(localStorage.getItem(KEY)).toBeTruthy();
    expect(loadTourState(USER_ID)).toEqual(state);
    // A different user sees a clean slate.
    expect(loadTourState(7)).toEqual(defaultTourState());
  });

  it('falls back to the default state on corrupted JSON', () => {
    localStorage.setItem(KEY, '{not json');
    expect(loadTourState(USER_ID)).toEqual(defaultTourState());
  });

  it('markTourCompleted records the version and clears any dismissal', () => {
    markTourDismissed(USER_ID, 'get-started', 1, 3);
    const state = markTourCompleted(USER_ID, 'get-started', 1);
    expect(state.tours['get-started']).toEqual({ completedVersion: 1 });
    expect(loadTourState(USER_ID).tours['get-started']).toEqual({ completedVersion: 1 });
  });

  it('markTourDismissed records the step and version', () => {
    const state = markTourDismissed(USER_ID, 'get-started', 1, 2);
    expect(state.tours['get-started']).toEqual({ dismissedVersion: 1, dismissedAtStep: 2 });
  });

  describe('shouldShowWelcomePrompt', () => {
    it('is true for a brand-new user', () => {
      expect(shouldShowWelcomePrompt(defaultTourState(), 'get-started')).toBe(true);
    });

    it('is false once the prompt was seen', () => {
      const state = dismissWelcomePrompt(USER_ID);
      expect(shouldShowWelcomePrompt(state, 'get-started')).toBe(false);
    });

    it('is false after MAX_WELCOME_DEFERRALS deferrals', () => {
      let state = defaultTourState();
      for (let i = 0; i < MAX_WELCOME_DEFERRALS; i++) {
        state = recordWelcomeDeferral(USER_ID);
      }
      expect(state.welcomePrompt.deferrals).toBe(MAX_WELCOME_DEFERRALS);
      expect(shouldShowWelcomePrompt(state, 'get-started')).toBe(false);
    });

    it('is false when the get-started tour was completed or dismissed', () => {
      const completed = markTourCompleted(USER_ID, 'get-started', 1);
      expect(shouldShowWelcomePrompt(completed, 'get-started')).toBe(false);
      localStorage.clear();
      const dismissed = markTourDismissed(USER_ID, 'get-started', 1, 0);
      expect(shouldShowWelcomePrompt(dismissed, 'get-started')).toBe(false);
    });
  });

  it('recordWelcomeDeferral also flags the current session', () => {
    expect(wasDeferredThisSession()).toBe(false);
    recordWelcomeDeferral(USER_ID);
    expect(wasDeferredThisSession()).toBe(true);
  });
});
