package gov.nist.oscal.tools.api.filter;

import gov.nist.oscal.tools.api.model.AuditEventType;
import gov.nist.oscal.tools.api.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Request Logging Filter
 * <p>
 * Captures all HTTP requests for security monitoring and analytics.
 * Logs request details including IP, browser, user, URL, method, and response status.
 * </p>
 *
 * <h2>Captured Data</h2>
 * <ul>
 *   <li>IP Address (with X-Forwarded-For support)</li>
 *   <li>User Agent (browser identification)</li>
 *   <li>Authenticated User</li>
 *   <li>Request URL and Query String</li>
 *   <li>HTTP Method</li>
 *   <li>Response Status Code</li>
 *   <li>Processing Time</li>
 * </ul>
 */
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final AuditLogService auditLogService;

    @Value("${request.logging.enabled:true}")
    private boolean enabled;

    @Value("${request.logging.exclude-paths:/api/health,/actuator,/swagger,/v3/api-docs,/favicon.ico}")
    private String excludePathsConfig;

    private Set<String> excludePaths;

    @Autowired
    public RequestLoggingFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        // Parse exclude paths from config
        if (excludePathsConfig != null && !excludePathsConfig.isEmpty()) {
            excludePaths = Set.of(excludePathsConfig.split(","));
        } else {
            excludePaths = Set.of();
        }
        logger.info("Request logging filter initialized. Enabled: {}, Excluded paths: {}",
            enabled, excludePaths);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        // Skip if disabled
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip excluded paths
        String requestUri = request.getRequestURI();
        if (shouldExclude(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequest(request, response, duration);
        }
    }

    /**
     * Check if the request path should be excluded from logging
     */
    private boolean shouldExclude(String requestUri) {
        if (requestUri == null) return true;

        for (String excludePath : excludePaths) {
            if (requestUri.startsWith(excludePath.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Log the request details to the audit system
     */
    private void logRequest(HttpServletRequest request, HttpServletResponse response, long duration) {
        try {
            String username = getAuthenticatedUsername();
            Long userId = getAuthenticatedUserId();
            String outcome = getOutcome(response.getStatus());

            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("httpStatus", response.getStatus());
            metadata.put("processingTimeMs", duration);
            metadata.put("contentType", response.getContentType());
            metadata.put("referer", request.getHeader("Referer"));
            metadata.put("accept", request.getHeader("Accept"));

            // Add query parameters (sanitized)
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                // Don't log sensitive query params
                metadata.put("queryString", sanitizeQueryString(queryString));
            }

            // Determine event type based on the request
            AuditEventType eventType = determineEventType(request, response);

            // Build resource string (URL path)
            String resource = request.getRequestURI();
            String action = request.getMethod();

            // Log the event
            auditLogService.logEvent(eventType, username, userId, outcome, resource, action, metadata);

        } catch (Exception e) {
            logger.warn("Failed to log request: {}", e.getMessage());
        }
    }

    /**
     * Get the authenticated username from security context
     */
    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return null;
    }

    /**
     * Get the authenticated user ID from security context
     */
    private Long getAuthenticatedUserId() {
        // User ID extraction would depend on your UserDetails implementation
        // For now, return null - can be enhanced based on your security setup
        return null;
    }

    /**
     * Determine outcome based on HTTP status code
     */
    private String getOutcome(int status) {
        if (status >= 200 && status < 300) {
            return "SUCCESS";
        } else if (status >= 400 && status < 500) {
            return "FAILURE";
        } else if (status >= 500) {
            return "ERROR";
        }
        return "SUCCESS";
    }

    /**
     * Determine the audit event type based on the request
     */
    private AuditEventType determineEventType(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();

        // Authentication endpoints
        if (uri.contains("/auth/login")) {
            return status == 200 ? AuditEventType.AUTH_LOGIN_SUCCESS : AuditEventType.AUTH_LOGIN_FAILURE;
        }
        if (uri.contains("/auth/register")) {
            return status == 200 || status == 201 ? AuditEventType.AUTH_REGISTER_SUCCESS : AuditEventType.AUTH_REGISTER_FAILURE;
        }
        if (uri.contains("/auth/logout")) {
            return AuditEventType.AUTH_LOGOUT;
        }
        // Matches both /auth/service-account-token (POST, create) and
        // /auth/service-account-tokens/{id} (DELETE, revoke) — the plural path
        // contains the singular as a prefix. GET listing falls through
        // deliberately: reading your own token list is not a privileged event.
        if (uri.contains("/auth/service-account-token")) {
            if ("DELETE".equals(method)) {
                return AuditEventType.AUTH_SERVICE_TOKEN_REVOKED;
            }
            if ("POST".equals(method)) {
                return AuditEventType.AUTH_SERVICE_TOKEN_GENERATED;
            }
        }

        // File operations
        if (uri.contains("/files") || uri.contains("/upload")) {
            if ("POST".equals(method)) {
                return AuditEventType.DATA_FILE_UPLOAD;
            }
            if ("DELETE".equals(method)) {
                return AuditEventType.DATA_FILE_DELETE;
            }
            if ("GET".equals(method)) {
                return AuditEventType.DATA_FILE_ACCESS;
            }
        }

        // Validation operations
        if (uri.contains("/validate") || uri.contains("/validation")) {
            return AuditEventType.DATA_VALIDATION;
        }

        // Conversion operations
        if (uri.contains("/convert")) {
            return AuditEventType.DATA_CONVERSION;
        }

        // Profile resolution
        if (uri.contains("/resolve") || uri.contains("/profile")) {
            return AuditEventType.DATA_PROFILE_RESOLVE;
        }

        // Authorization failures
        if (status == 403) {
            return AuditEventType.AUTHZ_ACCESS_DENIED;
        }
        if (status == 401) {
            return AuditEventType.AUTHZ_ACCESS_DENIED;
        }

        // API errors
        if (status >= 500) {
            return AuditEventType.SYSTEM_ERROR;
        }

        // Generic API requests (use the new API_REQUEST types)
        if (status >= 400) {
            return AuditEventType.API_REQUEST_ERROR;
        }
        return AuditEventType.API_REQUEST;
    }

    /**
     * Sanitize query string to remove sensitive parameters
     */
    private String sanitizeQueryString(String queryString) {
        if (queryString == null) return null;

        // Remove sensitive parameters
        return queryString
            .replaceAll("(password|token|secret|key|auth)=[^&]*", "$1=***")
            .replaceAll("(Password|Token|Secret|Key|Auth)=[^&]*", "$1=***");
    }
}
