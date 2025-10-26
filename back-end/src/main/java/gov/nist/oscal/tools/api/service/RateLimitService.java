package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.config.RateLimitConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing rate limiting using token bucket algorithm
 *
 * Uses Bucket4j for rate limiting implementation and Caffeine for in-memory cache
 *
 * Rate limits are applied per:
 * - IP address for unauthenticated endpoints (login, registration)
 * - User ID for authenticated API endpoints
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    @Autowired
    private RateLimitConfig rateLimitConfig;

    // Cache for login rate limits (key: IP address)
    private final Cache<String, Bucket> loginBucketCache = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    // Cache for registration rate limits (key: IP address)
    private final Cache<String, Bucket> registrationBucketCache = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    // Cache for general API rate limits (key: username or IP)
    private final Cache<String, Bucket> apiBucketCache = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(50_000)
            .build();

    /**
     * Check if a login attempt is allowed for the given IP address
     *
     * @param ipAddress The IP address making the request
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isLoginAllowed(String ipAddress) {
        if (!rateLimitConfig.isEnabled()) {
            return true;
        }

        Bucket bucket = loginBucketCache.get(ipAddress, key -> createLoginBucket());
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            logger.warn("Rate limit exceeded for login from IP: {}", ipAddress);
        }

        return allowed;
    }

    /**
     * Check if a registration attempt is allowed for the given IP address
     *
     * @param ipAddress The IP address making the request
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isRegistrationAllowed(String ipAddress) {
        if (!rateLimitConfig.isEnabled()) {
            return true;
        }

        Bucket bucket = registrationBucketCache.get(ipAddress, key -> createRegistrationBucket());
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            logger.warn("Rate limit exceeded for registration from IP: {}", ipAddress);
        }

        return allowed;
    }

    /**
     * Check if an API request is allowed for the given user/IP
     *
     * @param identifier User ID or IP address
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isApiRequestAllowed(String identifier) {
        if (!rateLimitConfig.isEnabled()) {
            return true;
        }

        Bucket bucket = apiBucketCache.get(identifier, key -> createApiBucket());
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            logger.warn("Rate limit exceeded for API requests from: {}", identifier);
        }

        return allowed;
    }

    /**
     * Get remaining attempts for login from IP address
     *
     * @param ipAddress The IP address
     * @return Number of remaining attempts
     */
    public long getLoginRemainingAttempts(String ipAddress) {
        if (!rateLimitConfig.isEnabled()) {
            return Long.MAX_VALUE;
        }

        Bucket bucket = loginBucketCache.getIfPresent(ipAddress);
        return bucket != null ? bucket.getAvailableTokens() : rateLimitConfig.getLogin().getAttempts();
    }

    /**
     * Get time in seconds until login rate limit resets for IP address
     *
     * @param ipAddress The IP address
     * @return Seconds until reset, or 0 if not rate limited
     */
    public long getLoginResetTime(String ipAddress) {
        if (!rateLimitConfig.isEnabled()) {
            return 0;
        }

        Bucket bucket = loginBucketCache.getIfPresent(ipAddress);
        if (bucket != null && bucket.getAvailableTokens() == 0) {
            return rateLimitConfig.getLogin().getDuration();
        }
        return 0;
    }

    /**
     * Get remaining attempts for API requests
     *
     * @param identifier User ID or IP address
     * @return Number of remaining requests
     */
    public long getApiRemainingRequests(String identifier) {
        if (!rateLimitConfig.isEnabled()) {
            return Long.MAX_VALUE;
        }

        Bucket bucket = apiBucketCache.getIfPresent(identifier);
        return bucket != null ? bucket.getAvailableTokens() : rateLimitConfig.getApi().getRequests();
    }

    /**
     * Create a bucket for login rate limiting
     */
    private Bucket createLoginBucket() {
        int capacity = rateLimitConfig.getLogin().getAttempts();
        int refillDuration = rateLimitConfig.getLogin().getDuration();

        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(capacity, Duration.ofSeconds(refillDuration))
        );

        BucketConfiguration config = BucketConfiguration.builder()
                .addLimit(limit)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create a bucket for registration rate limiting
     */
    private Bucket createRegistrationBucket() {
        int capacity = rateLimitConfig.getRegistration().getAttempts();
        int refillDuration = rateLimitConfig.getRegistration().getDuration();

        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(capacity, Duration.ofSeconds(refillDuration))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create a bucket for API rate limiting
     */
    private Bucket createApiBucket() {
        int capacity = rateLimitConfig.getApi().getRequests();
        int refillDuration = rateLimitConfig.getApi().getDuration();

        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(capacity, Duration.ofSeconds(refillDuration))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Clear all rate limit caches (useful for testing)
     */
    public void clearAllCaches() {
        loginBucketCache.invalidateAll();
        registrationBucketCache.invalidateAll();
        apiBucketCache.invalidateAll();
        logger.info("All rate limit caches cleared");
    }

    /**
     * Clear rate limit for specific IP address (useful for admin operations)
     *
     * @param ipAddress The IP address to clear
     */
    public void clearRateLimitForIp(String ipAddress) {
        loginBucketCache.invalidate(ipAddress);
        registrationBucketCache.invalidate(ipAddress);
        apiBucketCache.invalidate(ipAddress);
        logger.info("Rate limit cleared for IP: {}", ipAddress);
    }
}
