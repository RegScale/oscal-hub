package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.AuthRequest;
import gov.nist.oscal.tools.api.model.AuthResponse;
import gov.nist.oscal.tools.api.model.RegisterRequest;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
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

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
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
            // Check if email is already taken by another user
            userRepository.findByEmail(newEmail).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(user.getId())) {
                    throw new RuntimeException("Email already in use");
                }
            });
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
}
