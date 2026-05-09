package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.OrganizationNameInUseException;
import gov.nist.oscal.tools.api.model.AuthRequest;
import gov.nist.oscal.tools.api.model.AuthResponse;
import gov.nist.oscal.tools.api.model.ChangePasswordRequest;
import gov.nist.oscal.tools.api.model.RegisterRequest;
import gov.nist.oscal.tools.api.model.RequestAccessRequest;
import gov.nist.oscal.tools.api.model.ServiceAccountTokenRequest;
import gov.nist.oscal.tools.api.model.ServiceAccountTokenResponse;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.AuthService;
import gov.nist.oscal.tools.api.service.FileValidationService;
import gov.nist.oscal.tools.api.telemetry.EventNames;
import gov.nist.oscal.tools.api.telemetry.TelemetryService;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration APIs")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FileValidationService fileValidationService;

    @Autowired
    private TelemetryService telemetryService;

    @Operation(
        summary = "Register new user",
        description = "Register a new user account with username, password, and email"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registration successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request or username/email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (OrganizationNameInUseException e) {
            // Structured response specifically for org-name collisions so the frontend can
            // render a field-level error. Other exceptions from the service
            // (UsernameAlreadyExistsException, IllegalArgumentException,
            // DataIntegrityViolationException) are mapped by GlobalExceptionHandler. The
            // previous "catch (RuntimeException) → e.getMessage()" clause was removed
            // because it leaked raw Hibernate/JDBC details to clients.
            Map<String, String> error = new HashMap<>();
            error.put("error", "ORGANIZATION_NAME_IN_USE");
            error.put("field", "organizationName");
            error.put("message", "That organization name is already taken. Try another.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }

    @Operation(
        summary = "Login",
        description = "Authenticate user and receive JWT token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.login(request);
            try {
                telemetryService.emit(EventNames.AUTH_LOGIN_SUCCEEDED, Map.of(
                        "user_id", response.getUserId() != null ? String.valueOf(response.getUserId()) : "",
                        "mfa_required", Boolean.toString(Boolean.TRUE.equals(response.getMfaRequired()))
                ));
            } catch (Exception telEx) {
                logger.debug("Telemetry emit failed (non-fatal): {}", telEx.getMessage());
            }
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Login failed for user {}: {} - {}", request.getUsername(), e.getClass().getSimpleName(), e.getMessage(), e);
            try {
                telemetryService.emit(EventNames.AUTH_LOGIN_FAILED, Map.of(
                        "attempted_username_sha256", sha256(request.getUsername()),
                        "reason", "bad_credentials"
                ));
            } catch (Exception telEx) {
                logger.debug("Telemetry emit failed (non-fatal): {}", telEx.getMessage());
            }
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null && e.getMessage().contains("locked")
                ? e.getMessage()
                : "Invalid username or password");
            return ResponseEntity.status(401).body(error);
        }
    }

    private static String sha256(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return "sha256-error";
        }
    }

    @Operation(
        summary = "Get current user",
        description = "Get information about the currently authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User information retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        String username = authentication.getName();
        User user = authService.getCurrentUser(username);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("street", user.getStreet());
        response.put("city", user.getCity());
        response.put("state", user.getState());
        response.put("zip", user.getZip());
        response.put("title", user.getTitle());
        response.put("organization", user.getOrganization());
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("logo", user.getLogo());
        response.put("avatar", user.getAvatar());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Logout",
        description = "Logout current user (client should discard JWT token)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // With JWT, logout is handled client-side by removing the token
        // This endpoint is provided for consistency but doesn't need to do anything server-side
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Refresh token",
        description = "Refresh JWT token to extend session"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        String username = authentication.getName();
        User user = authService.getCurrentUser(username);

        // Extract org-context claims from the *current* token so we can
        // preserve them in the refreshed one. Previously this endpoint
        // generated a token with an empty claims map, which silently
        // stripped organizationId / orgRole every 5 minutes (the
        // frontend's auto-refresh cadence). After enough refreshes the
        // user's authorities collapsed to just ROLE_USER + globalRole and
        // any @PreAuthorize check that required ROLE_ORG_ADMIN started
        // returning 403. Re-issuing with the same claims keeps the
        // session intact across the refresh.
        String authHeader = ((org.springframework.web.context.request.ServletRequestAttributes)
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                .getRequest().getHeader("Authorization");

        Long organizationId = null;
        String orgRole = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String currentToken = authHeader.substring(7);
            try {
                organizationId = jwtUtil.extractOrganizationId(currentToken);
                orgRole = jwtUtil.extractOrganizationRole(currentToken);
            } catch (RuntimeException ignored) {
                // Token may have been issued by an older flow without these
                // claims; leave nulls and the refreshed token will simply
                // omit them too — same as before.
            }
        }

        String globalRoleName = user.getGlobalRole() != null ? user.getGlobalRole().name() : null;

        String newToken;
        if (organizationId != null && orgRole != null) {
            newToken = jwtUtil.generateTokenWithOrgContext(
                    user.getUsername(),
                    user.getId(),
                    globalRoleName,
                    organizationId,
                    orgRole,
                    user.getMustChangePassword()
            );
        } else {
            // No org context (e.g., super admin who hasn't selected an org,
            // or pre-org-selection state). Fall back to the legacy empty-
            // claims token; the JwtAuthenticationFilter still grants
            // ROLE_<globalRole> from the user record, so SUPER_ADMIN access
            // is unaffected.
            org.springframework.security.core.userdetails.UserDetails userDetails =
                    new org.springframework.security.core.userdetails.User(
                            user.getUsername(),
                            user.getPassword(),
                            user.getEnabled(),
                            true, true, true,
                            java.util.Collections.emptyList()
                    );
            newToken = authService.generateToken(userDetails);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("token", newToken);
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("userId", user.getId());
        response.put("globalRole", globalRoleName);
        response.put("organizationId", organizationId);
        response.put("orgRole", orgRole);
        response.put("street", user.getStreet());
        response.put("city", user.getCity());
        response.put("state", user.getState());
        response.put("zip", user.getZip());
        response.put("title", user.getTitle());
        response.put("organization", user.getOrganization());
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("logo", user.getLogo());
        response.put("avatar", user.getAvatar());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Update user profile",
        description = "Update email or password for the current user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        // Exceptions from the service are mapped by GlobalExceptionHandler.
        String username = authentication.getName();
        User user = authService.updateProfile(username, updates);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profile updated successfully");
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("street", user.getStreet());
        response.put("city", user.getCity());
        response.put("state", user.getState());
        response.put("zip", user.getZip());
        response.put("title", user.getTitle());
        response.put("organization", user.getOrganization());
        response.put("phoneNumber", user.getPhoneNumber());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Upload user logo",
        description = "Upload or update logo for the current user. Logo should be provided as base64-encoded data URL."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logo uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid logo data"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/logo")
    public ResponseEntity<?> uploadLogo(@RequestBody Map<String, String> logoData) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        try {
            String username = authentication.getName();
            String logo = logoData.get("logo");

            // Comprehensive logo validation
            fileValidationService.validateBase64Logo(logo);

            User user = authService.updateLogo(username, logo);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logo uploaded successfully");
            response.put("logo", user.getLogo());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Validation error - return 400 Bad Request
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            // Other errors - return 500 Internal Server Error
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload logo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @Operation(
        summary = "Upload user avatar",
        description = "Upload or update the avatar image shown in the header for the current user. Avatar should be provided as a base64-encoded data URL. Distinct from /logo, which manages the company logo used in authorization templates."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid avatar data"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestBody Map<String, String> avatarData) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        try {
            String username = authentication.getName();
            String avatar = avatarData.get("avatar");

            // Reuse the same image validation as /logo: same MIME whitelist,
            // size cap, and magic-number check apply to avatars.
            fileValidationService.validateBase64Logo(avatar);

            User user = authService.updateAvatar(username, avatar);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Avatar uploaded successfully");
            response.put("avatar", user.getAvatar());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload avatar: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @Operation(
        summary = "Generate Service Account Token",
        description = "Generate a service account JWT token with custom name and expiration. This token is not stored and must be saved by the user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/service-account-token")
    public ResponseEntity<?> generateServiceAccountToken(@Valid @RequestBody ServiceAccountTokenRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        try {
            String username = authentication.getName();

            // Calculate expiration date
            java.util.Date expirationDate = authService.generateServiceAccountToken(
                    username,
                    request.getTokenName(),
                    request.getExpirationDays()
            );

            // Generate the token using JwtUtil
            String token = jwtUtil.generateServiceAccountToken(
                    username,
                    request.getTokenName(),
                    request.getExpirationDays()
            );

            // Format the expiration date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            String expiresAt = dateFormat.format(expirationDate);

            ServiceAccountTokenResponse response = new ServiceAccountTokenResponse(
                    token,
                    request.getTokenName(),
                    username,
                    expiresAt,
                    request.getExpirationDays()
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ========================================================================
    // Organization Selection (Multi-Tenant)
    // ========================================================================

    @Operation(
        summary = "Get active organizations",
        description = "Get list of all active organizations (public endpoint for NASCAR page)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organizations retrieved successfully")
    })
    @GetMapping("/organizations")
    public ResponseEntity<?> getActiveOrganizations() {
        try {
            java.util.List<Map<String, Object>> organizations = authService.getActiveOrganizations();
            return ResponseEntity.ok(organizations);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Get my organizations",
        description = "Get organizations the current user has access to (with logos)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organizations retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/my-organizations")
    public ResponseEntity<?> getMyOrganizations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        try {
            // Extract userId from JWT token
            String authHeader = ((org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                    .getRequest().getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long userId = jwtUtil.extractUserId(token);

                if (userId == null) {
                    // Fallback to getting user by username
                    String username = authentication.getName();
                    User user = authService.getCurrentUser(username);
                    userId = user.getId();
                }

                java.util.List<Map<String, Object>> organizations = authService.getMyOrganizations(userId);
                return ResponseEntity.ok(organizations);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No authorization token found");
                return ResponseEntity.status(401).body(error);
            }
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Select organization",
        description = "Select an organization after initial login (generates full JWT with org context)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization selected successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid organization or no access"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/select-organization/{organizationId}")
    public ResponseEntity<?> selectOrganization(@PathVariable Long organizationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        try {
            // Extract userId from JWT token
            String authHeader = ((org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                    .getRequest().getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long userId = jwtUtil.extractUserId(token);

                if (userId == null) {
                    // Fallback to getting user by username
                    String username = authentication.getName();
                    User user = authService.getCurrentUser(username);
                    userId = user.getId();
                }

                Map<String, Object> response = authService.selectOrganization(userId, organizationId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No authorization token found");
                return ResponseEntity.status(401).body(error);
            }
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Switch organization",
        description = "Switch to a different organization (re-issues JWT with new org context)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization switched successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid organization or no access"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/switch-organization/{organizationId}")
    public ResponseEntity<?> switchOrganization(@PathVariable Long organizationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        try {
            // Extract userId from JWT token
            String authHeader = ((org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                    .getRequest().getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long userId = jwtUtil.extractUserId(token);

                if (userId == null) {
                    // Fallback to getting user by username
                    String username = authentication.getName();
                    User user = authService.getCurrentUser(username);
                    userId = user.getId();
                }

                Map<String, Object> response = authService.switchOrganization(userId, organizationId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No authorization token found");
                return ResponseEntity.status(401).body(error);
            }
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Request access to organization",
        description = "Submit an access request to join an organization (public endpoint)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Access request submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or organization not found")
    })
    @PostMapping("/request-access")
    public ResponseEntity<?> requestAccess(@Valid @RequestBody RequestAccessRequest request) {
        try {
            authService.requestAccess(request);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Access request submitted successfully. An administrator will review your request.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Get my pending access requests",
        description = "Get all pending access requests for the current user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pending requests retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/my-pending-requests")
    public ResponseEntity<?> getMyPendingRequests() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        try {
            // Extract userId from JWT token
            String authHeader = ((org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                    .getRequest().getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long userId = jwtUtil.extractUserId(token);

                if (userId == null) {
                    // Fallback to getting user by username
                    String username = authentication.getName();
                    User user = authService.getCurrentUser(username);
                    userId = user.getId();
                }

                java.util.List<Map<String, Object>> requests = authService.getMyPendingRequests(userId);
                return ResponseEntity.ok(requests);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No authorization token found");
                return ResponseEntity.status(401).body(error);
            }
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Change password",
        description = "Change current user's password (required for forced password changes)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or incorrect old password"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        try {
            // Extract userId from JWT token
            String authHeader = ((org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                    .getRequest().getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long userId = jwtUtil.extractUserId(token);

                if (userId == null) {
                    // Fallback to getting user by username
                    String username = authentication.getName();
                    User user = authService.getCurrentUser(username);
                    userId = user.getId();
                }

                authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());

                Map<String, String> response = new HashMap<>();
                response.put("message", "Password changed successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No authorization token found");
                return ResponseEntity.status(401).body(error);
            }
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
