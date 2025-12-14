package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import gov.nist.oscal.tools.api.model.AuthRequest;
import gov.nist.oscal.tools.api.model.AuthResponse;
import gov.nist.oscal.tools.api.model.RegisterRequest;
import gov.nist.oscal.tools.api.model.RequestAccessRequest;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserAccessRequestRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordValidationService passwordValidationService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccessRequestRepository accessRequestRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate password complexity using new PasswordValidationService
        try {
            passwordValidationService.validatePassword(request.getPassword(), request.getUsername());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e.getMessage());
        }

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);

        // Save user
        user = userRepository.save(user);

        logger.info("New user registered: {} (ID: {})", user.getUsername(), user.getId());

        // Log audit event
        auditLogService.logEvent(gov.nist.oscal.tools.api.model.AuditEventType.AUTH_REGISTER_SUCCESS,
            user.getUsername(), user.getId(), "SUCCESS", null, "REGISTER", null);

        // Generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getId());
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        String ipAddress = getClientIpAddress();
        String username = request.getUsername();

        // Check if account is locked
        if (loginAttemptService.isAccountLocked(username)) {
            long remainingTime = loginAttemptService.getRemainingLockoutTime(username);
            logger.warn("Login attempt for locked account: {} from IP: {}", username, ipAddress);
            throw new RuntimeException(
                "Account is temporarily locked due to multiple failed login attempts. " +
                "Please try again in " + remainingTime + " seconds."
            );
        }

        // Check if IP address is locked
        if (loginAttemptService.isIpLocked(ipAddress)) {
            logger.warn("Login attempt from locked IP: {}", ipAddress);
            throw new RuntimeException(
                "Too many failed login attempts from this IP address. Please try again later."
            );
        }

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );

            // Load user details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Update user on successful login
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setLastLogin(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            user.setLastFailedLogin(null);
            user.setLastFailedLoginIp(null);
            user.setAccountLockedUntil(null);
            userRepository.save(user);

            // Record successful login (clears failed attempts cache)
            loginAttemptService.recordSuccessfulLogin(username, ipAddress);

            logger.info("Successful login for user: {} from IP: {}", username, ipAddress);

            // Log audit event
            auditLogService.logAuthSuccess(username, user.getId());

            // Generate token
            String token = jwtUtil.generateToken(userDetails);

            return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getId());

        } catch (AuthenticationException e) {
            // Record failed login attempt
            loginAttemptService.recordFailedLogin(username, ipAddress);

            // Update user failed login tracking in database
            userRepository.findByUsername(username).ifPresent(user -> {
                int newFailedAttempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
                user.setFailedLoginAttempts(newFailedAttempts);
                user.setLastFailedLogin(LocalDateTime.now());
                user.setLastFailedLoginIp(ipAddress);

                // Check if account should be locked
                if (loginAttemptService.isAccountLocked(username)) {
                    user.setAccountLockedUntil(
                        LocalDateTime.now().plusSeconds(
                            loginAttemptService.getRemainingLockoutTime(username)
                        )
                    );
                    // Log account lockout event
                    auditLogService.logAccountLockout(username, user.getId(), newFailedAttempts);
                }

                userRepository.save(user);
            });

            // Get remaining attempts for user feedback
            int remainingAttempts = loginAttemptService.getRemainingAttempts(username);

            logger.warn("Failed login attempt for user: {} from IP: {} (remaining attempts: {})",
                username, ipAddress, remainingAttempts);

            // Log failed login audit event
            auditLogService.logAuthFailure(username, "Invalid credentials");

            if (remainingAttempts > 0) {
                throw new RuntimeException(
                    "Invalid username or password. " + remainingAttempts + " attempts remaining before account lockout."
                );
            } else {
                throw new RuntimeException(
                    "Invalid username or password. Account has been locked due to multiple failed login attempts."
                );
            }
        }
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public String generateToken(UserDetails userDetails) {
        return jwtUtil.generateToken(userDetails);
    }

    @Transactional
    public User updateProfile(String username, java.util.Map<String, String> updates) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update email if provided
        if (updates.containsKey("email") && updates.get("email") != null && !updates.get("email").isEmpty()) {
            String newEmail = updates.get("email");
            user.setEmail(newEmail);
        }

        // Update password if provided
        if (updates.containsKey("password") && updates.get("password") != null && !updates.get("password").isEmpty()) {
            String newPassword = updates.get("password");
            // Validate password complexity using new PasswordValidationService
            try {
                passwordValidationService.validatePassword(newPassword, username);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e.getMessage());
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordChangedAt(LocalDateTime.now());
            logger.info("Password changed for user: {}", username);
        }

        // Update profile metadata fields if provided
        if (updates.containsKey("firstName")) {
            user.setFirstName(updates.get("firstName"));
        }
        if (updates.containsKey("lastName")) {
            user.setLastName(updates.get("lastName"));
        }
        if (updates.containsKey("street")) {
            user.setStreet(updates.get("street"));
        }
        if (updates.containsKey("city")) {
            user.setCity(updates.get("city"));
        }
        if (updates.containsKey("state")) {
            user.setState(updates.get("state"));
        }
        if (updates.containsKey("zip")) {
            user.setZip(updates.get("zip"));
        }
        if (updates.containsKey("title")) {
            user.setTitle(updates.get("title"));
        }
        if (updates.containsKey("organization")) {
            user.setOrganization(updates.get("organization"));
        }
        if (updates.containsKey("phoneNumber")) {
            user.setPhoneNumber(updates.get("phoneNumber"));
        }

        return userRepository.save(user);
    }

    @Transactional
    public User updateLogo(String username, String logo) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLogo(logo);
        return userRepository.save(user);
    }

    /**
     * Generate a service account token for the current user
     * @param username The username to generate the token for
     * @param tokenName The name/description for the service account token
     * @param expirationDays Number of days until the token expires
     * @return Date when the token expires
     */
    public java.util.Date generateServiceAccountToken(String username, String tokenName, int expirationDays) {
        // Validate user exists
        userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Calculate expiration date
        java.util.Date now = new java.util.Date();
        long expirationMillis = (long) expirationDays * 24 * 60 * 60 * 1000;
        java.util.Date expirationDate = new java.util.Date(now.getTime() + expirationMillis);

        return expirationDate;
    }

    /**
     * Get the client's IP address from the current HTTP request
     * Handles X-Forwarded-For headers for proxied requests
     *
     * @return Client IP address, or "unknown" if not available
     */
    private String getClientIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();

        // Check X-Forwarded-For header (for proxied requests)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        // Check X-Real-IP header (used by some proxies)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Fall back to remote address
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    /**
     * Select organization after initial authentication
     * Validates user has active membership and generates full JWT with org context
     *
     * @param userId User ID from pre-org-selection token
     * @param organizationId Organization to select
     * @return Map containing token, username, email, userId, organizationId, orgRole
     */
    @Transactional
    public Map<String, Object> selectOrganization(Long userId, Long organizationId) {
        // Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate organization exists and is active
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        if (!organization.getActive()) {
            throw new RuntimeException("Organization is not active");
        }

        // Find membership
        OrganizationMembership membership = membershipRepository
                .findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new RuntimeException("You do not have access to this organization"));

        // Check membership status
        if (membership.getStatus() == MembershipStatus.DEACTIVATED) {
            throw new RuntimeException("Your membership in this organization has been deactivated");
        }

        if (membership.getStatus() == MembershipStatus.LOCKED) {
            throw new RuntimeException("Your account in this organization is locked");
        }

        // Generate full JWT with organization context
        String token = jwtUtil.generateTokenWithOrgContext(
                user.getUsername(),
                user.getId(),
                user.getGlobalRole().toString(),
                organization.getId(),
                membership.getRole().toString(),
                user.getMustChangePassword()
        );

        logger.info("User {} selected organization {} (role: {})",
                user.getUsername(), organization.getName(), membership.getRole());

        // Log audit event
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("organizationId", organizationId);
        metadata.put("organizationName", organization.getName());
        metadata.put("role", membership.getRole().toString());

        auditLogService.logEvent(
                gov.nist.oscal.tools.api.model.AuditEventType.AUTH_ORG_SELECTION,
                user.getUsername(),
                user.getId(),
                "SUCCESS",
                "organization:" + organizationId,
                "ORG_SELECTION",
                metadata
        );

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("userId", user.getId());
        response.put("organizationId", organization.getId());
        response.put("organizationName", organization.getName());
        response.put("orgRole", membership.getRole().toString());
        response.put("globalRole", user.getGlobalRole().toString());
        response.put("mustChangePassword", user.getMustChangePassword());

        return response;
    }

    /**
     * Switch to a different organization (re-issue JWT)
     * Used when user wants to switch org context without re-login
     *
     * @param userId Current user ID
     * @param organizationId Organization to switch to
     * @return Map containing new token and org details
     */
    @Transactional
    public Map<String, Object> switchOrganization(Long userId, Long organizationId) {
        // Reuse selectOrganization logic - it already does all the validation we need
        return selectOrganization(userId, organizationId);
    }

    /**
     * Get all organizations the user has access to (for NASCAR page)
     * Returns organization info including logos
     *
     * @param userId User ID
     * @return List of organizations with membership details
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyOrganizations(Long userId) {
        // Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get all active memberships
        List<OrganizationMembership> memberships = membershipRepository
                .findByUserId(userId).stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .collect(Collectors.toList());

        // Build response list
        return memberships.stream()
                .filter(m -> m.getOrganization().getActive())
                .map(m -> {
                    Organization org = m.getOrganization();
                    Map<String, Object> orgData = new HashMap<>();
                    orgData.put("organizationId", org.getId());
                    orgData.put("name", org.getName());
                    orgData.put("description", org.getDescription());
                    orgData.put("logoUrl", org.getLogoUrl());
                    orgData.put("role", m.getRole().toString());
                    orgData.put("joinedAt", m.getJoinedAt());
                    return orgData;
                })
                .collect(Collectors.toList());
    }

    /**
     * Change password (for forced password change or user-initiated change)
     * Validates old password and updates to new password
     *
     * @param userId User ID
     * @param oldPassword Current password
     * @param newPassword New password to set
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        // Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            logger.warn("Failed password change attempt for user: {} - incorrect old password", user.getUsername());
            throw new RuntimeException("Current password is incorrect");
        }

        // Validate new password complexity
        try {
            passwordValidationService.validatePassword(newPassword, user.getUsername());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e.getMessage());
        }

        // Check password is different from old password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false); // Clear forced password change flag
        userRepository.save(user);

        logger.info("Password changed successfully for user: {}", user.getUsername());

        // Log audit event
        auditLogService.logEvent(
                gov.nist.oscal.tools.api.model.AuditEventType.CONFIG_PASSWORD_CHANGE,
                user.getUsername(),
                user.getId(),
                "SUCCESS",
                "user:" + userId,
                "PASSWORD_CHANGE",
                null
        );
    }

    /**
     * Get all active organizations (for public NASCAR page)
     *
     * @return List of active organizations with basic info and logos
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveOrganizations() {
        List<Organization> organizations = organizationRepository.findByActiveTrue();

        return organizations.stream()
                .map(org -> {
                    Map<String, Object> orgData = new HashMap<>();
                    orgData.put("organizationId", org.getId());
                    orgData.put("name", org.getName());
                    orgData.put("description", org.getDescription());
                    orgData.put("logoUrl", org.getLogoUrl());
                    return orgData;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all pending access requests for a user
     *
     * @param userId User ID
     * @return List of pending access requests with organization details
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyPendingRequests(Long userId) {
        // Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get all pending requests for this user
        List<UserAccessRequest> pendingRequests = accessRequestRepository
                .findByUserAndStatus(user, UserAccessRequest.RequestStatus.PENDING);

        // Build response list
        return pendingRequests.stream()
                .map(request -> {
                    Map<String, Object> requestData = new HashMap<>();
                    requestData.put("requestId", request.getId());
                    requestData.put("organizationId", request.getOrganization().getId());
                    requestData.put("organizationName", request.getOrganization().getName());
                    requestData.put("requestDate", request.getRequestDate());
                    requestData.put("status", request.getStatus().toString());
                    requestData.put("message", request.getMessage());
                    return requestData;
                })
                .collect(Collectors.toList());
    }

    /**
     * Submit an access request to join an organization
     * Creates a UserAccessRequest that org admins can approve/reject
     *
     * @param request Access request details
     */
    @Transactional
    public void requestAccess(RequestAccessRequest request) {
        // Validate organization exists and is active
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        if (!organization.getActive()) {
            throw new RuntimeException("Organization is not active");
        }

        // Check for existing pending requests by email (use List version to handle duplicates)
        List<UserAccessRequest> existingRequests = accessRequestRepository.findPendingByEmailAndOrganization(
                request.getEmail(),
                request.getOrganizationId()
        );
        if (!existingRequests.isEmpty()) {
            throw new RuntimeException("An access request with this email already exists for this organization");
        }

        // Create access request (user will be null for new users who haven't registered yet)
        UserAccessRequest accessRequest = new UserAccessRequest();
        accessRequest.setUser(null);
        accessRequest.setOrganization(organization);
        accessRequest.setEmail(request.getEmail());
        accessRequest.setFirstName(request.getFirstName());
        accessRequest.setLastName(request.getLastName());
        accessRequest.setUsername(request.getUsername());
        accessRequest.setMessage(request.getMessage());
        accessRequest.setStatus(UserAccessRequest.RequestStatus.PENDING);
        accessRequest.setRequestDate(LocalDateTime.now());

        accessRequestRepository.save(accessRequest);

        logger.info("Access request created for {} to organization {} (ID: {})",
                request.getEmail(), organization.getName(), organization.getId());
    }
}
