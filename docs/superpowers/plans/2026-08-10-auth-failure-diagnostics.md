# Diagnosable Authentication Failures Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every 401 from the OSCAL Hub auth layer tell the caller which specific failure occurred, instead of returning one fixed "Authentication required or token invalid" string for all of them.

**Architecture:** `JwtAuthenticationFilter` already distinguishes expired, malformed, bad-signature, and unknown-token failures but only writes them to the server log. It will instead record a typed `AuthFailure` as a request attribute and continue the chain; the existing `AuthenticationEntryPoint` in `SecurityConfig` reads that attribute and renders it through a shared renderer. The filter must not write the response itself for these cases — public endpoints arrive with no `Authorization` header and must keep working, so only Spring Security may decide a request was unauthorized.

**Tech Stack:** Java 25, Spring Boot (webmvc test slices), Spring Security, jjwt, Jackson, JUnit 5, AssertJ, Mockito, `ReflectionTestUtils`.

**Spec:** `docs/superpowers/specs/2026-08-10-auth-failure-diagnostics-design.md`

## Global Constraints

- All work is in the `back-end/` Maven module. Run tests from `back-end/`.
- The `error` JSON field stays the literal string `"Unauthorized"` on every response, so existing clients keying off it are unaffected. New information goes in `message` and `code`.
- The filter **records and continues** for missing/malformed/expired/invalid-signature tokens. It never writes a response for those. Only the service-account revocation gate writes a response directly, which is existing behavior.
- `expiredAt` is an ISO-8601 instant in UTC produced with `DateTimeFormatter.ISO_INSTANT`. Never build it by appending a literal `Z` to a system-default-zone value.
- `expiredAt` is omitted from the JSON body entirely when null.
- Tests use JUnit 5 with AssertJ (`assertThat`), Mockito, and `MockHttpServletRequest`/`MockHttpServletResponse`. Field injection into filters is done with `ReflectionTestUtils.setField`, matching `JwtAuthenticationFilterServiceTokenTest`.
- If a test failure looks impossible (a symbol that plainly exists reported missing), run `mvn clean` first — a stale `target/` from the IDE compiler can poison the Maven run.

## File Structure

| File | Responsibility |
|---|---|
| `back-end/src/main/java/gov/nist/oscal/tools/api/security/AuthFailure.java` | New. The contract: one record plus one static factory per code, so every code↔message pairing is readable in one place. Also owns the request-attribute key and the `WWW-Authenticate` value. |
| `back-end/src/main/java/gov/nist/oscal/tools/api/security/AuthFailureRenderer.java` | New. Turns an `AuthFailure` into an HTTP response (status, headers, JSON body). Static utility with no Spring wiring, so it is callable from both the filter and the `SecurityConfig` entry-point lambda without changing bean graphs or affecting `@WebMvcTest` slices. |
| `back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilter.java` | Modified. Records an `AuthFailure` in every failure branch; routes the service-token gate through the shared renderer. |
| `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java` | Modified. Entry point renders the recorded attribute, falling back to `missingCredentials()`. |

Tests:

| File | Covers |
|---|---|
| `back-end/src/test/java/gov/nist/oscal/tools/api/security/AuthFailureTest.java` | Factory codes/messages, `WWW-Authenticate` formatting, `expiredAt` formatting. |
| `back-end/src/test/java/gov/nist/oscal/tools/api/security/AuthFailureRendererTest.java` | Status, headers, JSON shape, null-`expiredAt` omission, quote safety. |
| `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterDiagnosticsTest.java` | One case per filter branch, plus the public-path regression guard and the filter→entry-point handoff. |
| `back-end/src/test/java/gov/nist/oscal/tools/api/config/AuthenticationEntryPointTest.java` | Entry point renders attribute when present, generic when absent. |

---

### Task 1: The AuthFailure contract

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/security/AuthFailure.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/security/AuthFailureTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `public record AuthFailure(String code, String message, String expiredAt)`
  - `public static final String REQUEST_ATTRIBUTE = "gov.nist.oscal.tools.api.authFailure"`
  - `public static AuthFailure missingCredentials()`
  - `public static AuthFailure unsupportedScheme()`
  - `public static AuthFailure malformedToken()`
  - `public static AuthFailure tokenExpired(java.util.Date expiry)` — accepts null
  - `public static AuthFailure invalidSignature()`
  - `public static AuthFailure invalidToken()`
  - `public static AuthFailure serviceTokenRevoked()`
  - `public static AuthFailure serviceTokenUnknown()`
  - `public static AuthFailure serviceTokenLegacy()`
  - `public String wwwAuthenticate()`

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/security/AuthFailureTest.java`:

```java
package gov.nist.oscal.tools.api.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The wire contract for authentication failures. These strings are consumed by
 * external integrations (the RegScale Trust Center branches on `code`), so a
 * casual rename here is a breaking API change — hence a test per code.
 */
class AuthFailureTest {

    @Test
    void eachFactory_carriesItsDocumentedCode() {
        assertThat(AuthFailure.missingCredentials().code()).isEqualTo("missing_credentials");
        assertThat(AuthFailure.unsupportedScheme().code()).isEqualTo("unsupported_auth_scheme");
        assertThat(AuthFailure.malformedToken().code()).isEqualTo("malformed_token");
        assertThat(AuthFailure.tokenExpired(null).code()).isEqualTo("token_expired");
        assertThat(AuthFailure.invalidSignature().code()).isEqualTo("invalid_signature");
        assertThat(AuthFailure.invalidToken().code()).isEqualTo("invalid_token");
        assertThat(AuthFailure.serviceTokenRevoked().code()).isEqualTo("service_token_revoked");
        assertThat(AuthFailure.serviceTokenUnknown().code()).isEqualTo("service_token_unknown");
        assertThat(AuthFailure.serviceTokenLegacy().code()).isEqualTo("service_token_legacy");
    }

    @Test
    void everyFactory_hasNonEmptyActionableMessage() {
        AuthFailure[] all = {
                AuthFailure.missingCredentials(), AuthFailure.unsupportedScheme(),
                AuthFailure.malformedToken(), AuthFailure.tokenExpired(null),
                AuthFailure.invalidSignature(), AuthFailure.invalidToken(),
                AuthFailure.serviceTokenRevoked(), AuthFailure.serviceTokenUnknown(),
                AuthFailure.serviceTokenLegacy()
        };
        for (AuthFailure failure : all) {
            assertThat(failure.message()).as("message for %s", failure.code()).isNotBlank();
        }
    }

    @Test
    void missingCredentials_namesTheHeaderItWants() {
        // The failure that actually bit the Trust Center: the message has to say
        // what to send, not merely that something was absent.
        assertThat(AuthFailure.missingCredentials().message()).contains("Bearer");
    }

    @Test
    void tokenExpired_formatsExpiryAsUtcInstant() {
        Date expiry = Date.from(Instant.parse("2026-08-08T21:36:19Z"));
        assertThat(AuthFailure.tokenExpired(expiry).expiredAt()).isEqualTo("2026-08-08T21:36:19Z");
    }

    @Test
    void tokenExpired_toleratesUnknownExpiry() {
        assertThat(AuthFailure.tokenExpired(null).expiredAt()).isNull();
    }

    @Test
    void otherFailures_carryNoExpiry() {
        assertThat(AuthFailure.malformedToken().expiredAt()).isNull();
        assertThat(AuthFailure.invalidSignature().expiredAt()).isNull();
    }

    @Test
    void wwwAuthenticate_isBareBearerWhenNoCredentialWasSent() {
        // RFC 6750: omit the error parameter when the request carried no credential.
        assertThat(AuthFailure.missingCredentials().wwwAuthenticate()).isEqualTo("Bearer");
    }

    @Test
    void wwwAuthenticate_describesTheErrorForRejectedCredentials() {
        String header = AuthFailure.malformedToken().wwwAuthenticate();
        assertThat(header).startsWith("Bearer error=\"invalid_token\"");
        assertThat(header).contains("error_description=");
    }

    @Test
    void wwwAuthenticate_neverEmitsRawQuotesInsideTheQuotedString() {
        // RFC 6750 quoted-string cannot contain a raw double quote; a message
        // containing one would produce an unparseable header.
        AuthFailure quoted = new AuthFailure("invalid_token", "He said \"no\".", null);
        String header = quoted.wwwAuthenticate();
        assertThat(header.indexOf('"', header.indexOf("error_description=\"") + 19))
                .as("description must contain no raw quote before its closing quote")
                .isEqualTo(header.length() - 1);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=AuthFailureTest`
Expected: FAIL — compilation error, `AuthFailure` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/security/AuthFailure.java`:

```java
package gov.nist.oscal.tools.api.security;

import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * A single authentication failure, in the form the caller receives it.
 * <p>
 * Every 401 the auth layer produces is one of these. The {@code code} is a
 * stable identifier an integration branches on; the {@code message} is prose a
 * human acts on. Both are part of the public API — external integrations depend
 * on the codes, so treat renames as breaking changes.
 * </p>
 * <p>
 * These strings go only to a caller who already holds the credential and
 * already knows it was rejected, so distinguishing the failures leaks nothing
 * that the bare 401 did not. That is the same judgment already documented for
 * the service-account gate in {@link JwtAuthenticationFilter}.
 * </p>
 *
 * @param code      stable machine-readable identifier
 * @param message   actionable human-readable explanation
 * @param expiredAt ISO-8601 UTC instant, only for {@code token_expired}; null otherwise
 */
public record AuthFailure(String code, String message, String expiredAt) {

    /**
     * Request attribute under which {@code JwtAuthenticationFilter} leaves a
     * failure for {@code SecurityConfig}'s entry point to render. The filter
     * cannot respond itself: public endpoints legitimately arrive with no
     * credential, so only Spring Security may conclude a request was
     * unauthorized.
     */
    public static final String REQUEST_ATTRIBUTE = "gov.nist.oscal.tools.api.authFailure";

    public static AuthFailure missingCredentials() {
        return new AuthFailure("missing_credentials",
                "No Authorization header was provided. Send 'Authorization: Bearer <token>'.", null);
    }

    public static AuthFailure unsupportedScheme() {
        return new AuthFailure("unsupported_auth_scheme",
                "Authorization header must use the Bearer scheme, as in 'Authorization: Bearer <token>'.", null);
    }

    public static AuthFailure malformedToken() {
        return new AuthFailure("malformed_token",
                "The credential is not a well-formed JWT. Check that the whole token value was sent, "
                        + "and that it was not truncated or left encrypted.", null);
    }

    /**
     * @param expiry the moment the token expired, or null when it could not be read
     */
    public static AuthFailure tokenExpired(Date expiry) {
        String expiredAt = expiry == null
                ? null
                : DateTimeFormatter.ISO_INSTANT.format(expiry.toInstant());
        return new AuthFailure("token_expired",
                "The token has expired. Generate a replacement from your Profile page.", expiredAt);
    }

    public static AuthFailure invalidSignature() {
        return new AuthFailure("invalid_signature",
                "Token was signed with a different key. It may have been issued by another environment.", null);
    }

    public static AuthFailure invalidToken() {
        return new AuthFailure("invalid_token", "The token could not be validated.", null);
    }

    public static AuthFailure serviceTokenRevoked() {
        return new AuthFailure("service_token_revoked",
                "This service account token has been revoked.", null);
    }

    public static AuthFailure serviceTokenUnknown() {
        return new AuthFailure("service_token_unknown",
                "Service account token not recognized.", null);
    }

    public static AuthFailure serviceTokenLegacy() {
        return new AuthFailure("service_token_legacy",
                "This service account token predates revocation support. "
                        + "Generate a replacement from your Profile page.", null);
    }

    /**
     * The {@code WWW-Authenticate} challenge for this failure, per RFC 6750.
     * A request that carried no credential gets a bare challenge; anything else
     * gets {@code invalid_token} plus a description.
     */
    public String wwwAuthenticate() {
        if ("missing_credentials".equals(code)) {
            return "Bearer";
        }
        // RFC 6750 quoted-string admits no raw double quote.
        String description = message.replace('"', '\'');
        return "Bearer error=\"invalid_token\", error_description=\"" + description + "\"";
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd back-end && mvn test -Dtest=AuthFailureTest`
Expected: PASS, 9 tests.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/security/AuthFailure.java back-end/src/test/java/gov/nist/oscal/tools/api/security/AuthFailureTest.java
git commit -m "feat(auth): add AuthFailure contract for 401 diagnostics"
```

---

### Task 2: The response renderer

**Files:**
- Create: `back-end/src/main/java/gov/nist/oscal/tools/api/security/AuthFailureRenderer.java`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/security/AuthFailureRendererTest.java`

**Interfaces:**
- Consumes: `AuthFailure` and its accessors `code()`, `message()`, `expiredAt()`, `wwwAuthenticate()` from Task 1.
- Produces: `public static void render(HttpServletResponse response, AuthFailure failure) throws IOException`

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/security/AuthFailureRendererTest.java`:

```java
package gov.nist.oscal.tools.api.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFailureRendererTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void render_returns401WithJsonContentType() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthFailureRenderer.render(response, AuthFailure.malformedToken());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
    }

    @Test
    void render_keepsErrorFieldLiteralForBackwardCompatibility() throws Exception {
        // The frontend and existing clients read `error`; it must not become prose.
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthFailureRenderer.render(response, AuthFailure.invalidSignature());

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.get("error").asText()).isEqualTo("Unauthorized");
        assertThat(body.get("code").asText()).isEqualTo("invalid_signature");
        assertThat(body.get("message").asText()).isEqualTo(AuthFailure.invalidSignature().message());
    }

    @Test
    void render_includesExpiredAtWhenPresent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        Date expiry = Date.from(Instant.parse("2026-08-08T21:36:19Z"));

        AuthFailureRenderer.render(response, AuthFailure.tokenExpired(expiry));

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.get("expiredAt").asText()).isEqualTo("2026-08-08T21:36:19Z");
    }

    @Test
    void render_omitsExpiredAtWhenAbsent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthFailureRenderer.render(response, AuthFailure.missingCredentials());

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.has("expiredAt")).isFalse();
    }

    @Test
    void render_setsWwwAuthenticateChallenge() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthFailureRenderer.render(response, AuthFailure.missingCredentials());

        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }

    @Test
    void render_producesValidJsonWhenMessageContainsQuotes() throws Exception {
        // Guards the bug the old hand-built JSON had: string concatenation
        // emitted broken JSON the moment a message contained a quote.
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthFailureRenderer.render(response, new AuthFailure("invalid_token", "He said \"no\".", null));

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.get("message").asText()).isEqualTo("He said \"no\".");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=AuthFailureRendererTest`
Expected: FAIL — compilation error, `AuthFailureRenderer` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `back-end/src/main/java/gov/nist/oscal/tools/api/security/AuthFailureRenderer.java`:

```java
package gov.nist.oscal.tools.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes an {@link AuthFailure} to the response as the single 401 shape.
 * <p>
 * Deliberately a static utility rather than a Spring bean: both callers are
 * awkward injection sites — {@code JwtAuthenticationFilter} uses field
 * injection, and {@code SecurityConfig}'s entry point is a lambda in a
 * {@code @Bean} method. Keeping this dependency-free also means it adds nothing
 * to the bean graph that {@code @WebMvcTest} slices would need to mock.
 * </p>
 */
public final class AuthFailureRenderer {

    /** Serialization is stateless here; ObjectMapper is thread-safe once configured. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuthFailureRenderer() {
    }

    /**
     * Render the failure: 401, JSON body, and an RFC 6750 challenge header.
     * The {@code error} field stays the literal {@code "Unauthorized"} so
     * clients that key off it are unaffected by this diagnostic detail.
     *
     * @param response the response to write
     * @param failure  the failure to report
     * @throws IOException if the response writer fails
     */
    public static void render(HttpServletResponse response, AuthFailure failure) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("WWW-Authenticate", failure.wwwAuthenticate());

        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", "Unauthorized");
        body.put("message", failure.message());
        body.put("code", failure.code());
        if (failure.expiredAt() != null) {
            body.put("expiredAt", failure.expiredAt());
        }

        MAPPER.writeValue(response.getWriter(), body);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd back-end && mvn test -Dtest=AuthFailureRendererTest`
Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/security/AuthFailureRenderer.java back-end/src/test/java/gov/nist/oscal/tools/api/security/AuthFailureRendererTest.java
git commit -m "feat(auth): add shared renderer for 401 failure responses"
```

---

### Task 3: Filter records every failure branch

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilter.java:45-90`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterDiagnosticsTest.java`

**Interfaces:**
- Consumes: `AuthFailure` factories and `AuthFailure.REQUEST_ATTRIBUTE` from Task 1.
- Produces: after this task, a failed authentication leaves an `AuthFailure` in the request attribute named by `AuthFailure.REQUEST_ATTRIBUTE`. Task 4 reads it.

**Note:** this task changes no response behavior — it only records. That is intentional: the record-then-render split is what keeps public endpoints working. End-to-end behavior arrives in Task 4.

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterDiagnosticsTest.java`:

```java
package gov.nist.oscal.tools.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The filter classifies why authentication failed and records it for the
 * entry point to render. It must never write the response for these cases:
 * public endpoints arrive with no credential and have to keep working.
 */
class JwtAuthenticationFilterDiagnosticsTest {

    private JwtUtil jwtUtil;
    private UserDetailsService userDetailsService;
    private ServiceAccountTokenRepository repository;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

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

        request = new MockHttpServletRequest();
        request.setRequestURI("/api/validate");
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AuthFailure recordedFailure() {
        Object attribute = request.getAttribute(AuthFailure.REQUEST_ATTRIBUTE);
        assertThat(attribute).isInstanceOf(AuthFailure.class);
        return (AuthFailure) attribute;
    }

    @Test
    void noAuthorizationHeader_recordsMissingCredentials() throws Exception {
        filter.doFilterInternal(request, response, chain);

        assertThat(recordedFailure().code()).isEqualTo("missing_credentials");
    }

    @Test
    void nonBearerScheme_recordsUnsupportedScheme() throws Exception {
        // Previously silent: this branch logged nothing and returned a bare 401.
        request.addHeader("Authorization", "Token abc123");

        filter.doFilterInternal(request, response, chain);

        assertThat(recordedFailure().code()).isEqualTo("unsupported_auth_scheme");
    }

    @Test
    void malformedToken_recordsMalformedToken() throws Exception {
        // The production failure: a value with zero period characters.
        request.addHeader("Authorization", "Bearer not-a-jwt");
        when(jwtUtil.extractUsername("not-a-jwt"))
                .thenThrow(new MalformedJwtException("Found: 0"));

        filter.doFilterInternal(request, response, chain);

        assertThat(recordedFailure().code()).isEqualTo("malformed_token");
    }

    @Test
    void expiredToken_recordsExpiryInstant() throws Exception {
        request.addHeader("Authorization", "Bearer expired");
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(Date.from(Instant.parse("2026-08-08T21:36:19Z")));
        when(jwtUtil.extractUsername("expired"))
                .thenThrow(new ExpiredJwtException(null, claims, "expired"));

        filter.doFilterInternal(request, response, chain);

        AuthFailure failure = recordedFailure();
        assertThat(failure.code()).isEqualTo("token_expired");
        assertThat(failure.expiredAt()).isEqualTo("2026-08-08T21:36:19Z");
    }

    @Test
    void badSignature_recordsInvalidSignature() throws Exception {
        request.addHeader("Authorization", "Bearer forged");
        when(jwtUtil.extractUsername("forged"))
                .thenThrow(new SignatureException("bad signature"));

        filter.doFilterInternal(request, response, chain);

        assertThat(recordedFailure().code()).isEqualTo("invalid_signature");
    }

    @Test
    void unexpectedParseFailure_recordsInvalidToken() throws Exception {
        request.addHeader("Authorization", "Bearer weird");
        when(jwtUtil.extractUsername("weird"))
                .thenThrow(new IllegalArgumentException("something else"));

        filter.doFilterInternal(request, response, chain);

        assertThat(recordedFailure().code()).isEqualTo("invalid_token");
    }

    @Test
    void tokenThatFailsValidation_recordsInvalidToken() throws Exception {
        // Parses fine but validateToken() says no — previously recorded nothing.
        request.addHeader("Authorization", "Bearer parses");
        UserDetails userDetails = User.builder()
                .username("alice").password("p").authorities(new ArrayList<>()).build();
        when(jwtUtil.extractUsername("parses")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
        when(jwtUtil.validateToken(eq("parses"), any())).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertThat(recordedFailure().code()).isEqualTo("invalid_token");
    }

    @Test
    void failureIsRecordedButNeverWritten_soPublicEndpointsStillWork() throws Exception {
        // The load-bearing guarantee. /api/health arrives with no credential and
        // must reach the controller; if the filter answered here it would 401.
        request.setRequestURI("/api/health");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void successfulAuthentication_recordsNoFailure() throws Exception {
        request.addHeader("Authorization", "Bearer good");
        UserDetails userDetails = User.builder()
                .username("alice").password("p").authorities(new ArrayList<>()).build();
        when(jwtUtil.extractUsername("good")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
        when(jwtUtil.validateToken(eq("good"), any())).thenReturn(true);
        when(jwtUtil.extractTokenType("good")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertThat(request.getAttribute(AuthFailure.REQUEST_ATTRIBUTE)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void malformedToken_stillPassesDownTheChain() throws Exception {
        request.addHeader("Authorization", "Bearer not-a-jwt");
        when(jwtUtil.extractUsername("not-a-jwt"))
                .thenThrow(new MalformedJwtException("Found: 0"));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(chain, never()).doFilter(any(), eq(null));
    }
}
```

`doFilterInternal` is `protected` in `OncePerRequestFilter`; this test is in the same package as the filter, so it is directly callable.

**Known limitation to leave alone in this task.** In jjwt 0.13.0,
`io.jsonwebtoken.security.SignatureException` (what the filter imports and
catches) extends the deprecated `io.jsonwebtoken.SignatureException`. If any code
path throws the *parent* type, it falls through to the generic `Exception` catch
and reports `invalid_token` rather than `invalid_signature`. That is a graceful
degradation, not a regression — today every one of these cases is
indistinguishable anyway. Widening the catch is out of scope here; note it if the
live check in Task 6 ever shows `invalid_token` where a signature failure was
expected.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=JwtAuthenticationFilterDiagnosticsTest`
Expected: FAIL — the failure-code assertions fail because nothing sets the attribute yet. `failureIsRecordedButNeverWritten_soPublicEndpointsStillWork` should already pass, since that is existing behavior being locked down.

- [ ] **Step 3: Write minimal implementation**

In `JwtAuthenticationFilter.java`, replace the header-extraction block (currently lines 45-76) with:

```java
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Extract JWT from Authorization header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
                if (logger.isDebugEnabled()) {
                    logger.debug("JWT token validated for user: " + username);
                }
            } catch (ExpiredJwtException e) {
                logger.warn("JWT token has expired for request to " + request.getRequestURI() + ": " + e.getMessage()
                        + " [code=token_expired]");
                Date expiry = e.getClaims() == null ? null : e.getClaims().getExpiration();
                record(request, AuthFailure.tokenExpired(expiry));
            } catch (SignatureException e) {
                logger.warn("JWT signature validation failed for request to " + request.getRequestURI() + ": " + e.getMessage() + " - This may indicate the server was restarted with a different JWT secret [code=invalid_signature]");
                record(request, AuthFailure.invalidSignature());
            } catch (MalformedJwtException e) {
                logger.warn("Malformed JWT token for request to " + request.getRequestURI() + ": " + e.getMessage()
                        + " [code=malformed_token]");
                record(request, AuthFailure.malformedToken());
            } catch (Exception e) {
                // Invalid token - continue without authentication
                logger.warn("Invalid JWT token for request to " + request.getRequestURI() + ": " + e.getMessage()
                        + " [code=invalid_token]");
                record(request, AuthFailure.invalidToken());
            }
        } else if (authorizationHeader != null) {
            // Header present but not a Bearer credential. This branch used to be
            // silent, which made an integration sending the wrong scheme
            // indistinguishable from one sending nothing at all.
            logger.warn("Authorization header with unsupported scheme for request to "
                    + request.getRequestURI() + " [code=unsupported_auth_scheme]");
            record(request, AuthFailure.unsupportedScheme());
        } else {
            String uri = request.getRequestURI();
            if (!uri.contains("/auth/") && !uri.contains("/health") && !uri.contains("/swagger") && !uri.contains("/v3/api-docs")) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No Authorization header present for request to " + uri);
                }
            }
            record(request, AuthFailure.missingCredentials());
        }
```

Then, in the validation block, record the `validateToken` false case. Change:

```java
            if (jwtUtil.validateToken(jwt, userDetails)) {
```

to add an `else` at the end of that `if` body's enclosing block:

```java
            if (jwtUtil.validateToken(jwt, userDetails)) {
                // ... existing body unchanged ...
            } else {
                logger.warn("JWT token failed validation for request to " + request.getRequestURI()
                        + " [code=invalid_token]");
                record(request, AuthFailure.invalidToken());
            }
```

Add this helper method to the class, next to `serviceTokenRejection`:

```java
    /**
     * Record why authentication failed, for {@code SecurityConfig}'s entry point
     * to render if Spring Security goes on to reject the request.
     * <p>
     * Recording rather than responding is deliberate. Public endpoints
     * ({@code /api/health}, {@code /api/auth/login}) legitimately arrive with no
     * credential, so this filter cannot know a failure is fatal — only the
     * authorization rules do. Writing a 401 here would break them.
     * </p>
     */
    private void record(HttpServletRequest request, AuthFailure failure) {
        request.setAttribute(AuthFailure.REQUEST_ATTRIBUTE, failure);
    }
```

Add the import `java.util.Date` to the existing import block.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd back-end && mvn test -Dtest=JwtAuthenticationFilterDiagnosticsTest`
Expected: PASS, 10 tests.

- [ ] **Step 5: Verify the existing filter tests still pass**

Run: `cd back-end && mvn test -Dtest='JwtAuthenticationFilter*Test'`
Expected: PASS — `JwtAuthenticationFilterServiceTokenTest` and `JwtAuthenticationFilterBaggageTest` are unchanged by this task and must stay green.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilter.java back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterDiagnosticsTest.java
git commit -m "feat(auth): record the reason authentication failed"
```

---

### Task 4: Entry point renders the recorded failure

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java:180-187`
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/config/AuthenticationEntryPointTest.java`

**Interfaces:**
- Consumes: `AuthFailure.REQUEST_ATTRIBUTE`, `AuthFailure.missingCredentials()` (Task 1), `AuthFailureRenderer.render(...)` (Task 2), and the attribute written by the filter (Task 3).
- Produces: end-to-end behavior. No later task depends on new symbols from this one.

- [ ] **Step 1: Write the failing test**

Create `back-end/src/test/java/gov/nist/oscal/tools/api/config/AuthenticationEntryPointTest.java`:

```java
package gov.nist.oscal.tools.api.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.security.AuthFailure;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The entry point is where a recorded failure becomes a response. Instantiating
 * SecurityConfig directly is fine — authenticationEntryPoint() touches none of
 * its injected collaborators.
 */
class AuthenticationEntryPointTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthenticationEntryPoint entryPoint = new SecurityConfig().authenticationEntryPoint();

    private static final AuthenticationException NOT_AUTHENTICATED =
            new AuthenticationException("not authenticated") {
            };

    @Test
    void rendersTheRecordedFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthFailure.REQUEST_ATTRIBUTE, AuthFailure.malformedToken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, NOT_AUTHENTICATED);

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText()).isEqualTo("malformed_token");
        assertThat(body.get("error").asText()).isEqualTo("Unauthorized");
    }

    @Test
    void rendersExpiryWhenTheFailureCarriesIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthFailure.REQUEST_ATTRIBUTE,
                AuthFailure.tokenExpired(java.util.Date.from(java.time.Instant.parse("2026-08-08T21:36:19Z"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, NOT_AUTHENTICATED);

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.get("expiredAt").asText()).isEqualTo("2026-08-08T21:36:19Z");
    }

    @Test
    void fallsBackToMissingCredentialsWhenNothingWasRecorded() throws Exception {
        // e.g. a request rejected before the JWT filter ran.
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, NOT_AUTHENTICATED);

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo("missing_credentials");
    }

    @Test
    void alwaysSetsAChallengeHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, NOT_AUTHENTICATED);

        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=AuthenticationEntryPointTest`
Expected: FAIL — the body still has no `code` field, so `body.get("code")` is null and the assertion throws.

- [ ] **Step 3: Write minimal implementation**

In `SecurityConfig.java`, replace the `authenticationEntryPoint()` bean body (currently lines 180-187) with:

```java
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            Object recorded = request.getAttribute(AuthFailure.REQUEST_ATTRIBUTE);
            AuthFailure failure = recorded instanceof AuthFailure f
                    ? f
                    : AuthFailure.missingCredentials();
            AuthFailureRenderer.render(response, failure);
        };
    }
```

Update the Javadoc above it, which currently only explains 401-vs-403, to also note that the body now carries the specific reason recorded by `JwtAuthenticationFilter`.

Add imports:

```java
import gov.nist.oscal.tools.api.security.AuthFailure;
import gov.nist.oscal.tools.api.security.AuthFailureRenderer;
```

The `MediaType` import may become unused once the hand-written body is gone — check and remove it if so, since the build treats unused imports as noise. Leave it if other code in the file still uses it.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd back-end && mvn test -Dtest=AuthenticationEntryPointTest`
Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/config/SecurityConfig.java back-end/src/test/java/gov/nist/oscal/tools/api/config/AuthenticationEntryPointTest.java
git commit -m "feat(auth): return the specific reason a 401 was issued"
```

---

### Task 5: Route the service-token gate through the shared renderer

**Files:**
- Modify: `back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilter.java:84-90` and `serviceTokenRejection` (currently around lines 146-172)
- Test: `back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterServiceTokenTest.java`

**Interfaces:**
- Consumes: `AuthFailure` factories (Task 1), `AuthFailureRenderer.render(...)` (Task 2).
- Produces: `serviceTokenRejection` returns `AuthFailure` instead of `String`. Nothing outside the filter calls it (it is private).

- [ ] **Step 1: Write the failing test**

Read `JwtAuthenticationFilterServiceTokenTest.java` first — it already covers the three gate paths and asserts on the response body. Update its body assertions to the new shape and add a code assertion. Append these tests to that class:

```java
    @Test
    void revokedToken_reportsItsCodeInTheBody() throws Exception {
        ServiceAccountToken revoked = new ServiceAccountToken();
        revoked.setId(7L);
        revoked.setJti("jti-1");
        revoked.setRevokedAt(LocalDateTime.now().minusDays(1));
        when(jwtUtil.extractTokenType("tok")).thenReturn("service-account");
        when(jwtUtil.extractJti("tok")).thenReturn("jti-1");
        when(repository.findByJti("jti-1")).thenReturn(Optional.of(revoked));

        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, mock(FilterChain.class));

        assertEquals(401, response.getStatus());
        com.fasterxml.jackson.databind.JsonNode body =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getContentAsString());
        assertEquals("service_token_revoked", body.get("code").asText());
        assertEquals("Unauthorized", body.get("error").asText());
        assertEquals("This service account token has been revoked.", body.get("message").asText());
    }

    @Test
    void legacyToken_reportsItsCode() throws Exception {
        when(jwtUtil.extractTokenType("tok")).thenReturn("service-account");
        when(jwtUtil.extractJti("tok")).thenReturn(null);

        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, mock(FilterChain.class));

        com.fasterxml.jackson.databind.JsonNode body =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getContentAsString());
        assertEquals("service_token_legacy", body.get("code").asText());
    }

    @Test
    void unknownToken_reportsItsCode() throws Exception {
        when(jwtUtil.extractTokenType("tok")).thenReturn("service-account");
        when(jwtUtil.extractJti("tok")).thenReturn("jti-missing");
        when(repository.findByJti("jti-missing")).thenReturn(Optional.empty());

        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, mock(FilterChain.class));

        com.fasterxml.jackson.databind.JsonNode body =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getContentAsString());
        assertEquals("service_token_unknown", body.get("code").asText());
    }

    @Test
    void gateResponse_setsChallengeHeader() throws Exception {
        when(jwtUtil.extractTokenType("tok")).thenReturn("service-account");
        when(jwtUtil.extractJti("tok")).thenReturn(null);

        MockHttpServletRequest request = requestWithToken();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, mock(FilterChain.class));

        assertNotNull(response.getHeader("WWW-Authenticate"));
    }
```

If any existing test in that class asserts the old body shape — an `error` field containing prose such as `"This service account token has been revoked."` — update it to read `message` instead. Do not delete those tests; they are the regression guard that the gate still refuses these tokens.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd back-end && mvn test -Dtest=JwtAuthenticationFilterServiceTokenTest`
Expected: FAIL — the new `code` assertions fail because the current body is `{"error":"<prose>"}` with no `code`.

- [ ] **Step 3: Write minimal implementation**

In `JwtAuthenticationFilter.java`, change the gate call site (currently lines 84-90) from:

```java
                String rejection = serviceTokenRejection(jwt);
                if (rejection != null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"" + rejection + "\"}");
                    return;
                }
```

to:

```java
                AuthFailure rejection = serviceTokenRejection(jwt);
                if (rejection != null) {
                    // The one place this filter answers directly: the token
                    // authenticated, so the refusal is unambiguous and does not
                    // need the authorization rules to confirm it.
                    AuthFailureRenderer.render(response, rejection);
                    return;
                }
```

Change `serviceTokenRejection`'s signature and returns:

```java
    private AuthFailure serviceTokenRejection(String jwt) {
        if (!"service-account".equals(jwtUtil.extractTokenType(jwt))) {
            return null;
        }

        String jti = jwtUtil.extractJti(jwt);
        if (jti == null || jti.isBlank()) {
            logger.warn("Rejected a service account token issued before revocation support (no jti) [code=service_token_legacy]");
            return AuthFailure.serviceTokenLegacy();
        }

        Optional<ServiceAccountToken> found = serviceAccountTokenRepository.findByJti(jti);
        if (found.isEmpty()) {
            logger.warn("Rejected a service account token with an unrecognized jti: " + jti
                    + " [code=service_token_unknown]");
            return AuthFailure.serviceTokenUnknown();
        }

        ServiceAccountToken record = found.get();
        if (record.getRevokedAt() != null) {
            logger.warn("Rejected a revoked service account token: " + jti + " [code=service_token_revoked]");
            return AuthFailure.serviceTokenRevoked();
        }

        touchLastUsed(record);
        return null;
    }
```

Update the method's Javadoc: its second paragraph explains why the messages distinguish the three cases, which is still true — extend it to note that the codes are the machine-readable form of that same distinction.

`HttpServletResponse` may no longer be referenced in the gate path, but it remains a `doFilterInternal` parameter type, so keep the import.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd back-end && mvn test -Dtest=JwtAuthenticationFilterServiceTokenTest`
Expected: PASS, all tests including the four new ones.

- [ ] **Step 5: Run the full backend suite**

Run: `cd back-end && mvn test`
Expected: PASS. If anything unrelated fails, run `mvn clean test` once before investigating — a stale `target/` from the IDE compiler produces phantom failures in this repo.

- [ ] **Step 6: Commit**

```bash
git add back-end/src/main/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilter.java back-end/src/test/java/gov/nist/oscal/tools/api/security/JwtAuthenticationFilterServiceTokenTest.java
git commit -m "refactor(auth): render service-token refusals through shared renderer"
```

---

### Task 6: Verify against a running server

**Files:** none — this task produces evidence, not code.

**Interfaces:**
- Consumes: everything above.
- Produces: confirmation that the three failures observed in production now return distinct codes.

- [ ] **Step 1: Start the stack**

Run: `./dev.sh` from the repo root. Wait for the backend on port 8090.

- [ ] **Step 2: Confirm public endpoints are unaffected**

This is the regression that would matter most in production.

```bash
curl -s -o /dev/null -w 'health=%{http_code}\n' http://localhost:8090/api/health
```

Expected: `health=200`.

- [ ] **Step 3: Exercise each failure**

```bash
echo '--- missing header'; curl -s -i http://localhost:8090/api/validate -X POST -H 'Content-Type: application/json' -d '{"content":"{}","format":"JSON"}' | grep -E 'HTTP/|WWW-Authenticate|code'
echo '--- wrong scheme'; curl -s http://localhost:8090/api/validate -X POST -H 'Authorization: Token abc' -H 'Content-Type: application/json' -d '{"content":"{}","format":"JSON"}'
echo '--- malformed (the production case: zero dots)'; curl -s http://localhost:8090/api/validate -X POST -H 'Authorization: Bearer notajwt' -H 'Content-Type: application/json' -d '{"content":"{}","format":"JSON"}'
```

Expected: `missing_credentials`, `unsupported_auth_scheme`, and `malformed_token` respectively, each with `"error":"Unauthorized"` and a `WWW-Authenticate` header.

- [ ] **Step 4: Confirm a valid token still authenticates**

Log in through the UI at http://localhost:3010, generate a service account token on the Profile page, then:

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8090/api/validate -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"content":"{}","format":"JSON"}'
```

Expected: `200`. A 401 here means the change broke the success path.

- [ ] **Step 5: Record the results**

Append an "Execution results" section to `docs/superpowers/specs/2026-08-10-auth-failure-diagnostics-design.md` with the actual curl output, then commit:

```bash
git add docs/superpowers/specs/2026-08-10-auth-failure-diagnostics-design.md
git commit -m "docs(spec): record live verification of auth failure diagnostics"
```

---

## Self-Review

**Spec coverage:**

| Spec requirement | Task |
|---|---|
| Filter records `AuthFailure`, entry point renders | 3, 4 |
| Record-and-continue; public endpoints keep working | 3 (test), 6 (live check) |
| Nine codes with actionable messages | 1 |
| `error` stays literal `"Unauthorized"` | 2 |
| `WWW-Authenticate` per RFC 6750 | 1, 2 |
| `expiredAt`, ISO-8601 UTC, omitted when null | 1, 2 |
| Service-token gate normalized, hand-built JSON removed | 5 |
| `warn` logs keep wording, gain `code` | 3, 5 |
| `AuthFailure.java` — contract in one file | 1 |
| `AuthFailureRenderer.java` — shared renderer | 2 |
| Tests: contract, renderer, per-branch, entry point, public-path guard | 1, 2, 3, 4 |

The spec's testing section also names a MockMvc integration test. This plan replaces it with the direct entry-point test in Task 4 plus the live curl verification in Task 6. Reason: a full `@SpringBootTest` here would need a live PostgreSQL for context startup, which buys no coverage the entry-point unit test and the real HTTP check don't already give. This is a deliberate deviation, recorded here rather than silently dropped.

**Placeholder scan:** No TBD/TODO. Every code step carries real code. Task 5 asks the implementer to read an existing test file before editing it, which is a genuine prerequisite rather than a deferred decision.

**Type consistency:** `AuthFailure(String, String, String)` with accessors `code()`, `message()`, `expiredAt()`, `wwwAuthenticate()`, and `REQUEST_ATTRIBUTE`, used identically in Tasks 2-5. `AuthFailureRenderer.render(HttpServletResponse, AuthFailure)` matches at both call sites. `tokenExpired(Date)` takes `java.util.Date` — what `Claims.getExpiration()` returns in Task 3 and what the tests construct via `Date.from(Instant)`. `serviceTokenRejection` returns `AuthFailure` consistently in Task 5.
