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
import { toast } from 'sonner';

export default function ProfilePage() {
  const { user } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState(user?.email || '');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [street, setStreet] = useState(user?.street || '');
  const [city, setCity] = useState(user?.city || '');
  const [state, setState] = useState(user?.state || '');
  const [zip, setZip] = useState(user?.zip || '');
  const [title, setTitle] = useState(user?.title || '');
  const [organization, setOrganization] = useState(user?.organization || '');
  const [phoneNumber, setPhoneNumber] = useState(user?.phoneNumber || '');
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

      if (street !== user?.street) {
        updates.street = street;
      }

      if (city !== user?.city) {
        updates.city = city;
      }

      if (state !== user?.state) {
        updates.state = state;
      }

      if (zip !== user?.zip) {
        updates.zip = zip;
      }

      if (title !== user?.title) {
        updates.title = title;
      }

      if (organization !== user?.organization) {
        updates.organization = organization;
      }

      if (phoneNumber !== user?.phoneNumber) {
        updates.phoneNumber = phoneNumber;
      }

      if (Object.keys(updates).length === 0) {
        setErrorMessage('No changes to save');
        setIsUpdating(false);
        return;
      }

      await apiClient.updateProfile(updates);

      setSuccessMessage('Profile updated successfully');
      toast.success('Profile updated successfully');
      setNewPassword('');
      setConfirmPassword('');

      // If email was updated, update the user context
      if (updates.email) {
        // Refresh the page to reload user data
        setTimeout(() => {
          window.location.reload();
        }, 1500);
      }
    } catch (error: unknown) {
      console.error('Profile update error:', error);
      setErrorMessage(error instanceof Error ? error.message : 'Failed to update profile. Please try again.');
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

                {/* Profile Metadata Section */}
                <div className="space-y-4 pb-6 border-b">
                  <Label className="flex items-center gap-2 text-base">
                    <User className="h-4 w-4" />
                    Profile Information
                  </Label>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="title">Title</Label>
                      <Input
                        id="title"
                        type="text"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="e.g., Software Engineer"
                        disabled={isUpdating}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="organization">Organization/Company</Label>
                      <Input
                        id="organization"
                        type="text"
                        value={organization}
                        onChange={(e) => setOrganization(e.target.value)}
                        placeholder="e.g., ACME Corp"
                        disabled={isUpdating}
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="phoneNumber">Phone Number</Label>
                    <Input
                      id="phoneNumber"
                      type="tel"
                      value={phoneNumber}
                      onChange={(e) => setPhoneNumber(e.target.value)}
                      placeholder="e.g., (555) 123-4567"
                      disabled={isUpdating}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="street">Street Address</Label>
                    <Input
                      id="street"
                      type="text"
                      value={street}
                      onChange={(e) => setStreet(e.target.value)}
                      placeholder="e.g., 123 Main St"
                      disabled={isUpdating}
                    />
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="city">City</Label>
                      <Input
                        id="city"
                        type="text"
                        value={city}
                        onChange={(e) => setCity(e.target.value)}
                        placeholder="e.g., New York"
                        disabled={isUpdating}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="state">State</Label>
                      <Input
                        id="state"
                        type="text"
                        value={state}
                        onChange={(e) => setState(e.target.value)}
                        placeholder="e.g., NY"
                        disabled={isUpdating}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="zip">ZIP Code</Label>
                      <Input
                        id="zip"
                        type="text"
                        value={zip}
                        onChange={(e) => setZip(e.target.value)}
                        placeholder="e.g., 10001"
                        disabled={isUpdating}
                      />
                    </div>
                  </div>

                  {/* Save Profile Button */}
                  <div className="flex gap-3 pt-4">
                    <Button
                      type="submit"
                      disabled={isUpdating}
                      className="flex-1"
                    >
                      {isUpdating ? (
                        <>
                          <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                          Saving...
                        </>
                      ) : (
                        <>
                          <Save className="h-4 w-4 mr-2" />
                          Save Profile
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
                </div>

                {/* Password Section */}
                <div className="space-y-4">
                  <Label className="flex items-center gap-2 text-base">
                    <Lock className="h-4 w-4" />
                    Change Password (Optional)
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
                        Saving...
                      </>
                    ) : (
                      <>
                        <Save className="h-4 w-4 mr-2" />
                        Save All Changes
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
