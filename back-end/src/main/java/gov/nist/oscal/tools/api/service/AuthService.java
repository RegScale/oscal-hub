package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import gov.nist.oscal.tools.api.exception.OrganizationNameInUseException;
import gov.nist.oscal.tools.api.exception.UsernameAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import gov.nist.oscal.tools.api.model.AuditEventType;
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

    @Autowired
    @org.springframework.context.annotation.Lazy
    private SecurityPolicyService securityPolicyService;

    /**
     * Optional: present only under the {@code dev} Spring profile (see
     * {@link MfaDevBypass}). Null on staging / prod / gcp / default profiles
     * because the bean is not created there.
     */
    @Autowired(required = false)
    private MfaDevBypass mfaDevBypass;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private OrganizationService organizationService;

    @Autowired
    private gov.nist.oscal.tools.api.util.ClientIpResolver clientIpResolver;

    @Autowired
    private gov.nist.oscal.tools.api.config.AccountSecurityConfig accountSecurityConfig;

    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Normalize identity fields: stray whitespace ("iorga ") and case-only
        // variants ("Iorga" vs "iorga") create confusing duplicate identities and
        // failed logins, since login lookup is exact-match.
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        String emailAddress = request.getEmail() == null ? null : request.getEmail().trim();
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        // Validate password complexity. IllegalArgumentException propagates to GlobalErrorAdvice.
        passwordValidationService.validatePassword(request.getPassword(), username);

        // Case-insensitive: "Iorga" must not be creatable alongside "iorga"
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        // Email is intentionally NOT checked for uniqueness — the project supports
        // multiple users sharing an email (see migrations V1.11/V1.21). A user may
        // legitimately have parallel accounts in different organizations or have
        // submitted an access request before registering.

        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setEmail(emailAddress);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);

        // Save and flush so a concurrent registration that slipped past the
        // existsByUsername pre-check surfaces HERE as a constraint violation we can
        // translate into the same 409 the pre-check produces — instead of failing at
        // commit and reaching the client as a generic conflict.
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        logger.info("New user registered: {} (ID: {})", user.getUsername(), user.getId());

        // Log audit event
        auditLogService.logEvent(AuditEventType.AUTH_REGISTER_SUCCESS,
            user.getUsername(), user.getId(), "SUCCESS", null, "REGISTER", null);

        // Self-serve org creation: if organizationName was provided, create org + ORG_ADMIN membership
        String orgName = request.getOrganizationName();
        if (orgName != null && !orgName.isBlank()) {
            Organization org = organizationService.createOrganizationForUser(orgName, user);
            logger.info("User {} created organization {} (ID: {}) on registration",
                user.getUsername(), org.getName(), org.getId());
        }

        // Welcome email goes out after this transaction commits, async with retry
        // (TransactionalEmailListener) — SendGrid latency no longer extends the
        // registration transaction.
        eventPublisher.publishEvent(new gov.nist.oscal.tools.api.email.EmailEvents.WelcomeEmail(user.getId()));

        // Register the new user in the marketing CRM after commit (CrmSyncListener).
        // Signup consent is disclosed on the registration form.
        eventPublisher.publishEvent(new gov.nist.oscal.tools.api.crm.CrmEvents.ContactRegistered(
                user.getId(), "self_serve_registration"));

        // Enforce the global MFA policy at registration, mirroring login.
        // Without this, the first session bypassed a required MFA setup and the
        // requirement only kicked in on the next login.
        if (mfaDevBypass == null || !mfaDevBypass.isActive()) {
            boolean mfaGloballyRequired = false;
            try {
                mfaGloballyRequired = securityPolicyService.isMfaRequired();
            } catch (Exception e) {
                logger.warn("Could not check MFA policy at registration, defaulting to not required: {}", e.getMessage());
            }
            if (mfaGloballyRequired) {
                String mfaToken = jwtUtil.generateMfaSetupToken(user.getUsername(), user.getId());
                logger.info("MFA setup required for newly registered user: {} (global policy)", user.getUsername());
                return AuthResponse.mfaSetupRequired(mfaToken, user);
            }
        }

        // Generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(token, user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        String ipAddress = getClientIpAddress();
        String username = request.getUsername();

        // Per-instance fast path (in-memory). NOT authoritative: Cloud Run runs
        // multiple instances and restarts wipe this state.
        if (loginAttemptService.isAccountLocked(username)) {
            long remainingTime = loginAttemptService.getRemainingLockoutTime(username);
            logger.warn("Login attempt for locked account: {} from IP: {}", username, ipAddress);
            throw new RuntimeException(
                "Account is temporarily locked due to multiple failed login attempts. " +
                "Please try again in " + remainingTime + " seconds."
            );
        }

        // Authoritative DB-backed lockout: shared across instances, survives
        // restarts. Previously account_locked_until was written but never read
        // at login, so the effective lockout was whatever one instance remembered.
        User lockCheck = resolveUserForLogin(username);
        if (lockCheck != null && lockCheck.getAccountLockedUntil() != null
                && lockCheck.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            long remainingTime = java.time.Duration
                .between(LocalDateTime.now(), lockCheck.getAccountLockedUntil()).getSeconds();
            logger.warn("Login attempt for DB-locked account: {} from IP: {}", username, ipAddress);
            throw new RuntimeException(
                "Account is temporarily locked due to multiple failed login attempts. " +
                "Please try again in " + Math.max(1, remainingTime) + " seconds."
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

            // Update user on successful login. Look up by the CANONICAL username
            // from the authenticated principal — the typed form may differ in case
            // (CustomUserDetailsService accepts a unique case-insensitive match).
            User user = userRepository.findByUsername(userDetails.getUsername())
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

            // Dev-only bypass: skip MFA entirely when the dev-profile bean is present and enabled.
            // This is impossible in non-dev profiles because MfaDevBypass is @Profile("dev") and
            // the bean is not created elsewhere — mfaDevBypass is null on staging/prod/gcp.
            if (mfaDevBypass != null && mfaDevBypass.isActive()) {
                logger.warn("MFA bypassed for user {} (dev profile, security.mfa.dev-bypass.enabled=true)", username);
                String token = jwtUtil.generateToken(userDetails);
                return new AuthResponse(token, user);
            }

            // Check MFA requirements (safely handle null values)
            boolean mfaGloballyRequired = false;
            try {
                mfaGloballyRequired = securityPolicyService.isMfaRequired();
            } catch (Exception e) {
                logger.warn("Could not check MFA policy, defaulting to not required: {}", e.getMessage());
            }
            boolean userHasMfaEnabled = Boolean.TRUE.equals(user.getMfaEnabled()) &&
                                        Boolean.TRUE.equals(user.getMfaSetupCompleted());

            // Case 1: MFA is globally required but user hasn't completed setup
            if (mfaGloballyRequired && !userHasMfaEnabled) {
                String mfaToken = jwtUtil.generateMfaSetupToken(username, user.getId());
                logger.info("MFA setup required for user: {} (global policy)", username);
                return AuthResponse.mfaSetupRequired(mfaToken, user);
            }

            // Case 2: User has MFA enabled - require verification
            if (userHasMfaEnabled) {
                String mfaToken = jwtUtil.generateMfaPartialToken(username, user.getId());
                logger.info("MFA verification required for user: {}", username);
                return AuthResponse.mfaRequired(mfaToken, user);
            }

            // Case 3: No MFA required - generate full token
            String token = jwtUtil.generateToken(userDetails);

            return new AuthResponse(token, user);

        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user {}: {} - {}", username, e.getClass().getSimpleName(), e.getMessage());

            // Record failed login attempt (in-memory IP tracking + fast path)
            loginAttemptService.recordFailedLogin(username, ipAddress);

            // DB-backed failure tracking: the counter in `users` is shared by all
            // instances, so the lockout decision is made HERE from the DB counter,
            // not from per-instance memory.
            int remainingAttempts = accountSecurityConfig.getLockoutMaxAttempts();
            User failedUser = resolveUserForLogin(username);
            if (failedUser != null) {
                int priorAttempts = failedUser.getFailedLoginAttempts() != null
                        ? failedUser.getFailedLoginAttempts() : 0;

                // Start a fresh count when a previous lockout has expired, or when
                // the last failure is older than the sliding window — the DB counter
                // has no TTL, so without this, stale failures accumulate forever and
                // a single mistake months later would re-lock the account.
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime windowStart = now.minusSeconds(accountSecurityConfig.getLockoutWindowSeconds());
                boolean expiredLock = failedUser.getAccountLockedUntil() != null
                        && !failedUser.getAccountLockedUntil().isAfter(now);
                boolean staleWindow = failedUser.getLastFailedLogin() != null
                        && failedUser.getLastFailedLogin().isBefore(windowStart);
                if (expiredLock || staleWindow) {
                    priorAttempts = 0;
                    failedUser.setAccountLockedUntil(null);
                }

                int newFailedAttempts = priorAttempts + 1;
                failedUser.setFailedLoginAttempts(newFailedAttempts);
                failedUser.setLastFailedLogin(now);
                failedUser.setLastFailedLoginIp(ipAddress);

                if (accountSecurityConfig.isLockoutEnabled()
                        && newFailedAttempts >= accountSecurityConfig.getLockoutMaxAttempts()) {
                    failedUser.setAccountLockedUntil(
                        now.plusSeconds(accountSecurityConfig.getLockoutDurationSeconds()));
                    auditLogService.logAccountLockout(username, failedUser.getId(), newFailedAttempts);
                }

                userRepository.save(failedUser);
                remainingAttempts = Math.max(0,
                    accountSecurityConfig.getLockoutMaxAttempts() - newFailedAttempts);
            }

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

        // Update email if provided. Email is not unique across users (see register()
        // for rationale), so no collision check is required.
        if (updates.containsKey("email") && updates.get("email") != null && !updates.get("email").isEmpty()) {
            String newEmail = updates.get("email");
            user.setEmail(newEmail);
        }

        // Update password if provided
        if (updates.containsKey("password") && updates.get("password") != null && !updates.get("password").isEmpty()) {
            String newPassword = updates.get("password");
            // IllegalArgumentException from validation propagates to GlobalErrorAdvice.
            passwordValidationService.validatePassword(newPassword, username);
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

    @Transactional
    public User updateAvatar(String username, String avatar) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setAvatar(avatar);
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
        return clientIpResolver.resolveCurrent();
    }

    /**
     * Resolve the account a login attempt refers to: exact username first, then
     * a UNIQUE case-insensitive match (mirrors CustomUserDetailsService so the
     * lockout bookkeeping tracks the same account the authentication uses).
     */
    private User resolveUserForLogin(String username) {
        return userRepository.findByUsername(username)
                .or(() -> {
                    var matches = userRepository.findAllByUsernameIgnoreCase(username);
                    return matches.size() == 1
                            ? java.util.Optional.of(matches.get(0))
                            : java.util.Optional.<User>empty();
                })
                .orElse(null);
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
                AuditEventType.AUTH_ORG_SELECTION,
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
                AuditEventType.CONFIG_PASSWORD_CHANGE,
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

        // Create access request (user will be null for new users who haven't registered yet).
        // Trim identity fields — approval may create an account from them verbatim.
        UserAccessRequest accessRequest = new UserAccessRequest();
        accessRequest.setUser(null);
        accessRequest.setOrganization(organization);
        accessRequest.setEmail(request.getEmail() == null ? null : request.getEmail().trim());
        accessRequest.setFirstName(request.getFirstName());
        accessRequest.setLastName(request.getLastName());
        accessRequest.setUsername(request.getUsername() == null ? null : request.getUsername().trim());
        accessRequest.setMessage(request.getMessage());
        accessRequest.setStatus(UserAccessRequest.RequestStatus.PENDING);
        accessRequest.setRequestDate(LocalDateTime.now());

        UserAccessRequest savedRequest = accessRequestRepository.save(accessRequest);

        logger.info("Access request created for {} to organization {} (ID: {})",
                request.getEmail(), organization.getName(), organization.getId());

        // Acknowledgment + admin notifications go out after commit, async with
        // retry (TransactionalEmailListener), which also loads the org admins.
        eventPublisher.publishEvent(
                new gov.nist.oscal.tools.api.email.EmailEvents.AccessRequestSubmittedEmails(savedRequest.getId()));
    }
}
