'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Bug, Cog, Compass, Inbox, LogOut, Settings, UserCog } from 'lucide-react';
import { TourMenu } from '@/components/tour/TourMenu';
import type { User } from '@/types/auth';

function getInitials(user: User): string {
  const first = (user.firstName?.[0] ?? '').toUpperCase();
  const last = (user.lastName?.[0] ?? '').toUpperCase();
  if (first && last) return first + last;
  if (first) return first;
  return (user.username?.[0] ?? '?').toUpperCase();
}

export function UserAvatarMenu() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const [toursOpen, setToursOpen] = useState(false);
  const [isSuperAdmin, setIsSuperAdmin] = useState(false);
  const [isOrgAdmin, setIsOrgAdmin] = useState(false);

  // Mirror Navigation.tsx's gating: org-admin link is shown to org admins
  // who aren't also super admins (super admins use Admin Dashboard instead).
  useEffect(() => {
    setIsSuperAdmin(user?.globalRole === 'SUPER_ADMIN');
    setIsOrgAdmin(user?.orgRole === 'ORG_ADMIN' || !!user?.organizationId);
  }, [user]);

  if (!user) return null;

  const initials = getInitials(user);
  const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username;

  return (
    <>
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <button
          type="button"
          aria-label="User menu"
          className="flex h-9 w-9 items-center justify-center overflow-hidden rounded-full border border-border bg-gradient-to-br from-blue-500 to-purple-600 text-sm font-semibold text-white transition-opacity hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
        >
          {user.avatar ? (
            <img src={user.avatar} alt={fullName} className="h-full w-full object-cover" />
          ) : (
            <span>{initials}</span>
          )}
        </button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-56 p-1">
        <div className="border-b border-border px-3 py-2">
          <div className="truncate text-sm font-medium">{fullName}</div>
          <div className="truncate text-xs text-muted-foreground">{user.email}</div>
        </div>
        <div className="py-1">
          <Link
            href="/profile"
            onClick={() => setOpen(false)}
            className="flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors hover:bg-accent hover:text-accent-foreground"
          >
            <UserCog className="h-4 w-4" />
            Manage Profile
          </Link>
          <Link
            href="/tickets/new"
            onClick={() => setOpen(false)}
            className="flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors hover:bg-accent hover:text-accent-foreground"
          >
            <Bug className="h-4 w-4" />
            Open Ticket
          </Link>
          <Link
            href="/tickets"
            onClick={() => setOpen(false)}
            className="flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors hover:bg-accent hover:text-accent-foreground"
          >
            <Inbox className="h-4 w-4" />
            My Tickets
          </Link>
          {!isSuperAdmin && (
            <button
              type="button"
              onClick={() => {
                setOpen(false);
                setToursOpen(true);
              }}
              className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm transition-colors hover:bg-accent hover:text-accent-foreground"
            >
              <Compass className="h-4 w-4" />
              Guided Tours
            </button>
          )}
          {!isSuperAdmin && isOrgAdmin && (
            <Link
              href="/org-admin"
              onClick={() => setOpen(false)}
              className="flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors hover:bg-accent hover:text-accent-foreground"
            >
              <Cog className="h-4 w-4" />
              Org Admin Panel
            </Link>
          )}
          {isSuperAdmin && (
            <Link
              href="/admin"
              onClick={() => setOpen(false)}
              className="flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors hover:bg-accent hover:text-accent-foreground"
            >
              <Settings className="h-4 w-4" />
              Admin Dashboard
            </Link>
          )}
          <button
            type="button"
            onClick={() => {
              setOpen(false);
              logout();
            }}
            className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm transition-colors hover:bg-accent hover:text-accent-foreground"
          >
            <LogOut className="h-4 w-4" />
            Logout
          </button>
        </div>
      </PopoverContent>
    </Popover>
    <TourMenu open={toursOpen} onOpenChange={setToursOpen} />
    </>
  );
}
