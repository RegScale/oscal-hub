'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode, useRef, useCallback } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import type { User, AuthContextType } from '@/types/auth';
import { apiClient } from '@/lib/api-client';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const SESSION_TIMEOUT = 60 * 60 * 1000; // 1 hour in milliseconds
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
  const refreshTokenIfNeeded = useCallback(async () => {
    if (!token || !user) return;

    // Check if session expired
    if (isSessionExpired()) {
      handleSessionExpiration();
      return;
    }

    // Refresh token
    try {
      await apiClient.refreshToken();
      updateActivity();
      console.log('Token refreshed successfully');
    } catch (error) {
      console.error('Failed to refresh token:', error);
      handleSessionExpiration();
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
      setToken(response.token);
      setUser({
        userId: response.userId,
        username: response.username,
        email: response.email,
      });
      updateActivity();
      router.push('/');
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  };

  const register = async (username: string, password: string, email: string) => {
    try {
      const response = await apiClient.register(username, password, email);
      setToken(response.token);
      setUser({
        userId: response.userId,
        username: response.username,
        email: response.email,
      });
      updateActivity();
      router.push('/');
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

  const value: AuthContextType = {
    user,
    token,
    isAuthenticated: !!user && !!token,
    isLoading,
    login,
    register,
    logout,
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
