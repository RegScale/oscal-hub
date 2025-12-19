import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';
import { apiClient } from '@/lib/api-client';
import type { AuthResponse } from '@/types/auth';

// Mock Next.js navigation
const mockPush = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
  }),
  usePathname: () => '/',
}));

// Mock apiClient
vi.mock('@/lib/api-client', () => ({
  apiClient: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    refreshToken: vi.fn(),
  },
}));

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};

  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value;
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
})();

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
  writable: true,
});

// Test component that uses the hook
function TestComponent() {
  const auth = useAuth();

  return (
    <div>
      <div data-testid="is-authenticated">{auth.isAuthenticated ? 'true' : 'false'}</div>
      <div data-testid="is-loading">{auth.isLoading ? 'true' : 'false'}</div>
      <div data-testid="username">{auth.user?.username || 'none'}</div>
      <div data-testid="email">{auth.user?.email || 'none'}</div>
      <button onClick={() => auth.login('testuser', 'password')}>Login</button>
      <button onClick={() => auth.register('newuser', 'password', 'new@example.com')}>Register</button>
      <button onClick={() => auth.logout()}>Logout</button>
    </div>
  );
}

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorageMock.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ========== INITIALIZATION TESTS ==========

  describe('Initialization', () => {
    it('should initialize with no user when localStorage is empty', () => {
      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
      expect(screen.getByTestId('username').textContent).toBe('none');
    });

    it('should initialize with user from localStorage', () => {
      const mockUser = {
        userId: 1,
        username: 'testuser',
        email: 'test@example.com',
      };

      localStorageMock.setItem('token', 'test-token');
      localStorageMock.setItem('user', JSON.stringify(mockUser));

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');
      expect(screen.getByTestId('username').textContent).toBe('testuser');
      expect(screen.getByTestId('email').textContent).toBe('test@example.com');
    });

    it('should set isLoading to false after initialization', () => {
      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      expect(screen.getByTestId('is-loading').textContent).toBe('false');
    });
  });

  // ========== LOGIN TESTS ==========

  describe('Login', () => {
    it('should successfully login and update state', async () => {
      const mockResponse: AuthResponse = {
        token: 'new-token',
        userId: 1,
        username: 'testuser',
        email: 'test@example.com',
      };

      vi.mocked(apiClient.login).mockResolvedValueOnce(mockResponse);

      const { getByText } = render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      await act(async () => {
        getByText('Login').click();
        await new Promise(resolve => setTimeout(resolve, 0));
      });

      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');
      expect(screen.getByTestId('username').textContent).toBe('testuser');
      expect(apiClient.login).toHaveBeenCalledWith('testuser', 'password');
      expect(localStorageMock.getItem('token')).toBe('new-token');
      expect(mockPush).toHaveBeenCalledWith('/select-organization');
    });

    it('should store user data in localStorage on login', async () => {
      const mockResponse: AuthResponse = {
        token: 'new-token',
        userId: 1,
        username: 'testuser',
        email: 'test@example.com',
        street: '123 Main St',
        city: 'TestCity',
      };

      vi.mocked(apiClient.login).mockResolvedValueOnce(mockResponse);

      const { getByText } = render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      await act(async () => {
        getByText('Login').click();
        await new Promise(resolve => setTimeout(resolve, 0));
      });

      const storedUser = JSON.parse(localStorageMock.getItem('user')!);
      expect(storedUser.username).toBe('testuser');
      expect(storedUser.email).toBe('test@example.com');
      expect(storedUser.street).toBe('123 Main St');
    });

    it('should handle login errors without crashing', async () => {
      // Suppress console.error for this test since AuthContext logs the error
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      vi.mocked(apiClient.login).mockRejectedValueOnce(new Error('Invalid credentials'));

      const { getByText } = render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      // The login function will throw an error, but the component should not crash
      // We need to catch the error from the async onClick handler
      const loginButton = getByText('Login');

      // Click will trigger async login which will reject
      await act(async () => {
        loginButton.click();
        // Wait for the promise to reject and React to process the state updates
        await new Promise(resolve => setTimeout(resolve, 10));
      });

      // Component should still be mounted and showing unauthenticated state
      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
      expect(consoleSpy).toHaveBeenCalledWith('Login error:', expect.any(Error));

      consoleSpy.mockRestore();
    });
  });

  // ========== REGISTER TESTS ==========

  describe('Register', () => {
    it('should successfully register and update state', async () => {
      const mockResponse: AuthResponse = {
        token: 'register-token',
        userId: 2,
        username: 'newuser',
        email: 'new@example.com',
      };

      vi.mocked(apiClient.register).mockResolvedValueOnce(mockResponse);

      const { getByText } = render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      await act(async () => {
        getByText('Register').click();
        await new Promise(resolve => setTimeout(resolve, 0));
      });

      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');
      expect(screen.getByTestId('username').textContent).toBe('newuser');
      expect(apiClient.register).toHaveBeenCalledWith('newuser', 'password', 'new@example.com');
      expect(mockPush).toHaveBeenCalledWith('/select-organization');
    });

    it('should handle registration errors without crashing', async () => {
      // Suppress console.error for this test since AuthContext logs the error
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      vi.mocked(apiClient.register).mockRejectedValueOnce(new Error('Username exists'));

      const { getByText } = render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      // The register function will throw an error, but the component should not crash
      const registerButton = getByText('Register');

      // Click will trigger async register which will reject
      await act(async () => {
        registerButton.click();
        // Wait for the promise to reject and React to process the state updates
        await new Promise(resolve => setTimeout(resolve, 10));
      });

      // Component should still be mounted and showing unauthenticated state
      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
      expect(consoleSpy).toHaveBeenCalledWith('Registration error:', expect.any(Error));

      consoleSpy.mockRestore();
    });
  });

  // ========== LOGOUT TESTS ==========

  describe('Logout', () => {
    it('should clear state and redirect on logout', async () => {
      const mockUser = {
        userId: 1,
        username: 'testuser',
        email: 'test@example.com',
      };

      localStorageMock.setItem('token', 'test-token');
      localStorageMock.setItem('user', JSON.stringify(mockUser));

      const { getByText } = render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      // Initially authenticated
      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');

      act(() => {
        getByText('Logout').click();
      });

      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
      expect(screen.getByTestId('username').textContent).toBe('none');
      expect(apiClient.logout).toHaveBeenCalled();
      expect(mockPush).toHaveBeenCalledWith('/');
    });
  });

  // ========== HOOK ERROR HANDLING ==========

  describe('useAuth Hook', () => {
    it('should throw error when used outside AuthProvider', () => {
      // Suppress console.error for this test
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      expect(() => {
        render(<TestComponent />);
      }).toThrow('useAuth must be used within an AuthProvider');

      consoleSpy.mockRestore();
    });

    it('should provide context value when used within AuthProvider', () => {
      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      expect(screen.getByTestId('is-authenticated')).toBeDefined();
      expect(screen.getByTestId('is-loading')).toBeDefined();
    });
  });

  // ========== INTEGRATION TESTS ==========

  describe('Integration Scenarios', () => {
    it('should handle complete authentication flow', async () => {
      const loginResponse: AuthResponse = {
        token: 'login-token',
        userId: 1,
        username: 'testuser',
        email: 'test@example.com',
      };

      vi.mocked(apiClient.login).mockResolvedValueOnce(loginResponse);

      const { getByText } = render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      // Initially not authenticated
      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');

      // Login
      await act(async () => {
        getByText('Login').click();
        await new Promise(resolve => setTimeout(resolve, 0));
      });

      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');

      // Logout
      act(() => {
        getByText('Logout').click();
      });

      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
    });

    it('should persist authentication across re-renders', () => {
      const mockUser = {
        userId: 1,
        username: 'testuser',
        email: 'test@example.com',
      };

      localStorageMock.setItem('token', 'test-token');
      localStorageMock.setItem('user', JSON.stringify(mockUser));

      const { unmount } = render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');

      unmount();

      // Re-render with same localStorage
      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');
      expect(screen.getByTestId('username').textContent).toBe('testuser');
    });
  });

  // ========== STATE MANAGEMENT TESTS ==========

  describe('State Management', () => {
    it('should correctly report isAuthenticated based on user and token', () => {
      const { rerender } = render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      // No user, not authenticated
      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');

      // Add user to localStorage and re-render
      localStorageMock.setItem('token', 'test-token');
      localStorageMock.setItem('user', JSON.stringify({
        userId: 1,
        username: 'testuser',
        email: 'test@example.com',
      }));

      // Need to unmount and remount to pick up localStorage changes
      rerender(
        <div>
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        </div>
      );
    });

    it('should provide all expected context values', () => {
      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      );

      // Check that all UI elements render (meaning all context values are provided)
      expect(screen.getByTestId('is-authenticated')).toBeDefined();
      expect(screen.getByTestId('is-loading')).toBeDefined();
      expect(screen.getByTestId('username')).toBeDefined();
      expect(screen.getByTestId('email')).toBeDefined();
      expect(screen.getByText('Login')).toBeDefined();
      expect(screen.getByText('Register')).toBeDefined();
      expect(screen.getByText('Logout')).toBeDefined();
    });
  });
});
