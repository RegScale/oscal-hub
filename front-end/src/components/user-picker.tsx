'use client';

import { useState, useMemo } from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { ChevronDown, Check, Loader2 } from 'lucide-react';
import type { OrgMemberResponse } from '@/types/oscal';

interface UserPickerProps {
  value: number | null;
  onChange: (userId: number | null) => void;
  members: OrgMemberResponse[];
  loading?: boolean;
  excludeUserIds?: number[];
  placeholder?: string;
}

export function UserPicker({
  value,
  onChange,
  members,
  loading,
  excludeUserIds = [],
  placeholder = 'Select a user…',
}: UserPickerProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');

  const visible = useMemo(() => {
    const excluded = new Set(excludeUserIds);
    const q = query.trim().toLowerCase();
    return members
      .filter((m) => !excluded.has(m.userId))
      .filter((m) =>
        !q ||
        m.username.toLowerCase().includes(q) ||
        m.email.toLowerCase().includes(q) ||
        (m.firstName ?? '').toLowerCase().includes(q) ||
        (m.lastName ?? '').toLowerCase().includes(q)
      );
  }, [members, query, excludeUserIds]);

  const selected = members.find((m) => m.userId === value) ?? null;

  return (
    <div className="relative">
      <Button
        type="button"
        variant="outline"
        className="w-full justify-between"
        onClick={() => setOpen(!open)}
      >
        <span className="truncate">
          {selected
            ? `${selected.firstName ?? ''} ${selected.lastName ?? ''} (${selected.username})`.trim()
            : placeholder}
        </span>
        {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <ChevronDown className="h-4 w-4" />}
      </Button>

      {open && (
        <div className="absolute z-50 mt-1 w-full rounded-md border bg-popover shadow-md">
          <div className="p-2">
            <Input
              placeholder="Search users…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              autoFocus
            />
          </div>
          <div className="max-h-72 overflow-y-auto">
            {visible.length === 0 && (
              <div className="py-6 text-center text-sm text-muted-foreground">
                {loading ? 'Loading…' : 'No users found'}
              </div>
            )}
            {visible.map((m) => (
              <button
                key={m.userId}
                type="button"
                className="flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-accent"
                onClick={() => {
                  onChange(m.userId);
                  setOpen(false);
                  setQuery('');
                }}
              >
                <span className="flex flex-col">
                  <span className="font-medium">
                    {`${m.firstName ?? ''} ${m.lastName ?? ''}`.trim() || m.username}
                  </span>
                  <span className="text-xs text-muted-foreground">{m.email}</span>
                </span>
                {value === m.userId && <Check className="h-4 w-4" />}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
