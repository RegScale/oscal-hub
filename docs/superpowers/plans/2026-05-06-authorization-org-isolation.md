# Authorization Multi-Tenant Org Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `organization_id` to `Authorization` and `AuthorizationTemplate` so authorizations cannot leak across tenants. Every read/write path filters by the current user's primary organization. This is **PR 1 of 4** in the broader Authorizations expansion (see `docs/superpowers/specs/2026-05-06-conmon-and-documents-design.md`); ACL grants, ConMon, and Documents are subsequent PRs.

**Architecture:** Add `organization_id` FK to both entity tables via a Flyway migration that backfills from the creator's lowest-id ACTIVE `organization_membership` (fail-noisy if any creator has no active membership). Introduce a small `AuthorizationOrgContext` helper service that resolves "the current user's primary organization." Replace global repository queries with org-scoped variants. Service layer reads filter by org and out-of-org access returns 404. Controllers stay thin — they pull `Principal`, hand off to the service. **No ACL grants in this PR** — those come in PR 2; for now write-access still uses the existing "creator only" rule for delete and update.

**Tech Stack:** Spring Boot 4.0.x, Spring Data JPA, Flyway, PostgreSQL, JUnit 5, Mockito, `@WebMvcTest` for controller tests, `@DataJpaTest` for repository tests, JWT-based authentication via `Principal`.

---

## File Structure

**New files:**
- `back-end/src/main/resources/db/migration/V1.6__authorization_org_isolation.sql` — schema migration with backfill.
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationOrgContext.java` — single helper that resolves the current user's primary org. Reused by `AuthorizationService` and `AuthorizationTemplateService`. Will become the org-resolution arm of `AuthorizationAccessGuard` in PR 2.
- `back-end/src/test/java/gov/nist/oscal/tools/api/service/AuthorizationOrgContextTest.java` — unit tests for the org resolver.
- `back-end/src/test/java/gov/nist/oscal/tools/api/repository/AuthorizationRepositoryOrgScopeTest.java` — `@DataJpaTest` verifying SQL-level org filtering.
- `back-end/src/test/java/gov/nist/oscal/tools/api/service/AuthorizationServiceOrgIsolationTest.java` — service-layer org isolation behavior.

**Modified files:**
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/Authorization.java` — add `organization` `@ManyToOne` field.
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/AuthorizationTemplate.java` — same.
- `back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuthorizationRepository.java` — replace global queries with org-scoped variants.
- `back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuthorizationTemplateRepository.java` — same.
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationService.java` — every public method becomes org-scoped; create() sets organization from current user.
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationTemplateService.java` — same.
- `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationController.java` — minor: pass `Principal` into the search/list endpoints that don't currently take it.
- `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationTemplateController.java` — same.
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationResponse.java` — add `organizationId` field.
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationTemplateResponse.java` — add `organizationId` field.
- `front-end/src/types/oscal.ts` — add `organizationId: number` to `AuthorizationResponse`.
- `back-end/src/test/java/gov/nist/oscal/tools/api/controller/AuthorizationControllerTest.java` — only if it exists and breaks; mock new service signatures otherwise.

---

## Conventions Used in This Plan

- **Org resolution:** "Current user's primary organization" = the `Organization` from the user's lowest-id `OrganizationMembership` where `status = ACTIVE`. Deterministic, single-org-per-user model. If a user has no active membership, authorization create/list/etc. fail with `403 Forbidden — user has no active organization membership`.
- **404 vs 403:** Out-of-org reads return `404` (don't leak existence). Out-of-org writes return `404` (same — service short-circuits before checking write rules).
- **Existing creator-only delete rule:** Unchanged in this PR. PR 2 replaces it with the role matrix.
- **Migration idempotency:** All DDL uses `IF NOT EXISTS`. The backfill UPDATE is naturally idempotent (running it again on a populated row is a no-op since the values won't change). The NOT NULL enforcement at the end fails loudly if anything didn't backfill.
- **Test database:** `@DataJpaTest` requires a test profile with an embedded DB. If the project doesn't already have one configured (check `back-end/src/test/resources/application*.properties`), add `spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL` and `spring.jpa.hibernate.ddl-auto=create-drop` to a new `application-test.properties`, and annotate the @DataJpaTest with `@ActiveProfiles("test")`. We do not add Testcontainers in this PR.
- **Field naming caveat:** The plan assumes the existing `AuthorizationService` field for the template repo is named `authorizationTemplateRepository`. If the actual code names it differently (e.g., `templateRepository`), use the actual name. Same caveat for any other dep (e.g., `authorizationRepository` vs `authRepository`).
- **Lombok caveat:** The plan writes explicit getters/setters for new fields. If a given entity already uses Lombok `@Data`/`@Getter`/`@Setter`, skip the explicit accessors — Lombok generates them. Match whatever the surrounding code does.
- **Existing `UserRepository` dependency:** If `AuthorizationService` already injects `UserRepository`, reuse the existing field rather than adding a duplicate. Same for `AuthorizationTemplateService`.

---

## Task 1: Write the Flyway migration

**Files:**
- Create: `back-end/src/main/resources/db/migration/V1.6__authorization_org_isolation.sql`

- [ ] **Step 1: Create the migration file**

Write to `back-end/src/main/resources/db/migration/V1.6__authorization_org_isolation.sql`:

```sql
-- V1.6 — Add organization_id FK to authorizations and authorization_templates.
-- Backfills from the creator's lowest-id ACTIVE organization_membership.
-- Fails noisily if any row cannot be backfilled (creator has no active membership).
-- See docs/superpowers/specs/2026-05-06-conmon-and-documents-design.md.

-- 1. Add nullable column on both tables.
ALTER TABLE authorizations
    ADD COLUMN IF NOT EXISTS organization_id BIGINT;

ALTER TABLE authorization_templates
    ADD COLUMN IF NOT EXISTS organization_id BIGINT;

-- 2. Backfill: pick the creator's lowest-id ACTIVE membership.
UPDATE authorizations a
SET organization_id = (
    SELECT m.organization_id
    FROM organization_memberships m
    WHERE m.user_id = a.authorized_by
      AND m.status = 'ACTIVE'
    ORDER BY m.id ASC
    LIMIT 1
)
WHERE a.organization_id IS NULL;

UPDATE authorization_templates t
SET organization_id = (
    SELECT m.organization_id
    FROM organization_memberships m
    WHERE m.user_id = t.created_by
      AND m.status = 'ACTIVE'
    ORDER BY m.id ASC
    LIMIT 1
)
WHERE t.organization_id IS NULL;

-- 3. Fail-noisy guard: surface unbackfillable rows before NOT NULL.
DO $$
DECLARE
    auth_count INT;
    tpl_count INT;
    auth_ids TEXT;
    tpl_ids TEXT;
BEGIN
    SELECT COUNT(*), STRING_AGG(id::TEXT, ',')
      INTO auth_count, auth_ids
      FROM authorizations
      WHERE organization_id IS NULL;

    SELECT COUNT(*), STRING_AGG(id::TEXT, ',')
      INTO tpl_count, tpl_ids
      FROM authorization_templates
      WHERE organization_id IS NULL;

    IF auth_count > 0 OR tpl_count > 0 THEN
        RAISE EXCEPTION
          'V1.6 backfill incomplete. Authorizations missing org: % (ids: %). Templates missing org: % (ids: %). A SUPER_ADMIN must assign an active OrganizationMembership to each creator (or delete the orphan row), then re-run the migration.',
          auth_count, COALESCE(auth_ids, ''), tpl_count, COALESCE(tpl_ids, '');
    END IF;
END $$;

-- 4. Enforce NOT NULL and FK.
ALTER TABLE authorizations
    ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE authorization_templates
    ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE authorizations
    DROP CONSTRAINT IF EXISTS fk_authorizations_organization;
ALTER TABLE authorizations
    ADD CONSTRAINT fk_authorizations_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE authorization_templates
    DROP CONSTRAINT IF EXISTS fk_authorization_templates_organization;
ALTER TABLE authorization_templates
    ADD CONSTRAINT fk_authorization_templates_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- 5. Indexes for the most common query: list-by-org.
CREATE INDEX IF NOT EXISTS idx_authorizations_org
    ON authorizations (organization_id);

CREATE INDEX IF NOT EXISTS idx_authorization_templates_org
    ON authorization_templates (organization_id);
```

- [ ] **Step 2: Verify locally that the migration is well-formed**

Run a syntax check by attempting a Flyway dry-run is overkill here; instead, paste the file into a Postgres `psql` REPL connected to the dev database (after taking a snapshot) and confirm it runs. If you don't have psql access, skip — the integration test in Task 14 will exercise it.

Run: `psql -h localhost -U oscal_user -d oscal_dev -f back-end/src/main/resources/db/migration/V1.6__authorization_org_isolation.sql --single-transaction`
Expected: success with two `ALTER TABLE` notices and no errors. If the dev DB has authorizations whose creators lack active org memberships, the migration will RAISE EXCEPTION with their IDs — that is the intended behavior; assign memberships and re-run.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/resources/db/migration/V1.6__authorization_org_isolation.sql
git commit -m "db(authorizations): V1.6 add organization_id with fail-noisy backfill"
```

---

## Task 2: Add organizationId to Authorization entity

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/Authorization.java`

- [ ] **Step 1: Add the relationship field, getter, and setter**

Locate the `authorizedBy` `@ManyToOne` field in `Authorization.java`. Immediately after it, add:

```java
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
```

Add the matching imports at the top of the file:

```java
import gov.nist.oscal.tools.api.entity.Organization;
import jakarta.persistence.FetchType;
```

(If `Organization` is in the same package `gov.nist.oscal.tools.api.entity`, the import is unnecessary; remove it.)

At the end of the existing getters/setters block, add:

```java
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
```

- [ ] **Step 2: Compile to verify**

Run: `mvn -f back-end/pom.xml -pl back-end compile -DskipTests`
Expected: BUILD SUCCESS. If it fails, the most likely cause is a missing import; check the error message and fix.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/Authorization.java
git commit -m "feat(authorizations): add organization FK to Authorization entity"
```

---

## Task 3: Add organizationId to AuthorizationTemplate entity

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/AuthorizationTemplate.java`

- [ ] **Step 1: Add the relationship field, getter, and setter**

Locate the `createdBy` `@ManyToOne` field. Immediately after it, add:

```java
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
```

Add the import at the top of the file (if `Organization` lives in a different package — check, and skip if it doesn't):

```java
import jakarta.persistence.FetchType;
```

At the end of the existing getters/setters block, add:

```java
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
```

- [ ] **Step 2: Compile to verify**

Run: `mvn -f back-end/pom.xml -pl back-end compile -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/AuthorizationTemplate.java
git commit -m "feat(authorizations): add organization FK to AuthorizationTemplate entity"
```

---

## Task 4: Build the AuthorizationOrgContext helper (TDD)

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationOrgContext.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/AuthorizationOrgContextTest.java`

This service has two responsibilities:

1. `Organization requirePrimaryOrganization(User user)` — returns the user's lowest-id ACTIVE `OrganizationMembership.organization`, or throws a `NoActiveOrganizationException`.
2. `Organization requirePrimaryOrganization(String username)` — convenience overload that loads the user from `UserRepository` and delegates.

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/service/AuthorizationOrgContextTest.java`:

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.MembershipStatus;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.NoActiveOrganizationException;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationOrgContextTest {

    @Mock
    OrganizationMembershipRepository membershipRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    AuthorizationOrgContext orgContext;

    private User user;
    private Organization orgA;
    private Organization orgB;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
        user.setUsername("alice");

        orgA = new Organization();
        orgA.setId(100L);

        orgB = new Organization();
        orgB.setId(101L);
    }

    private OrganizationMembership membership(Long id, Organization org, MembershipStatus status) {
        OrganizationMembership m = new OrganizationMembership();
        m.setId(id);
        m.setUser(user);
        m.setOrganization(org);
        m.setRole(OrganizationRole.USER);
        m.setStatus(status);
        return m;
    }

    @Test
    void requirePrimaryOrganization_singleActiveMembership_returnsThatOrg() {
        when(membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership(1L, orgA, MembershipStatus.ACTIVE)));

        Organization result = orgContext.requirePrimaryOrganization(user);

        assertThat(result).isEqualTo(orgA);
    }

    @Test
    void requirePrimaryOrganization_multipleActiveMemberships_returnsLowestId() {
        when(membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE))
                .thenReturn(List.of(
                        membership(7L, orgB, MembershipStatus.ACTIVE),
                        membership(3L, orgA, MembershipStatus.ACTIVE)));

        Organization result = orgContext.requirePrimaryOrganization(user);

        assertThat(result).isEqualTo(orgA);
    }

    @Test
    void requirePrimaryOrganization_noActiveMembership_throws() {
        when(membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE))
                .thenReturn(List.of());

        assertThatThrownBy(() -> orgContext.requirePrimaryOrganization(user))
                .isInstanceOf(NoActiveOrganizationException.class)
                .hasMessageContaining("alice");
    }

    @Test
    void requirePrimaryOrganization_byUsername_resolvesUserThenOrg() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership(1L, orgA, MembershipStatus.ACTIVE)));

        Organization result = orgContext.requirePrimaryOrganization("alice");

        assertThat(result).isEqualTo(orgA);
    }

    @Test
    void requirePrimaryOrganization_byUsername_userNotFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orgContext.requirePrimaryOrganization("ghost"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost");
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails (compile error — class doesn't exist yet)**

Run: `mvn -f back-end/pom.xml -pl back-end test -Dtest=AuthorizationOrgContextTest`
Expected: COMPILATION FAILURE — `AuthorizationOrgContext` and/or `NoActiveOrganizationException` don't exist.

- [ ] **Step 3: Create the exception class**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/exception/NoActiveOrganizationException.java`:

```java
package gov.nist.oscal.tools.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NoActiveOrganizationException extends RuntimeException {

    public NoActiveOrganizationException(String username) {
        super("User '" + username + "' has no active organization membership.");
    }
}
```

- [ ] **Step 4: Create the AuthorizationOrgContext service**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationOrgContext.java`:

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.MembershipStatus;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.NoActiveOrganizationException;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Resolves the "primary" organization for a user. The primary organization
 * is the lowest-id ACTIVE OrganizationMembership. Authorizations created
 * by a user are scoped to that user's primary organization.
 */
@Service
public class AuthorizationOrgContext {

    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public AuthorizationOrgContext(OrganizationMembershipRepository membershipRepository,
                                   UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    public Organization requirePrimaryOrganization(User user) {
        List<OrganizationMembership> memberships =
                membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE);
        return memberships.stream()
                .min(Comparator.comparing(OrganizationMembership::getId))
                .map(OrganizationMembership::getOrganization)
                .orElseThrow(() -> new NoActiveOrganizationException(user.getUsername()));
    }

    public Organization requirePrimaryOrganization(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User '" + username + "' not found."));
        return requirePrimaryOrganization(user);
    }
}
```

- [ ] **Step 5: Run the test to confirm it passes**

Run: `mvn -f back-end/pom.xml -pl back-end test -Dtest=AuthorizationOrgContextTest`
Expected: BUILD SUCCESS, all 5 tests passing.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/exception/NoActiveOrganizationException.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationOrgContext.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/AuthorizationOrgContextTest.java
git commit -m "feat(authorizations): add AuthorizationOrgContext for primary-org resolution"
```

---

## Task 5: Org-scope AuthorizationRepository (TDD with @DataJpaTest)

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuthorizationRepository.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/repository/AuthorizationRepositoryOrgScopeTest.java`

We need org-scoped variants of every "list/search" method. Existing creator-scoped methods stay (for now — used by other features). New methods take an `Organization`.

- [ ] **Step 1: Write the failing repository test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/repository/AuthorizationRepositoryOrgScopeTest.java`:

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuthorizationRepositoryOrgScopeTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    AuthorizationRepository repo;

    Organization orgA;
    Organization orgB;
    User userA;
    User userB;
    AuthorizationTemplate templateA;
    AuthorizationTemplate templateB;

    @BeforeEach
    void setUp() {
        orgA = newOrg("Org A");
        orgB = newOrg("Org B");
        userA = newUser("alice", orgA);
        userB = newUser("bob", orgB);
        templateA = newTemplate("TA", userA, orgA);
        templateB = newTemplate("TB", userB, orgB);

        newAuthorization("Auth A1", userA, templateA, orgA);
        newAuthorization("Auth A2", userA, templateA, orgA);
        newAuthorization("Auth B1", userB, templateB, orgB);
        em.flush();
    }

    @Test
    void findByOrganization_returnsOnlyThatOrg() {
        List<Authorization> orgAResults = repo.findByOrganization(orgA);
        List<Authorization> orgBResults = repo.findByOrganization(orgB);

        assertThat(orgAResults).extracting(Authorization::getName)
                .containsExactlyInAnyOrder("Auth A1", "Auth A2");
        assertThat(orgBResults).extracting(Authorization::getName)
                .containsExactly("Auth B1");
    }

    @Test
    void findByIdAndOrganization_correctOrg_returnsRow() {
        Authorization a1 = repo.findByOrganization(orgA).get(0);

        var found = repo.findByIdAndOrganization(a1.getId(), orgA);

        assertThat(found).isPresent();
    }

    @Test
    void findByIdAndOrganization_wrongOrg_returnsEmpty() {
        Authorization a1 = repo.findByOrganization(orgA).get(0);

        var found = repo.findByIdAndOrganization(a1.getId(), orgB);

        assertThat(found).isEmpty();
    }

    @Test
    void findByOrganizationOrderByAuthorizedAtDesc_returnsOrgScopedNewestFirst() {
        List<Authorization> result = repo.findByOrganizationOrderByAuthorizedAtDesc(
                orgA, PageRequest.of(0, 10)).getContent();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Authorization::getOrganization)
                .containsOnly(orgA);
    }

    @Test
    void searchByNameOrSspItemIdAndOrganization_filtersByOrg() {
        List<Authorization> result =
                repo.searchByNameOrSspItemIdAndOrganization("Auth", orgA);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Authorization::getOrganization).containsOnly(orgA);
    }

    // --- helpers ---

    private Organization newOrg(String name) {
        Organization o = new Organization();
        o.setName(name);
        return em.persist(o);
    }

    private User newUser(String username, Organization org) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test");
        u.setPassword("x");
        return em.persist(u);
    }

    private AuthorizationTemplate newTemplate(String name, User creator, Organization org) {
        AuthorizationTemplate t = new AuthorizationTemplate();
        t.setName(name);
        t.setContent("body");
        t.setCreatedBy(creator);
        t.setCreatedAt(LocalDateTime.now());
        t.setLastUpdatedAt(LocalDateTime.now());
        t.setOrganization(org);
        return em.persist(t);
    }

    private Authorization newAuthorization(String name, User creator,
                                           AuthorizationTemplate template, Organization org) {
        Authorization a = new Authorization();
        a.setName(name);
        a.setSspItemId("ssp-" + name);
        a.setTemplate(template);
        a.setAuthorizedBy(creator);
        a.setAuthorizedAt(LocalDateTime.now());
        a.setCreatedAt(LocalDateTime.now());
        a.setVariableValues(new HashMap<>());
        a.setOrganization(org);
        a.setDateExpired(LocalDate.now().plusYears(1));
        a.setSystemOwner("o");
        a.setSecurityManager("sm");
        a.setAuthorizingOfficial("ao");
        return em.persist(a);
    }
}
```

- [ ] **Step 2: Run to confirm it fails**

Run: `mvn -f back-end/pom.xml -pl back-end test -Dtest=AuthorizationRepositoryOrgScopeTest`
Expected: COMPILATION FAILURE — methods `findByOrganization`, `findByIdAndOrganization`, etc. don't exist.

- [ ] **Step 3: Add the new methods to AuthorizationRepository**

Open `back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuthorizationRepository.java` and add (keeping existing methods intact):

```java
    // --- Org-scoped queries (multi-tenant isolation) ---

    List<Authorization> findByOrganization(Organization organization);

    Optional<Authorization> findByIdAndOrganization(Long id, Organization organization);

    Page<Authorization> findByOrganizationOrderByAuthorizedAtDesc(
            Organization organization, Pageable pageable);

    @Query("SELECT a FROM Authorization a WHERE a.organization = :organization AND " +
           "a.sspItemId = :sspItemId")
    List<Authorization> findBySspItemIdAndOrganization(
            @Param("sspItemId") String sspItemId,
            @Param("organization") Organization organization);

    @Query("SELECT a FROM Authorization a WHERE a.organization = :organization AND (" +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.sspItemId) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Authorization> searchByNameOrSspItemIdAndOrganization(
            @Param("searchTerm") String searchTerm,
            @Param("organization") Organization organization);
```

Add the imports if missing:

```java
import gov.nist.oscal.tools.api.entity.Organization;
import java.util.Optional;
```

- [ ] **Step 4: Run the test to confirm it passes**

Run: `mvn -f back-end/pom.xml -pl back-end test -Dtest=AuthorizationRepositoryOrgScopeTest`
Expected: BUILD SUCCESS, all 5 tests passing.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuthorizationRepository.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/repository/AuthorizationRepositoryOrgScopeTest.java
git commit -m "feat(authorizations): add org-scoped queries to AuthorizationRepository"
```

---

## Task 6: Org-scope AuthorizationTemplateRepository

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuthorizationTemplateRepository.java`

Templates need the same treatment but the surface area is smaller.

- [ ] **Step 1: Add new org-scoped methods**

In `AuthorizationTemplateRepository.java`, add:

```java
    // --- Org-scoped queries ---

    List<AuthorizationTemplate> findByOrganization(Organization organization);

    Optional<AuthorizationTemplate> findByIdAndOrganization(Long id, Organization organization);

    List<AuthorizationTemplate> findByOrganizationOrderByLastUpdatedAtDesc(
            Organization organization);

    @Query("SELECT t FROM AuthorizationTemplate t WHERE t.organization = :organization AND " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<AuthorizationTemplate> searchByNameAndOrganization(
            @Param("searchTerm") String searchTerm,
            @Param("organization") Organization organization);
```

Add imports if missing:

```java
import gov.nist.oscal.tools.api.entity.Organization;
import java.util.Optional;
```

- [ ] **Step 2: Compile to verify**

Run: `mvn -f back-end/pom.xml -pl back-end compile -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/repository/AuthorizationTemplateRepository.java
git commit -m "feat(authorizations): add org-scoped queries to AuthorizationTemplateRepository"
```

---

## Task 7: Org-isolate AuthorizationService (TDD)

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationService.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/service/AuthorizationServiceOrgIsolationTest.java`

This is the largest task. Each public read method gets the user's org filter; create/update set the org from the current user's primary org; out-of-org gets become 404.

- [ ] **Step 1: Write the failing service test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/service/AuthorizationServiceOrgIsolationTest.java`:

```java
package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.AuthorizationNotFoundException;
import gov.nist.oscal.tools.api.repository.AuthorizationRepository;
import gov.nist.oscal.tools.api.repository.AuthorizationTemplateRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceOrgIsolationTest {

    @Mock AuthorizationRepository authRepo;
    @Mock AuthorizationTemplateRepository templateRepo;
    @Mock UserRepository userRepo;
    @Mock AuthorizationOrgContext orgContext;

    @InjectMocks
    AuthorizationService service;

    Organization orgA;
    Organization orgB;
    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        orgA = new Organization(); orgA.setId(100L); orgA.setName("A");
        orgB = new Organization(); orgB.setId(101L); orgB.setName("B");

        alice = new User(); alice.setId(1L); alice.setUsername("alice");
        bob = new User(); bob.setId(2L); bob.setUsername("bob");
    }

    private Authorization auth(Long id, Organization org) {
        Authorization a = new Authorization();
        a.setId(id);
        a.setName("auth-" + id);
        a.setOrganization(org);
        a.setAuthorizedBy(alice);
        a.setAuthorizedAt(LocalDateTime.now());
        a.setCreatedAt(LocalDateTime.now());
        a.setVariableValues(new HashMap<>());
        return a;
    }

    @Test
    void getAllAuthorizations_filtersByCurrentUserOrg() {
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(orgContext.requirePrimaryOrganization(alice)).thenReturn(orgA);
        when(authRepo.findByOrganization(orgA)).thenReturn(List.of(auth(1L, orgA), auth(2L, orgA)));

        List<Authorization> result = service.getAllAuthorizationsForUser("alice");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Authorization::getOrganization).containsOnly(orgA);
    }

    @Test
    void getAuthorization_inUserOrg_returnsRow() {
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(orgContext.requirePrimaryOrganization(alice)).thenReturn(orgA);
        when(authRepo.findByIdAndOrganization(1L, orgA)).thenReturn(Optional.of(auth(1L, orgA)));

        Authorization result = service.getAuthorizationForUser(1L, "alice");

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getAuthorization_outsideUserOrg_throws404() {
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(orgContext.requirePrimaryOrganization(bob)).thenReturn(orgB);
        when(authRepo.findByIdAndOrganization(1L, orgB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAuthorizationForUser(1L, "bob"))
                .isInstanceOf(AuthorizationNotFoundException.class);
    }

    @Test
    void deleteAuthorization_outsideUserOrg_throws404() {
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(orgContext.requirePrimaryOrganization(bob)).thenReturn(orgB);
        when(authRepo.findByIdAndOrganization(1L, orgB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAuthorization(1L, "bob"))
                .isInstanceOf(AuthorizationNotFoundException.class);
    }

    @Test
    void createAuthorization_setsOrgFromCurrentUserPrimary() {
        AuthorizationTemplate t = new AuthorizationTemplate();
        t.setId(50L);
        t.setOrganization(orgA);
        t.setContent("body");

        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(orgContext.requirePrimaryOrganization(alice)).thenReturn(orgA);
        when(templateRepo.findByIdAndOrganization(50L, orgA)).thenReturn(Optional.of(t));
        when(authRepo.save(any(Authorization.class))).thenAnswer(inv -> inv.getArgument(0));

        Authorization created = service.createAuthorization(
                "name", "ssp1", null, 50L, new HashMap<>(), "alice",
                null, "2027-01-01", "owner", "sm", "ao", null, List.of());

        assertThat(created.getOrganization()).isEqualTo(orgA);
    }

    @Test
    void createAuthorization_templateOutsideUserOrg_throws() {
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(orgContext.requirePrimaryOrganization(bob)).thenReturn(orgB);
        when(templateRepo.findByIdAndOrganization(50L, orgB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAuthorization(
                "name", "ssp1", null, 50L, new HashMap<>(), "bob",
                null, "2027-01-01", "owner", "sm", "ao", null, List.of()))
                .isInstanceOf(AuthorizationNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run to confirm it fails**

Run: `mvn -f back-end/pom.xml -pl back-end test -Dtest=AuthorizationServiceOrgIsolationTest`
Expected: COMPILATION FAILURE — `getAllAuthorizationsForUser`, `getAuthorizationForUser`, `AuthorizationNotFoundException` don't exist.

- [ ] **Step 3: Create AuthorizationNotFoundException**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/exception/AuthorizationNotFoundException.java`:

```java
package gov.nist.oscal.tools.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AuthorizationNotFoundException extends RuntimeException {

    public AuthorizationNotFoundException(Long id) {
        super("Authorization " + id + " not found.");
    }
}
```

- [ ] **Step 4: Modify AuthorizationService**

In `AuthorizationService.java`:

(a) Add the new dependencies and constructor parameters. If the existing class uses field injection, switch to constructor injection on these two fields specifically (or match whatever the existing pattern is — preserve consistency). Add:

```java
    private final AuthorizationOrgContext orgContext;
    private final UserRepository userRepository;
```

If the existing class has a constructor, add `AuthorizationOrgContext orgContext` and `UserRepository userRepository` parameters and assign them. If the existing class uses `@Autowired` field injection, add those two fields with `@Autowired` to match.

(b) Add new public methods (do NOT delete existing methods — they're called by other features that we'll wean off in PR 2):

```java
    @Transactional(readOnly = true)
    public List<Authorization> getAllAuthorizationsForUser(String username) {
        Organization org = resolveUserOrg(username);
        return authorizationRepository.findByOrganization(org);
    }

    @Transactional(readOnly = true)
    public Authorization getAuthorizationForUser(Long id, String username) {
        Organization org = resolveUserOrg(username);
        return authorizationRepository.findByIdAndOrganization(id, org)
                .orElseThrow(() -> new AuthorizationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Authorization> searchAuthorizationsForUser(String username, String searchTerm) {
        Organization org = resolveUserOrg(username);
        if (searchTerm == null || searchTerm.isBlank()) {
            return authorizationRepository.findByOrganization(org);
        }
        return authorizationRepository.searchByNameOrSspItemIdAndOrganization(searchTerm, org);
    }

    @Transactional(readOnly = true)
    public List<Authorization> getAuthorizationsBySspForUser(String sspItemId, String username) {
        Organization org = resolveUserOrg(username);
        return authorizationRepository.findBySspItemIdAndOrganization(sspItemId, org);
    }

    private Organization resolveUserOrg(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User '" + username + "' not found."));
        return orgContext.requirePrimaryOrganization(user);
    }
```

(c) Modify the existing `createAuthorization` method. Find the line that loads the template (currently uses `authorizationTemplateRepository.findById(templateId)`). Replace that block with:

```java
        Organization userOrg = resolveUserOrg(username);
        AuthorizationTemplate template = authorizationTemplateRepository
                .findByIdAndOrganization(templateId, userOrg)
                .orElseThrow(() -> new AuthorizationNotFoundException(templateId));
```

Then, before the `authorizationRepository.save(authorization)` call, add:

```java
        authorization.setOrganization(userOrg);
```

(d) Modify the existing `updateAuthorization` method. Find the line that loads the authorization (currently `authorizationRepository.findById(id)`). Replace with:

```java
        Organization userOrg = resolveUserOrg(username);
        Authorization authorization = authorizationRepository.findByIdAndOrganization(id, userOrg)
                .orElseThrow(() -> new AuthorizationNotFoundException(id));
```

(e) Modify the existing `deleteAuthorization(Long id, String username)`. Replace the load line with the same pattern:

```java
        Organization userOrg = resolveUserOrg(username);
        Authorization authorization = authorizationRepository.findByIdAndOrganization(id, userOrg)
                .orElseThrow(() -> new AuthorizationNotFoundException(id));
```

The existing creator-only check (`authorization.getAuthorizedBy().getUsername().equals(username)`) stays — out-of-org users now get a 404 instead of reaching that check.

(f) Add imports as needed:

```java
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.exception.AuthorizationNotFoundException;
import gov.nist.oscal.tools.api.repository.UserRepository;
```

- [ ] **Step 5: Run the test to confirm it passes**

Run: `mvn -f back-end/pom.xml -pl back-end test -Dtest=AuthorizationServiceOrgIsolationTest`
Expected: BUILD SUCCESS, 6 tests passing.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/exception/AuthorizationNotFoundException.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationService.java \
        back-end/src/test/java/gov/nist/oscal/tools/api/service/AuthorizationServiceOrgIsolationTest.java
git commit -m "feat(authorizations): org-isolate AuthorizationService reads/writes"
```

---

## Task 8: Org-isolate AuthorizationTemplateService

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationTemplateService.java`

- [ ] **Step 1: Add dependencies**

In the class fields/constructor, add:

```java
    private final AuthorizationOrgContext orgContext;
    private final UserRepository userRepository;
```

(Match the existing injection pattern — field-injected or constructor-injected, whichever the file already uses for its other deps.)

- [ ] **Step 2: Add new org-scoped public methods**

Add to `AuthorizationTemplateService`:

```java
    @Transactional(readOnly = true)
    public List<AuthorizationTemplate> getAllTemplatesForUser(String username) {
        Organization org = resolveUserOrg(username);
        return authorizationTemplateRepository.findByOrganization(org);
    }

    @Transactional(readOnly = true)
    public AuthorizationTemplate getTemplateForUser(Long id, String username) {
        Organization org = resolveUserOrg(username);
        return authorizationTemplateRepository.findByIdAndOrganization(id, org)
                .orElseThrow(() -> new AuthorizationTemplateNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<AuthorizationTemplate> getRecentlyUpdatedForUser(String username, int limit) {
        Organization org = resolveUserOrg(username);
        return authorizationTemplateRepository.findByOrganizationOrderByLastUpdatedAtDesc(org)
                .stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorizationTemplate> searchTemplatesForUser(String username, String searchTerm) {
        Organization org = resolveUserOrg(username);
        if (searchTerm == null || searchTerm.isBlank()) {
            return authorizationTemplateRepository.findByOrganization(org);
        }
        return authorizationTemplateRepository.searchByNameAndOrganization(searchTerm, org);
    }

    private Organization resolveUserOrg(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User '" + username + "' not found."));
        return orgContext.requirePrimaryOrganization(user);
    }
```

- [ ] **Step 3: Modify existing create/update/delete to set/check org**

In `createTemplate`, before saving:

```java
        Organization userOrg = resolveUserOrg(username);
        template.setOrganization(userOrg);
```

In `updateTemplate`, replace the `findById(id)` load with:

```java
        Organization userOrg = resolveUserOrg(username);
        AuthorizationTemplate template = authorizationTemplateRepository
                .findByIdAndOrganization(id, userOrg)
                .orElseThrow(() -> new AuthorizationTemplateNotFoundException(id));
```

In `deleteTemplate`, same:

```java
        Organization userOrg = resolveUserOrg(username);
        AuthorizationTemplate template = authorizationTemplateRepository
                .findByIdAndOrganization(id, userOrg)
                .orElseThrow(() -> new AuthorizationTemplateNotFoundException(id));
```

(Existing creator-only check stays.)

- [ ] **Step 4: Create AuthorizationTemplateNotFoundException**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/exception/AuthorizationTemplateNotFoundException.java`:

```java
package gov.nist.oscal.tools.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AuthorizationTemplateNotFoundException extends RuntimeException {

    public AuthorizationTemplateNotFoundException(Long id) {
        super("Authorization template " + id + " not found.");
    }
}
```

- [ ] **Step 5: Add imports**

```java
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.AuthorizationTemplateNotFoundException;
import gov.nist.oscal.tools.api.repository.UserRepository;
```

- [ ] **Step 6: Compile and run all backend tests**

Run: `mvn -f back-end/pom.xml -pl back-end test`
Expected: BUILD SUCCESS. If existing tests break, the most likely cause is that they instantiated `AuthorizationService` or `AuthorizationTemplateService` directly with the old constructor signature. Update those tests to pass the new dependencies as mocks.

- [ ] **Step 7: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/exception/AuthorizationTemplateNotFoundException.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationTemplateService.java
git commit -m "feat(authorizations): org-isolate AuthorizationTemplateService reads/writes"
```

---

## Task 9: Update AuthorizationController to use org-scoped methods

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationController.java`

The controller already has `Principal` on most endpoints. We need to (a) thread `Principal` through the GET endpoints that don't take it yet, (b) call the new `*ForUser` methods.

- [ ] **Step 1: Update endpoints**

Find each endpoint and update:

```java
    @GetMapping("/{id}")
    public ResponseEntity<AuthorizationResponse> getAuthorization(@PathVariable Long id,
                                                                  Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(
                id, principal.getName());
        return ResponseEntity.ok(toResponse(authorization));
    }

    @GetMapping
    public ResponseEntity<List<AuthorizationResponse>> getAllAuthorizations(Principal principal) {
        List<Authorization> authorizations =
                authorizationService.getAllAuthorizationsForUser(principal.getName());
        return ResponseEntity.ok(authorizations.stream().map(this::toResponse).toList());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<AuthorizationResponse>> getRecentlyAuthorized(
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        List<Authorization> all =
                authorizationService.getAllAuthorizationsForUser(principal.getName());
        List<Authorization> recent = all.stream()
                .sorted((a, b) -> b.getAuthorizedAt().compareTo(a.getAuthorizedAt()))
                .limit(limit)
                .toList();
        return ResponseEntity.ok(recent.stream().map(this::toResponse).toList());
    }

    @GetMapping("/ssp/{sspItemId}")
    public ResponseEntity<List<AuthorizationResponse>> getAuthorizationsBySsp(
            @PathVariable String sspItemId,
            Principal principal) {
        List<Authorization> result = authorizationService.getAuthorizationsBySspForUser(
                sspItemId, principal.getName());
        return ResponseEntity.ok(result.stream().map(this::toResponse).toList());
    }

    @GetMapping("/search")
    public ResponseEntity<List<AuthorizationResponse>> searchAuthorizations(
            @RequestParam(required = false) String q,
            Principal principal) {
        List<Authorization> result = authorizationService.searchAuthorizationsForUser(
                principal.getName(), q);
        return ResponseEntity.ok(result.stream().map(this::toResponse).toList());
    }
```

Leave POST/PUT/DELETE endpoints alone — they already pass `Principal` to the service, which now does the org check internally.

- [ ] **Step 2: Compile**

Run: `mvn -f back-end/pom.xml -pl back-end compile -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationController.java
git commit -m "feat(authorizations): scope controller reads to current user's organization"
```

---

## Task 10: Update AuthorizationTemplateController

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationTemplateController.java`

- [ ] **Step 1: Update endpoints**

Apply the same `Principal` + `*ForUser` pattern to every GET/list/search endpoint on `AuthorizationTemplateController`. POST/PUT/DELETE already pass `Principal`.

For example:

```java
    @GetMapping
    public ResponseEntity<List<AuthorizationTemplateResponse>> getAllTemplates(Principal principal) {
        List<AuthorizationTemplate> templates =
                authorizationTemplateService.getAllTemplatesForUser(principal.getName());
        return ResponseEntity.ok(templates.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorizationTemplateResponse> getTemplate(@PathVariable Long id,
                                                                     Principal principal) {
        AuthorizationTemplate template = authorizationTemplateService
                .getTemplateForUser(id, principal.getName());
        return ResponseEntity.ok(toResponse(template));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<AuthorizationTemplateResponse>> getRecentlyUpdated(
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        List<AuthorizationTemplate> templates = authorizationTemplateService
                .getRecentlyUpdatedForUser(principal.getName(), limit);
        return ResponseEntity.ok(templates.stream().map(this::toResponse).toList());
    }

    @GetMapping("/search")
    public ResponseEntity<List<AuthorizationTemplateResponse>> searchTemplates(
            @RequestParam(required = false) String q,
            Principal principal) {
        List<AuthorizationTemplate> result = authorizationTemplateService
                .searchTemplatesForUser(principal.getName(), q);
        return ResponseEntity.ok(result.stream().map(this::toResponse).toList());
    }
```

- [ ] **Step 2: Compile**

Run: `mvn -f back-end/pom.xml -pl back-end compile -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationTemplateController.java
git commit -m "feat(authorizations): scope template controller reads to current user's organization"
```

---

## Task 11: Add organizationId to AuthorizationResponse

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationResponse.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationTemplateResponse.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationController.java` (the `toResponse` mapper)
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationTemplateController.java` (the `toResponse` mapper)

- [ ] **Step 1: Add field + accessor to AuthorizationResponse**

In `AuthorizationResponse.java`, add:

```java
    private Long organizationId;
```

And the matching getter/setter:

```java
    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
```

- [ ] **Step 2: Same for AuthorizationTemplateResponse**

Add the identical field + getter/setter.

- [ ] **Step 3: Populate it in the controller mappers**

In each controller's `toResponse(...)` method, after the existing `setX` calls, add:

```java
        response.setOrganizationId(authorization.getOrganization().getId());
```

(Use `template.getOrganization().getId()` for templates.)

- [ ] **Step 4: Compile**

Run: `mvn -f back-end/pom.xml -pl back-end compile -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationResponse.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationTemplateResponse.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationController.java \
        back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthorizationTemplateController.java
git commit -m "feat(authorizations): expose organizationId on response models"
```

---

## Task 12: Add organizationId to frontend types

**Files:**
- Modify: `front-end/src/types/oscal.ts`

- [ ] **Step 1: Update the AuthorizationResponse interface**

Find the `AuthorizationResponse` interface (around line 644) and add a new field after `id`:

```typescript
export interface AuthorizationResponse {
  id: number;
  organizationId: number;
  name: string;
  // ... existing fields unchanged
}
```

If `AuthorizationTemplateResponse` is also defined in this file, add `organizationId: number;` to it too.

- [ ] **Step 2: Run TypeScript compile**

Run: `cd front-end && npx tsc --noEmit`
Expected: clean. If a consumer of `AuthorizationResponse` complains about a missing property, it's because that consumer constructs a mock that's now incomplete — patch the mock.

- [ ] **Step 3: Commit**

```bash
git add front-end/src/types/oscal.ts
git commit -m "feat(authorizations): expose organizationId on frontend type"
```

---

## Task 13: Run the full backend test suite

**Files:** _no changes — verification only_

- [ ] **Step 1: Run all backend tests**

Run: `mvn -f back-end/pom.xml -pl back-end test`
Expected: BUILD SUCCESS. If any pre-existing tests fail, they're almost certainly tests that constructed `AuthorizationService` or `AuthorizationTemplateService` with the old constructor and need their mock setup updated to pass `AuthorizationOrgContext` and `UserRepository`.

For each broken test, find the constructor call and add the two new mocks. Example:

```java
@Mock AuthorizationOrgContext orgContext;
@Mock UserRepository userRepository;
```

…and rerun. Repeat until green.

- [ ] **Step 2: If all green, commit any test fixups**

If you needed to patch existing tests:

```bash
git add back-end/src/test/...
git commit -m "test: pass new org-context dependencies in existing service tests"
```

---

## Task 14: Smoke-test against a live dev DB

**Files:** _no changes — verification only. Skip this task in CI; it's for the developer running the change locally._

- [ ] **Step 1: Start the dev stack**

Run: `./dev.sh`
Expected: Postgres up, backend up, frontend up. Flyway runs `V1.6` and reports success in the backend log. If the migration fails noisily on existing data, the message will identify the offending authorization/template IDs — assign their creators an `OrganizationMembership` (status=ACTIVE) and run `./stop.sh && ./dev.sh` again.

- [ ] **Step 2: Authenticate and verify org isolation**

Open `http://localhost:3010/authorizations` and confirm the list loads as expected for the current user. Then, in a separate browser/incognito session, log in as a user from a different organization. Confirm the second user does NOT see the first user's authorizations.

If you have only one test user, create a second org + second user via the org-admin UI first.

- [ ] **Step 3: Verify a 404 on out-of-org access**

While logged in as the second user, attempt to GET `http://localhost:8090/api/authorizations/{id}` with the ID of a first-user authorization. Use:

```bash
curl -i -H "Authorization: Bearer YOUR_USER2_TOKEN" \
     http://localhost:8090/api/authorizations/{first_user_auth_id}
```

Expected: `HTTP/1.1 404 Not Found`.

- [ ] **Step 4: Stop the dev stack**

Run: `./stop.sh`

---

## Self-Review Checklist (run after Task 14)

- [ ] Spec coverage:
  - [ ] `organization_id` added to `authorizations` and `authorization_templates` ✓ (Task 1)
  - [ ] Backfill from creator's lowest-id active membership ✓ (Task 1)
  - [ ] Fail-noisy when backfill incomplete ✓ (Task 1)
  - [ ] NOT NULL + FK + index after backfill ✓ (Task 1)
  - [ ] Service-layer scoping on every read/write ✓ (Tasks 7, 8)
  - [ ] Out-of-org → 404 ✓ (Tasks 7, 8)
  - [ ] `AuthorizationAccessGuard` *org-resolution arm* present ✓ (Task 4 — the full guard with role logic comes in PR 2)
  - [ ] `organizationId` on response model ✓ (Task 11)
  - [ ] Frontend type updated ✓ (Task 12)
- [ ] No placeholders — every code block compiles, every test runs.
- [ ] Type consistency — `Organization`, `AuthorizationOrgContext`, `AuthorizationNotFoundException`, `AuthorizationTemplateNotFoundException`, `NoActiveOrganizationException` are referenced consistently across tasks.

## Out of Scope for This Plan (covered in subsequent PRs)

- `AuthorizationGrant` table and full role matrix (PR 2)
- `share_with_org_default_role` (PR 2)
- Tabbed Overview/ConMon/Documents page (PR 2)
- Continuous Monitoring (PR 4)
- Documents (PR 3)
