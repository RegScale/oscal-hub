package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.AuditLogService;
import gov.nist.oscal.tools.api.service.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for MFA (Multi-Factor Authentication) operations.
 */
@RestController
@RequestMapping("/api/auth/mfa")
@Tag(name = "MFA", description = "Multi-Factor Authentication operations")
public class MfaController {

    private static final Logger logger = LoggerFactory.getLogger(MfaController.class);

    private final MfaService mfaService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public MfaController(
            MfaService mfaService,
            JwtUtil jwtUtil,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.mfaService = mfaService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Initiate MFA setup - generates QR code and secret.
     * Accepts either regular JWT authentication OR an MFA setup token for users
     * who are required to set up MFA during login flow.
     */
    @PostMapping("/setup/initiate")
    @Operation(summary = "Initiate MFA setup", description = "Generate QR code and secret for authenticator app")
    public ResponseEntity<?> initiateMfaSetup(
            Authentication authentication,
            HttpServletRequest request) {

        User user = null;
        String username = null;

        // First, try to get user from MFA setup token in Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // Check if this is an MFA setup token (from login flow)
                if (jwtUtil.isMfaSetupToken(token)) {
                    username = jwtUtil.extractUsername(token);
                    Long userId = jwtUtil.extractUserId(token);
                    user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    logger.info("MFA setup initiated via setup token for user: {}", username);
                }
            } catch (Exception e) {
                logger.debug("Token is not a valid MFA setup token, will try regular auth");
            }
        }

        // Fall back to regular authentication (for already logged-in users)
        if (user == null && authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            username = authentication.getName();
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

        // If neither authentication method worked, return unauthorized
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required. Please provide a valid token."));
        }

        if (Boolean.TRUE.equals(user.getMfaEnabled()) && Boolean.TRUE.equals(user.getMfaSetupCompleted())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "MFA is already enabled for this account"));
        }

        MfaService.MfaSetupData setupData = mfaService.initiateMfaSetup(user.getId());

        // Generate setup token (short-lived, for completing setup)
        String setupToken = jwtUtil.generateMfaSetupToken(username, user.getId());

        MfaSetupResponse response = new MfaSetupResponse(
                setupData.qrCodeDataUri(),
                setupData.secret(),
                setupData.formattedSecret(),
                setupToken
        );

        auditLogService.logSecurityEvent(
                AuditEventType.MFA_SETUP_INITIATED,
                username,
                "MFA setup initiated",
                request);

        return ResponseEntity.ok(response);
    }

    /**
     * Complete MFA setup by verifying the first TOTP code.
     */
    @PostMapping("/setup/complete")
    @Operation(summary = "Complete MFA setup", description = "Verify TOTP code and enable MFA")
    public ResponseEntity<?> completeMfaSetup(
            @Valid @RequestBody MfaSetupCompleteRequest request,
            HttpServletRequest httpRequest) {

        try {
            // Validate setup token
            if (!jwtUtil.isMfaSetupToken(request.getSetupToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired setup token"));
            }

            String username = jwtUtil.extractUsername(request.getSetupToken());
            Long userId = jwtUtil.extractUserId(request.getSetupToken());

            // Complete MFA setup
            List<String> backupCodes = mfaService.completeMfaSetup(userId, request.getTotpCode());

            // Generate full JWT token
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String token = jwtUtil.generateToken(user);

            auditLogService.logSecurityEvent(
                    AuditEventType.MFA_SETUP_COMPLETED,
                    username,
                    "MFA setup completed successfully",
                    httpRequest);

            return ResponseEntity.ok(new MfaSetupCompleteResponse(token, backupCodes));

        } catch (MfaService.InvalidMfaCodeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid TOTP code"));
        } catch (Exception e) {
            logger.error("MFA setup completion failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "MFA setup failed"));
        }
    }

    /**
     * Verify TOTP code during login.
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify MFA code", description = "Verify TOTP code during login")
    public ResponseEntity<?> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest) {

        try {
            // Validate MFA token
            if (!jwtUtil.isMfaPartialToken(request.getMfaToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired MFA token"));
            }

            String username = jwtUtil.extractUsername(request.getMfaToken());
            Long userId = jwtUtil.extractUserId(request.getMfaToken());

            // Verify TOTP code
            if (!mfaService.verifyTotpCode(userId, request.getTotpCode())) {
                auditLogService.logSecurityEvent(
                        AuditEventType.MFA_VERIFICATION_FAILURE,
                        username,
                        "MFA verification failed - invalid code",
                        httpRequest);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid TOTP code"));
            }

            // Generate full JWT token
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String token = jwtUtil.generateToken(user);

            auditLogService.logSecurityEvent(
                    AuditEventType.MFA_VERIFICATION_SUCCESS,
                    username,
                    "MFA verification successful",
                    httpRequest);

            return ResponseEntity.ok(new AuthResponse(token, user));

        } catch (Exception e) {
            logger.error("MFA verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "MFA verification failed"));
        }
    }

    /**
     * Verify backup code during login.
     */
    @PostMapping("/verify-backup")
    @Operation(summary = "Verify backup code", description = "Use a backup code for login when authenticator is unavailable")
    public ResponseEntity<?> verifyBackupCode(
            @Valid @RequestBody MfaBackupCodeRequest request,
            HttpServletRequest httpRequest) {

        try {
            // Validate MFA token
            if (!jwtUtil.isMfaPartialToken(request.getMfaToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired MFA token"));
            }

            String username = jwtUtil.extractUsername(request.getMfaToken());
            Long userId = jwtUtil.extractUserId(request.getMfaToken());

            // Verify backup code
            if (!mfaService.verifyBackupCode(userId, request.getBackupCode())) {
                auditLogService.logSecurityEvent(
                        AuditEventType.MFA_VERIFICATION_FAILURE,
                        username,
                        "MFA verification failed - invalid backup code",
                        httpRequest);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid backup code"));
            }

            // Generate full JWT token
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String token = jwtUtil.generateToken(user);

            auditLogService.logSecurityEvent(
                    AuditEventType.MFA_BACKUP_CODE_USED,
                    username,
                    "Backup code used for MFA verification",
                    httpRequest);

            // Warn if backup codes are low
            int remaining = mfaService.getRemainingBackupCodesCount(userId);
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("token", token);
            response.put("user", user);
            response.put("backupCodesRemaining", remaining);
            if (remaining <= 3) {
                response.put("warning", "You have " + remaining + " backup codes remaining. Consider regenerating.");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Backup code verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Backup code verification failed"));
        }
    }

    /**
     * Get count of remaining backup codes.
     */
    @GetMapping("/backup-codes/count")
    @Operation(summary = "Get backup codes count", description = "Get the number of remaining unused backup codes")
    public ResponseEntity<Map<String, Integer>> getBackupCodesCount(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        int count = mfaService.getRemainingBackupCodesCount(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Regenerate backup codes (requires TOTP verification).
     */
    @PostMapping("/backup-codes/regenerate")
    @Operation(summary = "Regenerate backup codes", description = "Generate new backup codes (invalidates old codes)")
    public ResponseEntity<?> regenerateBackupCodes(
            @RequestParam String totpCode,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify TOTP before regenerating
        if (!mfaService.verifyTotpCode(user.getId(), totpCode)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid TOTP code"));
        }

        List<String> newCodes = mfaService.generateBackupCodes(user.getId());

        auditLogService.logSecurityEvent(
                AuditEventType.MFA_BACKUP_CODES_REGENERATED,
                username,
                "Backup codes regenerated",
                httpRequest);

        return ResponseEntity.ok(Map.of("backupCodes", newCodes));
    }

    /**
     * Disable MFA for the current user.
     */
    @DeleteMapping("/disable")
    @Operation(summary = "Disable MFA", description = "Disable MFA for your account (requires TOTP verification)")
    public ResponseEntity<?> disableMfa(
            @RequestParam String totpCode,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify TOTP before disabling
        if (!mfaService.verifyTotpCode(user.getId(), totpCode)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid TOTP code"));
        }

        mfaService.disableMfa(user.getId());

        auditLogService.logSecurityEvent(
                AuditEventType.MFA_DISABLED,
                username,
                "MFA disabled by user",
                httpRequest);

        return ResponseEntity.ok(Map.of("message", "MFA has been disabled"));
    }

    /**
     * Admin endpoint to disable MFA for another user.
     */
    @DeleteMapping("/admin/users/{userId}/mfa")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Admin: Disable user MFA", description = "Disable MFA for a user (Super Admin only)")
    public ResponseEntity<?> adminDisableMfa(
            @PathVariable Long userId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String adminUsername = authentication.getName();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        mfaService.disableMfa(userId);

        auditLogService.logSecurityEvent(
                AuditEventType.MFA_DISABLED,
                adminUsername,
                "MFA disabled for user " + targetUser.getUsername() + " by admin",
                httpRequest);

        return ResponseEntity.ok(Map.of(
                "message", "MFA disabled for user " + targetUser.getUsername()
        ));
    }

    /**
     * Check MFA status for current user.
     */
    @GetMapping("/status")
    @Operation(summary = "Get MFA status", description = "Check if MFA is enabled for your account")
    public ResponseEntity<Map<String, Object>> getMfaStatus(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of(
                "mfaEnabled", user.getMfaEnabled(),
                "mfaSetupCompleted", user.getMfaSetupCompleted(),
                "backupCodesRemaining", mfaService.getRemainingBackupCodesCount(user.getId())
        ));
    }
}
