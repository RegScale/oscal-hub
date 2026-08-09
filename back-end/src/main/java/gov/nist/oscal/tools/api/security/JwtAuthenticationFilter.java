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
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

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

        // Extract JWT from Authorization header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
                if (logger.isDebugEnabled()) {
                    logger.debug("JWT token validated for user: " + username);
                }
            } catch (ExpiredJwtException e) {
                logger.warn("JWT token has expired for request to " + request.getRequestURI() + ": " + e.getMessage());
            } catch (SignatureException e) {
                logger.warn("JWT signature validation failed for request to " + request.getRequestURI() + ": " + e.getMessage() + " - This may indicate the server was restarted with a different JWT secret");
            } catch (MalformedJwtException e) {
                logger.warn("Malformed JWT token for request to " + request.getRequestURI() + ": " + e.getMessage());
            } catch (Exception e) {
                // Invalid token - continue without authentication
                logger.warn("Invalid JWT token for request to " + request.getRequestURI() + ": " + e.getMessage());
            }
        } else {
            // Log when Authorization header is missing for protected endpoints
            String uri = request.getRequestURI();
            if (!uri.contains("/auth/") && !uri.contains("/health") && !uri.contains("/swagger") && !uri.contains("/v3/api-docs")) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No Authorization header present for request to " + uri);
                }
            }
        }

        // Validate token and set authentication
        Baggage baggage = Baggage.empty();
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                String rejection = serviceTokenRejection(jwt);
                if (rejection != null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"" + rejection + "\"}");
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
            }
        }

        try (Scope scope = baggage.makeCurrent()) {
            chain.doFilter(request, response);
        }
    }

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
}
