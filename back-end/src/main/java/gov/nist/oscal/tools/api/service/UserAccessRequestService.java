package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import gov.nist.oscal.tools.api.entity.UserAccessRequest.RequestStatus;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserAccessRequestRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing user access requests to organizations
 * Handles approval/rejection workflow and user account creation
 */
@Service
public class UserAccessRequestService {

    private static final Logger logger = LoggerFactory.getLogger(UserAccessRequestService.class);

    @Autowired
    private UserAccessRequestRepository accessRequestRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Create a new access request for an existing user
     */
    @Transactional
    public UserAccessRequest createAccessRequest(Long userId, Long organizationId, String message) {
        logger.info("Creating access request for user {} to organization {}", userId, organizationId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));

        // Check if user already has membership
        if (membershipRepository.existsByUserAndOrganization(user, organization)) {
            throw new RuntimeException("User is already a member of this organization");
        }

        // Check for existing pending request
        List<UserAccessRequest> existingRequests = accessRequestRepository
                .findPendingByEmailAndOrganization(user.getEmail(), organizationId);
        if (!existingRequests.isEmpty()) {
            throw new RuntimeException("A pending access request already exists for this user and organization");
        }

        UserAccessRequest request = new UserAccessRequest(user, organization, message);
        request = accessRequestRepository.save(request);

        logger.info("Created access request: {}", request.getId());
        return request;
    }

    /**
     * Create a new access request for a new user (who doesn't have an account yet)
     */
    @Transactional
    public UserAccessRequest createAccessRequestForNewUser(
            Long organizationId,
            String email,
            String firstName,
            String lastName,
            String username,
            String message) {

        logger.info("Creating access request for new user {} to organization {}", username, organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));

        // Check if username or email already exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists: " + email);
        }

        // Check for existing pending request with same email
        List<UserAccessRequest> existingRequests = accessRequestRepository
                .findPendingByEmailAndOrganization(email, organizationId);
        if (!existingRequests.isEmpty()) {
            throw new RuntimeException("A pending access request already exists for this email");
        }

        UserAccessRequest request = new UserAccessRequest(
                organization, email, firstName, lastName, username, message);
        request = accessRequestRepository.save(request);

        logger.info("Created access request for new user: {}", request.getId());
        return request;
    }

    /**
     * Get all pending requests for an organization
     */
    public List<UserAccessRequest> getPendingRequests(Long organizationId) {
        return accessRequestRepository.findPendingByOrganizationId(organizationId);
    }

    /**
     * Get all requests for an organization (regardless of status)
     */
    public List<UserAccessRequest> getAllRequests(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));
        return accessRequestRepository.findByOrganization(organization);
    }

    /**
     * Get count of pending requests for an organization
     */
    public long getPendingRequestCount(Long organizationId) {
        return accessRequestRepository.countPendingByOrganizationId(organizationId);
    }

    /**
     * Approve an access request
     * If user doesn't exist, creates a new user account with temporary password
     */
    @Transactional
    public UserAccessRequest approveRequest(Long requestId, Long reviewerId, String notes) {
        logger.info("Approving access request: {} by reviewer: {}", requestId, reviewerId);

        UserAccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Access request not found: " + requestId));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found: " + reviewerId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request has already been processed");
        }

        User user = request.getUser();

        // If user doesn't exist, create new account
        if (user == null) {
            user = createUserFromRequest(request);
        }

        // Create organization membership
        OrganizationMembership membership = new OrganizationMembership(
                user, request.getOrganization(), OrganizationRole.USER);
        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);

        // Update request status
        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedDate(LocalDateTime.now());
        request.setNotes(notes);
        request.setUser(user); // Link user if it was created

        request = accessRequestRepository.save(request);
        logger.info("Approved access request: {}", requestId);

        return request;
    }

    /**
     * Reject an access request
     */
    @Transactional
    public UserAccessRequest rejectRequest(Long requestId, Long reviewerId, String notes) {
        logger.info("Rejecting access request: {} by reviewer: {}", requestId, reviewerId);

        UserAccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Access request not found: " + requestId));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found: " + reviewerId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request has already been processed");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedBy(reviewer);
        request.setReviewedDate(LocalDateTime.now());
        request.setNotes(notes);

        request = accessRequestRepository.save(request);
        logger.info("Rejected access request: {}", requestId);

        return request;
    }

    /**
     * Create a new user account from an access request
     * Generates a temporary password and sets mustChangePassword flag
     */
    private User createUserFromRequest(UserAccessRequest request) {
        logger.info("Creating new user from access request: {}", request.getId());

        // Generate temporary password (12 characters with upper, lower, digit, special)
        String tempPassword = generateTemporaryPassword();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setEnabled(true);
        user.setMustChangePassword(true); // Force password change on first login
        user.setGlobalRole(User.GlobalRole.USER);

        // TODO: Send email with temporary password (if email service is configured)
        // For now, log it (in production, this should be emailed to the user)
        logger.warn("TEMPORARY PASSWORD for user {}: {}", user.getUsername(), tempPassword);
        logger.warn("User must change password on first login");

        user = userRepository.save(user);
        logger.info("Created new user: {} with ID: {}", user.getUsername(), user.getId());

        return user;
    }

    /**
     * Generate a secure temporary password
     * Format: 3 uppercase + 3 lowercase + 3 digits + 3 special characters (randomized)
     */
    private String generateTemporaryPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // Excludes I, O for clarity
        String lower = "abcdefghijkmnopqrstuvwxyz"; // Excludes l for clarity
        String digits = "23456789"; // Excludes 0, 1 for clarity
        String special = "!@#$%^&*";

        StringBuilder password = new StringBuilder();

        // Add 3 from each category
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

        // Shuffle the password characters
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
