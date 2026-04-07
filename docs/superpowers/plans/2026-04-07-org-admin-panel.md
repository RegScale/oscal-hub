# Org Admin Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a `/org-admin/` frontend route group with dashboard, user management, access request review, and analytics pages — plus one new backend analytics endpoint — so ORG_ADMIN users can manage their organization.

**Architecture:** New `OrgAnalyticsService` queries audit events scoped by org membership usernames. New `GET /api/org-admin/analytics` endpoint in existing `OrgAdminController`. Four new Next.js pages under `front-end/src/app/org-admin/`. New API client methods in `api-client.ts`. Nav update in `Navigation.tsx`.

**Tech Stack:** Java 11 / Spring Boot 3.5 / JPA, Next.js / React / TypeScript, Tailwind CSS, recharts, lucide-react

**Spec:** `docs/superpowers/specs/2026-04-07-org-admin-panel-design.md`

---

### Task 1: Backend — OrgAnalyticsService and Repository Queries

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/OrgAnalyticsService.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuditEventRepository.java` (add org-scoped queries after line ~528)
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/OrgAdminController.java` (add analytics endpoints after line ~375)

- [ ] **Step 1: Add org-scoped query methods to AuditEventRepository**

Add these methods after the existing `countByUsernameSince` method (around line 528) in `back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuditEventRepository.java`:

```java
    // ========================================
    // Organization-scoped analytics queries
    // ========================================

    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.username IN :usernames AND a.eventType = 'AUTH_LOGIN_SUCCESS' AND a.timestamp >= :since")
    long countLoginsByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.username IN :usernames AND a.category != 'Authentication' AND a.timestamp >= :since")
    long countOperationsByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.username IN :usernames AND a.eventType = 'AUTH_LOGIN_FAILURE' AND a.timestamp >= :since")
    long countFailedLoginsByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT a.username) FROM AuditEvent a WHERE a.username IN :usernames AND a.timestamp >= :since")
    long countActiveUsersByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    @Query("SELECT CAST(a.timestamp AS date), COUNT(a) FROM AuditEvent a WHERE a.username IN :usernames AND a.eventType = 'AUTH_LOGIN_SUCCESS' AND a.timestamp >= :since GROUP BY CAST(a.timestamp AS date) ORDER BY CAST(a.timestamp AS date)")
    List<Object[]> countLoginsPerDayByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    @Query("SELECT a.eventType, COUNT(a) FROM AuditEvent a WHERE a.username IN :usernames AND a.category != 'Authentication' AND a.timestamp >= :since GROUP BY a.eventType ORDER BY COUNT(a) DESC")
    List<Object[]> countOperationsByTypeByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    @Query("SELECT a.username, COUNT(a) FROM AuditEvent a WHERE a.username IN :usernames AND a.category != 'Authentication' AND a.timestamp >= :since GROUP BY a.username ORDER BY COUNT(a) DESC")
    List<Object[]> countTopUsersByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);
```

- [ ] **Step 2: Create OrgAnalyticsService**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/service/OrgAnalyticsService.java`:

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.AuditEventRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserAccessRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrgAnalyticsService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final AuditEventRepository auditEventRepository;
    private final UserAccessRequestRepository accessRequestRepository;

    public OrgAnalyticsService(OrganizationRepository organizationRepository,
                                OrganizationMembershipRepository membershipRepository,
                                AuditEventRepository auditEventRepository,
                                UserAccessRequestRepository accessRequestRepository) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.auditEventRepository = auditEventRepository;
        this.accessRequestRepository = accessRequestRepository;
    }

    public Map<String, Object> getAnalyticsSummary(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        List<String> usernames = getOrgUsernames(org);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        int totalMembers = membershipRepository.countByOrganizationId(organizationId);
        long pendingRequests = accessRequestRepository.countPendingByOrganizationId(organizationId);

        long loginsThisMonth = 0;
        long operationsThisMonth = 0;
        if (!usernames.isEmpty()) {
            loginsThisMonth = auditEventRepository.countLoginsByUsernamesSince(usernames, thirtyDaysAgo);
            operationsThisMonth = auditEventRepository.countOperationsByUsernamesSince(usernames, thirtyDaysAgo);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalMembers", totalMembers);
        summary.put("pendingRequests", pendingRequests);
        summary.put("loginsThisMonth", loginsThisMonth);
        summary.put("operationsThisMonth", operationsThisMonth);
        return summary;
    }

    public Map<String, Object> getFullAnalytics(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        List<String> usernames = getOrgUsernames(org);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        Map<String, Object> analytics = new LinkedHashMap<>();

        if (usernames.isEmpty()) {
            analytics.put("activeUsersLast7Days", 0);
            analytics.put("totalLoginsLast30Days", 0);
            analytics.put("totalOperationsLast30Days", 0);
            analytics.put("failedLoginsLast30Days", 0);
            analytics.put("loginsPerDay", Collections.emptyList());
            analytics.put("operationsByType", Collections.emptyList());
            analytics.put("topUsers", Collections.emptyList());
            return analytics;
        }

        analytics.put("activeUsersLast7Days", auditEventRepository.countActiveUsersByUsernamesSince(usernames, sevenDaysAgo));
        analytics.put("totalLoginsLast30Days", auditEventRepository.countLoginsByUsernamesSince(usernames, thirtyDaysAgo));
        analytics.put("totalOperationsLast30Days", auditEventRepository.countOperationsByUsernamesSince(usernames, thirtyDaysAgo));
        analytics.put("failedLoginsLast30Days", auditEventRepository.countFailedLoginsByUsernamesSince(usernames, thirtyDaysAgo));

        // Logins per day
        List<Object[]> loginsPerDayRaw = auditEventRepository.countLoginsPerDayByUsernamesSince(usernames, thirtyDaysAgo);
        List<Map<String, Object>> loginsPerDay = new ArrayList<>();
        for (Object[] row : loginsPerDayRaw) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", row[0].toString());
            entry.put("count", ((Number) row[1]).longValue());
            loginsPerDay.add(entry);
        }
        analytics.put("loginsPerDay", loginsPerDay);

        // Operations by type
        List<Object[]> opsByTypeRaw = auditEventRepository.countOperationsByTypeByUsernamesSince(usernames, thirtyDaysAgo);
        List<Map<String, Object>> opsByType = new ArrayList<>();
        for (Object[] row : opsByTypeRaw) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", row[0].toString());
            entry.put("count", ((Number) row[1]).longValue());
            opsByType.add(entry);
        }
        analytics.put("operationsByType", opsByType);

        // Top users
        List<Object[]> topUsersRaw = auditEventRepository.countTopUsersByUsernamesSince(usernames, thirtyDaysAgo);
        List<Map<String, Object>> topUsers = new ArrayList<>();
        int limit = Math.min(topUsersRaw.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = topUsersRaw.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("username", row[0].toString());
            entry.put("operationCount", ((Number) row[1]).longValue());
            topUsers.add(entry);
        }
        analytics.put("topUsers", topUsers);

        return analytics;
    }

    private List<String> getOrgUsernames(Organization org) {
        List<OrganizationMembership> memberships = membershipRepository.findByOrganization(org);
        return memberships.stream()
                .map(m -> m.getUser().getUsername())
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 3: Add countPendingByOrganizationId to UserAccessRequestRepository**

Check the repository and add the missing query. Add to `back-end/src/main/java/gov/nist/oscal/tools/api/repository/UserAccessRequestRepository.java`:

```java
    @Query("SELECT COUNT(r) FROM UserAccessRequest r WHERE r.organization.id = :orgId AND r.status = 'PENDING'")
    long countPendingByOrganizationId(@Param("orgId") Long orgId);
```

- [ ] **Step 4: Add analytics endpoints to OrgAdminController**

Add these two endpoints to `back-end/src/main/java/gov/nist/oscal/tools/api/controller/OrgAdminController.java`, after the existing `resetPassword` method (around line 375):

First, add the service field injection. Add to the constructor parameters and field:

```java
    private final OrgAnalyticsService orgAnalyticsService;
```

Update the constructor to include `OrgAnalyticsService orgAnalyticsService` and assign `this.orgAnalyticsService = orgAnalyticsService;`.

Then add the endpoint methods:

```java
    // ========================================
    // Organization Analytics Endpoints
    // ========================================

    @GetMapping("/analytics/summary")
    public ResponseEntity<?> getAnalyticsSummary(@RequestParam Long organizationId) {
        try {
            Map<String, Object> summary = orgAnalyticsService.getAnalyticsSummary(organizationId);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(@RequestParam Long organizationId) {
        try {
            Map<String, Object> analytics = orgAnalyticsService.getFullAnalytics(organizationId);
            return ResponseEntity.ok(analytics);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
```

- [ ] **Step 5: Commit backend changes**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuditEventRepository.java \
       back-end/src/main/java/gov/nist/oscal/tools/api/repository/UserAccessRequestRepository.java \
       back-end/src/main/java/gov/nist/oscal/tools/api/service/OrgAnalyticsService.java \
       back-end/src/main/java/gov/nist/oscal/tools/api/controller/OrgAdminController.java
git commit -m "feat: add org-scoped analytics backend for org admin panel"
```

---

### Task 2: Frontend — API Client Methods

**Files:**
- Modify: `front-end/src/lib/api-client.ts` (add org-admin section before the closing `}` of the class, around line 4845)

- [ ] **Step 1: Add org admin API methods to api-client.ts**

Add this section before the closing `}` of the `ApiClient` class (line 4845, just before `private async mockResolveProfile`). Insert before the mock methods section:

```typescript
  // ========================================
  // Org Admin API Methods
  // ========================================

  async getOrgUsers(organizationId: number): Promise<Array<{
    userId: number;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
    status: string;
    joinedAt: string;
    updatedAt: string;
  }>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/users?organizationId=${organizationId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to get organization users');
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get organization users:', error);
      throw error;
    }
  }

  async lockOrgUser(organizationId: number, userId: number): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/users/${userId}/lock?organizationId=${organizationId}`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to lock user');
      }
    } catch (error) {
      console.error('Failed to lock user:', error);
      throw error;
    }
  }

  async unlockOrgUser(organizationId: number, userId: number): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/users/${userId}/unlock?organizationId=${organizationId}`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to unlock user');
      }
    } catch (error) {
      console.error('Failed to unlock user:', error);
      throw error;
    }
  }

  async deactivateOrgUser(organizationId: number, userId: number): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/users/${userId}/deactivate?organizationId=${organizationId}`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to deactivate user');
      }
    } catch (error) {
      console.error('Failed to deactivate user:', error);
      throw error;
    }
  }

  async reactivateOrgUser(organizationId: number, userId: number): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/users/${userId}/reactivate?organizationId=${organizationId}`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to reactivate user');
      }
    } catch (error) {
      console.error('Failed to reactivate user:', error);
      throw error;
    }
  }

  async resetOrgUserPassword(organizationId: number, userId: number): Promise<{ tempPassword: string; username: string; email: string }> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/users/${userId}/reset-password?organizationId=${organizationId}`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to reset password');
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to reset password:', error);
      throw error;
    }
  }

  async getOrgPendingRequests(organizationId: number): Promise<Array<{
    id: number;
    userId: number | null;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    organizationId: number;
    organizationName: string;
    status: string;
    message: string | null;
    requestDate: string;
    reviewedBy: number | null;
    reviewedByUsername: string | null;
    reviewedDate: string | null;
    notes: string | null;
  }>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/access-requests?organizationId=${organizationId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get pending requests: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get pending requests:', error);
      throw error;
    }
  }

  async getOrgAllRequests(organizationId: number): Promise<Array<{
    id: number;
    userId: number | null;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    organizationId: number;
    organizationName: string;
    status: string;
    message: string | null;
    requestDate: string;
    reviewedBy: number | null;
    reviewedByUsername: string | null;
    reviewedDate: string | null;
    notes: string | null;
  }>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/access-requests/all?organizationId=${organizationId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get access requests: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get access requests:', error);
      throw error;
    }
  }

  async approveOrgRequest(requestId: number, notes?: string): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/access-requests/${requestId}/approve`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify({ notes }),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to approve access request');
      }
    } catch (error) {
      console.error('Failed to approve access request:', error);
      throw error;
    }
  }

  async rejectOrgRequest(requestId: number, notes?: string): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/access-requests/${requestId}/reject`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify({ notes }),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to reject access request');
      }
    } catch (error) {
      console.error('Failed to reject access request:', error);
      throw error;
    }
  }

  async getOrgAnalytics(organizationId: number): Promise<{
    activeUsersLast7Days: number;
    totalLoginsLast30Days: number;
    totalOperationsLast30Days: number;
    failedLoginsLast30Days: number;
    loginsPerDay: Array<{ date: string; count: number }>;
    operationsByType: Array<{ name: string; count: number }>;
    topUsers: Array<{ username: string; operationCount: number }>;
  }> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/analytics?organizationId=${organizationId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Failed to get org analytics: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get org analytics:', error);
      throw error;
    }
  }

  async getOrgAnalyticsSummary(organizationId: number): Promise<{
    totalMembers: number;
    pendingRequests: number;
    loginsThisMonth: number;
    operationsThisMonth: number;
  }> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/analytics/summary?organizationId=${organizationId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get org analytics summary: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get org analytics summary:', error);
      throw error;
    }
  }
```

- [ ] **Step 2: Commit**

```bash
git add front-end/src/lib/api-client.ts
git commit -m "feat: add org admin API client methods"
```

---

### Task 3: Frontend — Org Admin Dashboard Page

**Files:**
- Create: `front-end/src/app/org-admin/page.tsx`

- [ ] **Step 1: Create the org-admin directory**

```bash
mkdir -p front-end/src/app/org-admin
```

- [ ] **Step 2: Create the dashboard page**

Create `front-end/src/app/org-admin/page.tsx`:

```tsx
'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Users, ClipboardCheck, LogIn, Activity, ChevronRight, Loader2 } from 'lucide-react';
import { apiClient } from '@/lib/api-client';

interface DashboardSummary {
  totalMembers: number;
  pendingRequests: number;
  loginsThisMonth: number;
  operationsThisMonth: number;
}

export default function OrgAdminDashboardPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [orgName, setOrgName] = useState('');

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
      router.push('/login');
      return;
    }

    try {
      const userData = JSON.parse(storedUser);
      const isOrgAdmin = userData.orgRole === 'ORG_ADMIN';
      const isSuperAdmin = userData.globalRole === 'SUPER_ADMIN';

      if (!isOrgAdmin && !isSuperAdmin) {
        router.push('/');
        return;
      }

      const currentOrg = localStorage.getItem('currentOrganization');
      if (currentOrg) {
        const orgData = JSON.parse(currentOrg);
        setOrgName(orgData.name || '');
        fetchSummary(orgData.id);
      } else if (userData.organizationId) {
        setOrgName(userData.organizationName || '');
        fetchSummary(userData.organizationId);
      } else {
        setError('No organization selected');
        setLoading(false);
      }
    } catch {
      router.push('/login');
    }
  }, [router]);

  const fetchSummary = async (organizationId: number) => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiClient.getOrgAnalyticsSummary(organizationId);
      setSummary(data);
    } catch (err) {
      console.error('Failed to fetch summary:', err);
      setError('Failed to load dashboard data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-6xl mx-auto">
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 text-red-700 dark:text-red-300">
            {error}
          </div>
        </div>
      </div>
    );
  }

  const statCards = [
    { label: 'Total Members', value: summary?.totalMembers ?? 0, icon: Users, color: 'blue' },
    { label: 'Pending Requests', value: summary?.pendingRequests ?? 0, icon: ClipboardCheck, color: 'yellow', badge: true },
    { label: 'Logins This Month', value: summary?.loginsThisMonth ?? 0, icon: LogIn, color: 'green' },
    { label: 'Operations This Month', value: summary?.operationsThisMonth ?? 0, icon: Activity, color: 'purple' },
  ];

  const colorMap: Record<string, { bg: string; iconBg: string; text: string }> = {
    blue: { bg: 'bg-blue-50 dark:bg-blue-900/20', iconBg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-600 dark:text-blue-400' },
    yellow: { bg: 'bg-yellow-50 dark:bg-yellow-900/20', iconBg: 'bg-yellow-100 dark:bg-yellow-900/30', text: 'text-yellow-600 dark:text-yellow-400' },
    green: { bg: 'bg-green-50 dark:bg-green-900/20', iconBg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-600 dark:text-green-400' },
    purple: { bg: 'bg-purple-50 dark:bg-purple-900/20', iconBg: 'bg-purple-100 dark:bg-purple-900/30', text: 'text-purple-600 dark:text-purple-400' },
  };

  const quickLinks = [
    { label: 'Manage Users', description: 'View and manage organization members', href: '/org-admin/users', icon: Users, color: 'blue' },
    { label: 'Access Requests', description: 'Review pending access requests', href: '/org-admin/requests', icon: ClipboardCheck, color: 'green' },
    { label: 'Usage Analytics', description: 'View organization usage metrics', href: '/org-admin/analytics', icon: Activity, color: 'purple' },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="text-center mb-12">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
            Organization Admin
          </h1>
          <p className="mt-2 text-gray-600 dark:text-gray-400">
            {orgName ? `Managing ${orgName}` : 'Manage your organization'}
          </p>
        </div>

        {/* Stat Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {statCards.map((card) => {
            const colors = colorMap[card.color];
            const Icon = card.icon;
            return (
              <div
                key={card.label}
                className="bg-white dark:bg-gray-800 rounded-lg shadow-md border border-gray-200 dark:border-gray-700 p-6"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{card.label}</p>
                    <div className="flex items-center gap-2">
                      <p className="text-2xl font-bold text-gray-900 dark:text-white">{card.value}</p>
                      {card.badge && card.value > 0 && (
                        <span className="bg-red-100 dark:bg-red-900/50 text-red-800 dark:text-red-200 text-xs font-medium px-2 py-0.5 rounded-full">
                          New
                        </span>
                      )}
                    </div>
                  </div>
                  <div className={`p-3 rounded-lg ${colors.iconBg}`}>
                    <Icon className={`h-6 w-6 ${colors.text}`} />
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Quick Links */}
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {quickLinks.map((link) => {
            const colors = colorMap[link.color];
            const Icon = link.icon;
            return (
              <button
                key={link.label}
                onClick={() => router.push(link.href)}
                className="group bg-white dark:bg-gray-800 rounded-lg shadow-md hover:shadow-lg transition-all duration-200 p-8 text-left border border-gray-200 dark:border-gray-700 hover:border-blue-500 dark:hover:border-blue-400"
              >
                <div className={`flex items-center justify-center w-16 h-16 ${colors.iconBg} rounded-lg mb-6 group-hover:bg-blue-200 dark:group-hover:bg-blue-900/50 transition-colors`}>
                  <Icon className={`h-8 w-8 ${colors.text}`} />
                </div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">{link.label}</h3>
                <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">{link.description}</p>
                <div className="flex items-center text-blue-500 text-sm font-medium">
                  Go to {link.label.toLowerCase()}
                  <ChevronRight className="h-4 w-4 ml-1" />
                </div>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/org-admin/page.tsx
git commit -m "feat: add org admin dashboard page"
```

---

### Task 4: Frontend — Org Admin Users Page

**Files:**
- Create: `front-end/src/app/org-admin/users/page.tsx`

- [ ] **Step 1: Create the users directory**

```bash
mkdir -p front-end/src/app/org-admin/users
```

- [ ] **Step 2: Create the users page**

Create `front-end/src/app/org-admin/users/page.tsx`:

```tsx
'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ChevronLeft, Search, Loader2, Lock, Unlock, UserX, UserCheck, KeyRound } from 'lucide-react';
import { apiClient } from '@/lib/api-client';

interface OrgUser {
  userId: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  status: string;
  joinedAt: string;
  updatedAt: string;
}

export default function OrgAdminUsersPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [users, setUsers] = useState<OrgUser[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [roleFilter, setRoleFilter] = useState<string>('ALL');
  const [processingUserId, setProcessingUserId] = useState<number | null>(null);
  const [organizationId, setOrganizationId] = useState<number | null>(null);
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [tempPasswordData, setTempPasswordData] = useState<{ tempPassword: string; username: string; email: string } | null>(null);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
      router.push('/login');
      return;
    }

    try {
      const userData = JSON.parse(storedUser);
      const isOrgAdmin = userData.orgRole === 'ORG_ADMIN';
      const isSuperAdmin = userData.globalRole === 'SUPER_ADMIN';

      if (!isOrgAdmin && !isSuperAdmin) {
        router.push('/');
        return;
      }

      const currentOrg = localStorage.getItem('currentOrganization');
      let orgId: number | null = null;
      if (currentOrg) {
        orgId = JSON.parse(currentOrg).id;
      } else if (userData.organizationId) {
        orgId = userData.organizationId;
      }

      if (orgId) {
        setOrganizationId(orgId);
        loadUsers(orgId);
      } else {
        setError('No organization selected');
        setLoading(false);
      }
    } catch {
      router.push('/login');
    }
  }, [router]);

  const loadUsers = async (orgId: number) => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiClient.getOrgUsers(orgId);
      setUsers(data);
    } catch (err) {
      console.error('Failed to load users:', err);
      setError(err instanceof Error ? err.message : 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const handleAction = async (userId: number, action: 'lock' | 'unlock' | 'deactivate' | 'reactivate') => {
    if (!organizationId) return;
    try {
      setProcessingUserId(userId);
      setError(null);
      setSuccess(null);

      switch (action) {
        case 'lock':
          await apiClient.lockOrgUser(organizationId, userId);
          setSuccess('User locked successfully');
          break;
        case 'unlock':
          await apiClient.unlockOrgUser(organizationId, userId);
          setSuccess('User unlocked successfully');
          break;
        case 'deactivate':
          await apiClient.deactivateOrgUser(organizationId, userId);
          setSuccess('User deactivated successfully');
          break;
        case 'reactivate':
          await apiClient.reactivateOrgUser(organizationId, userId);
          setSuccess('User reactivated successfully');
          break;
      }

      await loadUsers(organizationId);
    } catch (err) {
      console.error(`Failed to ${action} user:`, err);
      setError(err instanceof Error ? err.message : `Failed to ${action} user`);
    } finally {
      setProcessingUserId(null);
    }
  };

  const handleResetPassword = async (userId: number) => {
    if (!organizationId) return;
    try {
      setProcessingUserId(userId);
      setError(null);
      setSuccess(null);
      const result = await apiClient.resetOrgUserPassword(organizationId, userId);
      setTempPasswordData(result);
      setShowPasswordModal(true);
    } catch (err) {
      console.error('Failed to reset password:', err);
      setError(err instanceof Error ? err.message : 'Failed to reset password');
    } finally {
      setProcessingUserId(null);
    }
  };

  const filteredUsers = users.filter((user) => {
    const matchesSearch =
      searchQuery === '' ||
      user.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
      `${user.firstName} ${user.lastName}`.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || user.status === statusFilter;
    const matchesRole = roleFilter === 'ALL' || user.role === roleFilter;
    return matchesSearch && matchesStatus && matchesRole;
  });

  const statusBadge = (status: string) => {
    const styles: Record<string, string> = {
      ACTIVE: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
      LOCKED: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
      DEACTIVATED: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
    };
    return (
      <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${styles[status] || 'bg-gray-100 text-gray-800'}`}>
        {status}
      </span>
    );
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        {/* Back link */}
        <button onClick={() => router.push('/org-admin')} className="flex items-center text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-6">
          <ChevronLeft className="h-4 w-4 mr-1" />
          Back to Dashboard
        </button>

        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Manage Users</h1>
        <p className="text-gray-600 dark:text-gray-400 mb-8">View and manage organization members</p>

        {/* Alerts */}
        {error && (
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-4 text-red-700 dark:text-red-300">
            {error}
          </div>
        )}
        {success && (
          <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4 mb-4 text-green-700 dark:text-green-300">
            {success}
          </div>
        )}

        {/* Filters */}
        <div className="flex flex-col sm:flex-row gap-4 mb-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search by name, email, or username..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
          >
            <option value="ALL">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="LOCKED">Locked</option>
            <option value="DEACTIVATED">Deactivated</option>
          </select>
          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
          >
            <option value="ALL">All Roles</option>
            <option value="ORG_ADMIN">Admin</option>
            <option value="USER">User</option>
          </select>
        </div>

        {/* Users Table */}
        {filteredUsers.length === 0 ? (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-12 text-center">
            <p className="text-gray-500 dark:text-gray-400">No users found matching your filters.</p>
          </div>
        ) : (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Name</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Email</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Role</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Status</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Joined</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                  {filteredUsers.map((user) => (
                    <tr key={user.userId} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900 dark:text-white">
                          {user.firstName && user.lastName ? `${user.firstName} ${user.lastName}` : user.username}
                        </div>
                        <div className="text-sm text-gray-500 dark:text-gray-400">{user.username}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">{user.email}</td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm">
                        <span className={user.role === 'ORG_ADMIN' ? 'text-blue-600 dark:text-blue-400 font-medium' : 'text-gray-500 dark:text-gray-400'}>
                          {user.role === 'ORG_ADMIN' ? 'Admin' : 'User'}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">{statusBadge(user.status)}</td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                        {user.joinedAt ? new Date(user.joinedAt).toLocaleDateString() : '-'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right">
                        <div className="flex items-center justify-end gap-2">
                          {user.status === 'ACTIVE' && (
                            <button
                              onClick={() => handleAction(user.userId, 'lock')}
                              disabled={processingUserId === user.userId}
                              className="p-1.5 text-yellow-600 hover:bg-yellow-50 dark:hover:bg-yellow-900/20 rounded-md"
                              title="Lock User"
                            >
                              <Lock className="h-4 w-4" />
                            </button>
                          )}
                          {user.status === 'LOCKED' && (
                            <button
                              onClick={() => handleAction(user.userId, 'unlock')}
                              disabled={processingUserId === user.userId}
                              className="p-1.5 text-green-600 hover:bg-green-50 dark:hover:bg-green-900/20 rounded-md"
                              title="Unlock User"
                            >
                              <Unlock className="h-4 w-4" />
                            </button>
                          )}
                          {(user.status === 'ACTIVE' || user.status === 'LOCKED') && (
                            <button
                              onClick={() => handleAction(user.userId, 'deactivate')}
                              disabled={processingUserId === user.userId}
                              className="p-1.5 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-md"
                              title="Deactivate User"
                            >
                              <UserX className="h-4 w-4" />
                            </button>
                          )}
                          {user.status === 'DEACTIVATED' && (
                            <button
                              onClick={() => handleAction(user.userId, 'reactivate')}
                              disabled={processingUserId === user.userId}
                              className="p-1.5 text-green-600 hover:bg-green-50 dark:hover:bg-green-900/20 rounded-md"
                              title="Reactivate User"
                            >
                              <UserCheck className="h-4 w-4" />
                            </button>
                          )}
                          <button
                            onClick={() => handleResetPassword(user.userId)}
                            disabled={processingUserId === user.userId}
                            className="p-1.5 text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-md"
                            title="Reset Password"
                          >
                            <KeyRound className="h-4 w-4" />
                          </button>
                          {processingUserId === user.userId && (
                            <Loader2 className="h-4 w-4 animate-spin text-gray-400" />
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Temp Password Modal */}
        {showPasswordModal && tempPasswordData && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Password Reset</h3>
              <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                A temporary password has been generated for <strong>{tempPasswordData.username}</strong> ({tempPasswordData.email}).
              </p>
              <div className="bg-gray-100 dark:bg-gray-700 rounded-md p-3 mb-4">
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">Temporary Password:</p>
                <p className="text-lg font-mono font-bold text-gray-900 dark:text-white select-all">{tempPasswordData.tempPassword}</p>
              </div>
              <p className="text-xs text-gray-500 dark:text-gray-400 mb-4">
                The user will be required to change this password on their next login.
              </p>
              <button
                onClick={() => { setShowPasswordModal(false); setTempPasswordData(null); }}
                className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              >
                Done
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/org-admin/users/page.tsx
git commit -m "feat: add org admin users management page"
```

---

### Task 5: Frontend — Org Admin Access Requests Page

**Files:**
- Create: `front-end/src/app/org-admin/requests/page.tsx`

- [ ] **Step 1: Create the requests directory**

```bash
mkdir -p front-end/src/app/org-admin/requests
```

- [ ] **Step 2: Create the access requests page**

Create `front-end/src/app/org-admin/requests/page.tsx`:

```tsx
'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ChevronLeft, Loader2, CheckCircle, XCircle, Clock } from 'lucide-react';
import { apiClient } from '@/lib/api-client';

interface AccessRequest {
  id: number;
  userId: number | null;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  organizationId: number;
  organizationName: string;
  status: string;
  message: string | null;
  requestDate: string;
  reviewedBy: number | null;
  reviewedByUsername: string | null;
  reviewedDate: string | null;
  notes: string | null;
}

export default function OrgAdminRequestsPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [requests, setRequests] = useState<AccessRequest[]>([]);
  const [activeTab, setActiveTab] = useState<'pending' | 'all'>('pending');
  const [organizationId, setOrganizationId] = useState<number | null>(null);
  const [reviewingRequest, setReviewingRequest] = useState<AccessRequest | null>(null);
  const [reviewAction, setReviewAction] = useState<'approve' | 'reject' | null>(null);
  const [reviewNotes, setReviewNotes] = useState('');
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
      router.push('/login');
      return;
    }

    try {
      const userData = JSON.parse(storedUser);
      const isOrgAdmin = userData.orgRole === 'ORG_ADMIN';
      const isSuperAdmin = userData.globalRole === 'SUPER_ADMIN';

      if (!isOrgAdmin && !isSuperAdmin) {
        router.push('/');
        return;
      }

      const currentOrg = localStorage.getItem('currentOrganization');
      let orgId: number | null = null;
      if (currentOrg) {
        orgId = JSON.parse(currentOrg).id;
      } else if (userData.organizationId) {
        orgId = userData.organizationId;
      }

      if (orgId) {
        setOrganizationId(orgId);
        loadRequests(orgId, 'pending');
      } else {
        setError('No organization selected');
        setLoading(false);
      }
    } catch {
      router.push('/login');
    }
  }, [router]);

  const loadRequests = async (orgId: number, tab: 'pending' | 'all') => {
    try {
      setLoading(true);
      setError(null);
      const data = tab === 'pending'
        ? await apiClient.getOrgPendingRequests(orgId)
        : await apiClient.getOrgAllRequests(orgId);
      setRequests(data);
    } catch (err) {
      console.error('Failed to load requests:', err);
      setError(err instanceof Error ? err.message : 'Failed to load requests');
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (tab: 'pending' | 'all') => {
    setActiveTab(tab);
    if (organizationId) {
      loadRequests(organizationId, tab);
    }
  };

  const handleReview = (request: AccessRequest, action: 'approve' | 'reject') => {
    setReviewingRequest(request);
    setReviewAction(action);
    setReviewNotes('');
  };

  const handleConfirmReview = async () => {
    if (!reviewingRequest || !reviewAction || !organizationId) return;

    try {
      setProcessing(true);
      setError(null);
      setSuccess(null);

      if (reviewAction === 'approve') {
        await apiClient.approveOrgRequest(reviewingRequest.id, reviewNotes || undefined);
        setSuccess(`Access request from ${reviewingRequest.firstName} ${reviewingRequest.lastName} approved`);
      } else {
        await apiClient.rejectOrgRequest(reviewingRequest.id, reviewNotes || undefined);
        setSuccess(`Access request from ${reviewingRequest.firstName} ${reviewingRequest.lastName} rejected`);
      }

      setReviewingRequest(null);
      setReviewAction(null);
      setReviewNotes('');
      await loadRequests(organizationId, activeTab);
    } catch (err) {
      console.error('Failed to process request:', err);
      setError(err instanceof Error ? err.message : 'Failed to process request');
    } finally {
      setProcessing(false);
    }
  };

  const pendingCount = requests.filter((r) => r.status === 'PENDING').length;

  const statusBadge = (status: string) => {
    const styles: Record<string, { className: string; icon: typeof Clock }> = {
      PENDING: { className: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200', icon: Clock },
      APPROVED: { className: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200', icon: CheckCircle },
      REJECTED: { className: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200', icon: XCircle },
    };
    const style = styles[status] || styles.PENDING;
    const Icon = style.icon;
    return (
      <span className={`inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full ${style.className}`}>
        <Icon className="h-3 w-3" />
        {status}
      </span>
    );
  };

  if (loading && requests.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        {/* Back link */}
        <button onClick={() => router.push('/org-admin')} className="flex items-center text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-6">
          <ChevronLeft className="h-4 w-4 mr-1" />
          Back to Dashboard
        </button>

        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Access Requests</h1>
        <p className="text-gray-600 dark:text-gray-400 mb-8">Review and manage access requests for your organization</p>

        {/* Alerts */}
        {error && (
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-4 text-red-700 dark:text-red-300">
            {error}
          </div>
        )}
        {success && (
          <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4 mb-4 text-green-700 dark:text-green-300">
            {success}
          </div>
        )}

        {/* Tabs */}
        <div className="border-b border-gray-200 dark:border-gray-700 mb-6">
          <nav className="flex space-x-8">
            <button
              onClick={() => handleTabChange('pending')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'pending'
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              Pending
              {activeTab === 'pending' && pendingCount > 0 && (
                <span className="ml-2 bg-red-100 dark:bg-red-900/50 text-red-800 dark:text-red-200 text-xs font-medium px-2 py-0.5 rounded-full">
                  {pendingCount}
                </span>
              )}
            </button>
            <button
              onClick={() => handleTabChange('all')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'all'
                  ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
              }`}
            >
              All Requests
            </button>
          </nav>
        </div>

        {/* Request Cards */}
        {requests.length === 0 ? (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-12 text-center">
            <p className="text-gray-500 dark:text-gray-400">
              {activeTab === 'pending' ? 'No pending access requests.' : 'No access requests found.'}
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {requests.map((request) => (
              <div
                key={request.id}
                className="bg-white dark:bg-gray-800 rounded-lg shadow-md border border-gray-200 dark:border-gray-700 p-6"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="text-lg font-medium text-gray-900 dark:text-white">
                        {request.firstName} {request.lastName}
                      </h3>
                      {statusBadge(request.status)}
                    </div>
                    <div className="space-y-1 text-sm text-gray-500 dark:text-gray-400">
                      <p>Email: {request.email}</p>
                      {request.username && <p>Username: {request.username}</p>}
                      <p>Requested: {new Date(request.requestDate).toLocaleString()}</p>
                    </div>
                    {request.message && (
                      <div className="mt-3 p-3 bg-gray-50 dark:bg-gray-700 rounded-md">
                        <p className="text-sm text-gray-600 dark:text-gray-300">
                          <span className="font-medium">Message:</span> {request.message}
                        </p>
                      </div>
                    )}
                    {request.notes && (
                      <div className="mt-3 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-md">
                        <p className="text-sm text-blue-700 dark:text-blue-300">
                          <span className="font-medium">Admin Notes:</span> {request.notes}
                        </p>
                        {request.reviewedByUsername && (
                          <p className="text-xs text-blue-500 dark:text-blue-400 mt-1">
                            Reviewed by {request.reviewedByUsername} on {request.reviewedDate ? new Date(request.reviewedDate).toLocaleString() : '-'}
                          </p>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Actions for pending requests */}
                  {request.status === 'PENDING' && (
                    <div className="flex gap-2 ml-4">
                      <button
                        onClick={() => handleReview(request, 'approve')}
                        className="px-4 py-2 bg-green-600 text-white text-sm font-medium rounded-md hover:bg-green-700"
                      >
                        Approve
                      </button>
                      <button
                        onClick={() => handleReview(request, 'reject')}
                        className="px-4 py-2 bg-red-600 text-white text-sm font-medium rounded-md hover:bg-red-700"
                      >
                        Reject
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Review Modal */}
        {reviewingRequest && reviewAction && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full mx-4">
              <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                  {reviewAction === 'approve' ? 'Approve' : 'Reject'} Access Request
                </h3>
              </div>
              <div className="px-6 py-4">
                <div className="space-y-2 text-sm mb-4">
                  <p><span className="font-medium text-gray-700 dark:text-gray-300">Name:</span> <span className="text-gray-600 dark:text-gray-400">{reviewingRequest.firstName} {reviewingRequest.lastName}</span></p>
                  <p><span className="font-medium text-gray-700 dark:text-gray-300">Email:</span> <span className="text-gray-600 dark:text-gray-400">{reviewingRequest.email}</span></p>
                  {reviewingRequest.message && (
                    <p><span className="font-medium text-gray-700 dark:text-gray-300">Message:</span> <span className="text-gray-600 dark:text-gray-400">{reviewingRequest.message}</span></p>
                  )}
                </div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Notes {reviewAction === 'reject' ? '(recommended)' : '(optional)'}
                </label>
                <textarea
                  value={reviewNotes}
                  onChange={(e) => setReviewNotes(e.target.value)}
                  rows={3}
                  placeholder={reviewAction === 'reject' ? 'Reason for rejection...' : 'Optional notes...'}
                  className="w-full px-4 py-2 border border-gray-300 dark:border-gray-700 rounded-md bg-white dark:bg-gray-900 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3">
                <button
                  onClick={() => { setReviewingRequest(null); setReviewAction(null); setReviewNotes(''); }}
                  disabled={processing}
                  className="px-4 py-2 text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 rounded-md hover:bg-gray-200 dark:hover:bg-gray-600"
                >
                  Cancel
                </button>
                <button
                  onClick={handleConfirmReview}
                  disabled={processing}
                  className={`px-4 py-2 text-white rounded-md flex items-center gap-2 ${
                    reviewAction === 'approve'
                      ? 'bg-green-600 hover:bg-green-700'
                      : 'bg-red-600 hover:bg-red-700'
                  }`}
                >
                  {processing && <Loader2 className="h-4 w-4 animate-spin" />}
                  {reviewAction === 'approve' ? 'Approve' : 'Reject'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/org-admin/requests/page.tsx
git commit -m "feat: add org admin access requests page"
```

---

### Task 6: Frontend — Org Admin Analytics Page

**Files:**
- Create: `front-end/src/app/org-admin/analytics/page.tsx`

- [ ] **Step 1: Create the analytics directory**

```bash
mkdir -p front-end/src/app/org-admin/analytics
```

- [ ] **Step 2: Create the analytics page**

Create `front-end/src/app/org-admin/analytics/page.tsx`:

```tsx
'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ChevronLeft, Loader2, Users, LogIn, Activity, ShieldAlert, RefreshCw } from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell
} from 'recharts';
import { apiClient } from '@/lib/api-client';

interface OrgAnalytics {
  activeUsersLast7Days: number;
  totalLoginsLast30Days: number;
  totalOperationsLast30Days: number;
  failedLoginsLast30Days: number;
  loginsPerDay: Array<{ date: string; count: number }>;
  operationsByType: Array<{ name: string; count: number }>;
  topUsers: Array<{ username: string; operationCount: number }>;
}

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#06B6D4', '#EC4899', '#F97316'];

export default function OrgAdminAnalyticsPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [analytics, setAnalytics] = useState<OrgAnalytics | null>(null);
  const [organizationId, setOrganizationId] = useState<number | null>(null);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
      router.push('/login');
      return;
    }

    try {
      const userData = JSON.parse(storedUser);
      const isOrgAdmin = userData.orgRole === 'ORG_ADMIN';
      const isSuperAdmin = userData.globalRole === 'SUPER_ADMIN';

      if (!isOrgAdmin && !isSuperAdmin) {
        router.push('/');
        return;
      }

      const currentOrg = localStorage.getItem('currentOrganization');
      let orgId: number | null = null;
      if (currentOrg) {
        orgId = JSON.parse(currentOrg).id;
      } else if (userData.organizationId) {
        orgId = userData.organizationId;
      }

      if (orgId) {
        setOrganizationId(orgId);
        fetchAnalytics(orgId, false);
      } else {
        setError('No organization selected');
        setLoading(false);
      }
    } catch {
      router.push('/login');
    }
  }, [router]);

  const fetchAnalytics = async (orgId: number, showRefreshIndicator: boolean) => {
    try {
      if (showRefreshIndicator) {
        setRefreshing(true);
      }
      setError(null);
      const data = await apiClient.getOrgAnalytics(orgId);
      setAnalytics(data);
    } catch (err) {
      console.error('Failed to fetch analytics:', err);
      setError('Failed to load analytics data. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return `${date.getMonth() + 1}/${date.getDate()}`;
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
      </div>
    );
  }

  const statCards = [
    { label: 'Active Users (7d)', value: analytics?.activeUsersLast7Days ?? 0, icon: Users, color: 'blue' },
    { label: 'Total Logins (30d)', value: analytics?.totalLoginsLast30Days ?? 0, icon: LogIn, color: 'green' },
    { label: 'Operations (30d)', value: analytics?.totalOperationsLast30Days ?? 0, icon: Activity, color: 'purple' },
    { label: 'Failed Logins (30d)', value: analytics?.failedLoginsLast30Days ?? 0, icon: ShieldAlert, color: 'red' },
  ];

  const colorMap: Record<string, string> = {
    blue: 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400',
    green: 'bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400',
    purple: 'bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400',
    red: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <button onClick={() => router.push('/org-admin')} className="flex items-center text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white mb-2">
              <ChevronLeft className="h-4 w-4 mr-1" />
              Back to Dashboard
            </button>
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Usage Analytics</h1>
            <p className="text-gray-600 dark:text-gray-400">Organization usage metrics for the last 30 days</p>
          </div>
          <button
            onClick={() => organizationId && fetchAnalytics(organizationId, true)}
            disabled={refreshing}
            className="flex items-center gap-2 px-4 py-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
          >
            <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>

        {error && (
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-6 text-red-700 dark:text-red-300">
            {error}
          </div>
        )}

        {/* Stat Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {statCards.map((card) => {
            const Icon = card.icon;
            return (
              <div key={card.label} className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{card.label}</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">{card.value}</p>
                  </div>
                  <div className={`p-3 rounded-lg ${colorMap[card.color]}`}>
                    <Icon className="h-6 w-6" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Charts Row */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          {/* Login Activity */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Login Activity</h2>
            <div className="h-64">
              {analytics?.loginsPerDay && analytics.loginsPerDay.length > 0 ? (
                <ResponsiveContainer width="100%" height={250}>
                  <BarChart data={analytics.loginsPerDay}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#374151" opacity={0.3} />
                    <XAxis dataKey="date" tickFormatter={formatDate} stroke="#9CA3AF" fontSize={12} />
                    <YAxis stroke="#9CA3AF" fontSize={12} />
                    <Tooltip
                      contentStyle={{ backgroundColor: '#1F2937', border: 'none', borderRadius: '8px', color: '#F9FAFB' }}
                      labelFormatter={(label) => new Date(label).toLocaleDateString()}
                    />
                    <Bar dataKey="count" fill="#3B82F6" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-gray-400">No login data available</div>
              )}
            </div>
          </div>

          {/* Operations by Type */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Operations by Type</h2>
            <div className="h-64">
              {analytics?.operationsByType && analytics.operationsByType.length > 0 ? (
                <ResponsiveContainer width="100%" height={250}>
                  <PieChart>
                    <Pie
                      data={analytics.operationsByType}
                      cx="50%"
                      cy="50%"
                      outerRadius={90}
                      dataKey="count"
                      nameKey="name"
                      label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                      labelLine={true}
                    >
                      {analytics.operationsByType.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip
                      contentStyle={{ backgroundColor: '#1F2937', border: 'none', borderRadius: '8px', color: '#F9FAFB' }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-gray-400">No operation data available</div>
              )}
            </div>
          </div>
        </div>

        {/* Top Users Table */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700">
          <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Most Active Users</h2>
          </div>
          {analytics?.topUsers && analytics.topUsers.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Rank</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Username</th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">Operations</th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                  {analytics.topUsers.map((user, index) => (
                    <tr key={user.username} className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">#{index + 1}</td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-white">{user.username}</td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-900 dark:text-white">{user.operationCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-12 text-center text-gray-400">No activity data available</div>
          )}
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/org-admin/analytics/page.tsx
git commit -m "feat: add org admin analytics page with charts"
```

---

### Task 7: Frontend — Navigation Update

**Files:**
- Modify: `front-end/src/components/Navigation.tsx`

- [ ] **Step 1: Update Navigation.tsx to add Org Admin link**

In `front-end/src/components/Navigation.tsx`, make these changes:

1. Add `Shield` to the lucide-react import (line 7):

Change:
```tsx
import { User, LogOut, Settings } from 'lucide-react';
```
To:
```tsx
import { User, LogOut, Settings, Shield } from 'lucide-react';
```

2. Add `isOrgAdminUser` state alongside `isSuperAdminUser` (line 13):

After line 13 (`const [isSuperAdminUser, setIsSuperAdminUser] = useState(false);`), add:
```tsx
  const [isOrgAdminUser, setIsOrgAdminUser] = useState(false);
```

3. In the first `useEffect` (lines 16-27), after `setIsSuperAdminUser(userData.globalRole === 'SUPER_ADMIN');` (line 22), add:
```tsx
        setIsOrgAdminUser(userData.orgRole === 'ORG_ADMIN');
```

4. In the second `useEffect` (lines 30-34), after `setIsSuperAdminUser(user.globalRole === 'SUPER_ADMIN');` (line 32), add:
```tsx
      setIsOrgAdminUser(user.orgRole === 'ORG_ADMIN');
```

5. After the `isSuperAdmin` function (line 39), add:
```tsx
  const isOrgAdmin = () => {
    return isOrgAdminUser || user?.orgRole === 'ORG_ADMIN';
  };
```

6. After the OrganizationSwitcher line (`{!isSuperAdmin() && <OrganizationSwitcher />}`, line 62), add:
```tsx
                {!isSuperAdmin() && isOrgAdmin() && (
                  <Link href="/org-admin">
                    <Button
                      variant="outline"
                      size="sm"
                      className="flex items-center space-x-2"
                      title="Org Admin"
                      aria-label="Organization Admin Panel"
                    >
                      <Shield className="h-4 w-4" aria-hidden="true" />
                    </Button>
                  </Link>
                )}
```

- [ ] **Step 2: Commit**

```bash
git add front-end/src/components/Navigation.tsx
git commit -m "feat: add org admin nav link for ORG_ADMIN users"
```

---

### Task 8: Verify and Final Commit

- [ ] **Step 1: Verify all files exist**

```bash
ls -la front-end/src/app/org-admin/page.tsx \
       front-end/src/app/org-admin/users/page.tsx \
       front-end/src/app/org-admin/requests/page.tsx \
       front-end/src/app/org-admin/analytics/page.tsx \
       back-end/src/main/java/gov/nist/oscal/tools/api/service/OrgAnalyticsService.java
```

Expected: All 5 files listed with sizes > 0.

- [ ] **Step 2: Verify no TypeScript syntax errors in frontend files**

```bash
cd front-end && npx tsc --noEmit --pretty 2>&1 | head -30
```

If there are type errors, fix them in the relevant files.

- [ ] **Step 3: Inform user to rebuild**

Tell the user: "All changes complete. Please rebuild the backend (`cd back-end && mvn clean install -DskipTests`) and frontend (`cd front-end && npm run build`) to test the new org admin panel at `/org-admin/`."
