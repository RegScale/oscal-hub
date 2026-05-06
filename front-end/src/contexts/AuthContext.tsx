'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode, useRef, useCallback } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import type { User, AuthContextType } from '@/types/auth';
import { apiClient } from '@/lib/api-client';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const SESSION_TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
const REFRESH_INTERVAL = 5 * 60 * 1000; // Refresh every 5 minutes if active

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();
  const pathname = usePathname();
  const lastActivityRef = useRef<number>(Date.now());
  const refreshTimerRef = useRef<NodeJS.Timeout | null>(null);

  // Update last activity time
  const updateActivity = useCallback(() => {
    lastActivityRef.current = Date.now();
  }, []);

  // Check if session is expired
  const isSessionExpired = useCallback(() => {
    const timeSinceActivity = Date.now() - lastActivityRef.current;
    return timeSinceActivity > SESSION_TIMEOUT;
  }, []);

  // Handle session expiration
  const handleSessionExpiration = useCallback(() => {
    console.log('Session expired due to inactivity');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
    // Only redirect if not already on login or home page
    if (pathname !== '/' && pathname !== '/login') {
      router.push('/');
    }
  }, [router, pathname]);

  // Refresh token if session is still active
  const refreshFailCountRef = useRef<number>(0);
  const refreshTokenIfNeeded = useCallback(async () => {
    if (!token || !user) return;

    // Check if session expired
    if (isSessionExpired()) {
      handleSessionExpiration();
      return;
    }

    // Refresh token — tolerate transient failures
    try {
      await apiClient.refreshToken();
      updateActivity();
      refreshFailCountRef.current = 0;
    } catch (error) {
      refreshFailCountRef.current += 1;
      console.warn(`Token refresh failed (attempt ${refreshFailCountRef.current}):`, error);
      // Only log out after 3 consecutive failures (15 minutes of failed refreshes)
      if (refreshFailCountRef.current >= 3) {
        console.error('Token refresh failed 3 times consecutively, logging out');
        handleSessionExpiration();
      }
    }
  }, [token, user, isSessionExpired, handleSessionExpiration, updateActivity]);

  // Initialize auth state from localStorage
  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');

    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
      updateActivity();
    }

    setIsLoading(false);
  }, [updateActivity]);

  // Set up automatic token refresh
  useEffect(() => {
    if (!token || !user) {
      if (refreshTimerRef.current) {
        clearInterval(refreshTimerRef.current);
        refreshTimerRef.current = null;
      }
      return;
    }

    // Refresh token periodically
    refreshTimerRef.current = setInterval(() => {
      refreshTokenIfNeeded();
    }, REFRESH_INTERVAL);

    return () => {
      if (refreshTimerRef.current) {
        clearInterval(refreshTimerRef.current);
      }
    };
  }, [token, user, refreshTokenIfNeeded]);

  // Track user activity
  useEffect(() => {
    if (!token || !user) return;

    const activityEvents = ['mousedown', 'keydown', 'scroll', 'touchstart'];

    activityEvents.forEach(event => {
      window.addEventListener(event, updateActivity);
    });

    return () => {
      activityEvents.forEach(event => {
        window.removeEventListener(event, updateActivity);
      });
    };
  }, [token, user, updateActivity]);

  const login = async (username: string, password: string) => {
    try {
      const response = await apiClient.login(username, password);

      // Check for MFA requirements
      if (response.mfaSetupRequired && response.mfaToken) {
        // User needs to set up MFA (policy requires MFA but user hasn't set it up)
        router.push(`/mfa-setup?token=${encodeURIComponent(response.mfaToken)}`);
        return;
      }

      if (response.mfaRequired && response.mfaToken) {
        // User has MFA enabled, need to verify
        router.push(`/mfa-verify?token=${encodeURIComponent(response.mfaToken)}`);
        return;
      }

      // No MFA required - normal login flow
      if (!response.token) {
        throw new Error('No authentication token received');
      }

      const userData = {
        userId: response.userId,
        username: response.username,
        email: response.email,
        globalRole: response.globalRole,
        firstName: response.firstName,
        lastName: response.lastName,
        street: response.street,
        city: response.city,
        state: response.state,
        zip: response.zip,
        title: response.title,
        organization: response.organization,
        phoneNumber: response.phoneNumber,
        logo: response.logo,
        avatar: response.avatar,
      };

      // Save to state
      setToken(response.token ?? null);
      setUser(userData);

      // Persist to localStorage
      localStorage.setItem('token', response.token ?? '');
      localStorage.setItem('user', JSON.stringify(userData));

      updateActivity();

      // Super admins go directly to admin dashboard
      if (response.globalRole === 'SUPER_ADMIN') {
        router.push('/admin');
      } else {
        // Regular users go to organization selector for two-step authentication
        router.push('/select-organization');
      }
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  };

  const register = async (username: string, password: string, email: string, organizationName?: string) => {
    try {
      const response = await apiClient.register(username, password, email, organizationName);
      const userData = {
        userId: response.userId,
        username: response.username,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        street: response.street,
        city: response.city,
        state: response.state,
        zip: response.zip,
        title: response.title,
        organization: response.organization,
        phoneNumber: response.phoneNumber,
        logo: response.logo,
        avatar: response.avatar,
      };

      // Save to state
      setToken(response.token ?? null);
      setUser(userData);

      // Persist to localStorage
      localStorage.setItem('token', response.token ?? '');
      localStorage.setItem('user', JSON.stringify(userData));

      updateActivity();
      // Redirect to organization selector for two-step authentication
      router.push('/select-organization');
    } catch (error) {
      console.error('Registration error:', error);
      throw error;
    }
  };

  const logout = () => {
    apiClient.logout();
    setToken(null);
    setUser(null);
    router.push('/');
  };

  const updateUser = useCallback(() => {
    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');

    if (storedToken && storedUser) {
      setToken(storedToken);
      setUser(JSON.parse(storedUser));
      updateActivity();
    }
  }, [updateActivity]);

  const value: AuthContextType = {
    user,
    token,
    isAuthenticated: !!user && !!token,
    isLoading,
    login,
    register,
    logout,
    updateUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
