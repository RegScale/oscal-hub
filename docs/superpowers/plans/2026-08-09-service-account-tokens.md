# Service Account Tokens — Permissions & Revocation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make service account tokens carry the issuing user's permissions and make them revocable.

**Architecture:** Role claims are snapshotted into the JWT at issuance. Each token also carries a `jti` (UUID) backed by a row in a new `service_account_tokens` table; `JwtAuthenticationFilter` looks that row up and refuses revoked or unknown tokens. Tokens minted before this change have no `jti` and are rejected.

**Tech Stack:** Spring Boot 3.5.9, Spring Security, jjwt 0.12 (builder style: `.claims()`, `.id()`, `.subject()`), JPA/Hibernate with `ddl-auto=validate`, Flyway, PostgreSQL, JUnit 5 + Mockito + MockMvc, Next.js 15 + React + Jest.

**Spec:** `docs/superpowers/specs/2026-08-09-service-account-tokens-design.md`

## Global Constraints

- **Flyway is the schema authority.** Every entity change needs a migration. Hibernate runs `ddl-auto=validate` and will refuse to boot on mismatch. Migrations must be idempotent (`CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`).
- **Next migration version is `V1.18`.** `V1.17__username_case_insensitive_unique.sql` is the current highest.
- **Do not run the app.** `mvn test` is fine and expected. Do not run `./dev.sh`, `mvn clean install`, or `npm run build`.
- **Java 11 source compatibility** in `cli/`; the back-end module targets Java 21. This plan touches only `back-end/` and `front-end/`.
- **Property naming:** the codebase uses `app.*` with `${ENV_VAR:default}` (e.g. `app.base-url=${APP_BASE_URL:http://localhost:3000}`). The spec wrote `oscal.service-tokens.max-expiration-days`; use `app.service-tokens.max-expiration-days` to match the codebase.
- **`@Max` cannot reference a property value** — Java annotations take compile-time constants only. The configurable ceiling is enforced in the controller against an injected `@Value`, not by a bean-validation annotation. `@Min(1)` on the DTO stays as-is.
- **Token type discriminator** is the string literal `service-account`, already used by `JwtUtil.generateServiceAccountToken`.
- **Commit after every task.** Co-author trailer: `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`

## File Structure

**Create:**
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/ServiceAccountToken.java` — the entity and its derived status
- `back-end/src/main/java/gov/nist/oscal/tools/api/repository/ServiceAccountTokenRepository.java` — lookup by jti, revoke-all, touch-last-used
- `back-end/src/main/resources/db/migration/V1.18__service_account_tokens.sql`
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/ServiceAccountTokenSummary.java` — list DTO
- `back-end/src/test/java/gov/nist/oscal/tools/api/entity/ServiceAccountTokenTest.java`
- `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterServiceTokenTest.java`
- `back-end/src/test/java/gov/nist/oscal/tools/api/controller/ServiceAccountTokenControllerTest.java`
- `front-end/__tests__/ServiceAccountTokenGenerator.test.tsx`

**Modify:**
- `back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtUtil.java` — `generateServiceAccountToken` takes the entity; add `extractJti`, `extractTokenType`
- `back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilter.java` — revocation gate
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthService.java` — persist the row
- `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthController.java` — wire creation, add list + revoke
- `back-end/src/main/java/gov/nist/oscal/tools/api/controller/OrganizationController.java` — revoke on archive
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuditEventType.java` — add revoke event
- `back-end/src/main/resources/application.properties` — max-expiration property
- `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtUtilTest.java`
- `front-end/src/lib/api-client.ts`, `front-end/src/types/oscal.ts`, `front-end/src/components/ServiceAccountTokenGenerator.tsx`
- `front-end/src/app/guide/account/service-tokens/page.mdx`, `front-end/src/app/guide/reference/api-automation/page.mdx`

---

### Task 1: Entity, migration, repository

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/ServiceAccountToken.java`
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/ServiceAccountTokenRepository.java`
- Create: `back-end/src/main/resources/db/migration/V1.18__service_account_tokens.sql`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/entity/ServiceAccountTokenTest.java`

**Interfaces:**
- Consumes: nothing (first task)
- Produces:
  - `ServiceAccountTokenRepository.findByIdAndUserId(Long, Long)` (owner-scoped lookup used by the revoke endpoint)
  - `ServiceAccountToken` with `Status { ACTIVE, EXPIRED, REVOKED }` and `Status getStatus()`
  - getters/setters: `getId/setId(Long)`, `getUser/setUser(User)`, `getTokenName/setTokenName(String)`, `getJti/setJti(String)`, `getGlobalRole/setGlobalRole(String)`, `getOrgRole/setOrgRole(String)`, `getOrganizationId/setOrganizationId(Long)`, `getExpiresAt/setExpiresAt(LocalDateTime)`, `getRevokedAt/setRevokedAt(LocalDateTime)`, `getRevokedBy/setRevokedBy(String)`, `getCreatedAt/setCreatedAt(LocalDateTime)`, `getLastUsedAt/setLastUsedAt(LocalDateTime)`
  - `ServiceAccountTokenRepository.findByJti(String)`, `.findByUserIdOrderByCreatedAtDesc(Long)`, `.revokeAllForUser(Long, LocalDateTime, String)`, `.touchLastUsed(Long, LocalDateTime)`

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/entity/ServiceAccountTokenTest.java`:

```java
package gov.nist.oscal.tools.api.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceAccountTokenTest {

    private ServiceAccountToken token(LocalDateTime expiresAt, LocalDateTime revokedAt) {
        ServiceAccountToken t = new ServiceAccountToken();
        t.setExpiresAt(expiresAt);
        t.setRevokedAt(revokedAt);
        return t;
    }

    @Test
    void unexpiredUnrevokedTokenIsActive() {
        ServiceAccountToken t = token(LocalDateTime.now().plusDays(1), null);

        assertEquals(ServiceAccountToken.Status.ACTIVE, t.getStatus());
    }

    @Test
    void pastExpiryReportsExpired() {
        ServiceAccountToken t = token(LocalDateTime.now().minusMinutes(1), null);

        assertEquals(ServiceAccountToken.Status.EXPIRED, t.getStatus());
    }

    @Test
    void revokedTakesPrecedenceOverExpired() {
        ServiceAccountToken t = token(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusHours(2));

        assertEquals(ServiceAccountToken.Status.REVOKED, t.getStatus());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run from `back-end/`: `mvn -q test -Dtest=ServiceAccountTokenTest`
Expected: compilation failure — `cannot find symbol: class ServiceAccountToken`.

- [ ] **Step 3: Write the entity**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/entity/ServiceAccountToken.java`:

```java
package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A long-lived API credential issued from the Profile page.
 * <p>
 * Unlike {@link PasswordResetToken}, no hash of the token is stored. The JWT
 * signature already proves authenticity; this row exists so the token can be
 * listed and revoked, keyed by the {@code jti} claim embedded in the JWT.
 * </p>
 */
@Entity
@Table(name = "service_account_tokens")
public class ServiceAccountToken {

    public enum Status { ACTIVE, EXPIRED, REVOKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_name", nullable = false)
    private String tokenName;

    /** UUID matching the JWT's {@code jti} claim. */
    @Column(name = "jti", nullable = false, unique = true, length = 36)
    private String jti;

    /** Permissions snapshotted at issuance, retained for display and audit. */
    @Column(name = "global_role", length = 50)
    private String globalRole;

    @Column(name = "org_role", length = 50)
    private String orgRole;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private String revokedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Written at most once per hour to keep the auth path cheap. */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Status getStatus() {
        if (revokedAt != null) return Status.REVOKED;
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) return Status.EXPIRED;
        return Status.ACTIVE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTokenName() { return tokenName; }
    public void setTokenName(String tokenName) { this.tokenName = tokenName; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public String getGlobalRole() { return globalRole; }
    public void setGlobalRole(String globalRole) { this.globalRole = globalRole; }

    public String getOrgRole() { return orgRole; }
    public void setOrgRole(String orgRole) { this.orgRole = orgRole; }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=ServiceAccountTokenTest`
Expected: PASS, 3 tests.

- [ ] **Step 5: Write the migration**

Create `back-end/src/main/resources/db/migration/V1.18__service_account_tokens.sql`:

```sql
-- Service account tokens become revocable: each issued JWT carries a jti that
-- maps to a row here. Revocation sets revoked_at; JwtAuthenticationFilter
-- refuses any service-account token whose row is missing or revoked.
--
-- No token value or hash is stored. The JWT signature proves authenticity;
-- this table only supplies identity, revocation state, and display metadata.

CREATE TABLE IF NOT EXISTS service_account_tokens (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_name varchar(255) NOT NULL,
    jti varchar(36) NOT NULL,
    global_role varchar(50),
    org_role varchar(50),
    organization_id bigint,
    expires_at timestamp(6) without time zone NOT NULL,
    revoked_at timestamp(6) without time zone,
    revoked_by varchar(255),
    created_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    last_used_at timestamp(6) without time zone
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_service_account_tokens_jti
    ON service_account_tokens (jti);

CREATE INDEX IF NOT EXISTS idx_service_account_tokens_user
    ON service_account_tokens (user_id);
```

- [ ] **Step 6: Write the repository**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/repository/ServiceAccountTokenRepository.java`:

```java
package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.ServiceAccountToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ServiceAccountTokenRepository extends JpaRepository<ServiceAccountToken, Long> {

    @Query("SELECT t FROM ServiceAccountToken t JOIN FETCH t.user WHERE t.jti = :jti")
    Optional<ServiceAccountToken> findByJti(@Param("jti") String jti);

    List<ServiceAccountToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ServiceAccountToken> findByIdAndUserId(Long id, Long userId);

    /** Revoke every live token for a user — used when an account is archived. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ServiceAccountToken t SET t.revokedAt = :now, t.revokedBy = :revokedBy "
         + "WHERE t.user.id = :userId AND t.revokedAt IS NULL")
    int revokeAllForUser(@Param("userId") Long userId,
                         @Param("now") LocalDateTime now,
                         @Param("revokedBy") String revokedBy);

    /**
     * Called from JwtAuthenticationFilter, which is not transactional — hence
     * the explicit @Transactional, without which @Modifying throws
     * TransactionRequiredException.
     */
    @Transactional
    @Modifying
    @Query("UPDATE ServiceAccountToken t SET t.lastUsedAt = :now WHERE t.id = :id")
    int touchLastUsed(@Param("id") Long id, @Param("now") LocalDateTime now);
}
```

- [ ] **Step 7: Run the full backend suite**

Run: `mvn -q test`
Expected: BUILD SUCCESS, 0 failures. (Adding an entity with `ddl-auto=validate` can break context-loading tests if the migration and entity disagree — this run is what catches that.)

- [ ] **Step 8: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/entity/ServiceAccountToken.java back-end/src/main/java/gov/nist/oscal/tools/api/repository/ServiceAccountTokenRepository.java back-end/src/main/resources/db/migration/V1.18__service_account_tokens.sql back-end/src/test/java/gov/nist/oscal/tools/api/entity/ServiceAccountTokenTest.java
git commit -m "feat(auth): add service_account_tokens entity, migration, repository"
```

---

### Task 2: Snapshot permissions and jti into the token

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtUtil.java:325-350` (`generateServiceAccountToken`)
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtUtilTest.java`

**Interfaces:**
- Consumes: `ServiceAccountToken` from Task 1
- Produces:
  - `String generateServiceAccountToken(ServiceAccountToken record)` — replaces the old `(String, String, int)` signature
  - `String extractJti(String token)`
  - `String extractTokenType(String token)`

**Note:** the existing 3-arg `generateServiceAccountToken(String, String, int)` is deleted. Its only production caller is `AuthController:465` (rewired in Task 3) and its only test caller is `JwtUtilTest.testGenerateServiceAccountToken` (rewritten below).

- [ ] **Step 1: Write the failing test**

In `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtUtilTest.java`, add these imports at the top:

```java
import gov.nist.oscal.tools.api.entity.ServiceAccountToken;
import java.time.LocalDateTime;
```

Then replace the existing `testGenerateServiceAccountToken` test with:

```java
    private ServiceAccountToken serviceTokenRecord() {
        gov.nist.oscal.tools.api.entity.User owner = new gov.nist.oscal.tools.api.entity.User();
        owner.setId(7L);
        owner.setUsername("serviceuser");

        ServiceAccountToken record = new ServiceAccountToken();
        record.setUser(owner);
        record.setTokenName("API Token");
        record.setJti("11111111-2222-3333-4444-555555555555");
        record.setGlobalRole("SUPER_ADMIN");
        record.setOrgRole("ORG_ADMIN");
        record.setOrganizationId(42L);
        record.setExpiresAt(LocalDateTime.now().plusDays(30));
        return record;
    }

    @Test
    void testGenerateServiceAccountToken() {
        String token = jwtUtil.generateServiceAccountToken(serviceTokenRecord());

        assertNotNull(token);
        assertEquals("serviceuser", jwtUtil.extractUsername(token));
        assertEquals("service-account", jwtUtil.extractTokenType(token));
    }

    /**
     * The whole point of the snapshot model: a token minted by a privileged
     * user must carry that privilege. Without these claims the filter's
     * claim-to-authority code grants nothing and the token acts as ROLE_USER.
     */
    @Test
    void testServiceAccountTokenCarriesIssuerPermissions() {
        String token = jwtUtil.generateServiceAccountToken(serviceTokenRecord());

        assertEquals("SUPER_ADMIN", jwtUtil.extractGlobalRole(token));
        assertEquals("ORG_ADMIN", jwtUtil.extractOrganizationRole(token));
        assertEquals(42L, jwtUtil.extractOrganizationId(token));
        assertEquals(7L, jwtUtil.extractUserId(token));
    }

    @Test
    void testServiceAccountTokenCarriesJti() {
        String token = jwtUtil.generateServiceAccountToken(serviceTokenRecord());

        assertEquals("11111111-2222-3333-4444-555555555555", jwtUtil.extractJti(token));
    }

    /** Session tokens have no jti; the filter relies on this to leave them alone. */
    @Test
    void testSessionTokenHasNoTokenType() {
        String token = jwtUtil.generateToken(userDetails);

        assertNull(jwtUtil.extractTokenType(token));
        assertNull(jwtUtil.extractJti(token));
    }
```

Also delete any other assertions in the file that call the old 3-arg signature — search for `generateServiceAccountToken(` and update every call site to `generateServiceAccountToken(serviceTokenRecord())`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=JwtUtilTest`
Expected: compilation failure — no `generateServiceAccountToken(ServiceAccountToken)`, no `extractJti`, no `extractTokenType`.

- [ ] **Step 3: Write the implementation**

In `JwtUtil.java`, add these imports:

```java
import gov.nist.oscal.tools.api.entity.ServiceAccountToken;
import java.time.ZoneId;
```

Add two extractors next to the existing `extractOrganizationId` (around line 103):

```java
    /** The {@code jti} claim — present only on service account tokens. */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    /** {@code "service-account"} for service tokens, null for session tokens. */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("tokenType", String.class));
    }
```

Replace the whole `generateServiceAccountToken` method at the end of the class:

```java
    /**
     * Mint a service account token for a persisted {@link ServiceAccountToken}.
     * <p>
     * Permissions are snapshotted here: the role claims are copied from the
     * record and stay fixed for the token's life. {@code JwtAuthenticationFilter}
     * turns them into authorities with no service-token-specific code — it reads
     * the same claims it reads for session tokens.
     * </p>
     * <p>
     * The {@code jti} makes the token revocable; a token without one cannot be
     * matched to a row and is refused.
     * </p>
     */
    public String generateServiceAccountToken(ServiceAccountToken record) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenName", record.getTokenName());
        claims.put("tokenType", "service-account");
        claims.put("userId", record.getUser().getId());
        claims.put("globalRole", record.getGlobalRole());
        claims.put("orgRole", record.getOrgRole());
        claims.put("organizationId", record.getOrganizationId());

        Date expirationDate = Date.from(
                record.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .claims(claims)
                .id(record.getJti())
                .subject(record.getUser().getUsername())
                .issuedAt(new Date())
                .expiration(expirationDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=JwtUtilTest`
Expected: PASS. Verify the count in `back-end/target/surefire-reports/TEST-gov.nist.oscal.tools.api.security.JwtUtilTest.xml` shows `failures="0" errors="0"`.

Note: `AuthController` still calls the deleted 3-arg overload, so `mvn test` for the whole module will not compile until Task 3. Run only `-Dtest=JwtUtilTest` here.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtUtil.java back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtUtilTest.java
git commit -m "feat(auth): snapshot issuer permissions and a jti into service account tokens"
```

---

### Task 3: Persist the token on creation, enforce the expiration ceiling

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthService.java:458-469`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthController.java:444-489`
- Modify: `back-end/src/main/resources/application.properties`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/ServiceAccountTokenControllerTest.java` (create)

**Interfaces:**
- Consumes: `ServiceAccountTokenRepository` (Task 1), `JwtUtil.generateServiceAccountToken(ServiceAccountToken)` (Task 2)
- Produces:
  - `AuthService.createServiceAccountToken(String username, String tokenName, int expirationDays, String globalRole, String orgRole, Long organizationId)` → `ServiceAccountToken`
  - `AuthController.currentBearerToken()` → `String` (nullable) — private helper
  - `ServiceAccountTokenResponse` gains an `id` field

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/controller/ServiceAccountTokenControllerTest.java`:

```java
package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.ServiceAccountToken;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.ServiceAccountTokenRequest;
import gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.AuthService;
import gov.nist.oscal.tools.api.service.FileValidationService;
import gov.nist.oscal.tools.api.service.PasswordResetService;
import gov.nist.oscal.tools.api.service.PasswordValidationService;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.telemetry.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class ServiceAccountTokenControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AuthService authService;
    @MockitoBean private PasswordResetService passwordResetService;
    @MockitoBean private PasswordValidationService passwordValidationService;
    @MockitoBean private FileValidationService fileValidationService;
    @MockitoBean private ServiceAccountTokenRepository serviceAccountTokenRepository;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private RateLimitService rateLimitService;
    @MockitoBean private RateLimitConfig rateLimitConfig;
    @MockitoBean private SecurityHeadersConfig securityHeadersConfig;
    @MockitoBean private TelemetryService telemetryService;

    private ServiceAccountToken record(long id, String name) {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("alice");

        ServiceAccountToken t = new ServiceAccountToken();
        t.setId(id);
        t.setUser(owner);
        t.setTokenName(name);
        t.setJti("jti-" + id);
        t.setGlobalRole("SUPER_ADMIN");
        t.setExpiresAt(LocalDateTime.now().plusDays(30));
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    @Test
    @WithMockUser(username = "alice")
    void generatingATokenPersistsItAndReturnsTheJwt() throws Exception {
        when(authService.createServiceAccountToken(eq("alice"), eq("CI"), eq(30), any(), any(), any()))
                .thenReturn(record(5L, "CI"));
        when(jwtUtil.generateServiceAccountToken(any(ServiceAccountToken.class)))
                .thenReturn("minted.jwt.value");

        mockMvc.perform(post("/api/auth/service-account-token").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ServiceAccountTokenRequest("CI", 30))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("minted.jwt.value"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.tokenName").value("CI"));

        verify(authService).createServiceAccountToken(eq("alice"), eq("CI"), eq(30), any(), any(), any());
    }

    /**
     * The UI caps expiration at 3650 but a direct API call bypasses the browser
     * entirely, so the ceiling has to be enforced server-side.
     */
    @Test
    @WithMockUser(username = "alice")
    void expirationBeyondTheConfiguredMaximumIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/service-account-token").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ServiceAccountTokenRequest("Forever", 40000))))
                .andExpect(status().isBadRequest());

        verify(authService, never()).createServiceAccountToken(any(), any(), anyInt(), any(), any(), any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=ServiceAccountTokenControllerTest`
Expected: compilation failure — `createServiceAccountToken` does not exist on `AuthService`.

- [ ] **Step 3: Add the property**

In `back-end/src/main/resources/application.properties`, add near the other `app.*` entries (after line 350):

```properties
# Ceiling on service account token lifetime. The UI offers 1-3650 days; this
# is the server-side enforcement, since a direct API call skips the UI.
app.service-tokens.max-expiration-days=${SERVICE_TOKEN_MAX_EXPIRATION_DAYS:3650}
```

- [ ] **Step 4: Replace the AuthService method**

In `AuthService.java`, replace the whole `generateServiceAccountToken` method (lines 458-469) with:

```java
    /**
     * Persist a service account token record. The JWT itself is minted by the
     * caller from the returned entity — this method owns the row (and therefore
     * the jti and the permission snapshot), not the token string.
     */
    @org.springframework.transaction.annotation.Transactional
    public gov.nist.oscal.tools.api.entity.ServiceAccountToken createServiceAccountToken(
            String username, String tokenName, int expirationDays,
            String globalRole, String orgRole, Long organizationId) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        gov.nist.oscal.tools.api.entity.ServiceAccountToken record =
                new gov.nist.oscal.tools.api.entity.ServiceAccountToken();
        record.setUser(user);
        record.setTokenName(tokenName);
        record.setJti(java.util.UUID.randomUUID().toString());
        record.setGlobalRole(globalRole);
        record.setOrgRole(orgRole);
        record.setOrganizationId(organizationId);
        record.setExpiresAt(java.time.LocalDateTime.now().plusDays(expirationDays));

        return serviceAccountTokenRepository.save(record);
    }
```

Add the repository field alongside the other `@Autowired` fields in `AuthService`:

```java
    @Autowired
    private gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository serviceAccountTokenRepository;
```

- [ ] **Step 5: Add `id`, `globalRole`, and `orgRole` to the response DTO**

In `back-end/src/main/java/gov/nist/oscal/tools/api/model/ServiceAccountTokenResponse.java`, add `private Long id;`, `private String globalRole;`, and `private String orgRole;` fields with getters and setters. The roles are what the success panel displays so the user can see the grant they just handed out (Task 7). The full constructor becomes:

```java
    public ServiceAccountTokenResponse(Long id, String token, String tokenName, String username,
                                       String expiresAt, Integer expirationDays,
                                       String globalRole, String orgRole) {
        this.id = id;
        this.token = token;
        this.tokenName = tokenName;
        this.username = username;
        this.expiresAt = expiresAt;
        this.expirationDays = expirationDays;
        this.globalRole = globalRole;
        this.orgRole = orgRole;
    }
```

Keep the existing no-arg constructor for Jackson.

- [ ] **Step 6: Rewire the controller**

In `AuthController.java`, add the injected ceiling near the other fields:

```java
    @org.springframework.beans.factory.annotation.Value("${app.service-tokens.max-expiration-days:3650}")
    private int maxServiceTokenExpirationDays;
```

Add this private helper at the bottom of the class. The `RequestContextHolder` dance is repeated at six call sites already (lines 238, 536, 586, 636, 754, 804); this names it once:

```java
    /** The raw bearer token on the current request, or null if absent. */
    private String currentBearerToken() {
        var attrs = (org.springframework.web.context.request.ServletRequestAttributes)
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        String header = attrs.getRequest().getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }
```

Replace the body of `generateServiceAccountToken` (keeping the existing `@Operation`/`@ApiResponses`/`@PostMapping` annotations, and updating the `description` to "Generate a service account JWT token. The token value is shown once and cannot be retrieved later; the token can be listed and revoked afterwards."):

```java
    public ResponseEntity<?> generateServiceAccountToken(@Valid @RequestBody ServiceAccountTokenRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        if (request.getExpirationDays() > maxServiceTokenExpirationDays) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Expiration must not exceed " + maxServiceTokenExpirationDays + " days");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            String username = authentication.getName();

            // Snapshot the caller's current permissions into the token. Read from
            // their session JWT, which is the same source the org-switch flow uses.
            String globalRole = null;
            String orgRole = null;
            Long organizationId = null;
            String currentToken = currentBearerToken();
            if (currentToken != null) {
                try {
                    globalRole = jwtUtil.extractGlobalRole(currentToken);
                    orgRole = jwtUtil.extractOrganizationRole(currentToken);
                    organizationId = jwtUtil.extractOrganizationId(currentToken);
                } catch (RuntimeException ignored) {
                    // Older token without these claims — mint with no elevated grant.
                }
            }

            gov.nist.oscal.tools.api.entity.ServiceAccountToken record =
                    authService.createServiceAccountToken(username, request.getTokenName(),
                            request.getExpirationDays(), globalRole, orgRole, organizationId);

            String token = jwtUtil.generateServiceAccountToken(record);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            String expiresAt = dateFormat.format(java.util.Date.from(
                    record.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant()));

            ServiceAccountTokenResponse response = new ServiceAccountTokenResponse(
                    record.getId(), token, record.getTokenName(), username,
                    expiresAt, request.getExpirationDays(),
                    record.getGlobalRole(), record.getOrgRole());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `mvn -q test -Dtest=ServiceAccountTokenControllerTest,JwtUtilTest`
Expected: PASS.

Then run the whole suite: `mvn -q test`
Expected: BUILD SUCCESS, 0 failures.

- [ ] **Step 8: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthService.java back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthController.java back-end/src/main/java/gov/nist/oscal/tools/api/model/ServiceAccountTokenResponse.java back-end/src/main/resources/application.properties back-end/src/test/java/gov/nist/oscal/tools/api/controller/ServiceAccountTokenControllerTest.java
git commit -m "feat(auth): persist service account tokens and enforce the expiration ceiling"
```

---

### Task 4: Reject revoked and legacy tokens in the filter

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilter.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterServiceTokenTest.java` (create)

**Interfaces:**
- Consumes: `ServiceAccountTokenRepository.findByJti/touchLastUsed` (Task 1), `JwtUtil.extractJti/extractTokenType` (Task 2)
- Produces: no new public API; the filter gains behavior only

This is the security core of the feature. A miss here means revocation silently does nothing.

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterServiceTokenTest.java`:

```java
package gov.nist.oscal.tools.api.security;

import gov.nist.oscal.tools.api.entity.ServiceAccountToken;
import gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterServiceTokenTest {

    private JwtUtil jwtUtil;
    private UserDetailsService userDetailsService;
    private ServiceAccountTokenRepository repository;
    private JwtAuthenticationFilter filter;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtUtil = mock(JwtUtil.class);
        userDetailsService = mock(UserDetailsService.class);
        repository = mock(ServiceAccountTokenRepository.class);

        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(filter, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(filter, "serviceAccountTokenRepository", repository);

        userDetails = User.builder()
                .username("alice").password("p").authorities(new ArrayList<>()).build();

        when(jwtUtil.extractUsername("tok")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
        when(jwtUtil.validateToken(eq("tok"), any())).thenReturn(true);
    }

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/validate");
        request.addHeader("Authorization", "Bearer tok");
        return request;
    }

    private ServiceAccountToken record(LocalDateTime revokedAt) {
        ServiceAccountToken t = new ServiceAccountToken();
        t.setId(3L);
        t.setJti("the-jti");
        t.setRevokedAt(revokedAt);
        t.setExpiresAt(LocalDateTime.now().plusDays(1));
        return t;
    }

    @Test
    void revokedServiceTokenIsRejectedWith401() throws Exception {
        when(jwtUtil.extractTokenType("tok")).thenReturn("service-account");
        when(jwtUtil.extractJti("tok")).thenReturn("the-jti");
        when(repository.findByJti("the-jti")).thenReturn(Optional.of(record(LocalDateTime.now())));

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestWithToken(), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain, never()).doFilter(any(), any());
    }

    /** Tokens minted before this feature carry no jti and cannot be revoked. */
    @Test
    void legacyServiceTokenWithoutJtiIsRejectedWith401() throws Exception {
        when(jwtUtil.extractTokenType("tok")).thenReturn("service-account");
        when(jwtUtil.extractJti("tok")).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestWithToken(), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(repository);
    }

    @Test
    void unknownJtiIsRejectedWith401() throws Exception {
        when(jwtUtil.extractTokenType("tok")).thenReturn("service-account");
        when(jwtUtil.extractJti("tok")).thenReturn("the-jti");
        when(repository.findByJti("the-jti")).thenReturn(Optional.empty());

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestWithToken(), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void liveServiceTokenAuthenticatesAndRecordsUse() throws Exception {
        when(jwtUtil.extractTokenType("tok")).thenReturn("service-account");
        when(jwtUtil.extractJti("tok")).thenReturn("the-jti");
        when(repository.findByJti("the-jti")).thenReturn(Optional.of(record(null)));

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestWithToken(), response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(any(), any());
        verify(repository).touchLastUsed(eq(3L), any(LocalDateTime.class));
    }

    /** lastUsedAt is throttled — a token used minutes ago must not write again. */
    @Test
    void recentlyUsedTokenIsNotWrittenAgain() throws Exception {
        ServiceAccountToken recent = record(null);
        recent.setLastUsedAt(LocalDateTime.now().minusMinutes(5));

        when(jwtUtil.extractTokenType("tok")).thenReturn("service-account");
        when(jwtUtil.extractJti("tok")).thenReturn("the-jti");
        when(repository.findByJti("the-jti")).thenReturn(Optional.of(recent));

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), mock(FilterChain.class));

        verify(repository, never()).touchLastUsed(anyLong(), any());
    }

    /** Session tokens have no tokenType and must not touch the repository at all. */
    @Test
    void sessionTokenIsUnaffected() throws Exception {
        when(jwtUtil.extractTokenType("tok")).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestWithToken(), response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(repository);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=JwtAuthenticationFilterServiceTokenTest`
Expected: failure — the filter has no `serviceAccountTokenRepository` field, so `ReflectionTestUtils.setField` throws `IllegalArgumentException: Could not find field`.

- [ ] **Step 3: Write the implementation**

In `JwtAuthenticationFilter.java`, add imports:

```java
import gov.nist.oscal.tools.api.entity.ServiceAccountToken;
import gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
```

Add the repository field next to the existing `@Autowired` fields:

```java
    @Autowired
    private ServiceAccountTokenRepository serviceAccountTokenRepository;
```

Inside `doFilterInternal`, replace the line `if (jwtUtil.validateToken(jwt, userDetails)) {` with:

```java
            if (jwtUtil.validateToken(jwt, userDetails)) {
                String rejection = serviceTokenRejection(jwt);
                if (rejection != null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"" + rejection + "\"}");
                    return;
                }
```

(The rest of the block — authorities, baggage — is unchanged.)

Add these two private methods at the bottom of the class:

```java
    /**
     * Revocation gate for service account tokens. Returns null when the request
     * may proceed, or a message explaining the refusal.
     * <p>
     * Session tokens carry no {@code tokenType} claim and pass straight through;
     * only service tokens are looked up. The messages distinguish revoked from
     * unknown from legacy because the caller already holds the credential — the
     * distinction leaks nothing and is what makes a failing pipeline diagnosable.
     * </p>
     */
    private String serviceTokenRejection(String jwt) {
        if (!"service-account".equals(jwtUtil.extractTokenType(jwt))) {
            return null;
        }

        String jti = jwtUtil.extractJti(jwt);
        if (jti == null || jti.isBlank()) {
            logger.warn("Rejected a service account token issued before revocation support (no jti)");
            return "This service account token predates revocation support. "
                 + "Generate a replacement from your Profile page.";
        }

        Optional<ServiceAccountToken> found = serviceAccountTokenRepository.findByJti(jti);
        if (found.isEmpty()) {
            logger.warn("Rejected a service account token with an unrecognized jti: " + jti);
            return "Service account token not recognized.";
        }

        ServiceAccountToken record = found.get();
        if (record.getRevokedAt() != null) {
            logger.warn("Rejected a revoked service account token: " + jti);
            return "This service account token has been revoked.";
        }

        touchLastUsed(record);
        return null;
    }

    /**
     * Record use, at most once an hour per token. An unthrottled write here
     * would put a database UPDATE on every authenticated API request.
     */
    private void touchLastUsed(ServiceAccountToken record) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastUsed = record.getLastUsedAt();
        if (lastUsed == null || lastUsed.isBefore(now.minusHours(1))) {
            serviceAccountTokenRepository.touchLastUsed(record.getId(), now);
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=JwtAuthenticationFilterServiceTokenTest`
Expected: PASS, 6 tests.

Then: `mvn -q test`
Expected: BUILD SUCCESS, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilter.java back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterServiceTokenTest.java
git commit -m "feat(auth): reject revoked and pre-jti service account tokens"
```

---

### Task 5: List and revoke endpoints

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/model/ServiceAccountTokenSummary.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuditEventType.java`
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthController.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/ServiceAccountTokenControllerTest.java` (extend)

**Interfaces:**
- Consumes: `ServiceAccountTokenRepository.findByUserIdOrderByCreatedAtDesc/findByIdAndUserId` (Task 1)
- Produces:
  - `GET /api/auth/service-account-tokens` → `List<ServiceAccountTokenSummary>`
  - `DELETE /api/auth/service-account-tokens/{id}` → 204, or 404
  - `ServiceAccountTokenSummary.from(ServiceAccountToken)` static factory
  - `AuditEventType.AUTH_SERVICE_TOKEN_REVOKED`

- [ ] **Step 1: Write the failing test**

Append to `ServiceAccountTokenControllerTest.java` (inside the class), and add the import `import java.util.List;`:

```java
    @Test
    @WithMockUser(username = "alice")
    void listReturnsTheCallersTokensWithoutTokenValues() throws Exception {
        User alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(java.util.Optional.of(alice));
        when(serviceAccountTokenRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(record(5L, "CI"), record(6L, "Staging")));

        mockMvc.perform(get("/api/auth/service-account-tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tokenName").value("CI"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].globalRole").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$[0].token").doesNotExist())
                .andExpect(jsonPath("$[1].tokenName").value("Staging"));
    }

    @Test
    @WithMockUser(username = "alice")
    void revokingOwnTokenSetsRevokedAt() throws Exception {
        User alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(java.util.Optional.of(alice));
        when(serviceAccountTokenRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(java.util.Optional.of(record(5L, "CI")));

        mockMvc.perform(delete("/api/auth/service-account-tokens/5").with(csrf()))
                .andExpect(status().isNoContent());

        verify(serviceAccountTokenRepository).save(argThat(t -> t.getRevokedAt() != null
                && "alice".equals(t.getRevokedBy())));
    }

    /**
     * 404 rather than 403 — a 403 would confirm the id exists and turn this
     * endpoint into a probe for other users' token ids.
     */
    @Test
    @WithMockUser(username = "mallory")
    void revokingSomeoneElsesTokenReturnsNotFound() throws Exception {
        User mallory = new User();
        mallory.setId(2L);
        mallory.setUsername("mallory");
        when(userRepository.findByUsername("mallory")).thenReturn(java.util.Optional.of(mallory));
        when(serviceAccountTokenRepository.findByIdAndUserId(5L, 2L))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(delete("/api/auth/service-account-tokens/5").with(csrf()))
                .andExpect(status().isNotFound());

        verify(serviceAccountTokenRepository, never()).save(any());
    }

    @Test
    @WithMockUser(username = "alice")
    void revokingAnAlreadyRevokedTokenIsANoOp() throws Exception {
        User alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        ServiceAccountToken alreadyRevoked = record(5L, "CI");
        alreadyRevoked.setRevokedAt(LocalDateTime.now().minusDays(1));

        when(userRepository.findByUsername("alice")).thenReturn(java.util.Optional.of(alice));
        when(serviceAccountTokenRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(java.util.Optional.of(alreadyRevoked));

        mockMvc.perform(delete("/api/auth/service-account-tokens/5").with(csrf()))
                .andExpect(status().isNoContent());

        verify(serviceAccountTokenRepository, never()).save(any());
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=ServiceAccountTokenControllerTest`
Expected: the four new tests fail with 404/405 — the endpoints do not exist.

- [ ] **Step 3: Write the summary DTO**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/model/ServiceAccountTokenSummary.java`:

```java
package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.ServiceAccountToken;
import java.time.LocalDateTime;

/**
 * Listing view of a service account token. Deliberately has no {@code token}
 * field — the value is shown once at creation and is never retrievable.
 */
public class ServiceAccountTokenSummary {

    private Long id;
    private String tokenName;
    private String globalRole;
    private String orgRole;
    private Long organizationId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;
    private String status;

    public static ServiceAccountTokenSummary from(ServiceAccountToken t) {
        ServiceAccountTokenSummary s = new ServiceAccountTokenSummary();
        s.id = t.getId();
        s.tokenName = t.getTokenName();
        s.globalRole = t.getGlobalRole();
        s.orgRole = t.getOrgRole();
        s.organizationId = t.getOrganizationId();
        s.createdAt = t.getCreatedAt();
        s.expiresAt = t.getExpiresAt();
        s.lastUsedAt = t.getLastUsedAt();
        s.revokedAt = t.getRevokedAt();
        s.status = t.getStatus().name();
        return s;
    }

    public Long getId() { return id; }
    public String getTokenName() { return tokenName; }
    public String getGlobalRole() { return globalRole; }
    public String getOrgRole() { return orgRole; }
    public Long getOrganizationId() { return organizationId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public String getStatus() { return status; }
}
```

- [ ] **Step 4: Add the audit event and actually emit it**

`AUTH_SERVICE_TOKEN_GENERATED` already exists in the enum but is **dead** — audit
events are derived in `RequestLoggingFilter.determineEventType()` by matching the
request URI, and that method has no branch for `/auth/service-account-token`. So
today no service-token activity is audited at all. Adding a second unused enum
constant would repeat the mistake; wire both.

In `AuditEventType.java`, immediately after the `AUTH_SERVICE_TOKEN_GENERATED`
entry (line 85), add:

```java
    /**
     * Service account token revoked
     * <p>Risk Level: MEDIUM (privileged operation)</p>
     * <p>Retention: LONG (security monitoring)</p>
     */
    AUTH_SERVICE_TOKEN_REVOKED("Authentication", "Service account token revoked", "MEDIUM"),
```

In `back-end/src/main/java/gov/nist/oscal/tools/api/filter/RequestLoggingFilter.java`,
inside `determineEventType`, add this branch immediately after the
`/auth/logout` branch (around line 205). It must come **before** any broader
`/auth` or `/profile` matching:

```java
        if (uri.contains("/auth/service-account-token")) {
            if ("DELETE".equals(method)) {
                return AuditEventType.AUTH_SERVICE_TOKEN_REVOKED;
            }
            if ("POST".equals(method)) {
                return AuditEventType.AUTH_SERVICE_TOKEN_GENERATED;
            }
        }
```

Note the URI substring `/auth/service-account-token` matches both
`/api/auth/service-account-token` (POST, create) and
`/api/auth/service-account-tokens/{id}` (DELETE, revoke), since the plural path
contains the singular as a prefix. GET listing falls through to the default and
is not audited, which is correct — reading your own token list is not a
privileged event.

Add a test to `back-end/src/test/java/gov/nist/oscal/tools/api/filter/` covering
both mappings if a `RequestLoggingFilterTest` exists; if not, assert the mapping
indirectly by confirming `AuditEventType.AUTH_SERVICE_TOKEN_REVOKED` is returned
for a `DELETE` to the plural path via a small direct test of the filter.

- [ ] **Step 5: Add the endpoints**

In `AuthController.java`, add the repository and user repository fields if not already present:

```java
    @Autowired
    private gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository serviceAccountTokenRepository;

    @Autowired
    private gov.nist.oscal.tools.api.repository.UserRepository userRepository;
```

Add both endpoints directly after `generateServiceAccountToken`:

```java
    @Operation(
        summary = "List service account tokens",
        description = "List the calling user's service account tokens. Token values are never returned."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tokens listed"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/service-account-tokens")
    public ResponseEntity<?> listServiceAccountTokens() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        return userRepository.findByUsername(authentication.getName())
                .map(user -> ResponseEntity.ok(
                        serviceAccountTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                                .stream()
                                .map(gov.nist.oscal.tools.api.model.ServiceAccountTokenSummary::from)
                                .toList()))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @Operation(
        summary = "Revoke a service account token",
        description = "Revoke one of the calling user's service account tokens. Idempotent."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Token revoked"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "404", description = "No such token for this user")
    })
    @DeleteMapping("/service-account-tokens/{id}")
    public ResponseEntity<?> revokeServiceAccountToken(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        String username = authentication.getName();
        var user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        // Scoped by owner, and absent-or-not-yours are the same 404 so this
        // endpoint cannot be used to discover other users' token ids.
        var found = serviceAccountTokenRepository.findByIdAndUserId(id, user.get().getId());
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        gov.nist.oscal.tools.api.entity.ServiceAccountToken record = found.get();
        if (record.getRevokedAt() == null) {
            record.setRevokedAt(java.time.LocalDateTime.now());
            record.setRevokedBy(username);
            serviceAccountTokenRepository.save(record);
            logger.info("Service account token {} revoked by {}", id, username);
        }

        return ResponseEntity.noContent().build();
    }
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `mvn -q test -Dtest=ServiceAccountTokenControllerTest`
Expected: PASS, 6 tests.

Then: `mvn -q test`
Expected: BUILD SUCCESS, 0 failures.

- [ ] **Step 7: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/model/ServiceAccountTokenSummary.java back-end/src/main/java/gov/nist/oscal/tools/api/model/AuditEventType.java back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthController.java back-end/src/test/java/gov/nist/oscal/tools/api/controller/ServiceAccountTokenControllerTest.java
git commit -m "feat(auth): add list and revoke endpoints for service account tokens"
```

---

### Task 6: Revoke tokens when an account is archived

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/OrganizationController.java:127-152`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/controller/OrganizationControllerArchiveTest.java` (create)

**Interfaces:**
- Consumes: `ServiceAccountTokenRepository.revokeAllForUser` (Task 1)
- Produces: no new public API

Note: there is no account-delete path in this codebase — accounts are archived, never deleted — so archive is the only trigger. `unarchiveUser` deliberately does not restore revoked tokens.

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/controller/OrganizationControllerArchiveTest.java`:

```java
package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganizationControllerArchiveTest {

    @Mock private UserRepository userRepository;
    @Mock private ServiceAccountTokenRepository serviceAccountTokenRepository;
    @InjectMocks private OrganizationController controller;

    /**
     * Archiving disables the account, but a service account token it issued is
     * a separate credential — without this the token keeps working for years.
     */
    @Test
    void archivingAUserRevokesTheirServiceAccountTokens() {
        User target = new User();
        target.setId(9L);
        target.setUsername("bob");
        target.setEnabled(true);
        when(userRepository.findById(9L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        controller.archiveUser(9L);

        verify(serviceAccountTokenRepository)
                .revokeAllForUser(eq(9L), any(LocalDateTime.class), anyString());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=OrganizationControllerArchiveTest`
Expected: `Wanted but not invoked: serviceAccountTokenRepository.revokeAllForUser(...)`.

- [ ] **Step 3: Write the implementation**

In `OrganizationController.java`, add the repository field alongside the existing ones:

```java
    @Autowired
    private gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository serviceAccountTokenRepository;
```

In `archiveUser`, immediately after `userRepository.save(user);`, add:

```java
            // Archiving ends the account's sessions (JwtUtil.validateToken checks
            // isEnabled), but service account tokens are separate credentials that
            // would otherwise stay live for their full multi-year lifetime.
            int revoked = serviceAccountTokenRepository.revokeAllForUser(
                    userId, java.time.LocalDateTime.now(), "system:archive");
            if (revoked > 0) {
                logger.info("Archived user {} — revoked {} service account token(s)",
                        user.getUsername(), revoked);
            }
```

If `OrganizationController` has no `logger` field, add at the top of the class:

```java
    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(OrganizationController.class);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=OrganizationControllerArchiveTest`
Expected: PASS.

Then: `mvn -q test`
Expected: BUILD SUCCESS, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/controller/OrganizationController.java back-end/src/test/java/gov/nist/oscal/tools/api/controller/OrganizationControllerArchiveTest.java
git commit -m "feat(auth): revoke service account tokens when an account is archived"
```

---

### Task 7: Frontend list and revoke

**Files:**
- Modify: `front-end/src/types/oscal.ts`
- Modify: `front-end/src/lib/api-client.ts` (near `generateServiceAccountToken`, line 2122)
- Modify: `front-end/src/components/ServiceAccountTokenGenerator.tsx`
- Test: `front-end/__tests__/ServiceAccountTokenGenerator.test.tsx` (create)

**Interfaces:**
- Consumes: `GET/DELETE /api/auth/service-account-tokens` (Task 5)
- Produces: `ServiceAccountTokenSummary` TS type; `apiClient.listServiceAccountTokens()`, `apiClient.revokeServiceAccountToken(id)`

- [ ] **Step 1: Add the type**

In `front-end/src/types/oscal.ts`, next to the existing `ServiceAccountTokenResponse`, add:

```typescript
export interface ServiceAccountTokenSummary {
  id: number;
  tokenName: string;
  globalRole: string | null;
  orgRole: string | null;
  organizationId: number | null;
  createdAt: string;
  expiresAt: string;
  lastUsedAt: string | null;
  revokedAt: string | null;
  status: 'ACTIVE' | 'EXPIRED' | 'REVOKED';
}
```

Also add these to the existing `ServiceAccountTokenResponse` interface, matching the backend DTO from Task 3 Step 5:

```typescript
  id: number;
  globalRole: string | null;
  orgRole: string | null;
```

- [ ] **Step 2: Write the failing test**

Create `front-end/__tests__/ServiceAccountTokenGenerator.test.tsx`:

```tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ServiceAccountTokenGenerator } from '@/components/ServiceAccountTokenGenerator';
import { apiClient } from '@/lib/api-client';

jest.mock('@/lib/api-client', () => ({
  apiClient: {
    generateServiceAccountToken: jest.fn(),
    listServiceAccountTokens: jest.fn(),
    revokeServiceAccountToken: jest.fn(),
  },
}));

jest.mock('sonner', () => ({
  toast: { success: jest.fn(), error: jest.fn() },
}));

const mockApi = apiClient as jest.Mocked<typeof apiClient>;

const activeToken = {
  id: 5,
  tokenName: 'CI Pipeline',
  globalRole: 'SUPER_ADMIN',
  orgRole: null,
  organizationId: null,
  createdAt: '2026-08-01T10:00:00',
  expiresAt: '2027-08-01T10:00:00',
  lastUsedAt: null,
  revokedAt: null,
  status: 'ACTIVE' as const,
};

describe('ServiceAccountTokenGenerator', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockApi.listServiceAccountTokens.mockResolvedValue([activeToken]);
    mockApi.revokeServiceAccountToken.mockResolvedValue(undefined);
  });

  it('lists existing tokens with their snapshotted permissions', async () => {
    render(<ServiceAccountTokenGenerator />);

    expect(await screen.findByText('CI Pipeline')).toBeInTheDocument();
    expect(screen.getByText('SUPER_ADMIN')).toBeInTheDocument();
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
  });

  it('revokes a token and refreshes the list', async () => {
    const user = userEvent.setup();
    render(<ServiceAccountTokenGenerator />);

    await screen.findByText('CI Pipeline');
    await user.click(screen.getByRole('button', { name: /revoke/i }));
    await user.click(screen.getByRole('button', { name: /confirm/i }));

    await waitFor(() => {
      expect(mockApi.revokeServiceAccountToken).toHaveBeenCalledWith(5);
    });
    expect(mockApi.listServiceAccountTokens).toHaveBeenCalledTimes(2);
  });
});
```

- [ ] **Step 3: Run test to verify it fails**

Run from `front-end/`: `npm test -- ServiceAccountTokenGenerator`
Expected: FAIL — `listServiceAccountTokens` is not called and "CI Pipeline" never renders.

- [ ] **Step 4: Add the api-client methods**

In `front-end/src/lib/api-client.ts`, immediately after `generateServiceAccountToken` (which ends around line 2150), add:

```typescript
  /**
   * List the current user's service account tokens. Token values are never
   * returned — only metadata.
   */
  async listServiceAccountTokens(): Promise<ServiceAccountTokenSummary[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/auth/service-account-tokens`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error('Failed to load service account tokens');
    }

    return response.json();
  }

  /** Revoke a service account token. Idempotent. */
  async revokeServiceAccountToken(id: number): Promise<void> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/auth/service-account-tokens/${id}`,
      {
        method: 'DELETE',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error('Failed to revoke service account token');
    }
  }
```

Add `ServiceAccountTokenSummary` to the type import at the top of the file.

- [ ] **Step 5: Update the component**

In `ServiceAccountTokenGenerator.tsx`, change the imports on lines 3 and 12:

```tsx
import { useState, useEffect, useCallback } from 'react';
import type { ServiceAccountTokenResponse, ServiceAccountTokenSummary } from '@/types/oscal';
```

Add state next to the existing `useState` calls (after line 19):

```tsx
  const [tokens, setTokens] = useState<ServiceAccountTokenSummary[]>([]);
  const [pendingRevoke, setPendingRevoke] = useState<ServiceAccountTokenSummary | null>(null);
  const [isRevoking, setIsRevoking] = useState(false);
```

Add the load and revoke handlers after the existing `handleGenerate`:

```tsx
  const loadTokens = useCallback(async () => {
    try {
      setTokens(await apiClient.listServiceAccountTokens());
    } catch (error) {
      console.error('Failed to load tokens:', error);
    }
  }, []);

  useEffect(() => {
    loadTokens();
  }, [loadTokens]);

  const handleRevoke = async () => {
    if (!pendingRevoke) return;

    setIsRevoking(true);
    try {
      await apiClient.revokeServiceAccountToken(pendingRevoke.id);
      toast.success(`Revoked "${pendingRevoke.tokenName}"`);
      setPendingRevoke(null);
      await loadTokens();
    } catch (error) {
      console.error('Failed to revoke token:', error);
      toast.error(error instanceof Error ? error.message : 'Failed to revoke token');
    } finally {
      setIsRevoking(false);
    }
  };
```

In `handleGenerate`, add `await loadTokens();` immediately after
`toast.success('Service account token generated successfully!');` so a new token
appears in the list right away.

Replace the alert text on lines 85-87 with:

```tsx
                Service account tokens act with your current permissions and can be
                revoked below. Treat them like passwords. The token value is shown
                once and cannot be retrieved afterwards, though you can always see
                and revoke the token here.
```

In the success panel, after the "Token Name" block (around line 155), add the
snapshotted grant so nobody hands out an admin token without noticing:

```tsx
              <div className="space-y-2">
                <Label>Permissions</Label>
                <p className="text-sm text-muted-foreground">
                  This token acts as <span className="font-medium">{generatedToken.globalRole ?? 'USER'}</span>
                  {generatedToken.orgRole ? ` / ${generatedToken.orgRole}` : ''}
                </p>
              </div>
```

(This requires `globalRole` and `orgRole` on `ServiceAccountTokenResponse` — add
them as `string | null` in `types/oscal.ts` alongside `id`, and include them in
the backend `ServiceAccountTokenResponse` constructor from Task 3 Step 5,
populated from `record.getGlobalRole()` / `record.getOrgRole()`.)

Add the token table as the last child of `<CardContent>`, after the closing brace
of the `{!generatedToken ? (...) : (...)}` expression:

```tsx
        <div className="space-y-3 pt-2 border-t">
          <Label>Your Tokens</Label>
          {tokens.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              You have not generated any service account tokens yet.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-muted-foreground">
                    <th className="py-2 pr-4">Name</th>
                    <th className="py-2 pr-4">Permissions</th>
                    <th className="py-2 pr-4">Created</th>
                    <th className="py-2 pr-4">Last used</th>
                    <th className="py-2 pr-4">Expires</th>
                    <th className="py-2 pr-4">Status</th>
                    <th className="py-2" />
                  </tr>
                </thead>
                <tbody>
                  {tokens.map((t) => (
                    <tr key={t.id} className="border-t">
                      <td className="py-2 pr-4 font-medium">{t.tokenName}</td>
                      <td className="py-2 pr-4">
                        {t.globalRole ?? 'USER'}{t.orgRole ? ` / ${t.orgRole}` : ''}
                      </td>
                      <td className="py-2 pr-4">{new Date(t.createdAt).toLocaleDateString()}</td>
                      <td className="py-2 pr-4">
                        {t.lastUsedAt ? new Date(t.lastUsedAt).toLocaleDateString() : 'Never'}
                      </td>
                      <td className="py-2 pr-4">{new Date(t.expiresAt).toLocaleDateString()}</td>
                      <td className="py-2 pr-4">{t.status}</td>
                      <td className="py-2">
                        {t.status === 'ACTIVE' && (
                          <Button size="sm" variant="outline" onClick={() => setPendingRevoke(t)}>
                            Revoke
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {pendingRevoke && (
          <Alert className="border-destructive/50">
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription className="space-y-3">
              <p>
                Revoke <span className="font-medium">{pendingRevoke.tokenName}</span>? Any
                integration using it will start failing immediately. This cannot be undone.
              </p>
              <div className="flex gap-2">
                <Button size="sm" variant="destructive" onClick={handleRevoke} disabled={isRevoking}>
                  {isRevoking ? 'Revoking...' : 'Confirm'}
                </Button>
                <Button size="sm" variant="outline" onClick={() => setPendingRevoke(null)}>
                  Cancel
                </Button>
              </div>
            </AlertDescription>
          </Alert>
        )}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `npm test -- ServiceAccountTokenGenerator`
Expected: PASS, 2 tests.

Then run the full frontend suite: `npm test`
Expected: all pass.

- [ ] **Step 7: Commit**

```bash
git add front-end/src/types/oscal.ts front-end/src/lib/api-client.ts front-end/src/components/ServiceAccountTokenGenerator.tsx front-end/__tests__/ServiceAccountTokenGenerator.test.tsx
git commit -m "feat(ui): list and revoke service account tokens on the profile page"
```

---

### Task 8: Documentation

**Files:**
- Modify: `front-end/src/app/guide/account/service-tokens/page.mdx`
- Modify: `front-end/src/app/guide/reference/api-automation/page.mdx`

**Interfaces:**
- Consumes: everything above
- Produces: no code

- [ ] **Step 1: Update the service tokens guide**

In `front-end/src/app/guide/account/service-tokens/page.mdx`:

1. Set `lastUpdated: 2026-08-09` in the frontmatter.
2. Under the intro, add a "Permissions" section:

```markdown
## Permissions

A service account token is issued with a snapshot of your permissions at the
moment you create it. A token generated while you hold an admin role carries
that role for its entire life — it does **not** lose the privilege if your role
later changes.

<Callout type="warn">
Because permissions are frozen at issuance, revoking a token is the only way to
take its privileges away. If your role changes, revoke and re-mint any tokens
you issued under the old role.
</Callout>
```

3. Replace the existing "Revoking a token" section body with:

```markdown
1. Navigate to your **Profile** page (`/profile`).
2. Scroll to the **Service Account Tokens** section, where every token you have
   issued is listed with its permissions, last-used time, and status.
3. Click **Revoke** on the token and confirm.

The token stops working immediately — the next request using it receives a
`401 Unauthorized`. Revoking is idempotent and cannot be undone; issue a new
token if you need to restore access.

Tokens are also revoked automatically when the account that issued them is
archived.
```

4. Add at the end, before "## Related":

```markdown
## Tokens issued before August 2026

Service account tokens created before revocation support was added cannot be
identified or revoked, so they are no longer accepted. A request using one
receives a `401` explaining that the token predates revocation support.
Generate a replacement from your Profile page.
```

- [ ] **Step 2: Update the API automation guide**

In `front-end/src/app/guide/reference/api-automation/page.mdx`, set `lastUpdated: 2026-08-09` and add after the "Using the Token" section:

```markdown
<Callout type="warn">
Tokens generated before August 2026 are no longer accepted and must be
regenerated. A token carries the permissions its creator held at the moment of
creation, and keeps them until the token is revoked or expires.
</Callout>
```

- [ ] **Step 3: Verify the docs build**

Run from `front-end/`: `npx tsc --noEmit`
Expected: no errors. (MDX pages are type-checked as part of the Next.js app.)

- [ ] **Step 4: Commit**

```bash
git add front-end/src/app/guide/account/service-tokens/page.mdx front-end/src/app/guide/reference/api-automation/page.mdx
git commit -m "docs: service account token permissions, revocation, and legacy token break"
```

---

## Before opening a PR

- [ ] Run the full backend suite: `mvn -q test` from `back-end/` — expect 0 failures.
- [ ] Run the full frontend suite: `npm test` from `front-end/`.
- [ ] **Accept that affected integrations cannot be enumerated.** The design spec assumed the `AUTH_SERVICE_TOKEN_GENERATED` audit events could identify who holds a live token. They cannot: that enum constant was never emitted (see Task 5, Step 4), so no historical record of token issuance exists. Nothing else in the schema records it either, because tokens were never persisted — that is the defect this project fixes.

  Consequence: the legacy-token break must be announced broadly rather than targeted at known holders. Announce before deploying, not after.

- [ ] Put the legacy-token break in the release notes. Every currently-integrated client stops working on deploy and must mint a replacement.

- [ ] After deploying, confirm auditing works end-to-end — generate a token and revoke it, then check the events landed:

```bash
gcloud logging read 'resource.type=cloud_run_revision AND resource.labels.service_name=oscal-tools-prod AND textPayload:"AUTH_SERVICE_TOKEN"' --project=oscal-hub --limit 20
```
