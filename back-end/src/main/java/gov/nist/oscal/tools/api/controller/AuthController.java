package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.AuthRequest;
import gov.nist.oscal.tools.api.model.AuthResponse;
import gov.nist.oscal.tools.api.model.RegisterRequest;
import gov.nist.oscal.tools.api.model.ServiceAccountTokenRequest;
import gov.nist.oscal.tools.api.model.ServiceAccountTokenResponse;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.AuthService;
import gov.nist.oscal.tools.api.service.FileValidationService;
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
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration APIs")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FileValidationService fileValidationService;

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
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
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
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid username or password");
            return ResponseEntity.status(401).body(error);
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
        response.put("street", user.getStreet());
        response.put("city", user.getCity());
        response.put("state", user.getState());
        response.put("zip", user.getZip());
        response.put("title", user.getTitle());
        response.put("organization", user.getOrganization());
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("logo", user.getLogo());

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

        // Generate new token
        org.springframework.security.core.userdetails.UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        user.getEnabled(),
                        true, true, true,
                        java.util.Collections.emptyList()
                );

        String newToken = authService.generateToken(userDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("token", newToken);
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("userId", user.getId());
        response.put("street", user.getStreet());
        response.put("city", user.getCity());
        response.put("state", user.getState());
        response.put("zip", user.getZip());
        response.put("title", user.getTitle());
        response.put("organization", user.getOrganization());
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("logo", user.getLogo());

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

        try {
            String username = authentication.getName();
            User user = authService.updateProfile(username, updates);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("street", user.getStreet());
            response.put("city", user.getCity());
            response.put("state", user.getState());
            response.put("zip", user.getZip());
            response.put("title", user.getTitle());
            response.put("organization", user.getOrganization());
            response.put("phoneNumber", user.getPhoneNumber());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
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
}
