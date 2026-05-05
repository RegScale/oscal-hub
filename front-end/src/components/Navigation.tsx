'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import {
  User, LogOut, Settings, Cog, Library, ChevronDown,
  FileText, Hammer, ShieldCheck, BarChart3, FileCheck,
  ArrowRightLeft, Folders, Clock, GitMerge,
} from 'lucide-react';
import { OrganizationSwitcher } from '@/components/organization-switcher';

interface UserAction {
  href: string;
  label: string;
  icon: typeof Library;
  description: string;
}

// Mirrors the dashboard tiles on /. Visible to every authenticated user
// so the same set of jumps is one click away from anywhere in the app.
const USER_ACTIONS: UserAction[] = [
  { href: '/library', label: 'Library', icon: Library, description: 'Browse, share, and download example OSCAL documents' },
  { href: '/artifacts', label: 'Artifacts', icon: FileText, description: 'Markdown templates with variables for compliance docs' },
  { href: '/build', label: 'Build', icon: Hammer, description: 'Visually create catalogs, profiles, components, SSPs, AP/AR/POAM' },
  { href: '/authorizations', label: 'Authorizations', icon: ShieldCheck, description: 'Create and manage system authorizations' },
  { href: '/visualize', label: 'Visualize', icon: BarChart3, description: 'Explore OSCAL documents through interactive visualizations' },
  { href: '/validate', label: 'Validate', icon: FileCheck, description: 'Check an OSCAL document against schema and constraints' },
  { href: '/convert', label: 'Convert', icon: ArrowRightLeft, description: 'Change format between XML, JSON, and YAML' },
  { href: '/rules', label: 'Validation Rules', icon: FileCheck, description: 'Browse the rules checked during validation' },
  { href: '/batch', label: 'Batch', icon: Folders, description: 'Process multiple files at once with progress tracking' },
  { href: '/history', label: 'History', icon: Clock, description: 'Past operations and results — re-run any of them' },
  { href: '/resolve', label: 'Resolve', icon: GitMerge, description: 'Resolve OSCAL profiles into catalogs' },
];

export function Navigation() {
  // Renders on every route, including /catalog. The public-catalog pages used
  // to ship their own minimal header inside (public)/layout.tsx, but that
  // dropped the Browse + Actions controls when an authenticated user followed
  // the Browse link. Letting the global Navigation cover those routes keeps
  // the primary nav consistent everywhere; the popovers and login state both
  // still render correctly because the AuthProvider lives in the root layout.
  const { user, isAuthenticated, logout } = useAuth();
  const [mounted, setMounted] = useState(false);
  const [isSuperAdminUser, setIsSuperAdminUser] = useState(false);
  const [isOrgAdminUser, setIsOrgAdminUser] = useState(false);
  const [hasOrgContext, setHasOrgContext] = useState(false);
  const [actionsOpen, setActionsOpen] = useState(false);

  // Check localStorage on mount to determine admin status
  useEffect(() => {
    setMounted(true);
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
        const userData = JSON.parse(storedUser);
        setIsSuperAdminUser(userData.globalRole === 'SUPER_ADMIN');
        setIsOrgAdminUser(userData.orgRole === 'ORG_ADMIN');
        setHasOrgContext(!!userData.organizationId);
      } catch (e) {
        setIsSuperAdminUser(false);
        setIsOrgAdminUser(false);
      }
    }
  }, []);

  // Also update when user changes
  useEffect(() => {
    if (user) {
      setIsSuperAdminUser(user.globalRole === 'SUPER_ADMIN');
      setIsOrgAdminUser(user.orgRole === 'ORG_ADMIN');
      setHasOrgContext(!!user.organizationId);
    }
  }, [user]);

  // Check if user is super admin
  const isSuperAdmin = () => {
    return isSuperAdminUser || user?.globalRole === 'SUPER_ADMIN';
  };

  const isOrgAdmin = () => {
    return isOrgAdminUser || user?.orgRole === 'ORG_ADMIN' || hasOrgContext;
  };

  return (
    <nav className="border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container mx-auto px-4">
        <div className="flex h-16 items-center justify-between">
          {/* Left: Logo + primary nav */}
          <div className="flex items-center gap-6">
            <Link href={isSuperAdmin() ? '/admin' : '/'} className="flex items-center space-x-2">
              <div className="text-2xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
                OSCAL Hub
              </div>
            </Link>
            {/* Browse — visible to everyone (logged-in or out). */}
            <Link
              href="/catalog"
              className="hidden sm:inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
              <Library className="h-4 w-4" />
              Browse
            </Link>
            {/* Actions — every authenticated user. Mirrors the dashboard tiles
                on / so the same set of jumps is one click away from anywhere. */}
            {mounted && isAuthenticated && (
              <Popover open={actionsOpen} onOpenChange={setActionsOpen}>
                <PopoverTrigger asChild>
                  <button
                    type="button"
                    className="hidden sm:inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
                  >
                    Actions
                    <ChevronDown className="h-3.5 w-3.5" />
                  </button>
                </PopoverTrigger>
                <PopoverContent align="start" className="w-80 p-1 max-h-[80vh] overflow-y-auto">
                  <div className="grid gap-0.5">
                    {USER_ACTIONS.map(({ href, label, icon: Icon, description }) => (
                      <Link
                        key={href}
                        href={href}
                        onClick={() => setActionsOpen(false)}
                        className="flex items-start gap-3 rounded-md px-3 py-2 hover:bg-accent hover:text-accent-foreground transition-colors"
                      >
                        <Icon className="h-4 w-4 mt-0.5 text-primary shrink-0" />
                        <div className="min-w-0">
                          <div className="text-sm font-medium leading-tight">{label}</div>
                          <div className="text-xs text-muted-foreground leading-tight mt-0.5">{description}</div>
                        </div>
                      </Link>
                    ))}
                  </div>
                </PopoverContent>
              </Popover>
            )}
          </div>

          {/* Right: User Section */}
          <div className="flex items-center space-x-4">
            {mounted && isAuthenticated && user ? (
              <>
                <Link href="/profile">
                  <div className="flex items-center space-x-2 text-sm text-muted-foreground cursor-pointer hover:text-foreground transition-colors">
                    <User className="h-4 w-4" aria-hidden="true" />
                    <span className="font-medium">{user.username}</span>
                  </div>
                </Link>
                {!isSuperAdmin() && <OrganizationSwitcher />}
                {!isSuperAdmin() && isOrgAdmin() && (
                  <Link href="/org-admin">
                    <Button
                      variant="outline"
                      size="sm"
                      className="flex items-center space-x-2"
                      title="Setup - Admin Panel"
                      aria-label="Setup - Admin Panel"
                    >
                      <Cog className="h-4 w-4" aria-hidden="true" />
                    </Button>
                  </Link>
                )}
                {isSuperAdmin() && (
                  <Link href="/admin">
                    <Button
                      variant="outline"
                      size="sm"
                      className="flex items-center space-x-2"
                      title="Admin Dashboard"
                      aria-label="Admin Dashboard"
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
            ) : mounted ? (
              <Link href="/login">
                <Button variant="default" size="sm">
                  Login
                </Button>
              </Link>
            ) : null}
          </div>
        </div>
      </div>
    </nav>
  );
}
