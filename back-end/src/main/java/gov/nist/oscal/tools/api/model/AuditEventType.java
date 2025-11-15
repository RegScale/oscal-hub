package gov.nist.oscal.tools.api.model;

/**
 * Audit Event Type Enumeration
 * <p>
 * Defines categories of security and operational events that are logged
 * for audit trails, compliance, and security monitoring.
 * </p>
 *
 * <h2>Event Categories</h2>
 * <ul>
 *   <li><b>Authentication Events</b>: User login, logout, registration</li>
 *   <li><b>Authorization Events</b>: Access denied, permission changes</li>
 *   <li><b>Data Access Events</b>: File read, write, delete operations</li>
 *   <li><b>Configuration Events</b>: Settings changes, profile updates</li>
 *   <li><b>Security Events</b>: Account lockout, password changes, suspicious activity</li>
 *   <li><b>System Events</b>: Application startup, shutdown, errors</li>
 * </ul>
 *
 * <h2>Compliance Mapping</h2>
 * <ul>
 *   <li>SOC 2 Type II: CC6.2, CC6.3, CC7.2</li>
 *   <li>HIPAA: §164.312(b) - Audit Controls</li>
 *   <li>PCI-DSS: Requirement 10 - Track and monitor all access</li>
 *   <li>GDPR: Article 30 - Records of processing activities</li>
 *   <li>ISO 27001: A.12.4.1 - Event logging</li>
 * </ul>
 *
 * @see gov.nist.oscal.tools.api.entity.AuditEvent
 * @see gov.nist.oscal.tools.api.service.AuditLogService
 */
public enum AuditEventType {

    // ========================================
    // Authentication Events (AUTH_*)
    // ========================================

    /**
     * User successfully registered a new account
     * <p>Risk Level: LOW</p>
     * <p>Retention: LONG (compliance)</p>
     */
    AUTH_REGISTER_SUCCESS("Authentication", "User registration successful", "LOW"),

    /**
     * User registration failed (validation error, duplicate username, etc.)
     * <p>Risk Level: LOW</p>
     * <p>Retention: MEDIUM</p>
     */
    AUTH_REGISTER_FAILURE("Authentication", "User registration failed", "LOW"),

    /**
     * User successfully authenticated (login)
     * <p>Risk Level: LOW</p>
     * <p>Retention: LONG (compliance)</p>
     */
    AUTH_LOGIN_SUCCESS("Authentication", "User login successful", "LOW"),

    /**
     * User login failed (invalid credentials)
     * <p>Risk Level: MEDIUM (potential attack)</p>
     * <p>Retention: LONG (security monitoring)</p>
     */
    AUTH_LOGIN_FAILURE("Authentication", "User login failed", "MEDIUM"),

    /**
     * User logged out
     * <p>Risk Level: LOW</p>
     * <p>Retention: SHORT</p>
     */
    AUTH_LOGOUT("Authentication", "User logout", "LOW"),

    /**
     * JWT token refreshed
     * <p>Risk Level: LOW</p>
     * <p>Retention: SHORT</p>
     */
    AUTH_TOKEN_REFRESH("Authentication", "Token refreshed", "LOW"),

    /**
     * Service account token generated
     * <p>Risk Level: MEDIUM (privileged operation)</p>
     * <p>Retention: LONG (security monitoring)</p>
     */
    AUTH_SERVICE_TOKEN_GENERATED("Authentication", "Service account token generated", "MEDIUM"),

    /**
     * User selected organization after initial authentication
     * <p>Risk Level: LOW</p>
     * <p>Retention: MEDIUM (audit trail)</p>
     */
    AUTH_ORG_SELECTION("Authentication", "Organization selected", "LOW"),

    // ========================================
    // Authorization Events (AUTHZ_*)
    // ========================================

    /**
     * Access denied due to insufficient permissions
     * <p>Risk Level: MEDIUM (potential unauthorized access attempt)</p>
     * <p>Retention: LONG (security monitoring)</p>
     */
    AUTHZ_ACCESS_DENIED("Authorization", "Access denied", "MEDIUM"),

    /**
     * Access granted to protected resource
     * <p>Risk Level: LOW</p>
     * <p>Retention: MEDIUM</p>
     */
    AUTHZ_ACCESS_GRANTED("Authorization", "Access granted", "LOW"),

    /**
     * User role or permissions changed
     * <p>Risk Level: HIGH (privilege escalation)</p>
     * <p>Retention: LONG (compliance)</p>
     */
    AUTHZ_PERMISSION_CHANGED("Authorization", "User permissions changed", "HIGH"),

    // ========================================
    // Data Access Events (DATA_*)
    // ========================================

    /**
     * OSCAL file uploaded
     * <p>Risk Level: MEDIUM (data modification)</p>
     * <p>Retention: LONG (compliance, data lineage)</p>
     */
    DATA_FILE_UPLOAD("Data Access", "File uploaded", "MEDIUM"),

    /**
     * OSCAL file downloaded/accessed
     * <p>Risk Level: LOW</p>
     * <p>Retention: MEDIUM (data access tracking)</p>
     */
    DATA_FILE_ACCESS("Data Access", "File accessed", "LOW"),

    /**
     * OSCAL file deleted
     * <p>Risk Level: HIGH (data loss)</p>
     * <p>Retention: LONG (compliance, forensics)</p>
     */
    DATA_FILE_DELETE("Data Access", "File deleted", "HIGH"),

    /**
     * OSCAL file modified/updated
     * <p>Risk Level: MEDIUM (data modification)</p>
     * <p>Retention: LONG (data lineage)</p>
     */
    DATA_FILE_MODIFY("Data Access", "File modified", "MEDIUM"),

    /**
     * OSCAL document validated
     * <p>Risk Level: LOW</p>
     * <p>Retention: SHORT</p>
     */
    DATA_VALIDATION("Data Access", "Document validated", "LOW"),

    /**
     * OSCAL document converted between formats
     * <p>Risk Level: LOW</p>
     * <p>Retention: SHORT</p>
     */
    DATA_CONVERSION("Data Access", "Document converted", "LOW"),

    /**
     * Profile resolution performed
     * <p>Risk Level: LOW</p>
     * <p>Retention: SHORT</p>
     */
    DATA_PROFILE_RESOLVE("Data Access", "Profile resolved", "LOW"),

    // ========================================
    // Configuration Events (CONFIG_*)
    // ========================================

    /**
     * User profile updated (email, address, etc.)
     * <p>Risk Level: LOW</p>
     * <p>Retention: MEDIUM (audit trail)</p>
     */
    CONFIG_PROFILE_UPDATE("Configuration", "User profile updated", "LOW"),

    /**
     * User password changed
     * <p>Risk Level: MEDIUM (security event)</p>
     * <p>Retention: LONG (security monitoring)</p>
     */
    CONFIG_PASSWORD_CHANGE("Configuration", "Password changed", "MEDIUM"),

    /**
     * User logo/avatar uploaded
     * <p>Risk Level: LOW</p>
     * <p>Retention: SHORT</p>
     */
    CONFIG_LOGO_UPLOAD("Configuration", "Logo uploaded", "LOW"),

    /**
     * System configuration changed
     * <p>Risk Level: HIGH (system-wide impact)</p>
     * <p>Retention: LONG (compliance)</p>
     */
    CONFIG_SYSTEM_CHANGE("Configuration", "System configuration changed", "HIGH"),

    // ========================================
    // Security Events (SECURITY_*)
    // ========================================

    /**
     * Account locked due to failed login attempts
     * <p>Risk Level: HIGH (potential attack)</p>
     * <p>Retention: LONG (security monitoring)</p>
     */
    SECURITY_ACCOUNT_LOCKED("Security", "Account locked", "HIGH"),

    /**
     * Account unlocked (automatic expiration or manual)
     * <p>Risk Level: MEDIUM</p>
     * <p>Retention: LONG (security monitoring)</p>
     */
    SECURITY_ACCOUNT_UNLOCKED("Security", "Account unlocked", "MEDIUM"),

    /**
     * IP address blocked due to excessive failed attempts
     * <p>Risk Level: HIGH (potential attack)</p>
     * <p>Retention: LONG (security monitoring)</p>
     */
    SECURITY_IP_BLOCKED("Security", "IP address blocked", "HIGH"),

    /**
     * Password reset requested
     * <p>Risk Level: MEDIUM (account takeover attempt?)</p>
     * <p>Retention: LONG (security monitoring)</p>
     */
    SECURITY_PASSWORD_RESET_REQUEST("Security", "Password reset requested", "MEDIUM"),

    /**
     * Password reset completed
     * <p>Risk Level: MEDIUM</p>
     * <p>Retention: LONG (security monitoring)</p>
     */
    SECURITY_PASSWORD_RESET_COMPLETE("Security", "Password reset completed", "MEDIUM"),

    /**
     * Suspicious activity detected (anomaly detection)
     * <p>Risk Level: HIGH</p>
     * <p>Retention: LONG (security monitoring, forensics)</p>
     */
    SECURITY_SUSPICIOUS_ACTIVITY("Security", "Suspicious activity detected", "HIGH"),

    /**
     * Rate limit exceeded (API abuse)
     * <p>Risk Level: MEDIUM (potential DoS)</p>
     * <p>Retention: MEDIUM</p>
     */
    SECURITY_RATE_LIMIT_EXCEEDED("Security", "Rate limit exceeded", "MEDIUM"),

    /**
     * Invalid file upload detected (security validation failure)
     * <p>Risk Level: HIGH (potential malware/exploit)</p>
     * <p>Retention: LONG (security monitoring)</p>
     */
    SECURITY_INVALID_FILE_UPLOAD("Security", "Invalid file upload detected", "HIGH"),

    // ========================================
    // System Events (SYSTEM_*)
    // ========================================

    /**
     * Application started
     * <p>Risk Level: LOW</p>
     * <p>Retention: LONG (operational monitoring)</p>
     */
    SYSTEM_STARTUP("System", "Application started", "LOW"),

    /**
     * Application shutting down
     * <p>Risk Level: LOW</p>
     * <p>Retention: LONG (operational monitoring)</p>
     */
    SYSTEM_SHUTDOWN("System", "Application shutting down", "LOW"),

    /**
     * Application error occurred
     * <p>Risk Level: MEDIUM</p>
     * <p>Retention: MEDIUM (troubleshooting)</p>
     */
    SYSTEM_ERROR("System", "Application error", "MEDIUM"),

    /**
     * Database connection issue
     * <p>Risk Level: HIGH (availability impact)</p>
     * <p>Retention: LONG (operational monitoring)</p>
     */
    SYSTEM_DATABASE_ERROR("System", "Database error", "HIGH"),

    /**
     * External API call failed
     * <p>Risk Level: MEDIUM</p>
     * <p>Retention: MEDIUM</p>
     */
    SYSTEM_EXTERNAL_API_ERROR("System", "External API error", "MEDIUM");

    // ========================================
    // Enum Fields
    // ========================================

    private final String category;
    private final String description;
    private final String riskLevel;

    /**
     * Constructor
     *
     * @param category Category of the event (Authentication, Authorization, etc.)
     * @param description Human-readable description
     * @param riskLevel Risk level (LOW, MEDIUM, HIGH)
     */
    AuditEventType(String category, String description, String riskLevel) {
        this.category = category;
        this.description = description;
        this.riskLevel = riskLevel;
    }

    /**
     * Get the category of this event type
     *
     * @return Event category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Get the description of this event type
     *
     * @return Event description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the risk level of this event type
     *
     * @return Risk level (LOW, MEDIUM, HIGH)
     */
    public String getRiskLevel() {
        return riskLevel;
    }

    /**
     * Check if this is a high-risk event
     *
     * @return true if risk level is HIGH
     */
    public boolean isHighRisk() {
        return "HIGH".equals(riskLevel);
    }

    /**
     * Check if this is a security-related event
     *
     * @return true if category is Security or risk level is HIGH
     */
    public boolean isSecurityEvent() {
        return "Security".equals(category) || isHighRisk();
    }

    /**
     * Get the recommended retention period in days
     *
     * @return Retention period in days
     */
    public int getRecommendedRetentionDays() {
        switch (riskLevel) {
            case "HIGH":
                return 365 * 7; // 7 years for high-risk events (compliance)
            case "MEDIUM":
                return 365 * 2; // 2 years for medium-risk events
            case "LOW":
            default:
                return 90; // 90 days for low-risk events
        }
    }

    /**
     * Get a formatted display string for this event type
     *
     * @return Formatted string: "[CATEGORY] Description (RISK)"
     */
    public String getDisplayString() {
        return String.format("[%s] %s (%s)", category, description, riskLevel);
    }
}
