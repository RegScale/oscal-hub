'use client';

import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { User, LogOut, Settings } from 'lucide-react';
import { OrganizationSwitcher } from '@/components/organization-switcher';

export function Navigation() {
  const { user, isAuthenticated, logout } = useAuth();

  // Check globalRole from localStorage as well (in case user object doesn't have it)
  const isSuperAdmin = () => {
    if (user?.globalRole === 'SUPER_ADMIN') return true;

    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
        const userData = JSON.parse(storedUser);
        return userData.globalRole === 'SUPER_ADMIN';
      } catch (e) {
        return false;
      }
    }
    return false;
  };

  return (
    <nav className="border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container mx-auto px-4">
        <div className="flex h-16 items-center justify-between">
          {/* Logo/Brand */}
          <Link href="/" className="flex items-center space-x-2">
            <div className="text-2xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
              OSCAL Hub
            </div>
          </Link>

          {/* User Section */}
          <div className="flex items-center space-x-4">
            {isAuthenticated && user ? (
              <>
                <Link href="/profile">
                  <div className="flex items-center space-x-2 text-sm text-muted-foreground cursor-pointer hover:text-foreground transition-colors">
                    <User className="h-4 w-4" aria-hidden="true" />
                    <span className="font-medium">{user.username}</span>
                  </div>
                </Link>
                <OrganizationSwitcher />
                {isSuperAdmin() && (
                  <Link href="/admin/organizations">
                    <Button
                      variant="outline"
                      size="sm"
                      className="flex items-center space-x-2"
                      title="Admin Settings"
                      aria-label="Admin Settings"
                    >
                      <Settings className="h-4 w-4" aria-hidden="true" />
                    </Button>
                  </Link>
                )}
                <Button
                  variant="outline"
                  size="sm"
                  onClick={logout}
                  className="flex items-center space-x-2"
                  aria-label="Logout"
                >
                  <LogOut className="h-4 w-4" aria-hidden="true" />
                  <span>Logout</span>
                </Button>
              </>
            ) : (
              <Link href="/login">
                <Button variant="default" size="sm">
                  Login
                </Button>
              </Link>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
