package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for organization admins to manage users within their organization
 * Provides lock, deactivate, and password reset functionality
 */
@Service
public class UserManagementService {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Get all users in an organization
     */
    public List<OrganizationMembership> getOrganizationUsers(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));
        return membershipRepository.findByOrganization(organization);
    }

    /**
     * Get active users in an organization
     */
    public List<OrganizationMembership> getActiveOrganizationUsers(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));
        return membershipRepository.findByOrganizationAndStatus(organization, MembershipStatus.ACTIVE);
    }

    /**
     * Lock a user's access to an organization
     * Sets membership status to LOCKED
     */
    @Transactional
    public OrganizationMembership lockUser(Long userId, Long organizationId, Long adminId) {
        logger.info("Locking user {} in organization {} by admin {}", userId, organizationId, adminId);

        OrganizationMembership membership = getMembershipAndValidate(userId, organizationId, adminId);

        if (membership.getStatus() == MembershipStatus.LOCKED) {
            throw new RuntimeException("User is already locked");
        }

        membership.setStatus(MembershipStatus.LOCKED);
        membership.setUpdatedAt(LocalDateTime.now());
        membership = membershipRepository.save(membership);

        logger.info("Locked user {} in organization {}", userId, organizationId);
        return membership;
    }

    /**
     * Unlock a user's access to an organization
     * Sets membership status to ACTIVE
     */
    @Transactional
    public OrganizationMembership unlockUser(Long userId, Long organizationId, Long adminId) {
        logger.info("Unlocking user {} in organization {} by admin {}", userId, organizationId, adminId);

        OrganizationMembership membership = getMembershipAndValidate(userId, organizationId, adminId);

        if (membership.getStatus() == MembershipStatus.ACTIVE) {
            throw new RuntimeException("User is already active");
        }

        if (membership.getStatus() == MembershipStatus.DEACTIVATED) {
            throw new RuntimeException("Cannot unlock a deactivated user. Please reactivate instead.");
        }

        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setUpdatedAt(LocalDateTime.now());
        membership = membershipRepository.save(membership);

        logger.info("Unlocked user {} in organization {}", userId, organizationId);
        return membership;
    }

    /**
     * Deactivate a user's access to an organization
     * Sets membership status to DEACTIVATED (soft delete)
     */
    @Transactional
    public OrganizationMembership deactivateUser(Long userId, Long organizationId, Long adminId) {
        logger.info("Deactivating user {} in organization {} by admin {}", userId, organizationId, adminId);

        OrganizationMembership membership = getMembershipAndValidate(userId, organizationId, adminId);

        if (membership.getStatus() == MembershipStatus.DEACTIVATED) {
            throw new RuntimeException("User is already deactivated");
        }

        membership.setStatus(MembershipStatus.DEACTIVATED);
        membership.setUpdatedAt(LocalDateTime.now());
        membership = membershipRepository.save(membership);

        logger.info("Deactivated user {} in organization {}", userId, organizationId);
        return membership;
    }

    /**
     * Reactivate a previously deactivated user
     * Sets membership status to ACTIVE
     */
    @Transactional
    public OrganizationMembership reactivateUser(Long userId, Long organizationId, Long adminId) {
        logger.info("Reactivating user {} in organization {} by admin {}", userId, organizationId, adminId);

        OrganizationMembership membership = getMembershipAndValidate(userId, organizationId, adminId);

        if (membership.getStatus() == MembershipStatus.ACTIVE) {
            throw new RuntimeException("User is already active");
        }

        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setUpdatedAt(LocalDateTime.now());
        membership = membershipRepository.save(membership);

        logger.info("Reactivated user {} in organization {}", userId, organizationId);
        return membership;
    }

    /**
     * Reset a user's password
     * Generates a new temporary password and sets mustChangePassword flag
     * Returns the temporary password (should be sent to user via email in production)
     */
    @Transactional
    public Map<String, String> resetPassword(Long userId, Long organizationId, Long adminId) {
        logger.info("Resetting password for user {} in organization {} by admin {}", userId, organizationId, adminId);

        OrganizationMembership membership = getMembershipAndValidate(userId, organizationId, adminId);
        User user = membership.getUser();

        // Generate temporary password
        String tempPassword = generateTemporaryPassword();

        // Update user password and set mustChangePassword flag
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // TODO: Send email with temporary password (if email service is configured)
        // For now, return it in the response (in production, this should be emailed)
        logger.warn("TEMPORARY PASSWORD for user {}: {}", user.getUsername(), tempPassword);
        logger.warn("User must change password on next login");

        Map<String, String> result = new HashMap<>();
        result.put("tempPassword", tempPassword);
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());

        logger.info("Reset password for user {}", userId);
        return result;
    }

    /**
     * Get membership and validate admin permissions
     * Ensures:
     * - User is a member of the organization
     * - Admin is a member of the organization
     * - Admin has ORG_ADMIN role in the organization
     * - Admin cannot modify their own account
     */
    private OrganizationMembership getMembershipAndValidate(Long userId, Long organizationId, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + adminId));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));

        // Get user's membership in the organization
        OrganizationMembership userMembership = membershipRepository
                .findByUserAndOrganization(user, organization)
                .orElseThrow(() -> new RuntimeException("User is not a member of this organization"));

        // Verify admin's membership and role
        OrganizationMembership adminMembership = membershipRepository
                .findByUserAndOrganization(admin, organization)
                .orElseThrow(() -> new RuntimeException("Admin is not a member of this organization"));

        if (adminMembership.getRole() != OrganizationMembership.OrganizationRole.ORG_ADMIN
            && admin.getGlobalRole() != User.GlobalRole.SUPER_ADMIN) {
            throw new RuntimeException("Insufficient permissions. ORG_ADMIN role required.");
        }

        // Prevent admins from modifying their own account
        if (userId.equals(adminId)) {
            throw new RuntimeException("Cannot modify your own account");
        }

        return userMembership;
    }

    /**
     * Generate a secure temporary password
     * Format: 3 uppercase + 3 lowercase + 3 digits + 3 special characters (randomized)
     */
    private String generateTemporaryPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String special = "!@#$%^&*";

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 3; i++) {
            password.append(upper.charAt((int) (Math.random() * upper.length())));
        }
        for (int i = 0; i < 3; i++) {
            password.append(lower.charAt((int) (Math.random() * lower.length())));
        }
        for (int i = 0; i < 3; i++) {
            password.append(digits.charAt((int) (Math.random() * digits.length())));
        }
        for (int i = 0; i < 3; i++) {
            password.append(special.charAt((int) (Math.random() * special.length())));
        }

        // Shuffle
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }
}
