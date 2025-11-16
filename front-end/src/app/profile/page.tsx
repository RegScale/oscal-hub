'use client';

import { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowLeft, User, Mail, Lock, Save, Loader2, Upload, Image as ImageIcon } from 'lucide-react';
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
  const [email, setEmail] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [street, setStreet] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [zip, setZip] = useState('');
  const [title, setTitle] = useState('');
  const [organization, setOrganization] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [logoPreview, setLogoPreview] = useState<string | null>(null);
  const [pendingLogo, setPendingLogo] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Update form state when user data loads
  useEffect(() => {
    if (user) {
      setEmail(user.email || '');
      setFirstName(user.firstName || '');
      setLastName(user.lastName || '');
      setStreet(user.street || '');
      setCity(user.city || '');
      setState(user.state || '');
      setZip(user.zip || '');
      setTitle(user.title || '');
      setOrganization(user.organization || '');
      setPhoneNumber(user.phoneNumber || '');
      setLogoPreview(user.logo || null);
    }
  }, [user]);

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

      if (firstName !== user?.firstName) {
        updates.firstName = firstName;
      }

      if (lastName !== user?.lastName) {
        updates.lastName = lastName;
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

      // Check if there are any changes (including logo)
      if (Object.keys(updates).length === 0 && !pendingLogo) {
        setErrorMessage('No changes to save');
        setIsUpdating(false);
        return;
      }

      // Update profile fields if there are any
      if (Object.keys(updates).length > 0) {
        await apiClient.updateProfile(updates);
      }

      // Upload logo if there's a pending logo change
      if (pendingLogo) {
        await apiClient.uploadLogo(pendingLogo);
        setPendingLogo(null);
      }

      // Fetch the updated user data from the server
      const updatedUser = await apiClient.getCurrentUser();

      // Update localStorage with the complete user data
      localStorage.setItem('user', JSON.stringify(updatedUser));

      setSuccessMessage('Profile updated successfully');
      toast.success('Profile updated successfully');
      setNewPassword('');
      setConfirmPassword('');

      // Refresh the page to reload user data
      setTimeout(() => {
        window.location.reload();
      }, 1500);
    } catch (error: unknown) {
      console.error('Profile update error:', error);
      setErrorMessage(error instanceof Error ? error.message : 'Failed to update profile. Please try again.');
    } finally {
      setIsUpdating(false);
    }
  };

  const handleLogoSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      setErrorMessage('Please select an image file');
      toast.error('Please select an image file');
      return;
    }

    // Validate file size (max 2MB)
    if (file.size > 2 * 1024 * 1024) {
      setErrorMessage('Logo file size must be less than 2MB');
      toast.error('Logo file size must be less than 2MB');
      return;
    }

    setSuccessMessage(null);
    setErrorMessage(null);

    // Convert file to base64 data URL for preview
    const reader = new FileReader();
    reader.onloadend = () => {
      const base64Logo = reader.result as string;
      setLogoPreview(base64Logo);
      setPendingLogo(base64Logo);
      toast.success('Logo selected. Click "Save Profile" or "Save All Changes" to upload.');
    };
    reader.onerror = () => {
      setErrorMessage('Failed to read file. Please try again.');
      toast.error('Failed to read file');
    };
    reader.readAsDataURL(file);
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

          {/* Logo Upload Card */}
          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <ImageIcon className="h-5 w-5" />
                User Logo
              </CardTitle>
              <CardDescription>
                Select your logo to use in authorization templates with the {'{'}&#123; logo &#125;{'}'} tag. Logo will be saved with your profile.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* Logo Preview */}
              {logoPreview && (
                <div className="flex justify-center p-4 border rounded-lg bg-muted/50">
                  <img
                    src={logoPreview}
                    alt="User logo"
                    className="max-h-32 max-w-full object-contain"
                  />
                </div>
              )}

              {/* Upload Button */}
              <div className="flex gap-3">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  onChange={handleLogoSelect}
                  className="hidden"
                />
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={isUpdating}
                  className="flex-1"
                >
                  <Upload className="h-4 w-4 mr-2" />
                  {logoPreview ? 'Change Logo' : 'Select Logo'}
                </Button>
              </div>
              {pendingLogo && (
                <p className="text-sm text-amber-600 font-medium">
                  Logo selected. Click &quot;Save Profile&quot; or &quot;Save All Changes&quot; below to upload.
                </p>
              )}
              <p className="text-xs text-muted-foreground">
                Supported formats: PNG, JPG, GIF (max 2MB). Logo will be saved when you click &quot;Save Profile&quot; or &quot;Save All Changes&quot; and will be available in authorization templates using the {'{{ logo }}'} variable.
              </p>
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
                      <Label htmlFor="firstName">First Name</Label>
                      <Input
                        id="firstName"
                        type="text"
                        value={firstName}
                        onChange={(e) => setFirstName(e.target.value)}
                        placeholder="e.g., John"
                        disabled={isUpdating}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="lastName">Last Name</Label>
                      <Input
                        id="lastName"
                        type="text"
                        value={lastName}
                        onChange={(e) => setLastName(e.target.value)}
                        placeholder="e.g., Doe"
                        disabled={isUpdating}
                      />
                    </div>
                  </div>

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
