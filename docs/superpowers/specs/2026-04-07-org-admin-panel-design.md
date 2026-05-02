# Org Admin Panel Design Spec

**Date:** 2026-04-07
**Status:** Approved

## Problem

ORG_ADMIN users have no frontend UI. The backend endpoints exist at `/api/org-admin/*` for managing users and access requests within an organization, but the frontend admin pages (`/admin/*`) are gated exclusively to `SUPER_ADMIN` users via `globalRole !== 'SUPER_ADMIN'` checks. Org admins cannot review access requests, manage their members, or see usage analytics.

## Solution

Build a new `/org-admin/` route group in the frontend with 4 pages, plus one new backend analytics endpoint. The pages follow existing admin page patterns (Tailwind, lucide-react icons, recharts for charts, dark mode support).

## Access Control

- Pages are accessible to users with `orgRole === 'ORG_ADMIN'` OR `globalRole === 'SUPER_ADMIN'`
- Organization ID comes from the user's current org context (stored in `localStorage` as `currentOrganization`)
- All backend calls pass `organizationId` as a query parameter

## Pages

### 1. Dashboard (`/org-admin/`)

Summary landing page with stat cards and quick links.

**Stat cards (2x2 grid on desktop):**
- Total Members — count of active members in the org
- Pending Requests — count of pending access requests (red badge if > 0)
- Logins This Month — count of login events for org members in last 30 days
- Operations This Month — count of non-auth operations for org members in last 30 days

**Quick links section (3-column grid):**
- "Manage Users" card linking to `/org-admin/users`
- "Access Requests" card linking to `/org-admin/requests`
- "Usage Analytics" card linking to `/org-admin/analytics`

**Data source:** New `GET /api/org-admin/analytics/summary` endpoint.

**Role check:** On mount, read user from localStorage, verify `orgRole === 'ORG_ADMIN'` or `globalRole === 'SUPER_ADMIN'`, redirect to `/` if neither.

### 2. Users (`/org-admin/users`)

Table of all organization members with management actions.

**Table columns:**
| Column | Source |
|--------|--------|
| Name | firstName + lastName (or username as fallback) |
| Email | email |
| Role | membership role (ORG_ADMIN / USER) |
| Status | membership status (ACTIVE / LOCKED / DEACTIVATED) |
| Joined | membership joinedAt date |

**Features:**
- Search bar filtering by name/email/username (client-side)
- Filter dropdown by status (All / Active / Locked / Deactivated)
- Filter dropdown by role (All / Admin / User)
- Status badge colors: green=ACTIVE, yellow=LOCKED, red=DEACTIVATED

**Row actions (dropdown or inline buttons):**
- Lock (if ACTIVE) — calls `POST /api/org-admin/users/{userId}/lock?organizationId=X`
- Unlock (if LOCKED) — calls `POST /api/org-admin/users/{userId}/unlock?organizationId=X`
- Deactivate (if ACTIVE/LOCKED) — calls `POST /api/org-admin/users/{userId}/deactivate?organizationId=X`
- Reactivate (if DEACTIVATED) — calls `POST /api/org-admin/users/{userId}/reactivate?organizationId=X`
- Reset Password — calls `POST /api/org-admin/users/{userId}/reset-password?organizationId=X`, shows temp password in a modal

**Data source:** Existing `GET /api/org-admin/users?organizationId=X`

### 3. Access Requests (`/org-admin/requests`)

Review and manage access requests for the organization.

**Tab bar:**
- Pending (default) — with count badge
- All — shows approved/rejected history

**Request cards (list layout, not table):**
Each card shows:
- Name (firstName lastName)
- Email
- Username (if existing user)
- Message from requester
- Request date
- Status badge (PENDING=yellow, APPROVED=green, REJECTED=red)

**Actions on pending requests:**
- Approve button (green) — opens confirmation modal, optional notes textarea, calls `POST /api/org-admin/access-requests/{id}/approve`
- Reject button (red) — opens modal with required notes textarea, calls `POST /api/org-admin/access-requests/{id}/reject`

**For approved/rejected requests, also show:**
- Reviewed by (username)
- Reviewed date
- Admin notes

**Data sources:**
- Pending: `GET /api/org-admin/access-requests?organizationId=X`
- All: `GET /api/org-admin/access-requests/all?organizationId=X`

### 4. Analytics (`/org-admin/analytics`)

Org-scoped usage analytics with charts and tables.

**Summary stat cards (4-column grid):**
- Active Users (last 7 days)
- Total Logins (last 30 days)
- Total Operations (last 30 days)
- Failed Logins (last 30 days)

**Charts:**

1. **Login Activity (bar chart)** — logins per day for last 30 days
   - X axis: date
   - Y axis: login count
   - Uses recharts BarChart

2. **Operations by Type (pie/donut chart)** — breakdown of operation types
   - Segments: VALIDATE, CONVERT, RESOLVE, FILE_UPLOAD, etc.
   - Uses recharts PieChart

**Tables:**

3. **Most Active Users (top 10 table)**
   - Columns: Username, Name, Operations Count, Last Active
   - Sorted by operations count descending

**Data source:** New `GET /api/org-admin/analytics?organizationId=X`

## Backend Changes

### New Endpoint: `GET /api/org-admin/analytics`

Added to `OrgAdminController.java`.

**Auth:** `@PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")`

**Query param:** `organizationId` (Long)

**Response shape:**
```json
{
  "activeUsersLast7Days": 12,
  "totalLoginsLast30Days": 156,
  "totalOperationsLast30Days": 423,
  "failedLoginsLast30Days": 8,
  "loginsPerDay": [
    { "date": "2026-03-08", "count": 5 },
    { "date": "2026-03-09", "count": 8 }
  ],
  "operationsByType": [
    { "name": "VALIDATE", "count": 200 },
    { "name": "CONVERT", "count": 150 }
  ],
  "topUsers": [
    { "username": "jdoe", "firstName": "John", "lastName": "Doe", "operationCount": 45, "lastActive": "2026-04-06T14:30:00" }
  ]
}
```

### New Endpoint: `GET /api/org-admin/analytics/summary`

Lightweight version for the dashboard page — returns just the 4 stat card numbers plus pending request count.

**Response shape:**
```json
{
  "totalMembers": 25,
  "pendingRequests": 3,
  "loginsThisMonth": 156,
  "operationsThisMonth": 423
}
```

### Implementation Approach for Analytics

The `AuditEvent` entity does not have an `organizationId` column. To scope analytics by organization:

1. Query `OrganizationMembership` to get all usernames for the org
2. Use those usernames to filter `AuditEvent` queries with `WHERE username IN (:usernames)`
3. Add new repository methods to `AuditEventRepository`:
   - `countByUsernameInAndTimestampAfter(List<String> usernames, LocalDateTime since)`
   - `countByUsernameInAndEventTypeAndTimestampAfter(List<String> usernames, AuditEventType type, LocalDateTime since)`
   - `findLoginsPerDayByUsernames(List<String> usernames, LocalDateTime since)` (native query with GROUP BY DATE)
   - `findOperationsByTypeByUsernames(List<String> usernames, LocalDateTime since)` (native query with GROUP BY event_type)
   - `findTopUsersByUsernames(List<String> usernames, LocalDateTime since)` (native query with GROUP BY username ORDER BY count DESC LIMIT 10)

New service: `OrgAnalyticsService` — orchestrates the membership lookup + audit queries.

## Frontend API Client Changes

Add to `api-client.ts`:

```typescript
// Org Admin methods
async getOrgUsers(organizationId: number): Promise<OrgUser[]>
async lockOrgUser(organizationId: number, userId: number): Promise<void>
async unlockOrgUser(organizationId: number, userId: number): Promise<void>
async deactivateOrgUser(organizationId: number, userId: number): Promise<void>
async reactivateOrgUser(organizationId: number, userId: number): Promise<void>
async resetOrgUserPassword(organizationId: number, userId: number): Promise<{tempPassword: string, username: string, email: string}>
async getOrgPendingRequests(organizationId: number): Promise<AccessRequest[]>
async getOrgAllRequests(organizationId: number): Promise<AccessRequest[]>
async approveOrgRequest(requestId: number, notes?: string): Promise<void>
async rejectOrgRequest(requestId: number, notes?: string): Promise<void>
async getOrgAnalytics(organizationId: number): Promise<OrgAnalytics>
async getOrgAnalyticsSummary(organizationId: number): Promise<OrgAnalyticsSummary>
```

## Navigation Changes

In `Navigation.tsx`, add an "Org Admin" button visible when the user's `orgRole === 'ORG_ADMIN'` (and they are NOT a SUPER_ADMIN, who already has the admin button):

```tsx
{!isSuperAdmin() && isOrgAdmin() && (
  <Link href="/org-admin">
    <Button variant="outline" size="sm">
      <Shield className="h-4 w-4" />
    </Button>
  </Link>
)}
```

The org admin role is read from `localStorage.getItem('user')` -> `userData.orgRole`.

## Styling

All pages follow existing admin page patterns:
- Container: `min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8`
- Max width: `max-w-6xl mx-auto`
- Cards: `bg-white dark:bg-gray-800 rounded-lg shadow-md border border-gray-200 dark:border-gray-700`
- Tables: `min-w-full divide-y divide-gray-200 dark:divide-gray-700`
- Modals: fixed overlay with `bg-black bg-opacity-50` and centered white card
- Icons: `lucide-react`
- Charts: `recharts` (ResponsiveContainer, BarChart, PieChart)

## What We Are NOT Building

- Per-user activity detail drill-down pages
- Time range selectors or trend lines
- Export functionality
- Email/push notifications for new requests
- Ability for ORG_ADMIN to create new organizations
- Security policy management (remains SUPER_ADMIN only)
