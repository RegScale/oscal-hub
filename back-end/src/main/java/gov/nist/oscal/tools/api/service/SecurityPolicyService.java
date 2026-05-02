package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.SecurityPolicy;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.SecurityPolicyUpdateRequest;
import gov.nist.oscal.tools.api.repository.SecurityPolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    /**
     * Self-reference for calling @Transactional methods through the Spring proxy.
     * Without this, internal method calls bypass the proxy and @Transactional is ignored.
     */
    @Autowired
    @Lazy
    private SecurityPolicyService self;

    public SecurityPolicyService(SecurityPolicyRepository securityPolicyRepository) {
        this.securityPolicyRepository = securityPolicyRepository;
    }

    /**
     * Get the current security policy.
     * If no policy exists, creates a default one in a separate transaction.
     *
     * @return the security policy
     */
    @Cacheable("securityPolicy")
    public SecurityPolicy getPolicy() {
        return securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)
                .orElseGet(() -> self.createDefaultPolicy());
    }

    /**
     * Create and save a default security policy.
     * Called when no policy exists in the database.
     * Uses REQUIRES_NEW to run in a separate transaction, preventing concurrent
     * creation attempts from marking the parent transaction for rollback.
     *
     * @return the newly created default policy
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityPolicy createDefaultPolicy() {
        // Double-check if another thread created it (handles race conditions)
        return securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)
                .orElseGet(() -> {
                    try {
                        logger.info("Creating default security policy");
                        SecurityPolicy policy = new SecurityPolicy();
                        policy.setId(SecurityPolicy.SINGLETON_ID);
                        policy.setMfaRequired(false);
                        policy.setPasswordMinLength(10);
                        policy.setPasswordMaxLength(128);
                        policy.setPasswordRotationDays(0);
                        policy.setAuditLogRetentionDays(90);
                        return securityPolicyRepository.save(policy);
                    } catch (Exception e) {
                        // Another thread might have created it, try to fetch again
                        logger.warn("Failed to create default policy (likely concurrent creation): {}", e.getMessage());
                        return securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID)
                                .orElseThrow(() -> new RuntimeException("Failed to create or fetch security policy", e));
                    }
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
     * Uses a separate transaction to avoid affecting the caller's transaction.
     *
     * @return true if MFA is required for all users
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean isMfaRequired() {
        try {
            SecurityPolicy policy = securityPolicyRepository.findById(SecurityPolicy.SINGLETON_ID).orElse(null);
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
