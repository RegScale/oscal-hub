package gov.nist.oscal.tools.api.security;

import gov.nist.oscal.tools.api.entity.ServiceAccountToken;
import gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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

    private void stubServiceToken(String jti) {
        when(jwtUtil.extractTokenType("tok")).thenReturn("service-account");
        when(jwtUtil.extractJti("tok")).thenReturn(jti);
    }

    @Test
    void revokedServiceTokenIsRejectedWith401() throws Exception {
        stubServiceToken("the-jti");
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
        stubServiceToken(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestWithToken(), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(repository);
    }

    @Test
    void unknownJtiIsRejectedWith401() throws Exception {
        stubServiceToken("the-jti");
        when(repository.findByJti("the-jti")).thenReturn(Optional.empty());

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requestWithToken(), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void liveServiceTokenAuthenticatesAndRecordsUse() throws Exception {
        stubServiceToken("the-jti");
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

        stubServiceToken("the-jti");
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
}
