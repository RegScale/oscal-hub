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
    void lowercaseBearerScheme_authenticatesRatherThanRecordingUnsupportedScheme() throws Exception {
        // RFC 7235: the auth-scheme token is case-insensitive. A client sending
        // "bearer <token>" is not making a wrong-scheme mistake.
        request.addHeader("Authorization", "bearer good");
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
    void blankTokenAfterBearerPrefix_recordsMissingCredentials() throws Exception {
        // "Authorization: Bearer " (trailing space, empty token) - typically an
        // unset environment variable interpolated into the header. This should
        // be diagnosed precisely rather than falling through to the JWT parser
        // and coming out as the generic invalid_token.
        request.addHeader("Authorization", "Bearer ");

        filter.doFilterInternal(request, response, chain);

        assertThat(recordedFailure().code()).isEqualTo("missing_credentials");
        verify(jwtUtil, never()).extractUsername(any());
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
