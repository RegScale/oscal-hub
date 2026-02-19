package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.SecurityPolicy;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.SecurityPolicyUpdateRequest;
import gov.nist.oscal.tools.api.repository.SecurityPolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Service for managing Security Policy settings.
 * <p>
 * The security policy is a singleton configuration that controls:
 * - MFA requirements
 * - Password policy (length, rotation)
 * - Audit log retention
 * </p>
 */
@Service
public class SecurityPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityPolicyService.class);

    private final SecurityPolicyRepository securityPolicyRepository;

    public SecurityPolicyService(SecurityPolicyRepository securityPolicyRepository) {
        this.securityPolicyRepository = securityPolicyRepository;
    }

    /**
     * Get the current security policy.
     * If no policy exists, creates a default one.
     *
     * @return the security policy
     */
    @Cacheable("securityPolicy")
    public SecurityPolicy getPolicy() {
        return securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)
                .orElseGet(this::createDefaultPolicy);
    }

    /**
     * Create and save a default security policy.
     * Called when no policy exists in the database.
     *
     * @return the newly created default policy
     */
    @Transactional
    public SecurityPolicy createDefaultPolicy() {
        // Double-check if another thread created it
        return securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)
                .orElseGet(() -> {
                    logger.info("Creating default security policy");
                    SecurityPolicy policy = new SecurityPolicy();
                    policy.setId(SecurityPolicy.SINGLETON_ID);
                    policy.setMfaRequired(false);
                    policy.setPasswordMinLength(10);
                    policy.setPasswordMaxLength(128);
                    policy.setPasswordRotationDays(0);
                    policy.setAuditLogRetentionDays(90);
                    return securityPolicyRepository.save(policy);
                });
    }

    /**
     * Update the security policy.
     *
     * @param request   the update request
     * @param updatedBy username of the admin making the change
     * @return the updated security policy
     */
    @Transactional
    @CacheEvict(value = "securityPolicy", allEntries = true)
    public SecurityPolicy updatePolicy(SecurityPolicyUpdateRequest request, String updatedBy) {
        SecurityPolicy policy = securityPolicyRepository.getPolicy();

        // Validate password length consistency
        if (request.getPasswordMinLength() > request.getPasswordMaxLength()) {
            throw new IllegalArgumentException(
                    "Minimum password length cannot be greater than maximum password length");
        }

        // Update fields
        policy.setMfaRequired(request.getMfaRequired());
        policy.setPasswordMinLength(request.getPasswordMinLength());
        policy.setPasswordMaxLength(request.getPasswordMaxLength());
        policy.setPasswordRotationDays(request.getPasswordRotationDays());
        policy.setAuditLogRetentionDays(request.getAuditLogRetentionDays());
        policy.setUpdatedBy(updatedBy);
        policy.setUpdatedAt(LocalDateTime.now());

        SecurityPolicy saved = securityPolicyRepository.save(policy);

        logger.info("Security policy updated by {}: mfaRequired={}, passwordMinLength={}, " +
                        "passwordRotationDays={}, auditLogRetentionDays={}",
                updatedBy,
                saved.getMfaRequired(),
                saved.getPasswordMinLength(),
                saved.getPasswordRotationDays(),
                saved.getAuditLogRetentionDays());

        return saved;
    }

    /**
     * Check if MFA is required globally.
     * Returns false if policy cannot be loaded (fail-safe).
     *
     * @return true if MFA is required for all users
     */
    public boolean isMfaRequired() {
        try {
            SecurityPolicy policy = getPolicy();
            return policy != null && Boolean.TRUE.equals(policy.getMfaRequired());
        } catch (Exception e) {
            logger.warn("Could not check MFA requirement, defaulting to false: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if a user's password has expired based on the rotation policy.
     *
     * @param user the user to check
     * @return true if the password has expired
     */
    public boolean isPasswordExpired(User user) {
        SecurityPolicy policy = getPolicy();

        // If rotation is disabled (0 days), password never expires
        if (!policy.isPasswordRotationEnabled()) {
            return false;
        }

        // If password has never been changed, consider it expired
        if (user.getPasswordChangedAt() == null) {
            return true;
        }

        // Check if password is older than rotation period
        LocalDateTime expirationDate = user.getPasswordChangedAt()
                .plusDays(policy.getPasswordRotationDays());

        return LocalDateTime.now().isAfter(expirationDate);
    }

    /**
     * Get days until password expires.
     *
     * @param user the user to check
     * @return days until expiration, or -1 if rotation is disabled
     */
    public long getDaysUntilPasswordExpires(User user) {
        SecurityPolicy policy = getPolicy();

        if (!policy.isPasswordRotationEnabled()) {
            return -1; // Never expires
        }

        if (user.getPasswordChangedAt() == null) {
            return 0; // Already expired
        }

        LocalDateTime expirationDate = user.getPasswordChangedAt()
                .plusDays(policy.getPasswordRotationDays());

        long daysRemaining = ChronoUnit.DAYS.between(LocalDateTime.now(), expirationDate);
        return Math.max(0, daysRemaining);
    }

    /**
     * Get the audit log retention period in days.
     *
     * @return retention period in days
     */
    public int getAuditLogRetentionDays() {
        return getPolicy().getAuditLogRetentionDays();
    }

    /**
     * Get the minimum password length.
     *
     * @return minimum password length
     */
    public int getPasswordMinLength() {
        return getPolicy().getPasswordMinLength();
    }

    /**
     * Get the maximum password length.
     *
     * @return maximum password length
     */
    public int getPasswordMaxLength() {
        return getPolicy().getPasswordMaxLength();
    }
}
