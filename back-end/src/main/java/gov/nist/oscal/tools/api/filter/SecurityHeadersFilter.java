package gov.nist.oscal.tools.api.filter;

import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

/**
 * Filter to add security headers to HTTP responses
 *
 * Implements OWASP recommended security headers:
 * - Strict-Transport-Security (HSTS): Enforces HTTPS
 * - Content-Security-Policy (CSP): Prevents XSS and injection attacks
 * - X-Frame-Options: Prevents clickjacking
 * - X-Content-Type-Options: Prevents MIME sniffing
 * - X-XSS-Protection: Enables browser XSS filter
 * - Referrer-Policy: Controls referrer information
 * - Permissions-Policy: Controls browser features
 *
 * Headers are configurable and can be disabled for development
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersFilter.class);

    @Autowired
    private SecurityHeadersConfig config;

    @PostConstruct
    public void init() {
        if (config.isEnabled()) {
            logger.info("Security headers filter enabled");
            logger.info("  HSTS: {}", config.getHsts().isEnabled());
            logger.info("  CSP: {}", config.getCsp().isEnabled());
            logger.info("  X-Frame-Options: {}", config.getFrameOptions().getPolicy());
            logger.info("  X-Content-Type-Options: {}", config.isEnableContentTypeOptions());
            logger.info("  X-XSS-Protection: {}", config.isEnableXssProtection());
            logger.info("  Referrer-Policy: {}", config.getReferrerPolicy());
            logger.info("  Permissions-Policy: {}", config.getPermissionsPolicy().isEnabled());
        } else {
            logger.info("Security headers filter disabled");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip if security headers are disabled
        if (!config.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Add Strict-Transport-Security (HSTS)
        if (config.getHsts().isEnabled() && isSecureConnection(request)) {
            response.setHeader("Strict-Transport-Security", config.getHsts().build());
        }

        // Add Content-Security-Policy (CSP)
        if (config.getCsp().isEnabled()) {
            String headerName = config.getCsp().isReportOnly()
                ? "Content-Security-Policy-Report-Only"
                : "Content-Security-Policy";
            response.setHeader(headerName, config.getCsp().build());
        }

        // Add X-Frame-Options
        response.setHeader("X-Frame-Options", config.getFrameOptions().getPolicy());

        // Add X-Content-Type-Options
        if (config.isEnableContentTypeOptions()) {
            response.setHeader("X-Content-Type-Options", "nosniff");
        }

        // Add X-XSS-Protection
        if (config.isEnableXssProtection()) {
            response.setHeader("X-XSS-Protection", "1; mode=block");
        }

        // Add Referrer-Policy
        if (config.getReferrerPolicy() != null && !config.getReferrerPolicy().isEmpty()) {
            response.setHeader("Referrer-Policy", config.getReferrerPolicy());
        }

        // Add Permissions-Policy
        if (config.getPermissionsPolicy().isEnabled()) {
            response.setHeader("Permissions-Policy", config.getPermissionsPolicy().build());
        }

        // Continue with request
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the connection is secure (HTTPS)
     * Considers X-Forwarded-Proto header for proxy/load balancer scenarios
     */
    private boolean isSecureConnection(HttpServletRequest request) {
        // Check if direct connection is secure
        if (request.isSecure()) {
            return true;
        }

        // Check X-Forwarded-Proto header (set by load balancers/proxies)
        String xForwardedProto = request.getHeader("X-Forwarded-Proto");
        if ("https".equalsIgnoreCase(xForwardedProto)) {
            return true;
        }

        // If requireHttps is set, always add HSTS (for development with localhost)
        if (config.isRequireHttps()) {
            return true;
        }

        return false;
    }

    /**
     * Skip security headers for certain paths (optional)
     * Currently applies to all paths, but can be customized
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Optionally skip for health checks (though headers don't hurt)
        // if (path.equals("/api/health")) {
        //     return true;
        // }

        return false;
    }
}
