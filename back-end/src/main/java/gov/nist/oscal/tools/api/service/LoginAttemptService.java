package gov.nist.oscal.tools.api.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import gov.nist.oscal.tools.api.config.AccountSecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Login Attempt Tracking Service
 * <p>
 * Tracks failed login attempts and implements account lockout to protect against:
 * - Brute force attacks
 * - Credential stuffing
 * - Password spraying
 * - Account enumeration
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Track failed login attempts by username and IP address</li>
 *   <li>Automatic account lockout after configurable failed attempts</li>
 *   <li>Time-based lockout with automatic unlock</li>
 *   <li>Sliding window for attempt counting</li>
 *   <li>IP-based rate limiting</li>
 * </ul>
 *
 * <h2>Implementation Details</h2>
 * <p>
 * Uses Caffeine cache for in-memory tracking with automatic expiration.
 * In a distributed system, consider using Redis or a database for
 * cross-instance tracking.
 * </p>
 *
 * @see AccountSecurityConfig
 */
@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

    private final AccountSecurityConfig config;

    /**
     * Cache for tracking failed login attempts by username
     * Key: username, Value: List of attempt timestamps
     */
    private final Cache<String, List<LocalDateTime>> usernameAttemptsCache;

    /**
     * Cache for tracking failed login attempts by IP address
     * Key: IP address, Value: List of attempt timestamps
     */
    private final Cache<String, List<LocalDateTime>> ipAttemptsCache;

    /**
     * Cache for locked accounts
     * Key: username, Value: Lockout expiration time
     */
    private final Cache<String, LocalDateTime> accountLockoutCache;

    /**
     * Cache for locked IP addresses
     * Key: IP address, Value: Lockout expiration time
     */
    private final Cache<String, LocalDateTime> ipLockoutCache;

    @Autowired
    public LoginAttemptService(AccountSecurityConfig config) {
        this.config = config;

        // Initialize caches with expiration based on config
        long cacheExpiration = config.getLockoutWindowSeconds() + config.getLockoutDurationSeconds();

        this.usernameAttemptsCache = Caffeine.newBuilder()
            .expireAfterWrite(cacheExpiration, TimeUnit.SECONDS)
            .maximumSize(10000)
            .build();

        this.ipAttemptsCache = Caffeine.newBuilder()
            .expireAfterWrite(cacheExpiration, TimeUnit.SECONDS)
            .maximumSize(10000)
            .build();

        this.accountLockoutCache = Caffeine.newBuilder()
            .expireAfterWrite(config.getLockoutDurationSeconds(), TimeUnit.SECONDS)
            .maximumSize(10000)
            .build();

        this.ipLockoutCache = Caffeine.newBuilder()
            .expireAfterWrite(config.getLockoutDurationSeconds(), TimeUnit.SECONDS)
            .maximumSize(10000)
            .build();

        logger.info("LoginAttemptService initialized with lockout config: {} attempts in {} seconds, lockout for {} seconds",
            config.getLockoutMaxAttempts(), config.getLockoutWindowSeconds(), config.getLockoutDurationSeconds());
    }

    /**
     * Record a failed login attempt
     *
     * @param username Username that failed to login
     * @param ipAddress IP address of the login attempt
     */
    public void recordFailedLogin(String username, String ipAddress) {
        if (!config.isLockoutEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // Record failed attempt for username
        if (username != null && !username.isEmpty()) {
            List<LocalDateTime> attempts = usernameAttemptsCache.getIfPresent(username);
            if (attempts == null) {
                attempts = new ArrayList<>();
            }
            attempts.add(now);
            usernameAttemptsCache.put(username, attempts);

            // Check if account should be locked
            if (shouldLockAccount(username)) {
                lockAccount(username);
            }

            logger.info("Recorded failed login attempt for username: {} (total in window: {})",
                username, getRecentAttemptCount(attempts));
        }

        // Record failed attempt for IP address
        if (config.isTrackLoginAttemptsByIp() && ipAddress != null && !ipAddress.isEmpty()) {
            List<LocalDateTime> ipAttempts = ipAttemptsCache.getIfPresent(ipAddress);
            if (ipAttempts == null) {
                ipAttempts = new ArrayList<>();
            }
            ipAttempts.add(now);
            ipAttemptsCache.put(ipAddress, ipAttempts);

            // Check if IP should be locked
            if (shouldLockIp(ipAddress)) {
                lockIpAddress(ipAddress);
            }

            logger.info("Recorded failed login attempt from IP: {} (total in window: {})",
                ipAddress, getRecentAttemptCount(ipAttempts));
        }
    }

    /**
     * Record a successful login (clears failed attempts)
     *
     * @param username Username that successfully logged in
     * @param ipAddress IP address of the successful login
     */
    public void recordSuccessfulLogin(String username, String ipAddress) {
        if (!config.isLockoutEnabled()) {
            return;
        }

        // Clear failed attempts for username
        if (username != null && !username.isEmpty()) {
            usernameAttemptsCache.invalidate(username);
            logger.debug("Cleared failed login attempts for username: {}", username);
        }

        // Clear failed attempts for IP address
        if (config.isTrackLoginAttemptsByIp() && ipAddress != null && !ipAddress.isEmpty()) {
            ipAttemptsCache.invalidate(ipAddress);
            logger.debug("Cleared failed login attempts for IP: {}", ipAddress);
        }
    }

    /**
     * Check if an account is currently locked
     *
     * @param username Username to check
     * @return true if account is locked
     */
    public boolean isAccountLocked(String username) {
        if (!config.isLockoutEnabled() || username == null || username.isEmpty()) {
            return false;
        }

        LocalDateTime lockoutExpiration = accountLockoutCache.getIfPresent(username);
        if (lockoutExpiration == null) {
            return false;
        }

        // Check if lockout has expired
        if (LocalDateTime.now().isAfter(lockoutExpiration)) {
            accountLockoutCache.invalidate(username);
            logger.info("Account lockout expired for username: {}", username);
            return false;
        }

        return true;
    }

    /**
     * Check if an IP address is currently locked
     *
     * @param ipAddress IP address to check
     * @return true if IP is locked
     */
    public boolean isIpLocked(String ipAddress) {
        if (!config.isLockoutEnabled() || !config.isTrackLoginAttemptsByIp() ||
            ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }

        LocalDateTime lockoutExpiration = ipLockoutCache.getIfPresent(ipAddress);
        if (lockoutExpiration == null) {
            return false;
        }

        // Check if lockout has expired
        if (LocalDateTime.now().isAfter(lockoutExpiration)) {
            ipLockoutCache.invalidate(ipAddress);
            logger.info("IP lockout expired for address: {}", ipAddress);
            return false;
        }

        return true;
    }

    /**
     * Get remaining lockout time in seconds
     *
     * @param username Username to check
     * @return Remaining lockout time in seconds, or 0 if not locked
     */
    public long getRemainingLockoutTime(String username) {
        if (!isAccountLocked(username)) {
            return 0;
        }

        LocalDateTime lockoutExpiration = accountLockoutCache.getIfPresent(username);
        if (lockoutExpiration == null) {
            return 0;
        }

        return ChronoUnit.SECONDS.between(LocalDateTime.now(), lockoutExpiration);
    }

    /**
     * Get number of remaining login attempts before lockout
     *
     * @param username Username to check
     * @return Number of remaining attempts, or -1 if lockout disabled
     */
    public int getRemainingAttempts(String username) {
        if (!config.isLockoutEnabled() || username == null || username.isEmpty()) {
            return -1;
        }

        List<LocalDateTime> attempts = usernameAttemptsCache.getIfPresent(username);
        if (attempts == null) {
            return config.getLockoutMaxAttempts();
        }

        int recentAttempts = getRecentAttemptCount(attempts);
        return Math.max(0, config.getLockoutMaxAttempts() - recentAttempts);
    }

    /**
     * Manually unlock an account (for admin use)
     *
     * @param username Username to unlock
     */
    public void unlockAccount(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }

        accountLockoutCache.invalidate(username);
        usernameAttemptsCache.invalidate(username);
        logger.info("Manually unlocked account for username: {}", username);
    }

    /**
     * Manually unlock an IP address (for admin use)
     *
     * @param ipAddress IP address to unlock
     */
    public void unlockIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return;
        }

        ipLockoutCache.invalidate(ipAddress);
        ipAttemptsCache.invalidate(ipAddress);
        logger.info("Manually unlocked IP address: {}", ipAddress);
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * Check if account should be locked based on failed attempts
     *
     * @param username Username to check
     * @return true if account should be locked
     */
    private boolean shouldLockAccount(String username) {
        List<LocalDateTime> attempts = usernameAttemptsCache.getIfPresent(username);
        if (attempts == null) {
            return false;
        }

        int recentAttempts = getRecentAttemptCount(attempts);
        return recentAttempts >= config.getLockoutMaxAttempts();
    }

    /**
     * Check if IP should be locked based on failed attempts
     *
     * @param ipAddress IP address to check
     * @return true if IP should be locked
     */
    private boolean shouldLockIp(String ipAddress) {
        List<LocalDateTime> attempts = ipAttemptsCache.getIfPresent(ipAddress);
        if (attempts == null) {
            return false;
        }

        int recentAttempts = getRecentAttemptCount(attempts);
        return recentAttempts >= config.getIpLockoutMaxAttempts();
    }

    /**
     * Lock an account for the configured duration
     *
     * @param username Username to lock
     */
    private void lockAccount(String username) {
        LocalDateTime lockoutExpiration = LocalDateTime.now()
            .plusSeconds(config.getLockoutDurationSeconds());

        accountLockoutCache.put(username, lockoutExpiration);

        logger.warn("SECURITY: Account locked for username: {} until {}",
            username, lockoutExpiration);
    }

    /**
     * Lock an IP address for the configured duration
     *
     * @param ipAddress IP address to lock
     */
    private void lockIpAddress(String ipAddress) {
        LocalDateTime lockoutExpiration = LocalDateTime.now()
            .plusSeconds(config.getLockoutDurationSeconds());

        ipLockoutCache.put(ipAddress, lockoutExpiration);

        logger.warn("SECURITY: IP address locked: {} until {}",
            ipAddress, lockoutExpiration);
    }

    /**
     * Count recent attempts within the configured time window
     *
     * @param attempts List of all attempt timestamps
     * @return Number of attempts within the time window
     */
    private int getRecentAttemptCount(List<LocalDateTime> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return 0;
        }

        LocalDateTime windowStart = LocalDateTime.now()
            .minusSeconds(config.getLockoutWindowSeconds());

        return (int) attempts.stream()
            .filter(attemptTime -> attemptTime.isAfter(windowStart))
            .count();
    }

    /**
     * Get cache statistics (for monitoring/debugging)
     *
     * @return String representation of cache statistics
     */
    public String getCacheStatistics() {
        return String.format(
            "LoginAttemptService Cache Stats - " +
            "Username Attempts: %d, IP Attempts: %d, Locked Accounts: %d, Locked IPs: %d",
            usernameAttemptsCache.estimatedSize(),
            ipAttemptsCache.estimatedSize(),
            accountLockoutCache.estimatedSize(),
            ipLockoutCache.estimatedSize()
        );
    }
}
