package gov.nist.oscal.tools.api.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

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
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
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
            }
        }

        chain.doFilter(request, response);
    }
}
