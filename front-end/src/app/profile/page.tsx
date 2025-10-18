'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowLeft, User, Mail, Lock, Save, Loader2 } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useAuth } from '@/contexts/AuthContext';
import { apiClient } from '@/lib/api-client';
import ProtectedRoute from '@/components/ProtectedRoute';
import { ServiceAccountTokenGenerator } from '@/components/ServiceAccountTokenGenerator';

export default function ProfilePage() {
  const { user } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState(user?.email || '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setSuccessMessage(null);
    setErrorMessage(null);

    // Validate passwords match if user is changing password
    if (newPassword && newPassword !== confirmPassword) {
      setErrorMessage('New passwords do not match');
      return;
    }

    // Validate password length
    if (newPassword && newPassword.length < 8) {
      setErrorMessage('Password must be at least 8 characters');
      return;
    }

    setIsUpdating(true);

    try {
      const updates: Record<string, string> = {};

      if (email && email !== user?.email) {
        updates.email = email;
      }

      if (newPassword) {
        updates.password = newPassword;
      }

      if (Object.keys(updates).length === 0) {
        setErrorMessage('No changes to save');
        setIsUpdating(false);
        return;
      }

      await apiClient.updateProfile(updates);

      setSuccessMessage('Profile updated successfully');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');

      // If email was updated, update the user context
      if (updates.email) {
        // Refresh the page to reload user data
        setTimeout(() => {
          window.location.reload();
        }, 1500);
      }
    } catch (error: any) {
      console.error('Profile update error:', error);
      setErrorMessage(error.message || 'Failed to update profile. Please try again.');
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-background">
        <div className="container mx-auto py-8 px-4 max-w-2xl">
          {/* Header */}
          <header className="mb-8">
            <Link
              href="/"
              className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors"
            >
              <ArrowLeft className="h-4 w-4" />
              Back to Dashboard
            </Link>
            <h1 className="text-4xl font-bold mb-2">User Profile</h1>
            <p className="text-muted-foreground">
              Manage your account settings and preferences
            </p>
          </header>

          {/* Success/Error Messages */}
          {successMessage && (
            <Alert className="mb-6 border-green-500/50 bg-green-500/10">
              <AlertDescription className="text-green-600">
                {successMessage}
              </AlertDescription>
            </Alert>
          )}

          {errorMessage && (
            <Alert variant="destructive" className="mb-6">
              <AlertDescription>{errorMessage}</AlertDescription>
            </Alert>
          )}

          {/* User Info Card */}
          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <User className="h-5 w-5" />
                Account Information
              </CardTitle>
              <CardDescription>
                Your current account details
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <Label className="text-sm text-muted-foreground">Username</Label>
                <p className="text-lg font-medium">{user?.username}</p>
              </div>
              <div>
                <Label className="text-sm text-muted-foreground">User ID</Label>
                <p className="text-sm font-mono text-muted-foreground">{user?.userId}</p>
              </div>
            </CardContent>
          </Card>

          {/* Update Profile Form */}
          <Card>
            <CardHeader>
              <CardTitle>Update Profile</CardTitle>
              <CardDescription>
                Change your email address or password
              </CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleUpdateProfile} className="space-y-6">
                {/* Email Section */}
                <div className="space-y-4 pb-6 border-b">
                  <div className="space-y-2">
                    <Label htmlFor="email" className="flex items-center gap-2">
                      <Mail className="h-4 w-4" />
                      Email Address
                    </Label>
                    <Input
                      id="email"
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="your.email@example.com"
                      disabled={isUpdating}
                    />
                    <p className="text-xs text-muted-foreground">
                      Update your email address for account notifications
                    </p>
                  </div>
                </div>

                {/* Password Section */}
                <div className="space-y-4">
                  <Label className="flex items-center gap-2 text-base">
                    <Lock className="h-4 w-4" />
                    Change Password
                  </Label>

                  <div className="space-y-2">
                    <Label htmlFor="newPassword">New Password</Label>
                    <Input
                      id="newPassword"
                      type="password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="Enter new password"
                      disabled={isUpdating}
                      autoComplete="new-password"
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="confirmPassword">Confirm New Password</Label>
                    <Input
                      id="confirmPassword"
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Confirm new password"
                      disabled={isUpdating}
                      autoComplete="new-password"
                    />
                  </div>

                  <div className="text-xs text-muted-foreground space-y-1">
                    <p className="font-medium">Password requirements:</p>
                    <ul className="list-disc list-inside space-y-0.5">
                      <li>At least 8 characters</li>
                      <li>At least one uppercase letter</li>
                      <li>At least one lowercase letter</li>
                      <li>At least one number</li>
                      <li>At least one special character (!@#$%^&*)</li>
                    </ul>
                  </div>
                </div>

                {/* Submit Button */}
                <div className="flex gap-3 pt-4">
                  <Button
                    type="submit"
                    disabled={isUpdating}
                    className="flex-1"
                  >
                    {isUpdating ? (
                      <>
                        <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                        Updating...
                      </>
                    ) : (
                      <>
                        <Save className="h-4 w-4 mr-2" />
                        Save Changes
                      </>
                    )}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => router.push('/')}
                    disabled={isUpdating}
                  >
                    Cancel
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          {/* Service Account Token Generator */}
          <div className="mt-6">
            <ServiceAccountTokenGenerator />
          </div>
        </div>
      </div>
    </ProtectedRoute>
  );
}
