package gov.nist.oscal.tools.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Account Security Configuration
 * <p>
 * Provides configuration for password complexity requirements, account lockout policies,
 * and login attempt tracking. These settings help protect against:
 * - Weak password attacks
 * - Brute force login attempts
 * - Account enumeration
 * - Credential stuffing attacks
 * </p>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li>Password Complexity: Minimum length, character requirements</li>
 *   <li>Account Lockout: Temporary lockout after failed login attempts</li>
 *   <li>Login Tracking: Track attempts by IP address and username</li>
 *   <li>Password Expiration: Force password changes after specified days</li>
 *   <li>Password History: Prevent password reuse</li>
 * </ul>
 *
 * @see gov.nist.oscal.tools.api.service.PasswordValidationService
 * @see gov.nist.oscal.tools.api.service.LoginAttemptService
 */
@Configuration
@ConfigurationProperties(prefix = "account.security")
public class AccountSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(AccountSecurityConfig.class);

    // ========================================
    // Password Complexity Settings
    // ========================================

    /**
     * Minimum password length (NIST recommends 8, OWASP recommends 10)
     */
    private int passwordMinLength = 10;

    /**
     * Maximum password length (prevent DoS via extremely long passwords)
     */
    private int passwordMaxLength = 128;

    /**
     * Require at least one uppercase letter (A-Z)
     */
    private boolean passwordRequireUppercase = true;

    /**
     * Require at least one lowercase letter (a-z)
     */
    private boolean passwordRequireLowercase = true;

    /**
     * Require at least one digit (0-9)
     */
    private boolean passwordRequireDigit = true;

    /**
     * Require at least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)
     */
    private boolean passwordRequireSpecial = true;

    /**
     * Check password against common password list (top 10,000 common passwords)
     */
    private boolean passwordCheckCommonPasswords = true;

    /**
     * Prevent username in password
     */
    private boolean passwordPreventUsernameInPassword = true;

    // ========================================
    // Account Lockout Settings
    // ========================================

    /**
     * Enable account lockout after failed login attempts
     */
    private boolean lockoutEnabled = true;

    /**
     * Number of failed login attempts before account lockout
     */
    private int lockoutMaxAttempts = 5;

    /**
     * Lockout duration in seconds (default: 15 minutes)
     */
    private long lockoutDurationSeconds = 900;

    /**
     * Time window for failed attempts in seconds (default: 10 minutes)
     * Failed attempts outside this window are not counted
     */
    private long lockoutWindowSeconds = 600;

    /**
     * Track login attempts by IP address (in addition to username)
     */
    private boolean trackLoginAttemptsByIp = true;

    /**
     * Maximum failed attempts from same IP address before lockout
     */
    private int ipLockoutMaxAttempts = 10;

    // ========================================
    // Password Expiration Settings
    // ========================================

    /**
     * Enable password expiration (force password change after X days)
     */
    private boolean passwordExpirationEnabled = false;

    /**
     * Password expiration days (default: 90 days)
     */
    private int passwordExpirationDays = 90;

    /**
     * Days before expiration to warn user
     */
    private int passwordExpirationWarningDays = 14;

    // ========================================
    // Password History Settings
    // ========================================

    /**
     * Enable password history (prevent password reuse)
     */
    private boolean passwordHistoryEnabled = false;

    /**
     * Number of previous passwords to remember
     */
    private int passwordHistoryCount = 5;

    // ========================================
    // Login Tracking Settings
    // ========================================

    /**
     * Track successful login attempts
     */
    private boolean trackSuccessfulLogins = true;

    /**
     * Track failed login attempts
     */
    private boolean trackFailedLogins = true;

    /**
     * Maximum number of login history entries to keep per user
     */
    private int maxLoginHistoryEntries = 100;

    /**
     * Enable email notifications for suspicious login activity
     */
    private boolean emailNotificationsEnabled = false;

    /**
     * Email notification on account lockout
     */
    private boolean emailOnAccountLockout = false;

    /**
     * Email notification on password change
     */
    private boolean emailOnPasswordChange = false;

    // ========================================
    // Validation
    // ========================================

    @PostConstruct
    public void validateConfiguration() {
        logger.info("Initializing Account Security Configuration...");

        if (passwordMinLength < 8) {
            logger.warn("Password minimum length is less than 8 characters. NIST recommends at least 8.");
        }

        if (passwordMinLength > passwordMaxLength) {
            throw new IllegalStateException(
                "Password minimum length (" + passwordMinLength + ") cannot be greater than maximum length (" + passwordMaxLength + ")"
            );
        }

        if (lockoutEnabled && lockoutMaxAttempts < 1) {
            throw new IllegalStateException(
                "Lockout max attempts must be at least 1 when lockout is enabled"
            );
        }

        if (lockoutEnabled && lockoutDurationSeconds < 60) {
            logger.warn("Account lockout duration is less than 60 seconds. This may not be effective against brute force attacks.");
        }

        if (passwordExpirationEnabled && passwordExpirationDays < 1) {
            throw new IllegalStateException(
                "Password expiration days must be at least 1 when expiration is enabled"
            );
        }

        if (passwordHistoryEnabled && passwordHistoryCount < 1) {
            throw new IllegalStateException(
                "Password history count must be at least 1 when history is enabled"
            );
        }

        logger.info("Account Security Configuration validated successfully:");
        logger.info("  - Password min length: {}", passwordMinLength);
        logger.info("  - Password complexity: uppercase={}, lowercase={}, digit={}, special={}",
            passwordRequireUppercase, passwordRequireLowercase, passwordRequireDigit, passwordRequireSpecial);
        logger.info("  - Account lockout: enabled={}, max_attempts={}, duration={}s",
            lockoutEnabled, lockoutMaxAttempts, lockoutDurationSeconds);
        logger.info("  - Password expiration: enabled={}, days={}",
            passwordExpirationEnabled, passwordExpirationDays);
        logger.info("  - Password history: enabled={}, count={}",
            passwordHistoryEnabled, passwordHistoryCount);
    }

    // ========================================
    // Getters and Setters
    // ========================================

    public int getPasswordMinLength() {
        return passwordMinLength;
    }

    public void setPasswordMinLength(int passwordMinLength) {
        this.passwordMinLength = passwordMinLength;
    }

    public int getPasswordMaxLength() {
        return passwordMaxLength;
    }

    public void setPasswordMaxLength(int passwordMaxLength) {
        this.passwordMaxLength = passwordMaxLength;
    }

    public boolean isPasswordRequireUppercase() {
        return passwordRequireUppercase;
    }

    public void setPasswordRequireUppercase(boolean passwordRequireUppercase) {
        this.passwordRequireUppercase = passwordRequireUppercase;
    }

    public boolean isPasswordRequireLowercase() {
        return passwordRequireLowercase;
    }

    public void setPasswordRequireLowercase(boolean passwordRequireLowercase) {
        this.passwordRequireLowercase = passwordRequireLowercase;
    }

    public boolean isPasswordRequireDigit() {
        return passwordRequireDigit;
    }

    public void setPasswordRequireDigit(boolean passwordRequireDigit) {
        this.passwordRequireDigit = passwordRequireDigit;
    }

    public boolean isPasswordRequireSpecial() {
        return passwordRequireSpecial;
    }

    public void setPasswordRequireSpecial(boolean passwordRequireSpecial) {
        this.passwordRequireSpecial = passwordRequireSpecial;
    }

    public boolean isPasswordCheckCommonPasswords() {
        return passwordCheckCommonPasswords;
    }

    public void setPasswordCheckCommonPasswords(boolean passwordCheckCommonPasswords) {
        this.passwordCheckCommonPasswords = passwordCheckCommonPasswords;
    }

    public boolean isPasswordPreventUsernameInPassword() {
        return passwordPreventUsernameInPassword;
    }

    public void setPasswordPreventUsernameInPassword(boolean passwordPreventUsernameInPassword) {
        this.passwordPreventUsernameInPassword = passwordPreventUsernameInPassword;
    }

    public boolean isLockoutEnabled() {
        return lockoutEnabled;
    }

    public void setLockoutEnabled(boolean lockoutEnabled) {
        this.lockoutEnabled = lockoutEnabled;
    }

    public int getLockoutMaxAttempts() {
        return lockoutMaxAttempts;
    }

    public void setLockoutMaxAttempts(int lockoutMaxAttempts) {
        this.lockoutMaxAttempts = lockoutMaxAttempts;
    }

    public long getLockoutDurationSeconds() {
        return lockoutDurationSeconds;
    }

    public void setLockoutDurationSeconds(long lockoutDurationSeconds) {
        this.lockoutDurationSeconds = lockoutDurationSeconds;
    }

    public long getLockoutWindowSeconds() {
        return lockoutWindowSeconds;
    }

    public void setLockoutWindowSeconds(long lockoutWindowSeconds) {
        this.lockoutWindowSeconds = lockoutWindowSeconds;
    }

    public boolean isTrackLoginAttemptsByIp() {
        return trackLoginAttemptsByIp;
    }

    public void setTrackLoginAttemptsByIp(boolean trackLoginAttemptsByIp) {
        this.trackLoginAttemptsByIp = trackLoginAttemptsByIp;
    }

    public int getIpLockoutMaxAttempts() {
        return ipLockoutMaxAttempts;
    }

    public void setIpLockoutMaxAttempts(int ipLockoutMaxAttempts) {
        this.ipLockoutMaxAttempts = ipLockoutMaxAttempts;
    }

    public boolean isPasswordExpirationEnabled() {
        return passwordExpirationEnabled;
    }

    public void setPasswordExpirationEnabled(boolean passwordExpirationEnabled) {
        this.passwordExpirationEnabled = passwordExpirationEnabled;
    }

    public int getPasswordExpirationDays() {
        return passwordExpirationDays;
    }

    public void setPasswordExpirationDays(int passwordExpirationDays) {
        this.passwordExpirationDays = passwordExpirationDays;
    }

    public int getPasswordExpirationWarningDays() {
        return passwordExpirationWarningDays;
    }

    public void setPasswordExpirationWarningDays(int passwordExpirationWarningDays) {
        this.passwordExpirationWarningDays = passwordExpirationWarningDays;
    }

    public boolean isPasswordHistoryEnabled() {
        return passwordHistoryEnabled;
    }

    public void setPasswordHistoryEnabled(boolean passwordHistoryEnabled) {
        this.passwordHistoryEnabled = passwordHistoryEnabled;
    }

    public int getPasswordHistoryCount() {
        return passwordHistoryCount;
    }

    public void setPasswordHistoryCount(int passwordHistoryCount) {
        this.passwordHistoryCount = passwordHistoryCount;
    }

    public boolean isTrackSuccessfulLogins() {
        return trackSuccessfulLogins;
    }

    public void setTrackSuccessfulLogins(boolean trackSuccessfulLogins) {
        this.trackSuccessfulLogins = trackSuccessfulLogins;
    }

    public boolean isTrackFailedLogins() {
        return trackFailedLogins;
    }

    public void setTrackFailedLogins(boolean trackFailedLogins) {
        this.trackFailedLogins = trackFailedLogins;
    }

    public int getMaxLoginHistoryEntries() {
        return maxLoginHistoryEntries;
    }

    public void setMaxLoginHistoryEntries(int maxLoginHistoryEntries) {
        this.maxLoginHistoryEntries = maxLoginHistoryEntries;
    }

    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public boolean isEmailOnAccountLockout() {
        return emailOnAccountLockout;
    }

    public void setEmailOnAccountLockout(boolean emailOnAccountLockout) {
        this.emailOnAccountLockout = emailOnAccountLockout;
    }

    public boolean isEmailOnPasswordChange() {
        return emailOnPasswordChange;
    }

    public void setEmailOnPasswordChange(boolean emailOnPasswordChange) {
        this.emailOnPasswordChange = emailOnPasswordChange;
    }
}
