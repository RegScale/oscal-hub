package gov.nist.oscal.tools.api.security;

import gov.nist.oscal.tools.api.entity.ServiceAccountToken;
import gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * The RFC 7235 auth-scheme token this filter accepts, matched
     * case-insensitively. Kept as a named constant so the scheme name and its
     * length (used for the prefix match below) never drift apart.
     */
    private static final String BEARER_SCHEME = "Bearer";
    private static final String BEARER_PREFIX = BEARER_SCHEME + " ";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private ServiceAccountTokenRepository serviceAccountTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Extract JWT from Authorization header. The scheme token is matched
        // case-insensitively per RFC 7235; a client sending "bearer <token>"
        // is not making a wrong-scheme mistake and must still authenticate.
        if (authorizationHeader != null
                && authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            jwt = authorizationHeader.substring(BEARER_PREFIX.length());
            if (jwt.isBlank()) {
                // "Authorization: Bearer " with nothing after it - most often an
                // unset environment variable interpolated into the header. This
                // is a precise, common misconfiguration; don't let it fall through
                // to the JWT parser and come out as the generic invalid_token.
                record(request, AuthFailure.missingCredentials());
            } else {
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
            }
        } else if (authorizationHeader != null && authorizationHeader.equalsIgnoreCase(BEARER_SCHEME)) {
            // "Authorization: Bearer" with no trailing space and no token at all.
            // HTTP clients (curl included) strip trailing whitespace, so
            // "Authorization: Bearer $UNSET_VAR" arrives on the wire as exactly
            // this - the scheme with no separator or credential, not the
            // "Bearer " form handled above. This is a missing credential, not an
            // unsupported scheme: the caller did send Bearer.
            record(request, AuthFailure.missingCredentials());
        } else if (authorizationHeader != null) {
            // Header present but not a Bearer credential. This branch used to be
            // silent, which made an integration sending the wrong scheme
            // indistinguishable from one sending nothing at all.
            String uri = request.getRequestURI();
            String message = "Authorization header with unsupported scheme for request to "
                    + uri + " [code=unsupported_auth_scheme]";
            if (isRoutineUnauthenticatedPath(uri)) {
                if (logger.isDebugEnabled()) {
                    logger.debug(message);
                }
            } else {
                logger.warn(message);
            }
            record(request, AuthFailure.unsupportedScheme());
        } else {
            // Log when Authorization header is missing for protected endpoints
            String uri = request.getRequestURI();
            if (!isRoutineUnauthenticatedPath(uri)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No Authorization header present for request to " + uri);
                }
            }
            record(request, AuthFailure.missingCredentials());
        }

        // Validate token and set authentication
        Baggage baggage = Baggage.empty();
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                AuthFailure rejection = serviceTokenRejection(jwt);
                if (rejection != null) {
                    // The one place this filter answers directly: the token
                    // authenticated, so the refusal is unambiguous and does not
                    // need the authorization rules to confirm it.
                    AuthFailureRenderer.render(response, rejection);
                    return;
                }

                // Extract globalRole and orgRole from JWT token and add to authorities
                Collection<GrantedAuthority> authorities = new ArrayList<>(userDetails.getAuthorities());

                // Add global role (SUPER_ADMIN or USER)
                String globalRole = jwtUtil.extractGlobalRole(jwt);
                if (globalRole != null && !globalRole.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + globalRole));
                }

                // Add organization role (ORG_ADMIN or USER)
                String orgRole = jwtUtil.extractOrganizationRole(jwt);
                if (orgRole != null && !orgRole.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + orgRole));
                }

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                // Build OTel baggage with user/org identity for downstream propagation
                Long userId = jwtUtil.extractUserId(jwt);
                Long orgId = jwtUtil.extractOrganizationId(jwt);
                var builder = Baggage.current().toBuilder();
                if (userId != null) {
                    builder.put("user.id", String.valueOf(userId));
                }
                if (orgId != null) {
                    builder.put("org.id", String.valueOf(orgId));
                }
                if (globalRole != null && !globalRole.isEmpty()) {
                    builder.put("user.role.global", globalRole);
                }
                if (orgRole != null && !orgRole.isEmpty()) {
                    builder.put("user.role.org", orgRole);
                }
                baggage = builder.build();
            } else {
                logger.warn("JWT token failed validation for request to " + request.getRequestURI()
                        + " [code=invalid_token]");
                record(request, AuthFailure.invalidToken());
            }
        }

        try (Scope scope = baggage.makeCurrent()) {
            chain.doFilter(request, response);
        }
    }

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

    /**
     * True for request URIs that routinely arrive with no (or an unusual)
     * {@code Authorization} header as part of normal operation - auth endpoints,
     * health checks, and API docs. {@code /api/health} in particular is exempted
     * from rate limiting ({@code RateLimitFilter.shouldNotFilter}), so anything
     * that probes it frequently must not be able to drive unthrottled WARN
     * volume here; those paths log at DEBUG instead.
     */
    private boolean isRoutineUnauthenticatedPath(String uri) {
        return uri.contains("/auth/") || uri.contains("/health") || uri.contains("/swagger")
                || uri.contains("/v3/api-docs");
    }

    /**
     * Revocation gate for service account tokens. Returns null when the request
     * may proceed, or the {@link AuthFailure} explaining the refusal.
     * <p>
     * Session tokens carry no {@code tokenType} claim and pass straight through;
     * only service tokens are looked up. The messages distinguish revoked from
     * unknown from legacy because the caller already holds the credential — the
     * distinction leaks nothing and is what makes a failing pipeline diagnosable.
     * The failure's {@code code} is the machine-readable form of that same
     * distinction, for callers that branch on it instead of parsing prose.
     * </p>
     */
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
}
