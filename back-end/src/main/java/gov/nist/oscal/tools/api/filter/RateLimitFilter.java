package gov.nist.oscal.tools.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter to apply rate limiting to HTTP requests
 *
 * Applies different rate limits based on endpoint:
 * - /api/auth/login: Strict rate limiting per IP (prevent brute force)
 * - /api/auth/register: Moderate rate limiting per IP (prevent spam)
 * - /api/*: General rate limiting per user/IP (prevent abuse)
 *
 * Returns HTTP 429 (Too Many Requests) when rate limit is exceeded
 * Includes rate limit headers in response for client awareness
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip rate limiting if disabled
        if (!rateLimitConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = request.getRequestURI();
        String ipAddress = getClientIpAddress(request);

        boolean allowed = true;
        String limitType = "api";
        long remainingRequests = 0;
        long resetTime = 0;

        // Apply endpoint-specific rate limiting
        if (requestPath.endsWith("/api/auth/login")) {
            allowed = rateLimitService.isLoginAllowed(ipAddress);
            limitType = "login";
            remainingRequests = rateLimitService.getLoginRemainingAttempts(ipAddress);
            resetTime = rateLimitService.getLoginResetTime(ipAddress);
        } else if (requestPath.endsWith("/api/auth/register")) {
            allowed = rateLimitService.isRegistrationAllowed(ipAddress);
            limitType = "registration";
            remainingRequests = rateLimitConfig.getRegistration().getAttempts();
            resetTime = rateLimitConfig.getRegistration().getDuration();
        } else if (requestPath.startsWith("/api/")) {
            // Apply general API rate limiting
            String identifier = getRequestIdentifier(request, ipAddress);
            allowed = rateLimitService.isApiRequestAllowed(identifier);
            remainingRequests = rateLimitService.getApiRemainingRequests(identifier);
            resetTime = rateLimitConfig.getApi().getDuration();
        }

        // Add rate limit headers to response
        addRateLimitHeaders(response, allowed, remainingRequests, resetTime, limitType);

        if (!allowed) {
            // Rate limit exceeded - return 429 Too Many Requests
            handleRateLimitExceeded(request, response, limitType, resetTime);
            return;
        }

        // Continue with request
        filterChain.doFilter(request, response);
    }

    /**
     * Get identifier for rate limiting
     * Uses authenticated username if available, otherwise IP address
     */
    private String getRequestIdentifier(HttpServletRequest request, String ipAddress) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            return "user:" + authentication.getName();
        }

        return "ip:" + ipAddress;
    }

    /**
     * Extract client IP address from request
     * Handles X-Forwarded-For header for proxy/load balancer scenarios
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Add rate limit headers to response
     * Headers follow industry standard conventions
     */
    private void addRateLimitHeaders(HttpServletResponse response, boolean allowed,
                                     long remaining, long resetTime, String limitType) {
        // Standard rate limit headers
        response.setHeader("X-RateLimit-Limit", getRateLimitForType(limitType));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + resetTime));

        if (!allowed) {
            response.setHeader("Retry-After", String.valueOf(resetTime));
        }
    }

    /**
     * Get the configured rate limit for a specific type
     */
    private String getRateLimitForType(String limitType) {
        switch (limitType) {
            case "login":
                return String.valueOf(rateLimitConfig.getLogin().getAttempts());
            case "registration":
                return String.valueOf(rateLimitConfig.getRegistration().getAttempts());
            case "api":
            default:
                return String.valueOf(rateLimitConfig.getApi().getRequests());
        }
    }

    /**
     * Handle rate limit exceeded response
     * Returns HTTP 429 with JSON error message
     */
    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response,
                                        String limitType, long resetTime) throws IOException {
        logger.warn("Rate limit exceeded for {} from IP: {} - Path: {}",
                limitType,
                getClientIpAddress(request),
                request.getRequestURI());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", getRateLimitMessage(limitType));
        errorResponse.put("retryAfter", resetTime);
        errorResponse.put("timestamp", System.currentTimeMillis());

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }

    /**
     * Get user-friendly rate limit error message
     */
    private String getRateLimitMessage(String limitType) {
        switch (limitType) {
            case "login":
                return String.format("Too many login attempts. Please try again in %d seconds.",
                        rateLimitConfig.getLogin().getDuration());
            case "registration":
                return String.format("Too many registration attempts. Please try again later.");
            case "api":
            default:
                return "API rate limit exceeded. Please slow down your requests.";
        }
    }

    /**
     * Skip rate limiting for certain paths (health checks, static resources, etc.)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Don't rate limit health checks
        if (path.equals("/api/health")) {
            return true;
        }

        // Don't rate limit Swagger UI
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            return true;
        }

        // Don't rate limit static resources
        if (path.startsWith("/static/") || path.startsWith("/public/")) {
            return true;
        }

        return false;
    }
}
