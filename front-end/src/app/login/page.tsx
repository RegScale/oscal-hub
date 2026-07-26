'use client';

import { Suspense, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { PasswordRequirements, usePasswordPolicy } from '@/components/password-requirements';
import { isPasswordValid } from '@/lib/password-policy';
import Link from 'next/link';

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginPageContent />
    </Suspense>
  );
}

function LoginPageContent() {
  const { login, register } = useAuth();
  const searchParams = useSearchParams();
  const initialMode = searchParams.get('mode') === 'signup' ? false : true;
  const [isLogin, setIsLogin] = useState(initialMode);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [email, setEmail] = useState('');
  const [organizationName, setOrganizationName] = useState('');
  const [orgNameFieldError, setOrgNameFieldError] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const passwordPolicy = usePasswordPolicy();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      if (isLogin) {
        await login(username, password);
      } else {
        // Validation for registration
        if (!email) {
          setError('Email is required for registration');
          setIsLoading(false);
          return;
        }
        if (password !== confirmPassword) {
          setError('Passwords do not match');
          setIsLoading(false);
          return;
        }
        if (!isPasswordValid(password, username, passwordPolicy)) {
          setError('Password does not meet all the requirements listed below the password field');
          setIsLoading(false);
          return;
        }
        try {
          await register(username, password, email, organizationName.trim() || undefined);
        } catch (err: unknown) {
          const e = err as { field?: string; message?: string };
          if (e?.field === 'organizationName') {
            setOrgNameFieldError(e.message || 'That name is taken');
            setIsLoading(false);
            return;
          }
          throw err;
        }
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Authentication failed');
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <div className="w-full max-w-md">
        <Link href="/" className="block mb-2">
          <h1 className="text-4xl font-bold text-center bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
            OSCAL Hub
          </h1>
        </Link>
        <p className="text-center text-sm text-muted-foreground mb-8">
          <Link href="/catalog" className="hover:underline">
            Browse OSCAL Data Products
          </Link>
          <span className="mx-2">·</span>
          No account needed
        </p>

        <Card>
          <CardHeader>
            <CardTitle>{isLogin ? 'Login' : 'Create Account'}</CardTitle>
            <CardDescription>
              {isLogin
                ? 'Enter your credentials to access OSCAL Hub'
                : 'Create a new account to get started'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && (
                <Alert variant="destructive">
                  {/* Alert is a grid with a zero-width first column for the icon slot;
                      content must go through AlertDescription (col-start-2) or it
                      collapses to one word per line. */}
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              <div className="space-y-2">
                <Label htmlFor="username">Username</Label>
                <Input
                  id="username"
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  autoComplete="username"
                  placeholder="Enter your username"
                />
              </div>

              {!isLogin && (
                <div className="space-y-2">
                  <Label htmlFor="email">Email</Label>
                  <Input
                    id="email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required={!isLogin}
                    autoComplete="email"
                    placeholder="Enter your email"
                  />
                </div>
              )}

              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <Input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete={isLogin ? 'current-password' : 'new-password'}
                  placeholder="Enter your password"
                  minLength={isLogin ? undefined : passwordPolicy.minLength}
                />
                {!isLogin && (
                  <PasswordRequirements password={password} username={username} policy={passwordPolicy} />
                )}
              </div>

              {!isLogin && (
                <div className="space-y-2">
                  <Label htmlFor="confirmPassword">Confirm Password</Label>
                  <Input
                    id="confirmPassword"
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required={!isLogin}
                    autoComplete="new-password"
                    placeholder="Confirm your password"
                    minLength={passwordPolicy.minLength}
                  />
                  {confirmPassword.length > 0 && password !== confirmPassword && (
                    <p className="text-xs text-destructive">Passwords do not match</p>
                  )}
                </div>
              )}

              {!isLogin && (
                <div className="space-y-2">
                  <Label htmlFor="organizationName">Organization name</Label>
                  <Input
                    id="organizationName"
                    type="text"
                    value={organizationName}
                    onChange={(e) => { setOrganizationName(e.target.value); setOrgNameFieldError(''); }}
                    placeholder="Your organization or workspace"
                    autoComplete="organization"
                  />
                  <p className="text-xs text-muted-foreground">
                    You&apos;ll be the admin. You can invite teammates or rename later.
                  </p>
                  {orgNameFieldError && (
                    <p className="text-xs text-destructive">{orgNameFieldError}</p>
                  )}
                </div>
              )}

              {!isLogin && (
                <p className="text-xs text-muted-foreground">
                  OSCAL Hub is free to use. By creating an account, you agree to receive
                  occasional emails about new OSCAL-related products and services. You can
                  unsubscribe at any time.
                </p>
              )}

              <Button type="submit" className="w-full" disabled={isLoading}>
                {isLoading ? 'Please wait...' : isLogin ? 'Login' : 'Create Account'}
              </Button>
            </form>

            {isLogin && (
              <div className="mt-3 text-center text-sm">
                <Link href="/forgot-password" className="text-muted-foreground hover:underline">
                  Forgot your password?
                </Link>
              </div>
            )}

            <div className="mt-4 text-center text-sm">
              <button
                type="button"
                onClick={() => {
                  setIsLogin(!isLogin);
                  setError('');
                  setOrgNameFieldError('');
                }}
                className="text-primary hover:underline"
              >
                {isLogin
                  ? "Don't have an account? Sign up"
                  : 'Already have an account? Login'}
              </button>
            </div>

            {!isLogin && (
              <div className="mt-3 text-center text-sm">
                <a
                  href="/request-access"
                  onClick={(e) => {
                    e.preventDefault();
                    if (email) sessionStorage.setItem('pendingRegistration.email', email);
                    if (username) sessionStorage.setItem('pendingRegistration.username', username);
                    window.location.href = '/request-access';
                  }}
                  className="text-muted-foreground underline"
                >
                  Looking to join an existing organization? Request access
                </a>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
