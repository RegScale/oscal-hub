package gov.nist.oscal.tools.api.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RateLimitFilter — the filter that returns 429 when a
 * client exceeds the configured per-endpoint rate.
 *
 * Coverage focus:
 *  - login vs register vs general API branch routing
 *  - allowed → chain continues, denied → 429 + JSON body
 *  - X-Forwarded-For multi-IP parsing (LB scenarios) and X-Real-IP fallback
 *  - authenticated user → "user:" identifier, anonymous → "ip:" identifier
 *  - shouldNotFilter for health, swagger, static resources
 *  - rate limit headers on every response (Limit, Remaining, Reset)
 *    and Retry-After only when denied
 */
class RateLimitFilterTest {

    private RateLimitFilter filter;
    private RateLimitService service;
    private RateLimitConfig config;
    private FilterChain chain;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        service = mock(RateLimitService.class);
        config = stubConfig();
        chain = mock(FilterChain.class);
        ReflectionTestUtils.setField(filter, "rateLimitService", service);
        ReflectionTestUtils.setField(filter, "rateLimitConfig", config);
        ReflectionTestUtils.setField(filter, "objectMapper", mapper);
        ReflectionTestUtils.setField(filter, "clientIpResolver",
                new gov.nist.oscal.tools.api.util.ClientIpResolver(1));
        SecurityContextHolder.clearContext();
    }

    @Test
    void disabled_bypassesAllChecks() throws Exception {
        config.setEnabled(false);
        MockHttpServletRequest req = req("GET", "/api/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(any(), any());
        verify(service, never()).isLoginAllowed(any());
        // No rate-limit headers when disabled
        assertThat(res.getHeader("X-RateLimit-Limit")).isNull();
    }

    @Test
    void loginEndpoint_routedToLoginRateLimit() throws Exception {
        when(service.isLoginAllowed("1.2.3.4")).thenReturn(true);
        when(service.getLoginRemainingAttempts("1.2.3.4")).thenReturn(3L);
        when(service.getLoginResetTime("1.2.3.4")).thenReturn(60L);

        MockHttpServletRequest req = req("POST", "/api/auth/login");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(service, times(1)).isLoginAllowed("1.2.3.4");
        verify(chain, times(1)).doFilter(any(), any());
        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("5"); // login attempts default
        assertThat(res.getHeader("X-RateLimit-Remaining")).isEqualTo("3");
        assertThat(res.getHeader("Retry-After")).isNull();
    }

    @Test
    void loginEndpoint_denied_returns429_withJsonBody_andRetryAfter() throws Exception {
        when(service.isLoginAllowed("1.2.3.4")).thenReturn(false);
        when(service.getLoginRemainingAttempts("1.2.3.4")).thenReturn(0L);
        when(service.getLoginResetTime("1.2.3.4")).thenReturn(120L);

        MockHttpServletRequest req = req("POST", "/api/auth/login");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        // 429 returned, chain NOT continued
        assertThat(res.getStatus()).isEqualTo(429);
        verify(chain, never()).doFilter(any(), any());
        assertThat(res.getContentType()).startsWith("application/json");
        assertThat(res.getHeader("Retry-After")).isEqualTo("120");
        assertThat(res.getHeader("X-RateLimit-Remaining")).isEqualTo("0");

        // Body is well-formed JSON with the documented keys
        JsonNode body = mapper.readTree(res.getContentAsString());
        assertThat(body.get("error").asText()).isEqualTo("Too Many Requests");
        assertThat(body.get("message").asText()).contains("login attempts");
        assertThat(body.get("retryAfter").asLong()).isEqualTo(120L);
        assertThat(body.has("timestamp")).isTrue();
    }

    @Test
    void registerEndpoint_routedToRegistrationRateLimit() throws Exception {
        when(service.isRegistrationAllowed("1.2.3.4")).thenReturn(true);

        MockHttpServletRequest req = req("POST", "/api/auth/register");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(service, times(1)).isRegistrationAllowed("1.2.3.4");
        verify(chain, times(1)).doFilter(any(), any());
        assertThat(res.getHeader("X-RateLimit-Limit")).isEqualTo("3"); // registration attempts default
    }

    @Test
    void registerEndpoint_denied_messageMentionsRegistration() throws Exception {
        when(service.isRegistrationAllowed("1.2.3.4")).thenReturn(false);

        MockHttpServletRequest req = req("POST", "/api/auth/register");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        JsonNode body = mapper.readTree(res.getContentAsString());
        assertThat(body.get("message").asText()).contains("registration");
    }

    @Test
    void generalApi_anonymous_usesIpIdentifier() throws Exception {
        when(service.isApiRequestAllowed("ip:9.9.9.9")).thenReturn(true);
        when(service.getApiRemainingRequests("ip:9.9.9.9")).thenReturn(50L);

        MockHttpServletRequest req = req("GET", "/api/library/items");
        req.setRemoteAddr("9.9.9.9");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(service, times(1)).isApiRequestAllowed("ip:9.9.9.9");
    }

    @Test
    void generalApi_authenticated_usesUsernameIdentifier() throws Exception {
        // Authenticated calls share a quota across IPs (same user from laptop + phone),
        // which prevents users from sneaking around their own quota by hopping networks.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "x", List.of()));
        when(service.isApiRequestAllowed("user:alice")).thenReturn(true);
        when(service.getApiRemainingRequests("user:alice")).thenReturn(99L);

        MockHttpServletRequest req = req("GET", "/api/library/items");
        req.setRemoteAddr("9.9.9.9");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(service, times(1)).isApiRequestAllowed("user:alice");
    }

    @Test
    void generalApi_anonymousPrincipalLiteral_treatedAsAnonymous() throws Exception {
        // Spring Security sets principal == "anonymousUser" string when no auth.
        // The filter must NOT bucket that as a user.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", "", List.of()));
        when(service.isApiRequestAllowed("ip:9.9.9.9")).thenReturn(true);

        MockHttpServletRequest req = req("GET", "/api/library/items");
        req.setRemoteAddr("9.9.9.9");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(service, times(1)).isApiRequestAllowed("ip:9.9.9.9");
        verify(service, never()).isApiRequestAllowed("user:anonymousUser");
    }

    @Test
    void xForwardedFor_multipleIps_takesRightmostTrustedEntry() throws Exception {
        // Proxies APPEND the connection IP they observed: earlier entries are
        // client-supplied and spoofable. With one trusted hop (Cloud Run), the
        // rightmost entry is the real client — rate-limiting the FIRST entry let
        // attackers rotate fake IPs to bypass the per-IP buckets.
        when(service.isLoginAllowed("203.0.113.5")).thenReturn(true);

        MockHttpServletRequest req = req("POST", "/api/auth/login");
        req.addHeader("X-Forwarded-For", "6.6.6.6, 6.6.6.7, 203.0.113.5");
        req.setRemoteAddr("10.0.0.99"); // the proxy itself
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(service, times(1)).isLoginAllowed("203.0.113.5");
    }

    @Test
    void xForwardedFor_singleIp_isUsedDirectly() throws Exception {
        when(service.isLoginAllowed("203.0.113.5")).thenReturn(true);

        MockHttpServletRequest req = req("POST", "/api/auth/login");
        req.addHeader("X-Forwarded-For", "203.0.113.5");
        req.setRemoteAddr("10.0.0.99");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(service, times(1)).isLoginAllowed("203.0.113.5");
    }

    @Test
    void xRealIp_isIgnored_asClientSpoofable() throws Exception {
        // X-Real-IP is not set by Cloud Run/GFE and is trivially client-spoofable;
        // without X-Forwarded-For the socket address is authoritative.
        when(service.isLoginAllowed("10.0.0.99")).thenReturn(true);

        MockHttpServletRequest req = req("POST", "/api/auth/login");
        req.addHeader("X-Real-IP", "198.51.100.7");
        req.setRemoteAddr("10.0.0.99");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(service, times(1)).isLoginAllowed("10.0.0.99");
    }

    @Test
    void remoteAddr_used_whenNoProxyHeaders() throws Exception {
        when(service.isLoginAllowed("127.0.0.1")).thenReturn(true);

        MockHttpServletRequest req = req("POST", "/api/auth/login");
        req.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(service, times(1)).isLoginAllowed("127.0.0.1");
    }

    @Test
    void shouldNotFilter_health_skipsRateLimitEntirely() {
        MockHttpServletRequest req = req("GET", "/api/health");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    void shouldNotFilter_swaggerUi_andApiDocs_skipped() {
        assertThat(filter.shouldNotFilter(req("GET", "/swagger-ui/index.html"))).isTrue();
        assertThat(filter.shouldNotFilter(req("GET", "/v3/api-docs"))).isTrue();
        assertThat(filter.shouldNotFilter(req("GET", "/v3/api-docs/oscal"))).isTrue();
    }

    @Test
    void shouldNotFilter_staticResources_skipped() {
        assertThat(filter.shouldNotFilter(req("GET", "/static/main.css"))).isTrue();
        assertThat(filter.shouldNotFilter(req("GET", "/public/logo.png"))).isTrue();
    }

    @Test
    void shouldNotFilter_apiPaths_arePeerReviewed() {
        // API paths must NOT be skipped — that's the whole point of the filter.
        assertThat(filter.shouldNotFilter(req("POST", "/api/auth/login"))).isFalse();
        assertThat(filter.shouldNotFilter(req("GET", "/api/library/items"))).isFalse();
    }

    @Test
    void rateLimitHeaders_alwaysIncludeLimitRemainingAndReset() throws Exception {
        // Verify the standard "X-RateLimit-*" set is present on a happy-path response,
        // not just on 429s — clients use them for backoff.
        when(service.isLoginAllowed("1.2.3.4")).thenReturn(true);
        when(service.getLoginRemainingAttempts("1.2.3.4")).thenReturn(2L);
        when(service.getLoginResetTime("1.2.3.4")).thenReturn(60L);

        MockHttpServletRequest req = req("POST", "/api/auth/login");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader("X-RateLimit-Limit")).isNotBlank();
        assertThat(res.getHeader("X-RateLimit-Remaining")).isEqualTo("2");
        assertThat(res.getHeader("X-RateLimit-Reset")).isNotBlank();
        // Reset is unix seconds; loose check that it's a positive number
        assertThat(Long.parseLong(res.getHeader("X-RateLimit-Reset"))).isPositive();
    }

    @Test
    void remainingAttempts_negative_isClampedToZeroInHeader() throws Exception {
        // A buggy service implementation could return -1; the Math.max(0, …) clamp
        // ensures clients never see "X-RateLimit-Remaining: -1".
        when(service.isLoginAllowed("1.2.3.4")).thenReturn(true);
        when(service.getLoginRemainingAttempts("1.2.3.4")).thenReturn(-5L);
        when(service.getLoginResetTime("1.2.3.4")).thenReturn(60L);

        MockHttpServletRequest req = req("POST", "/api/auth/login");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
    }

    // ---- helpers ----

    private static MockHttpServletRequest req(String method, String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod(method);
        req.setRequestURI(uri);
        return req;
    }

    private static RateLimitConfig stubConfig() {
        RateLimitConfig c = new RateLimitConfig();
        c.setEnabled(true);
        // Defaults from the @ConfigurationProperties class — set explicit values
        // so the test doesn't depend on fields the service might add later.
        c.getLogin().setAttempts(5);
        c.getLogin().setDuration(900);
        c.getRegistration().setAttempts(3);
        c.getRegistration().setDuration(3600);
        c.getApi().setRequests(100);
        c.getApi().setDuration(60);
        return c;
    }
}
