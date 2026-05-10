# Authorization ACL Grants + Tabbed Detail Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add per-authorization role-based access control (`OWNER`/`EDITOR`/`CONTRIBUTOR`/`VIEWER` grants + `share_with_org_default_role` convenience), wire the role matrix into all write paths, expose grant management endpoints, and refactor the authorization detail page into a tabbed layout (`Overview` / `Continuous Monitoring` / `Documents`) with a Sharing & Access UI on the Overview tab. **PR 2 of 4** in the broader Authorizations expansion (see `docs/superpowers/specs/2026-05-06-conmon-and-documents-design.md`). The new ConMon and Documents tabs are stub placeholders here; they get filled in PRs 4 and 3 respectively.

**Architecture:** A new `AuthorizationAccessGuard` service sits alongside the existing `AuthorizationOrgContext` (built in PR 1). The guard answers "what is user U's effective role on authorization A?" by checking SUPER_ADMIN bypass → ORG_ADMIN-of-this-org bypass → creator (`authorizedBy`) auto-OWNER → explicit `AuthorizationGrant` row → `share_with_org_default_role` fallback. Service-layer write methods consult the guard before allowing edits/deletes. The frontend detail page wraps existing sections in `<Tabs>` from shadcn (already in the project); the Overview tab gets the existing content plus a new Sharing & Access card. ConMon and Documents tabs are empty placeholders rendering "Coming soon — see PR 3/4."

**Tech Stack:** Spring Boot 4.0.6, Spring Data JPA, Flyway, PostgreSQL, JUnit 5 + Mockito, Spring Security `@PreAuthorize` / `Principal`, Next.js (App Router), shadcn UI primitives (Tabs, Select, Card, Dialog), sonner toasts.

---

## Important Conventions for This PR

- **Working tree caveat:** the user has many unrelated uncommitted modifications (controllers, email service, frontend pages, etc.) plus the freshly-committed PR 1 commits and the V1.7 health-test cleanup commit `5daaecc`. **Every commit in this plan must stage files explicitly by path.** NEVER use `git add -A` or `git add .`. After each `git add`, run `git diff --cached --stat` to confirm the staged set is exactly what you intended; if anything else snuck in, `git restore --staged <file>` and retry.
- **Lombok caveat:** entities and DTOs use manual getters/setters. Match that style.
- **Spring Boot 4 caveat:** `@DataJpaTest` does not exist in Spring Boot 4. Use `@SpringBootTest` + `@Transactional` + `@PersistenceContext EntityManager` for repository integration tests, as established by the PR 1 `AuthorizationRepositoryOrgScopeTest`.
- **Pre-existing test breakage was resolved** in commit `5daaecc` (the health-response slim-down). `mvn test-compile` should be clean now. If something is still broken at boot, that's new and worth investigating — don't paper over it.
- **`@PreAuthorize` vs the access guard:** `@PreAuthorize("hasRole('SUPER_ADMIN')")` checks Spring Security authorities derived from `User.globalRole` and `OrganizationMembership.role`. The new access guard performs *per-authorization* role resolution which Spring Security can't express. Both layers coexist: `@PreAuthorize` keeps gating at the coarse level (e.g., admin-only routes), and the guard kicks in at the fine-grained level inside service methods.
- **404 vs 403 leakage:** Out-of-org reads continue to return 404 (the existing pattern from PR 1). Insufficient-role writes within the same org return 403 with the role name in the body — leaking *existence* of the resource within the org is fine; leaking it across orgs is not.

---

## File Structure

**New backend files:**
- `back-end/src/main/resources/db/migration/V1.7__authorization_grants.sql`
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/AuthorizationRole.java` (top-level enum)
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/AuthorizationGrant.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuthorizationGrantRepository.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationAccessGuard.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/exception/InsufficientAuthorizationRoleException.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationGrantRequest.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationGrantResponse.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/ShareWithOrgRequest.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/OrgMemberResponse.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/controller/OrgMembersController.java` (lightweight: lets non-admin in-org users discover who's in their org for the picker)
- `back-end/src/test/java/gov/nist/oscal/tools/api/service/AuthorizationAccessGuardTest.java`

**Modified backend files:**
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/Authorization.java` — add `shareWithOrgDefaultRole` field + grants `@OneToMany`.
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationService.java` — wire access guard into update/delete; populate response role.
- `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationController.java` — 5 new grant endpoints + 1 share-with-org endpoint; populate `effectiveRole` on responses.
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationResponse.java` — add `effectiveRole` and `shareWithOrgDefaultRole`.
- `back-end/src/test/java/gov/nist/oscal/tools/api/controller/AuthorizationControllerTest.java` — patch for new dependencies + add tests for new endpoints.

**New frontend files:**
- `front-end/src/components/user-picker.tsx`
- `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/overview-tab.tsx`
- `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/conmon-tab.tsx` (stub)
- `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/documents-tab.tsx` (stub)
- `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/sharing-access-card.tsx`

**Modified frontend files:**
- `front-end/src/app/authorizations/authorization/[authorizationId]/page.tsx` — wraps existing sections in tabs, plumbs URL state for `?tab=`.
- `front-end/src/types/oscal.ts` — add `AuthorizationRole` type, `AuthorizationGrantResponse`, `OrgMemberResponse`, and extend `AuthorizationResponse` with `effectiveRole` + `shareWithOrgDefaultRole`.
- `front-end/src/lib/api-client.ts` — add `listGrants`, `addGrant`, `updateGrant`, `removeGrant`, `setShareWithOrg`, `listMyOrgMembers`.

---

## Task 1: Migration V1.7

**Files:**
- Create: `back-end/src/main/resources/db/migration/V1.7__authorization_grants.sql`

- [ ] **Step 1: Write the migration**

```sql
-- V1.7 — Per-authorization role-based ACL grants.
-- See docs/superpowers/specs/2026-05-06-conmon-and-documents-design.md.
-- Adds an authorization_grants table for explicit (user, role) entries
-- on a specific authorization, and a share_with_org_default_role column
-- on authorizations for the "share with whole org as VIEWER/CONTRIBUTOR/EDITOR"
-- convenience case (no fan-out — resolved at access-check time).

CREATE TABLE IF NOT EXISTS authorization_grants (
    id               BIGSERIAL PRIMARY KEY,
    authorization_id BIGINT NOT NULL REFERENCES authorizations(id) ON DELETE CASCADE,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role             VARCHAR(32) NOT NULL,
    granted_by       BIGINT NOT NULL REFERENCES users(id),
    granted_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_authorization_grants_user UNIQUE (authorization_id, user_id),
    CONSTRAINT ck_authorization_grants_role CHECK (role IN ('OWNER', 'EDITOR', 'CONTRIBUTOR', 'VIEWER'))
);

CREATE INDEX IF NOT EXISTS idx_authorization_grants_user
    ON authorization_grants (user_id);

CREATE INDEX IF NOT EXISTS idx_authorization_grants_auth
    ON authorization_grants (authorization_id);

ALTER TABLE authorizations
    ADD COLUMN IF NOT EXISTS share_with_org_default_role VARCHAR(32) NULL;

ALTER TABLE authorizations
    DROP CONSTRAINT IF EXISTS ck_authorizations_share_role;
ALTER TABLE authorizations
    ADD CONSTRAINT ck_authorizations_share_role
        CHECK (share_with_org_default_role IS NULL OR share_with_org_default_role IN ('VIEWER', 'CONTRIBUTOR', 'EDITOR'));
```

Note: `OWNER` is intentionally NOT permitted as a `share_with_org_default_role` — you cannot make every org member a co-owner.

- [ ] **Step 2: Verify the file syntactically**

Boot the back-end on a clean dev DB will exercise it. We don't run psql here. Confirmed-good migrations follow this pattern from `V1.6`.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/resources/db/migration/V1.7__authorization_grants.sql
git diff --cached --stat   # confirm exactly 1 file staged
git commit -m "db(authorizations): V1.7 add authorization_grants and share_with_org_default_role"
```

---

## Task 2: AuthorizationRole enum

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/AuthorizationRole.java`

- [ ] **Step 1: Write the enum**

```java
package gov.nist.oscal.tools.api.entity;

/**
 * Per-authorization role granted to a user.
 * <ul>
 *   <li>{@code OWNER} — full control: edit, share, delete.</li>
 *   <li>{@code EDITOR} — edit content, conditions, signature; cannot manage grants or delete.</li>
 *   <li>{@code CONTRIBUTOR} — upload ConMon / Documents; cannot edit core authorization.</li>
 *   <li>{@code VIEWER} — read-only.</li>
 * </ul>
 *
 * The creator (Authorization.authorizedBy) is implicitly OWNER. Org admins and
 * SUPER_ADMINs bypass this enum and are treated as effective OWNER.
 */
public enum AuthorizationRole {
    OWNER,
    EDITOR,
    CONTRIBUTOR,
    VIEWER;

    /**
     * Roles that may be assigned via the share-with-org convenience setting.
     * OWNER is intentionally excluded — see V1.7 CHECK constraint.
     */
    public static boolean isAssignableAsShareDefault(AuthorizationRole role) {
        return role == VIEWER || role == CONTRIBUTOR || role == EDITOR;
    }
}
```

- [ ] **Step 2: Compile**

```bash
cd back-end && mvn compile -DskipTests 2>&1 | tail -5
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/AuthorizationRole.java
git diff --cached --stat
git commit -m "feat(authorizations): add AuthorizationRole enum"
```

---

## Task 3: AuthorizationGrant entity

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/AuthorizationGrant.java`

- [ ] **Step 1: Write the entity**

```java
package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "authorization_grants",
       uniqueConstraints = @UniqueConstraint(name = "uq_authorization_grants_user",
                                             columnNames = {"authorization_id", "user_id"}))
public class AuthorizationGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "authorization_id", nullable = false)
    private Authorization authorization;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuthorizationRole role;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by", nullable = false)
    private User grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt = LocalDateTime.now();

    public AuthorizationGrant() {
    }

    public AuthorizationGrant(Authorization authorization, User user, AuthorizationRole role, User grantedBy) {
        this.authorization = authorization;
        this.user = user;
        this.role = role;
        this.grantedBy = grantedBy;
        this.grantedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Authorization getAuthorization() { return authorization; }
    public void setAuthorization(Authorization authorization) { this.authorization = authorization; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public AuthorizationRole getRole() { return role; }
    public void setRole(AuthorizationRole role) { this.role = role; }

    public User getGrantedBy() { return grantedBy; }
    public void setGrantedBy(User grantedBy) { this.grantedBy = grantedBy; }

    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
}
```

- [ ] **Step 2: Compile**

```bash
cd back-end && mvn compile -DskipTests 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/AuthorizationGrant.java
git diff --cached --stat
git commit -m "feat(authorizations): add AuthorizationGrant entity"
```

---

## Task 4: AuthorizationGrantRepository

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuthorizationGrantRepository.java`

- [ ] **Step 1: Write the repository**

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorizationGrantRepository extends JpaRepository<AuthorizationGrant, Long> {

    List<AuthorizationGrant> findByAuthorization(Authorization authorization);

    Optional<AuthorizationGrant> findByAuthorizationAndUser(Authorization authorization, User user);

    @Query("SELECT g FROM AuthorizationGrant g " +
           "WHERE g.user = :user AND g.authorization.organization.id = :organizationId")
    List<AuthorizationGrant> findByUserInOrganization(
            @Param("user") User user,
            @Param("organizationId") Long organizationId);

    void deleteByAuthorizationAndUser(Authorization authorization, User user);
}
```

- [ ] **Step 2: Compile**

```bash
cd back-end && mvn compile -DskipTests 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuthorizationGrantRepository.java
git diff --cached --stat
git commit -m "feat(authorizations): add AuthorizationGrantRepository"
```

---

## Task 5: Add shareWithOrgDefaultRole + grants to Authorization entity

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/Authorization.java`

- [ ] **Step 1: Add the field**

Open `Authorization.java`. Find the `organization` field (added in PR 1). Immediately after it, insert:

```java
    @Enumerated(EnumType.STRING)
    @Column(name = "share_with_org_default_role", length = 32)
    private AuthorizationRole shareWithOrgDefaultRole;

    @OneToMany(mappedBy = "authorization", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AuthorizationGrant> grants = new ArrayList<>();
```

(The `List` and `ArrayList` imports should already be present because `conditions` is already a `List`. Verify, add if missing.)

- [ ] **Step 2: Add getters/setters**

Append at the end of the existing accessor block (matching the manual style of every other field):

```java
    public AuthorizationRole getShareWithOrgDefaultRole() {
        return shareWithOrgDefaultRole;
    }

    public void setShareWithOrgDefaultRole(AuthorizationRole shareWithOrgDefaultRole) {
        this.shareWithOrgDefaultRole = shareWithOrgDefaultRole;
    }

    public List<AuthorizationGrant> getGrants() {
        return grants;
    }

    public void setGrants(List<AuthorizationGrant> grants) {
        this.grants = grants;
    }
```

- [ ] **Step 3: Compile**

```bash
cd back-end && mvn compile -DskipTests 2>&1 | tail -5
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/Authorization.java
git diff --cached --stat
git commit -m "feat(authorizations): wire shareWithOrgDefaultRole and grants on Authorization"
```

---

## Task 6: Build AuthorizationAccessGuard (TDD)

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/exception/InsufficientAuthorizationRoleException.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationAccessGuard.java`
- Create: `back-end/src/test/java/gov/nist/oscal/tools/api/service/AuthorizationAccessGuardTest.java`

The guard answers "what role does this user have on this authorization?" and provides `requireXxx(...)` helpers for write-paths.

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/service/AuthorizationAccessGuardTest.java`:

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.User.GlobalRole;
import gov.nist.oscal.tools.api.exception.InsufficientAuthorizationRoleException;
import gov.nist.oscal.tools.api.repository.AuthorizationGrantRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationAccessGuardTest {

    @Mock AuthorizationGrantRepository grantRepository;
    @Mock OrganizationMembershipRepository membershipRepository;

    @InjectMocks
    AuthorizationAccessGuard guard;

    Organization orgA;
    User creator;
    User otherInOrg;
    User outOfOrg;
    User superAdmin;
    User orgAdmin;
    Authorization auth;

    @BeforeEach
    void setUp() {
        orgA = new Organization(); orgA.setId(100L); orgA.setName("A");

        creator = newUser(1L, "alice");
        otherInOrg = newUser(2L, "bob");
        outOfOrg = newUser(3L, "carol");
        superAdmin = newUser(4L, "root"); superAdmin.setGlobalRole(GlobalRole.SUPER_ADMIN);
        orgAdmin = newUser(5L, "admin");

        auth = new Authorization();
        auth.setId(50L);
        auth.setOrganization(orgA);
        auth.setAuthorizedBy(creator);
    }

    private User newUser(Long id, String name) {
        User u = new User();
        u.setId(id);
        u.setUsername(name);
        u.setGlobalRole(GlobalRole.USER);
        return u;
    }

    private OrganizationMembership membership(User u, Organization o, OrganizationRole role, MembershipStatus status) {
        OrganizationMembership m = new OrganizationMembership();
        m.setId(1L);
        m.setUser(u);
        m.setOrganization(o);
        m.setRole(role);
        m.setStatus(status);
        return m;
    }

    private AuthorizationGrant grant(User u, AuthorizationRole role) {
        AuthorizationGrant g = new AuthorizationGrant();
        g.setAuthorization(auth);
        g.setUser(u);
        g.setRole(role);
        g.setGrantedBy(creator);
        return g;
    }

    private void inOrg(User u, OrganizationRole role) {
        when(membershipRepository.findByUserAndStatus(u, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership(u, orgA, role, MembershipStatus.ACTIVE)));
    }

    @Test
    void effectiveRole_superAdmin_returnsOwnerRegardlessOfMembership() {
        when(membershipRepository.findByUserAndStatus(superAdmin, MembershipStatus.ACTIVE))
                .thenReturn(List.of()); // not even in this org

        AuthorizationRole result = guard.effectiveRole(auth, superAdmin);

        assertThat(result).isEqualTo(AuthorizationRole.OWNER);
    }

    @Test
    void effectiveRole_orgAdminOfThisOrg_returnsOwner() {
        inOrg(orgAdmin, OrganizationRole.ORG_ADMIN);

        AuthorizationRole result = guard.effectiveRole(auth, orgAdmin);

        assertThat(result).isEqualTo(AuthorizationRole.OWNER);
    }

    @Test
    void effectiveRole_creator_returnsOwner() {
        inOrg(creator, OrganizationRole.USER);

        AuthorizationRole result = guard.effectiveRole(auth, creator);

        assertThat(result).isEqualTo(AuthorizationRole.OWNER);
    }

    @Test
    void effectiveRole_userWithExplicitGrant_returnsThatRole() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.EDITOR)));

        AuthorizationRole result = guard.effectiveRole(auth, otherInOrg);

        assertThat(result).isEqualTo(AuthorizationRole.EDITOR);
    }

    @Test
    void effectiveRole_userWithNoGrantButShareWithOrgSet_returnsDefaultRole() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg)).thenReturn(Optional.empty());
        auth.setShareWithOrgDefaultRole(AuthorizationRole.VIEWER);

        AuthorizationRole result = guard.effectiveRole(auth, otherInOrg);

        assertThat(result).isEqualTo(AuthorizationRole.VIEWER);
    }

    @Test
    void effectiveRole_userInOrgButNoGrantOrShareDefault_returnsNull() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg)).thenReturn(Optional.empty());
        // shareWithOrgDefaultRole is null

        AuthorizationRole result = guard.effectiveRole(auth, otherInOrg);

        assertThat(result).isNull();
    }

    @Test
    void effectiveRole_outOfOrgUser_returnsNull() {
        when(membershipRepository.findByUserAndStatus(outOfOrg, MembershipStatus.ACTIVE))
                .thenReturn(List.of()); // no membership in any org

        AuthorizationRole result = guard.effectiveRole(auth, outOfOrg);

        assertThat(result).isNull();
    }

    @Test
    void effectiveRole_userWithGrantAndAlsoSharedOrg_explicitGrantWins() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.CONTRIBUTOR)));
        auth.setShareWithOrgDefaultRole(AuthorizationRole.VIEWER);

        AuthorizationRole result = guard.effectiveRole(auth, otherInOrg);

        // CONTRIBUTOR is more privileged than VIEWER — keeps the grant
        assertThat(result).isEqualTo(AuthorizationRole.CONTRIBUTOR);
    }

    // --- requireXxx behavior ---

    @Test
    void requireWriteDetails_owner_passes() {
        inOrg(creator, OrganizationRole.USER);

        assertThatNoException().isThrownBy(() -> guard.requireWriteDetails(auth, creator));
    }

    @Test
    void requireWriteDetails_editor_passes() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.EDITOR)));

        assertThatNoException().isThrownBy(() -> guard.requireWriteDetails(auth, otherInOrg));
    }

    @Test
    void requireWriteDetails_contributor_throws403() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.CONTRIBUTOR)));

        assertThatThrownBy(() -> guard.requireWriteDetails(auth, otherInOrg))
                .isInstanceOf(InsufficientAuthorizationRoleException.class);
    }

    @Test
    void requireManageGrants_orgAdmin_passes() {
        inOrg(orgAdmin, OrganizationRole.ORG_ADMIN);

        assertThatNoException().isThrownBy(() -> guard.requireManageGrants(auth, orgAdmin));
    }

    @Test
    void requireManageGrants_editor_throws403() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.EDITOR)));

        assertThatThrownBy(() -> guard.requireManageGrants(auth, otherInOrg))
                .isInstanceOf(InsufficientAuthorizationRoleException.class);
    }

    @Test
    void requireDelete_creator_passes() {
        inOrg(creator, OrganizationRole.USER);

        assertThatNoException().isThrownBy(() -> guard.requireDelete(auth, creator));
    }

    @Test
    void requireDelete_editor_throws403() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.EDITOR)));

        assertThatThrownBy(() -> guard.requireDelete(auth, otherInOrg))
                .isInstanceOf(InsufficientAuthorizationRoleException.class);
    }

    @Test
    void requireDeleteOwnedItem_contributorOwnsItem_passes() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.CONTRIBUTOR)));

        assertThatNoException().isThrownBy(() ->
                guard.requireDeleteOwnedItem(auth, otherInOrg, otherInOrg.getId()));
    }

    @Test
    void requireDeleteOwnedItem_contributorOtherUsersItem_throws403() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.CONTRIBUTOR)));

        assertThatThrownBy(() ->
                guard.requireDeleteOwnedItem(auth, otherInOrg, 99L))
                .isInstanceOf(InsufficientAuthorizationRoleException.class);
    }

    @Test
    void requireDeleteOwnedItem_owner_passesEvenForOtherUsersItem() {
        inOrg(creator, OrganizationRole.USER);

        assertThatNoException().isThrownBy(() ->
                guard.requireDeleteOwnedItem(auth, creator, 99L));
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

```bash
cd back-end && mvn surefire:test -Dtest=AuthorizationAccessGuardTest -DfailIfNoTests=false 2>&1 | tail -20
```

Expected: compile failure — `AuthorizationAccessGuard` and `InsufficientAuthorizationRoleException` don't exist yet.

- [ ] **Step 3: Create the exception**

`back-end/src/main/java/gov/nist/oscal/tools/api/exception/InsufficientAuthorizationRoleException.java`:

```java
package gov.nist.oscal.tools.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class InsufficientAuthorizationRoleException extends RuntimeException {

    public InsufficientAuthorizationRoleException(String currentRole, String requiredRole) {
        super("Insufficient role: have " + currentRole + ", need " + requiredRole + ".");
    }
}
```

- [ ] **Step 4: Implement the access guard**

`back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationAccessGuard.java`:

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.User.GlobalRole;
import gov.nist.oscal.tools.api.exception.InsufficientAuthorizationRoleException;
import gov.nist.oscal.tools.api.repository.AuthorizationGrantRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Resolves a user's effective role on a specific authorization, applying
 * SUPER_ADMIN and ORG_ADMIN bypasses, the creator's implicit OWNER status,
 * any explicit AuthorizationGrant rows, and the share-with-org default.
 *
 * Out-of-org users always resolve to {@code null} (no role at all).
 */
@Service
public class AuthorizationAccessGuard {

    private final AuthorizationGrantRepository grantRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public AuthorizationAccessGuard(AuthorizationGrantRepository grantRepository,
                                    OrganizationMembershipRepository membershipRepository) {
        this.grantRepository = grantRepository;
        this.membershipRepository = membershipRepository;
    }

    public AuthorizationRole effectiveRole(Authorization authorization, User user) {
        if (user.getGlobalRole() == GlobalRole.SUPER_ADMIN) {
            return AuthorizationRole.OWNER;
        }

        Optional<OrganizationMembership> membership = activeMembershipInOrg(user, authorization.getOrganization().getId());
        if (membership.isEmpty()) {
            return null;
        }

        if (membership.get().getRole() == OrganizationRole.ORG_ADMIN) {
            return AuthorizationRole.OWNER;
        }

        if (authorization.getAuthorizedBy() != null
                && user.getId().equals(authorization.getAuthorizedBy().getId())) {
            return AuthorizationRole.OWNER;
        }

        AuthorizationRole grantRole = grantRepository.findByAuthorizationAndUser(authorization, user)
                .map(AuthorizationGrant::getRole)
                .orElse(null);
        AuthorizationRole shareRole = authorization.getShareWithOrgDefaultRole();

        return moreSenior(grantRole, shareRole);
    }

    public void requireRead(Authorization authorization, User user) {
        if (effectiveRole(authorization, user) == null) {
            throw new InsufficientAuthorizationRoleException("none", "VIEWER");
        }
    }

    public void requireWriteDetails(Authorization authorization, User user) {
        AuthorizationRole role = effectiveRole(authorization, user);
        if (!isAtLeast(role, AuthorizationRole.EDITOR)) {
            throw new InsufficientAuthorizationRoleException(roleName(role), "EDITOR");
        }
    }

    public void requireUploadConMon(Authorization authorization, User user) {
        AuthorizationRole role = effectiveRole(authorization, user);
        if (!isAtLeast(role, AuthorizationRole.CONTRIBUTOR)) {
            throw new InsufficientAuthorizationRoleException(roleName(role), "CONTRIBUTOR");
        }
    }

    public void requireUploadDocument(Authorization authorization, User user) {
        AuthorizationRole role = effectiveRole(authorization, user);
        if (!isAtLeast(role, AuthorizationRole.CONTRIBUTOR)) {
            throw new InsufficientAuthorizationRoleException(roleName(role), "CONTRIBUTOR");
        }
    }

    public void requireDeleteOwnedItem(Authorization authorization, User user, Long ownerUserId) {
        AuthorizationRole role = effectiveRole(authorization, user);
        if (isAtLeast(role, AuthorizationRole.EDITOR)) {
            return;
        }
        if (role == AuthorizationRole.CONTRIBUTOR && user.getId().equals(ownerUserId)) {
            return;
        }
        throw new InsufficientAuthorizationRoleException(roleName(role), "EDITOR or CONTRIBUTOR-of-own-item");
    }

    public void requireManageGrants(Authorization authorization, User user) {
        AuthorizationRole role = effectiveRole(authorization, user);
        if (role != AuthorizationRole.OWNER) {
            throw new InsufficientAuthorizationRoleException(roleName(role), "OWNER");
        }
    }

    public void requireDelete(Authorization authorization, User user) {
        AuthorizationRole role = effectiveRole(authorization, user);
        if (role != AuthorizationRole.OWNER) {
            throw new InsufficientAuthorizationRoleException(roleName(role), "OWNER");
        }
    }

    private Optional<OrganizationMembership> activeMembershipInOrg(User user, Long orgId) {
        List<OrganizationMembership> memberships =
                membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE);
        return memberships.stream()
                .filter(m -> orgId.equals(m.getOrganization().getId()))
                .findFirst();
    }

    private static boolean isAtLeast(AuthorizationRole have, AuthorizationRole need) {
        if (have == null) return false;
        return seniority(have) >= seniority(need);
    }

    private static int seniority(AuthorizationRole role) {
        return switch (role) {
            case OWNER -> 4;
            case EDITOR -> 3;
            case CONTRIBUTOR -> 2;
            case VIEWER -> 1;
        };
    }

    private static AuthorizationRole moreSenior(AuthorizationRole a, AuthorizationRole b) {
        if (a == null) return b;
        if (b == null) return a;
        return seniority(a) >= seniority(b) ? a : b;
    }

    private static String roleName(AuthorizationRole role) {
        return role == null ? "none" : role.name();
    }
}
```

- [ ] **Step 5: Run the test to confirm it passes**

```bash
cd back-end && mvn surefire:test -Dtest=AuthorizationAccessGuardTest -DfailIfNoTests=false 2>&1 | tail -20
```

Expected: 18 tests pass.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/exception/InsufficientAuthorizationRoleException.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationAccessGuard.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/AuthorizationAccessGuardTest.java
git diff --cached --stat
git commit -m "feat(authorizations): add AuthorizationAccessGuard for per-auth role resolution"
```

---

## Task 7: Wire access guard into AuthorizationService write paths

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationService.java`

The guard must run on `updateAuthorization` and `deleteAuthorization` so that users without sufficient role get 403 instead of being able to edit/delete.

- [ ] **Step 1: Inject the guard**

In `AuthorizationService`, add a new field:

```java
    @Autowired
    private AuthorizationAccessGuard accessGuard;
```

- [ ] **Step 2: Modify updateAuthorization**

Find the line that loads the authorization:

```java
        Organization userOrg = resolveUserOrg(username);
        Authorization authorization = authorizationRepository.findByIdAndOrganization(id, userOrg)
                .orElseThrow(() -> new AuthorizationNotFoundException(id));
```

Immediately after that block, before any field mutations, add:

```java
        accessGuard.requireWriteDetails(authorization,
                userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException("User '" + username + "' not found.")));
```

- [ ] **Step 3: Modify deleteAuthorization**

Find the same load pattern in `deleteAuthorization`. Replace the existing creator-only check (currently `if (!authorization.getAuthorizedBy().getUsername().equals(username)) throw ...`) with:

```java
        accessGuard.requireDelete(authorization,
                userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException("User '" + username + "' not found.")));
```

The guard's `requireDelete` enforces "OWNER only" (which includes the creator and bypass roles), exactly preserving the prior creator-only semantics while adding ORG_ADMIN/SUPER_ADMIN bypass.

- [ ] **Step 4: Filter list reads by access (private-by-default)**

After PR 1, `getAllAuthorizationsForUser` returns every authorization in the user's org. With grants in place, the spec requires private-by-default: users only see authorizations where they have a non-null effective role. Update the method:

```java
    @Transactional(readOnly = true)
    public List<Authorization> getAllAuthorizationsForUser(String username) {
        Organization org = resolveUserOrg(username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User '" + username + "' not found."));
        return authorizationRepository.findByOrganization(org).stream()
                .filter(a -> accessGuard.effectiveRole(a, user) != null)
                .toList();
    }
```

Apply the same filter to `searchAuthorizationsForUser` and `getAuthorizationsBySspForUser` so search/SSP-lookup don't leak inaccessible authorizations.

**Note:** This is an N+1 query under the hood (one grant lookup per authorization). Acceptable for tens-of-authorizations-per-org; needs a single SQL query as a future optimization. Add a code comment:

```java
        // TODO(perf): replace with a single JPQL/SQL query that joins grants
        // and applies the access predicate when authorization counts grow large.
```

- [ ] **Step 5: Compile**

```bash
cd back-end && mvn compile -DskipTests 2>&1 | tail -5
```

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationService.java
git diff --cached --stat
git commit -m "feat(authorizations): enforce role matrix on update/delete and filter list reads by access"
```

---

## Task 8: Grant management endpoints + DTOs

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationGrantRequest.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationGrantResponse.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/ShareWithOrgRequest.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationController.java`

- [ ] **Step 1: Create AuthorizationGrantRequest**

```java
package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import jakarta.validation.constraints.NotNull;

public class AuthorizationGrantRequest {

    @NotNull
    private Long userId;

    @NotNull
    private AuthorizationRole role;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public AuthorizationRole getRole() { return role; }
    public void setRole(AuthorizationRole role) { this.role = role; }
}
```

- [ ] **Step 2: Create AuthorizationGrantResponse**

```java
package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;

import java.time.LocalDateTime;

public class AuthorizationGrantResponse {

    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private AuthorizationRole role;
    private String grantedByUsername;
    private LocalDateTime grantedAt;

    public AuthorizationGrantResponse() {}

    public AuthorizationGrantResponse(AuthorizationGrant grant) {
        this.id = grant.getId();
        this.userId = grant.getUser().getId();
        this.username = grant.getUser().getUsername();
        this.email = grant.getUser().getEmail();
        this.firstName = grant.getUser().getFirstName();
        this.lastName = grant.getUser().getLastName();
        this.role = grant.getRole();
        this.grantedByUsername = grant.getGrantedBy() != null ? grant.getGrantedBy().getUsername() : null;
        this.grantedAt = grant.getGrantedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public AuthorizationRole getRole() { return role; }
    public void setRole(AuthorizationRole role) { this.role = role; }
    public String getGrantedByUsername() { return grantedByUsername; }
    public void setGrantedByUsername(String grantedByUsername) { this.grantedByUsername = grantedByUsername; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
}
```

If `User` doesn't have `getFirstName()` / `getLastName()` accessors, fall back to populating just username + email. Verify by reading `User.java`.

- [ ] **Step 3: Create ShareWithOrgRequest**

```java
package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.AuthorizationRole;

public class ShareWithOrgRequest {

    /** May be null to clear the share-with-org default. */
    private AuthorizationRole role;

    public AuthorizationRole getRole() { return role; }
    public void setRole(AuthorizationRole role) { this.role = role; }
}
```

- [ ] **Step 4: Add the controller endpoints**

In `AuthorizationController.java`:

(a) Add these dependencies via constructor injection (extending the existing constructor):

```java
    private final AuthorizationAccessGuard accessGuard;
    private final AuthorizationGrantRepository grantRepository;
    private final UserRepository userRepository;
    // (already injected: authorizationService, digitalSignatureService, telemetryService)
```

Update the constructor signature and the `@Autowired` constructor body to assign these.

(b) Add the 5 grant management endpoints at the end of the class (before the closing brace):

```java
    @GetMapping("/{id}/grants")
    public ResponseEntity<List<AuthorizationGrantResponse>> listGrants(@PathVariable Long id,
                                                                       Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(id, principal.getName());
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User '" + principal.getName() + "' not found."));
        accessGuard.requireManageGrants(authorization, currentUser);

        List<AuthorizationGrantResponse> grants = grantRepository.findByAuthorization(authorization).stream()
                .map(AuthorizationGrantResponse::new)
                .toList();
        return ResponseEntity.ok(grants);
    }

    @PostMapping("/{id}/grants")
    public ResponseEntity<AuthorizationGrantResponse> addGrant(@PathVariable Long id,
                                                               @Valid @RequestBody AuthorizationGrantRequest request,
                                                               Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(id, principal.getName());
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User '" + principal.getName() + "' not found."));
        accessGuard.requireManageGrants(authorization, currentUser);

        User grantee = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User " + request.getUserId() + " not found."));

        // Reject grants for users not in the authorization's organization.
        if (accessGuard.effectiveRole(authorization, grantee) == null
                && !isInSameOrg(authorization, grantee)) {
            throw new IllegalArgumentException("User is not a member of this authorization's organization.");
        }

        AuthorizationGrant grant = grantRepository.findByAuthorizationAndUser(authorization, grantee)
                .orElseGet(() -> new AuthorizationGrant(authorization, grantee, request.getRole(), currentUser));
        grant.setRole(request.getRole());
        grant.setGrantedBy(currentUser);
        grantRepository.save(grant);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthorizationGrantResponse(grant));
    }

    @PatchMapping("/{id}/grants/{grantId}")
    public ResponseEntity<AuthorizationGrantResponse> updateGrant(@PathVariable Long id,
                                                                  @PathVariable Long grantId,
                                                                  @Valid @RequestBody AuthorizationGrantRequest request,
                                                                  Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(id, principal.getName());
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User '" + principal.getName() + "' not found."));
        accessGuard.requireManageGrants(authorization, currentUser);

        AuthorizationGrant grant = grantRepository.findById(grantId)
                .filter(g -> g.getAuthorization().getId().equals(id))
                .orElseThrow(() -> new IllegalArgumentException("Grant " + grantId + " not found on authorization " + id));

        grant.setRole(request.getRole());
        grant.setGrantedBy(currentUser);
        grantRepository.save(grant);

        return ResponseEntity.ok(new AuthorizationGrantResponse(grant));
    }

    @DeleteMapping("/{id}/grants/{grantId}")
    public ResponseEntity<Void> removeGrant(@PathVariable Long id,
                                            @PathVariable Long grantId,
                                            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(id, principal.getName());
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User '" + principal.getName() + "' not found."));
        accessGuard.requireManageGrants(authorization, currentUser);

        AuthorizationGrant grant = grantRepository.findById(grantId)
                .filter(g -> g.getAuthorization().getId().equals(id))
                .orElseThrow(() -> new IllegalArgumentException("Grant " + grantId + " not found on authorization " + id));

        grantRepository.delete(grant);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/share-with-org")
    public ResponseEntity<AuthorizationResponse> setShareWithOrg(@PathVariable Long id,
                                                                 @RequestBody ShareWithOrgRequest request,
                                                                 Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(id, principal.getName());
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User '" + principal.getName() + "' not found."));
        accessGuard.requireManageGrants(authorization, currentUser);

        if (request.getRole() != null && !AuthorizationRole.isAssignableAsShareDefault(request.getRole())) {
            throw new IllegalArgumentException("Cannot set share-with-org default to " + request.getRole()
                    + ". Allowed: VIEWER, CONTRIBUTOR, EDITOR.");
        }

        authorization.setShareWithOrgDefaultRole(request.getRole());
        authorizationService.save(authorization);

        AuthorizationResponse response = new AuthorizationResponse(authorization);
        response.setEffectiveRole(accessGuard.effectiveRole(authorization, currentUser));
        response.setShareWithOrgDefaultRole(request.getRole());
        return ResponseEntity.ok(response);
    }

    private boolean isInSameOrg(Authorization authorization, User user) {
        return user.getOrganizationMemberships().stream()
                .anyMatch(m -> m.getOrganization().getId().equals(authorization.getOrganization().getId())
                        && m.getStatus() == OrganizationMembership.MembershipStatus.ACTIVE);
    }
```

(c) Add imports:

```java
import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.AuthorizationGrantRequest;
import gov.nist.oscal.tools.api.model.AuthorizationGrantResponse;
import gov.nist.oscal.tools.api.model.ShareWithOrgRequest;
import gov.nist.oscal.tools.api.repository.AuthorizationGrantRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.AuthorizationAccessGuard;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
```

(d) `AuthorizationService.save(...)` already exists (per PR 1 plan). If not, the implementer can use `authorizationRepository.save(authorization)` directly via a small package-private helper. Verify by reading `AuthorizationService` first.

- [ ] **Step 5: Verify the response sets effectiveRole**

In every existing controller method that builds `new AuthorizationResponse(authorization)` and returns it (`getAuthorization`, `getAllAuthorizations`, etc.), add a line right before the `return` that populates `effectiveRole` and `shareWithOrgDefaultRole`. Pattern:

```java
        AuthorizationResponse response = new AuthorizationResponse(authorization);
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User '" + principal.getName() + "' not found."));
        response.setEffectiveRole(accessGuard.effectiveRole(authorization, currentUser));
        response.setShareWithOrgDefaultRole(authorization.getShareWithOrgDefaultRole());
```

For list endpoints, pull `currentUser` once outside the stream and reuse.

To DRY this up, extract a private helper:

```java
    private AuthorizationResponse toResponse(Authorization authorization, User currentUser) {
        AuthorizationResponse response = new AuthorizationResponse(authorization);
        response.setEffectiveRole(accessGuard.effectiveRole(authorization, currentUser));
        response.setShareWithOrgDefaultRole(authorization.getShareWithOrgDefaultRole());
        return response;
    }
```

…and replace every `new AuthorizationResponse(authorization)` in the controller with a call to `toResponse(authorization, currentUser)`. Read carefully and match.

- [ ] **Step 6: Compile**

```bash
cd back-end && mvn compile -DskipTests 2>&1 | tail -5
```

Expected: BUILD SUCCESS. (`AuthorizationResponse.setEffectiveRole` and `setShareWithOrgDefaultRole` will fail until Task 9 lands. To unblock this task, do Task 9 first if you haven't yet.)

**Important:** This task depends on Task 9 (response model fields). Either reorder so Task 9 lands before Task 8's controller edits, or implement Tasks 8+9 together as a single commit. The plan keeps them separate for clarity but they can be combined. **Recommended: do Task 9 first.**

- [ ] **Step 7: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationController.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationGrantRequest.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationGrantResponse.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/model/ShareWithOrgRequest.java
git diff --cached --stat
git commit -m "feat(authorizations): add grant management and share-with-org endpoints"
```

---

## Task 9: Add effectiveRole and shareWithOrgDefaultRole to AuthorizationResponse

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationResponse.java`

**Do this BEFORE Task 8.** (Tasks renumbered for narrative clarity but ordering matters.)

- [ ] **Step 1: Add fields**

In `AuthorizationResponse.java`, near the top of the field list (after `organizationId`), add:

```java
    private gov.nist.oscal.tools.api.entity.AuthorizationRole effectiveRole;
    private gov.nist.oscal.tools.api.entity.AuthorizationRole shareWithOrgDefaultRole;
```

(Or use simple imports — the choice depends on existing import-style conventions in the file. Match what's there.)

- [ ] **Step 2: Add accessors**

```java
    public gov.nist.oscal.tools.api.entity.AuthorizationRole getEffectiveRole() {
        return effectiveRole;
    }

    public void setEffectiveRole(gov.nist.oscal.tools.api.entity.AuthorizationRole effectiveRole) {
        this.effectiveRole = effectiveRole;
    }

    public gov.nist.oscal.tools.api.entity.AuthorizationRole getShareWithOrgDefaultRole() {
        return shareWithOrgDefaultRole;
    }

    public void setShareWithOrgDefaultRole(gov.nist.oscal.tools.api.entity.AuthorizationRole shareWithOrgDefaultRole) {
        this.shareWithOrgDefaultRole = shareWithOrgDefaultRole;
    }
```

- [ ] **Step 3: Populate from constructor (Option A from PR 1)**

If the constructor `AuthorizationResponse(Authorization authorization)` already sets fields from the entity, add:

```java
        this.shareWithOrgDefaultRole = authorization.getShareWithOrgDefaultRole();
```

`effectiveRole` is NOT populated in the constructor — it requires the current user, so the controller sets it explicitly via `setEffectiveRole`.

- [ ] **Step 4: Compile**

```bash
cd back-end && mvn compile -DskipTests 2>&1 | tail -5
```

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationResponse.java
git diff --cached --stat
git commit -m "feat(authorizations): expose effectiveRole and shareWithOrgDefaultRole on response"
```

---

## Task 10: OrgMembersController for the user picker

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/OrgMemberResponse.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/OrgMembersController.java`

The Sharing & Access UI needs to list members of the current user's primary org so a grant can be added by user. Existing org-admin endpoints are gated behind `@PreAuthorize`; we need one accessible to ordinary in-org users (so a non-admin OWNER of an authorization can pick from their org).

- [ ] **Step 1: Create OrgMemberResponse**

```java
package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.User;

public class OrgMemberResponse {

    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;

    public OrgMemberResponse() {}

    public OrgMemberResponse(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}
```

If `User.getFirstName()` / `getLastName()` don't exist, drop those fields and set them to null. Verify before writing.

- [ ] **Step 2: Create OrgMembersController**

```java
package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.model.OrgMemberResponse;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.service.AuthorizationOrgContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organization Members", description = "Read-only access to the current user's org members for pickers")
public class OrgMembersController {

    private final AuthorizationOrgContext orgContext;
    private final OrganizationMembershipRepository membershipRepository;

    public OrgMembersController(AuthorizationOrgContext orgContext,
                                OrganizationMembershipRepository membershipRepository) {
        this.orgContext = orgContext;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping("/me/members")
    public ResponseEntity<List<OrgMemberResponse>> listMyOrgMembers(Principal principal) {
        Organization org = orgContext.requirePrimaryOrganization(principal.getName());
        List<OrgMemberResponse> members = membershipRepository
                .findByOrganizationAndStatusWithUser(org, MembershipStatus.ACTIVE)
                .stream()
                .map(OrganizationMembership::getUser)
                .map(OrgMemberResponse::new)
                .toList();
        return ResponseEntity.ok(members);
    }
}
```

This is intentionally NOT gated by `@PreAuthorize` beyond the global "must be authenticated" rule. Any authenticated user in an org can list their fellow members (the same data the org admin UI surfaces, just without the management actions).

- [ ] **Step 3: Compile**

```bash
cd back-end && mvn compile -DskipTests 2>&1 | tail -5
```

- [ ] **Step 4: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/model/OrgMemberResponse.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/controller/OrgMembersController.java
git diff --cached --stat
git commit -m "feat(orgs): expose GET /api/organizations/me/members for in-org pickers"
```

---

## Task 11: Update existing AuthorizationControllerTest

**Files:**
- Modify: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/AuthorizationControllerTest.java`

The new dependencies (`AuthorizationAccessGuard`, `AuthorizationGrantRepository`, `UserRepository`) need to be mocked in the existing test class so it compiles. PR 1's `0b380e5` already mocked `AuthorizationOrgContext`; we extend that.

- [ ] **Step 1: Add @MockitoBean fields**

In `AuthorizationControllerTest.java`, alongside the existing `@MockitoBean` declarations, add:

```java
    @MockitoBean
    private AuthorizationAccessGuard accessGuard;

    @MockitoBean
    private AuthorizationGrantRepository grantRepository;

    // userRepository may already be mocked from PR 1's fixup; if not, add it.
    // @MockitoBean
    // private UserRepository userRepository;
```

(Verify which are already present by reading the file first.)

- [ ] **Step 2: Stub default behavior**

In the existing `setUp()` (or wherever defaults are stubbed), default `accessGuard.effectiveRole(any(), any())` to return `OWNER` for happy-path tests, and stub `userRepository.findByUsername("testuser")` to return a populated `User`. Existing tests will still need updates wherever a controller method now calls `accessGuard.requireXxx(...)` or expects `getEffectiveRole()` on the response.

A reasonable pattern for the existing happy-path tests:

```java
    @BeforeEach
    void setUpAccess() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accessGuard.effectiveRole(any(Authorization.class), any(User.class)))
                .thenReturn(AuthorizationRole.OWNER);
    }
```

- [ ] **Step 3: Run the existing tests**

```bash
cd back-end && mvn surefire:test -Dtest=AuthorizationControllerTest -DfailIfNoTests=false 2>&1 | tail -25
```

Expected: all tests still pass. If a test fails because the controller now reads from `accessGuard` in a path that wasn't previously mocked, add the stub for that path.

- [ ] **Step 4: Commit**

```bash
git add back-end/src/test/java/gov/nist/oscal/tools/api/controller/AuthorizationControllerTest.java
git diff --cached --stat
git commit -m "test(authorizations): mock access guard in existing controller tests"
```

---

## Task 12: New tests for grant endpoints

**Files:**
- Modify: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/AuthorizationControllerTest.java`

Add focused tests for the new endpoints. Place them at the end of the existing test class.

- [ ] **Step 1: Add the test methods**

```java
    @Test
    @WithMockUser(username = "testuser")
    void listGrants_owner_returns200() throws Exception {
        Authorization auth = mockAuthorization(1L);
        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(auth);
        when(grantRepository.findByAuthorization(auth)).thenReturn(List.of());

        mockMvc.perform(get("/api/authorizations/1/grants"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void addGrant_owner_returns201() throws Exception {
        Authorization auth = mockAuthorization(1L);
        User grantee = new User(); grantee.setId(2L); grantee.setUsername("bob");
        Organization org = new Organization(); org.setId(100L);
        OrganizationMembership granteeMembership = new OrganizationMembership();
        granteeMembership.setOrganization(org);
        granteeMembership.setStatus(OrganizationMembership.MembershipStatus.ACTIVE);
        grantee.setOrganizationMemberships(Set.of(granteeMembership));
        auth.setOrganization(org);

        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(auth);
        when(userRepository.findById(2L)).thenReturn(Optional.of(grantee));
        when(grantRepository.findByAuthorizationAndUser(auth, grantee)).thenReturn(Optional.empty());
        when(grantRepository.save(any(AuthorizationGrant.class)))
                .thenAnswer(inv -> {
                    AuthorizationGrant g = inv.getArgument(0);
                    g.setId(99L);
                    return g;
                });

        AuthorizationGrantRequest body = new AuthorizationGrantRequest();
        body.setUserId(2L);
        body.setRole(AuthorizationRole.EDITOR);

        mockMvc.perform(post("/api/authorizations/1/grants")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "viewer-user")
    void addGrant_nonOwner_returns403() throws Exception {
        Authorization auth = mockAuthorization(1L);
        when(authorizationService.getAuthorizationForUser(1L, "viewer-user")).thenReturn(auth);
        // accessGuard.requireManageGrants throws for non-owner
        org.mockito.Mockito.doThrow(new gov.nist.oscal.tools.api.exception.InsufficientAuthorizationRoleException("VIEWER", "OWNER"))
                .when(accessGuard).requireManageGrants(eq(auth), any(User.class));

        AuthorizationGrantRequest body = new AuthorizationGrantRequest();
        body.setUserId(2L);
        body.setRole(AuthorizationRole.EDITOR);

        mockMvc.perform(post("/api/authorizations/1/grants")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void setShareWithOrg_validRole_returns200() throws Exception {
        Authorization auth = mockAuthorization(1L);
        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(auth);
        when(authorizationService.save(any(Authorization.class))).thenAnswer(inv -> inv.getArgument(0));

        ShareWithOrgRequest body = new ShareWithOrgRequest();
        body.setRole(AuthorizationRole.VIEWER);

        mockMvc.perform(patch("/api/authorizations/1/share-with-org")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void setShareWithOrg_ownerRole_returns400() throws Exception {
        Authorization auth = mockAuthorization(1L);
        when(authorizationService.getAuthorizationForUser(1L, "testuser")).thenReturn(auth);

        ShareWithOrgRequest body = new ShareWithOrgRequest();
        body.setRole(AuthorizationRole.OWNER);

        mockMvc.perform(patch("/api/authorizations/1/share-with-org")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    private Authorization mockAuthorization(Long id) {
        Authorization auth = new Authorization();
        auth.setId(id);
        Organization org = new Organization(); org.setId(100L);
        auth.setOrganization(org);
        User creator = new User(); creator.setId(1L); creator.setUsername("testuser");
        auth.setAuthorizedBy(creator);
        return auth;
    }
```

Add the necessary imports if missing.

- [ ] **Step 2: Run tests**

```bash
cd back-end && mvn surefire:test -Dtest=AuthorizationControllerTest -DfailIfNoTests=false 2>&1 | tail -25
```

Expected: all tests pass.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/test/java/gov/nist/oscal/tools/api/controller/AuthorizationControllerTest.java
git diff --cached --stat
git commit -m "test(authorizations): cover grant endpoints and share-with-org rules"
```

---

## Task 13: Frontend types

**Files:**
- Modify: `front-end/src/types/oscal.ts`

- [ ] **Step 1: Add types**

In `front-end/src/types/oscal.ts`, near the existing `AuthorizationResponse` interface, add:

```typescript
export type AuthorizationRole = 'OWNER' | 'EDITOR' | 'CONTRIBUTOR' | 'VIEWER';

export interface AuthorizationGrantResponse {
  id: number;
  userId: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: AuthorizationRole;
  grantedByUsername?: string;
  grantedAt: string;
}

export interface OrgMemberResponse {
  userId: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
}
```

Then update the existing `AuthorizationResponse` interface to add:

```typescript
export interface AuthorizationResponse {
  // ... existing fields ...
  effectiveRole?: AuthorizationRole;
  shareWithOrgDefaultRole?: AuthorizationRole | null;
}
```

- [ ] **Step 2: Type-check**

```bash
cd front-end && npx tsc --noEmit 2>&1 | tail -10
```

Expected: no new errors. Pre-existing errors in `catalog/page.tsx` are fine.

- [ ] **Step 3: Verify the staged diff is clean**

The user has WIP on `oscal.ts`. Run:

```bash
git diff -- front-end/src/types/oscal.ts | head -100
```

Confirm only your additions are visible. If any pre-existing WIP shows in the diff, either:
- (a) Coordinate with the user to commit/stash that WIP first; or
- (b) Restore the file's HEAD state, apply only your additions, and commit. (Same approach used in PR 1's `d27215f`.)

- [ ] **Step 4: Commit**

```bash
git add front-end/src/types/oscal.ts
git diff --cached --stat
git commit -m "feat(authorizations): add ACL grant types to frontend"
```

---

## Task 14: API client methods

**Files:**
- Modify: `front-end/src/lib/api-client.ts`

- [ ] **Step 1: Add methods**

Add these methods to the `apiClient` (matching the existing pattern of `getAuthorization`, `updateAuthorization`):

```typescript
  async listGrants(authorizationId: number): Promise<AuthorizationGrantResponse[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/authorizations/${authorizationId}/grants`,
      { method: 'GET', headers: this.getAuthHeaders() },
      5000
    );
    return await response.json();
  }

  async addGrant(authorizationId: number, userId: number, role: AuthorizationRole): Promise<AuthorizationGrantResponse> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/authorizations/${authorizationId}/grants`,
      {
        method: 'POST',
        headers: this.getAuthHeaders(),
        body: JSON.stringify({ userId, role }),
      },
      5000
    );
    if (!response.ok) {
      throw new Error(`Failed to add grant: ${response.status}`);
    }
    return await response.json();
  }

  async updateGrant(authorizationId: number, grantId: number, role: AuthorizationRole): Promise<AuthorizationGrantResponse> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/authorizations/${authorizationId}/grants/${grantId}`,
      {
        method: 'PATCH',
        headers: this.getAuthHeaders(),
        body: JSON.stringify({ userId: 0, role }),  // backend ignores userId on PATCH
      },
      5000
    );
    return await response.json();
  }

  async removeGrant(authorizationId: number, grantId: number): Promise<void> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/authorizations/${authorizationId}/grants/${grantId}`,
      { method: 'DELETE', headers: this.getAuthHeaders() },
      5000
    );
    if (!response.ok) {
      throw new Error(`Failed to remove grant: ${response.status}`);
    }
  }

  async setShareWithOrg(authorizationId: number, role: AuthorizationRole | null): Promise<AuthorizationResponse> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/authorizations/${authorizationId}/share-with-org`,
      {
        method: 'PATCH',
        headers: this.getAuthHeaders(),
        body: JSON.stringify({ role }),
      },
      5000
    );
    return await response.json();
  }

  async listMyOrgMembers(): Promise<OrgMemberResponse[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/organizations/me/members`,
      { method: 'GET', headers: this.getAuthHeaders() },
      5000
    );
    return await response.json();
  }
```

Add imports for the new types at the top of the file:

```typescript
import type {
  // ... existing imports ...
  AuthorizationRole,
  AuthorizationGrantResponse,
  OrgMemberResponse,
} from '@/types/oscal';
```

- [ ] **Step 2: Type-check**

```bash
cd front-end && npx tsc --noEmit 2>&1 | tail -10
```

- [ ] **Step 3: Commit**

Same scope discipline — `api-client.ts` is in the user's modified set.

```bash
git diff -- front-end/src/lib/api-client.ts | head -50  # confirm only your additions
git add front-end/src/lib/api-client.ts
git diff --cached --stat
git commit -m "feat(authorizations): add grant management methods to api client"
```

If the diff includes user WIP, follow the same restore-then-apply pattern as Task 13.

---

## Task 15: UserPicker component

**Files:**
- Create: `front-end/src/components/user-picker.tsx`

A combobox-style picker for selecting an org member. Built on existing shadcn primitives (`Command`, `Popover`) if present; otherwise a simpler dropdown using `Select`.

- [ ] **Step 1: Check what shadcn primitives exist**

```bash
ls front-end/src/components/ui/ | grep -E "command|popover|combobox" 2>/dev/null
```

If `command.tsx` and `popover.tsx` exist, use the standard shadcn combobox pattern. If not, fall back to `Select` with a search input above it.

- [ ] **Step 2: Implement the picker (fallback Select-based version)**

```tsx
'use client';

import { useState, useEffect, useMemo } from 'react';
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
```

- [ ] **Step 3: Type-check**

```bash
cd front-end && npx tsc --noEmit 2>&1 | tail -5
```

- [ ] **Step 4: Commit**

```bash
git add front-end/src/components/user-picker.tsx
git diff --cached --stat
git commit -m "feat(ui): add UserPicker for org-member selection"
```

---

## Task 16: SharingAccessCard component

**Files:**
- Create: `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/sharing-access-card.tsx`

The Sharing & Access UI block: visible only when `effectiveRole === 'OWNER'`. Shows current grants, add-grant form, and the share-with-org default-role select.

- [ ] **Step 1: Implement the card**

```tsx
'use client';

import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Trash2, Loader2, Plus } from 'lucide-react';
import { toast } from 'sonner';
import { apiClient } from '@/lib/api-client';
import { UserPicker } from '@/components/user-picker';
import type {
  AuthorizationRole,
  AuthorizationGrantResponse,
  OrgMemberResponse,
  AuthorizationResponse,
} from '@/types/oscal';

const ROLE_OPTIONS: AuthorizationRole[] = ['VIEWER', 'CONTRIBUTOR', 'EDITOR', 'OWNER'];
const SHARE_OPTIONS: AuthorizationRole[] = ['VIEWER', 'CONTRIBUTOR', 'EDITOR'];

interface Props {
  authorization: AuthorizationResponse;
  onAuthorizationUpdated: (a: AuthorizationResponse) => void;
}

export function SharingAccessCard({ authorization, onAuthorizationUpdated }: Props) {
  const [grants, setGrants] = useState<AuthorizationGrantResponse[]>([]);
  const [members, setMembers] = useState<OrgMemberResponse[]>([]);
  const [loadingGrants, setLoadingGrants] = useState(true);
  const [loadingMembers, setLoadingMembers] = useState(true);
  const [pickerValue, setPickerValue] = useState<number | null>(null);
  const [pickerRole, setPickerRole] = useState<AuthorizationRole>('VIEWER');
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    void refresh();
    void loadMembers();
  }, [authorization.id]);

  const refresh = async () => {
    setLoadingGrants(true);
    try {
      const data = await apiClient.listGrants(authorization.id);
      setGrants(data);
    } catch (e) {
      toast.error('Failed to load grants');
    } finally {
      setLoadingGrants(false);
    }
  };

  const loadMembers = async () => {
    setLoadingMembers(true);
    try {
      const data = await apiClient.listMyOrgMembers();
      setMembers(data);
    } catch (e) {
      toast.error('Failed to load org members');
    } finally {
      setLoadingMembers(false);
    }
  };

  const handleAdd = async () => {
    if (pickerValue == null) return;
    setAdding(true);
    try {
      await apiClient.addGrant(authorization.id, pickerValue, pickerRole);
      setPickerValue(null);
      setPickerRole('VIEWER');
      await refresh();
      toast.success('Grant added');
    } catch (e) {
      toast.error('Failed to add grant');
    } finally {
      setAdding(false);
    }
  };

  const handleRoleChange = async (grantId: number, role: AuthorizationRole) => {
    try {
      await apiClient.updateGrant(authorization.id, grantId, role);
      await refresh();
      toast.success('Role updated');
    } catch (e) {
      toast.error('Failed to update role');
    }
  };

  const handleRemove = async (grantId: number) => {
    try {
      await apiClient.removeGrant(authorization.id, grantId);
      await refresh();
      toast.success('Grant removed');
    } catch (e) {
      toast.error('Failed to remove grant');
    }
  };

  const handleShareChange = async (value: string) => {
    const role = value === 'NONE' ? null : (value as AuthorizationRole);
    try {
      const updated = await apiClient.setShareWithOrg(authorization.id, role);
      onAuthorizationUpdated(updated);
      toast.success(role ? `Shared with org as ${role}` : 'Org-wide sharing cleared');
    } catch (e) {
      toast.error('Failed to update sharing');
    }
  };

  const grantedUserIds = grants.map((g) => g.userId);

  return (
    <Card className="p-6">
      <h2 className="mb-1 text-lg font-semibold">Sharing &amp; Access</h2>
      <p className="mb-4 text-sm text-muted-foreground">
        Manage who can view, edit, or contribute to this authorization within your organization.
      </p>

      <section className="mb-6">
        <Label className="mb-2 block text-sm font-medium">Share with all org members as</Label>
        <Select
          value={authorization.shareWithOrgDefaultRole ?? 'NONE'}
          onValueChange={handleShareChange}
        >
          <SelectTrigger className="w-64">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="NONE">Not shared</SelectItem>
            {SHARE_OPTIONS.map((r) => (
              <SelectItem key={r} value={r}>{r}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        <p className="mt-1 text-xs text-muted-foreground">
          Every active member of your organization will get this role unless overridden by an explicit grant below.
        </p>
      </section>

      <section className="mb-6">
        <h3 className="mb-2 text-sm font-medium">Add a person</h3>
        <div className="flex items-end gap-2">
          <div className="flex-1">
            <UserPicker
              value={pickerValue}
              onChange={setPickerValue}
              members={members}
              loading={loadingMembers}
              excludeUserIds={grantedUserIds}
            />
          </div>
          <Select value={pickerRole} onValueChange={(v) => setPickerRole(v as AuthorizationRole)}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {ROLE_OPTIONS.map((r) => (
                <SelectItem key={r} value={r}>{r}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button onClick={handleAdd} disabled={pickerValue == null || adding}>
            {adding ? <Loader2 className="h-4 w-4 animate-spin" /> : <Plus className="h-4 w-4" />}
            Add
          </Button>
        </div>
      </section>

      <section>
        <h3 className="mb-2 text-sm font-medium">People with access</h3>
        {loadingGrants ? (
          <div className="py-4 text-center text-sm text-muted-foreground">Loading…</div>
        ) : grants.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            No explicit grants yet. {authorization.shareWithOrgDefaultRole
              ? `Org-wide ${authorization.shareWithOrgDefaultRole} sharing is active above.`
              : 'Only the creator and org admins can see this authorization.'}
          </p>
        ) : (
          <div className="divide-y">
            {grants.map((g) => (
              <div key={g.id} className="flex items-center justify-between py-2">
                <div className="flex flex-col">
                  <span className="text-sm font-medium">
                    {`${g.firstName ?? ''} ${g.lastName ?? ''}`.trim() || g.username}
                  </span>
                  <span className="text-xs text-muted-foreground">{g.email}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Select value={g.role} onValueChange={(v) => handleRoleChange(g.id, v as AuthorizationRole)}>
                    <SelectTrigger className="w-36">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {ROLE_OPTIONS.map((r) => (
                        <SelectItem key={r} value={r}>{r}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => handleRemove(g.id)}
                    aria-label={`Remove ${g.username}`}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </Card>
  );
}
```

- [ ] **Step 2: Type-check**

```bash
cd front-end && npx tsc --noEmit 2>&1 | tail -10
```

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/sharing-access-card.tsx
git diff --cached --stat
git commit -m "feat(authorizations): add Sharing & Access card UI"
```

---

## Task 17: Stub tabs for ConMon and Documents

**Files:**
- Create: `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/conmon-tab.tsx`
- Create: `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/documents-tab.tsx`

These are placeholder bodies — the actual content lands in PRs 4 and 3.

- [ ] **Step 1: ConMon stub**

```tsx
'use client';

import { Card } from '@/components/ui/card';

export function ContinuousMonitoringTab() {
  return (
    <Card className="p-8 text-center">
      <h2 className="mb-2 text-lg font-semibold">Continuous Monitoring</h2>
      <p className="text-sm text-muted-foreground">
        Coming soon — POAM upload, snapshot history, reconciliation, and analytics.
      </p>
    </Card>
  );
}
```

- [ ] **Step 2: Documents stub**

```tsx
'use client';

import { Card } from '@/components/ui/card';

export function DocumentsTab() {
  return (
    <Card className="p-8 text-center">
      <h2 className="mb-2 text-lg font-semibold">Documents</h2>
      <p className="text-sm text-muted-foreground">
        Coming soon — upload vuln scans, pen tests, asset inventories, and other supporting artifacts.
      </p>
    </Card>
  );
}
```

- [ ] **Step 3: Commit**

```bash
git add front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/conmon-tab.tsx \
        front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/documents-tab.tsx
git diff --cached --stat
git commit -m "feat(authorizations): add stub ConMon and Documents tabs"
```

---

## Task 18: Refactor detail page into Overview tab

**Files:**
- Create: `front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/overview-tab.tsx`

This tab body holds the existing detail-page content (Authorization Details, Digital Signature, Conditions of Approval, Authorization Document). The simplest path: extract the JSX between the page header and the loading/error guards into this new component, accepting the same state/handlers from the parent.

- [ ] **Step 1: Read the existing page**

```bash
sed -n '1,80p' front-end/src/app/authorizations/authorization/[authorizationId]/page.tsx
sed -n '270,340p' front-end/src/app/authorizations/authorization/[authorizationId]/page.tsx
```

Identify which sections form the Overview content vs. which JSX is the page chrome (header, back button, error state, loading state).

- [ ] **Step 2: Create the tab**

For now, accept the entire current Overview content as a single JSX block exposing the same state via props from the parent. Sketch the file:

```tsx
'use client';

import type { ReactNode } from 'react';
import { Card } from '@/components/ui/card';
import { SharingAccessCard } from './sharing-access-card';
import type { AuthorizationResponse } from '@/types/oscal';

interface Props {
  authorization: AuthorizationResponse;
  /**
   * The existing detail content extracted from the parent page (details card,
   * digital signature card, conditions card, authorization document card).
   * Passed in as children to keep this initial refactor mechanical — a future
   * cleanup can decompose into smaller components.
   */
  children: ReactNode;
  onAuthorizationUpdated: (a: AuthorizationResponse) => void;
}

export function OverviewTab({ authorization, children, onAuthorizationUpdated }: Props) {
  const isOwner = authorization.effectiveRole === 'OWNER';
  return (
    <div className="space-y-6">
      {children}
      {isOwner && (
        <SharingAccessCard
          authorization={authorization}
          onAuthorizationUpdated={onAuthorizationUpdated}
        />
      )}
    </div>
  );
}
```

This deliberately accepts `children` so the parent's existing JSX continues to render unchanged inside it. In a future cleanup we can decompose the children into smaller components, but that's out of scope for this PR.

- [ ] **Step 3: Compile**

```bash
cd front-end && npx tsc --noEmit 2>&1 | tail -10
```

- [ ] **Step 4: Commit**

```bash
git add front-end/src/app/authorizations/authorization/[authorizationId]/_tabs/overview-tab.tsx
git diff --cached --stat
git commit -m "feat(authorizations): add OverviewTab wrapper hosting Sharing & Access"
```

---

## Task 19: Wire tabs into the detail page

**Files:**
- Modify: `front-end/src/app/authorizations/authorization/[authorizationId]/page.tsx`

- [ ] **Step 1: Add imports**

At the top of the page file, add:

```tsx
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { useSearchParams, useRouter } from 'next/navigation';
import { OverviewTab } from './_tabs/overview-tab';
import { ContinuousMonitoringTab } from './_tabs/conmon-tab';
import { DocumentsTab } from './_tabs/documents-tab';
```

(`useRouter` and `useSearchParams` may already be imported — check.)

- [ ] **Step 2: Read & write `?tab=`**

Inside the page component, after existing state declarations:

```tsx
  const searchParams = useSearchParams();
  const tabParam = searchParams.get('tab');
  const initialTab =
    tabParam === 'conmon' || tabParam === 'documents' ? tabParam : 'overview';
  const [activeTab, setActiveTab] = useState<'overview' | 'conmon' | 'documents'>(initialTab);

  useEffect(() => {
    const url = new URL(window.location.href);
    url.searchParams.set('tab', activeTab);
    window.history.replaceState({}, '', url.toString());
  }, [activeTab]);
```

- [ ] **Step 3: Wrap existing content in tabs**

Locate the JSX section that currently renders the existing Authorization Details + Digital Signature + Conditions + Authorization Document cards. Wrap it like this (the existing markup goes inside the `OverviewTab`'s `children` slot):

```tsx
  return (
    <div className="container mx-auto py-8">
      {/* Existing back button + page header stays at top, unchanged */}
      {/* ... existing header ... */}

      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as 'overview' | 'conmon' | 'documents')}>
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="conmon">Continuous Monitoring</TabsTrigger>
          <TabsTrigger value="documents">Documents</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="mt-6">
          <OverviewTab
            authorization={authorization!}
            onAuthorizationUpdated={(updated) => setAuthorization(updated)}
          >
            {/* The existing detail-card markup goes here, unchanged */}
            {/* (Authorization Details card, Digital Signature card, */}
            {/* Conditions card, Authorization Document card) */}
          </OverviewTab>
        </TabsContent>

        <TabsContent value="conmon" className="mt-6">
          <ContinuousMonitoringTab />
        </TabsContent>

        <TabsContent value="documents" className="mt-6">
          <DocumentsTab />
        </TabsContent>
      </Tabs>
    </div>
  );
```

The existing cards inside `<OverviewTab>` keep their existing state bindings (`isEditing`, `editName`, etc.). The `<SharingAccessCard>` rendered inside `OverviewTab` is gated by `effectiveRole === 'OWNER'` so non-owners simply don't see it.

- [ ] **Step 4: Visual check**

Start the dev server (the user has authorized this), open `http://localhost:3010/authorizations/authorization/<id>`:

```bash
./dev.sh
```

Verify:
- Overview tab shows existing content + Sharing & Access (if you're OWNER)
- Continuous Monitoring tab shows the placeholder
- Documents tab shows the placeholder
- URL updates to `?tab=conmon` etc. when switching tabs

If anything looks broken, capture the error and stop.

- [ ] **Step 5: Commit**

```bash
git add front-end/src/app/authorizations/authorization/[authorizationId]/page.tsx
git diff --cached --stat
git commit -m "feat(authorizations): refactor detail page into Overview/ConMon/Documents tabs"
```

---

## Task 20: Run all relevant tests

**Files:** verification only.

- [ ] **Step 1: Run backend tests**

```bash
cd back-end && mvn surefire:test \
    -Dtest='AuthorizationOrgContextTest,AuthorizationRepositoryOrgScopeTest,AuthorizationServiceOrgIsolationTest,AuthorizationAccessGuardTest,AuthorizationServiceTest,AuthorizationTemplateServiceTest,AuthorizationControllerTest,AuthorizationTemplateControllerTest,AuthorizationResponseTest,AuthorizationTemplateResponseTest,DigitalSignatureServiceTest' \
    -DfailIfNoTests=false 2>&1 | tail -30
```

Expected: all pass.

- [ ] **Step 2: Type-check frontend**

```bash
cd front-end && npx tsc --noEmit 2>&1 | tail -15
```

Expected: no new errors. Pre-existing `catalog/page.tsx` errors are fine.

- [ ] **Step 3: Manual smoke test**

```bash
./dev.sh
```

Then exercise:
1. Login as the authorization creator → see Sharing & Access on Overview.
2. Switch to ConMon and Documents tabs — both render the placeholder.
3. URL shows `?tab=` correctly; refreshing keeps the active tab.
4. Add a grant for a colleague (must be in the same org) — they appear in the list.
5. Change a grant role — toast shows success, list updates.
6. Remove a grant — confirmation toast.
7. Set "Share with all org members as VIEWER" — verify with a second account that they can now see the authorization.
8. Try `OWNER` in the share dropdown (it's not in the list, so this should be impossible from the UI; if you hit the API directly with `OWNER`, expect 400).
9. Login as a non-owner colleague (with VIEWER grant) → they see the page but no Sharing & Access card and no edit buttons.

If anything breaks, fix it — don't ship a broken UI.

---

## Self-Review Checklist (run after Task 20)

- [ ] Spec coverage:
  - [ ] V1.7 migration creates `authorization_grants` table + `share_with_org_default_role` column ✓ (Task 1)
  - [ ] `AuthorizationRole` enum + `AuthorizationGrant` entity + repository ✓ (Tasks 2, 3, 4)
  - [ ] Authorization entity has `shareWithOrgDefaultRole` and grants relationship ✓ (Task 5)
  - [ ] `AuthorizationAccessGuard` resolves effective role with all 5 sources (super-admin, org-admin, creator, grant, share-with-org) ✓ (Task 6)
  - [ ] Service-layer write paths consult the guard ✓ (Task 7)
  - [ ] 5 grant management endpoints + 1 share-with-org endpoint ✓ (Task 8)
  - [ ] `effectiveRole` and `shareWithOrgDefaultRole` on response ✓ (Task 9)
  - [ ] Org-members endpoint for the picker ✓ (Task 10)
  - [ ] Frontend types + API client + UserPicker + SharingAccessCard ✓ (Tasks 13–16)
  - [ ] Tabs (Overview, ConMon stub, Documents stub) on detail page ✓ (Tasks 17–19)
  - [ ] Tests covering the access guard and the new endpoints ✓ (Tasks 6, 12)

- [ ] No placeholders — every code block compiles, every test runs.
- [ ] Type consistency — `AuthorizationRole`, `AuthorizationGrantResponse`, `OrgMemberResponse`, `effectiveRole`, `shareWithOrgDefaultRole`, `AuthorizationAccessGuard`, `InsufficientAuthorizationRoleException`, `requireWriteDetails`/`requireDelete`/`requireManageGrants`/`requireDeleteOwnedItem` are used consistently across tasks.

## Out of Scope for This Plan (covered in subsequent PRs)

- ConMon implementation (PR 4)
- Documents implementation (PR 3)
- Pagination of the grants list (current implementation returns all grants — fine for sub-100 grants per authorization)
- Bulk-grant operations
- Audit log of grant changes (the `grantedAt` and `grantedBy` columns capture the data; UI surface for it is a future enhancement)
- `@ControllerAdvice` to surface `NoActiveOrganizationException` as 403 instead of 500 (noted in PR 1's follow-ups; still applies here)
