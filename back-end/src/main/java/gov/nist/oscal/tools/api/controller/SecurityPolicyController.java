package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.SecurityPolicy;
import gov.nist.oscal.tools.api.model.SecurityPolicyResponse;
import gov.nist.oscal.tools.api.model.SecurityPolicyUpdateRequest;
import gov.nist.oscal.tools.api.service.AuditLogCleanupService;
import gov.nist.oscal.tools.api.service.AuditLogService;
import gov.nist.oscal.tools.api.service.SecurityPolicyService;
import gov.nist.oscal.tools.api.model.AuditEventType;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for Security Policy management.
 * <p>
 * All endpoints require SUPER_ADMIN role.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/security-policy")
@Tag(name = "Security Policy", description = "Security policy management (Super Admin only)")
@Hidden
public class SecurityPolicyController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityPolicyController.class);

    private final SecurityPolicyService securityPolicyService;
    private final AuditLogService auditLogService;
    private final AuditLogCleanupService auditLogCleanupService;

    public SecurityPolicyController(
            SecurityPolicyService securityPolicyService,
            AuditLogService auditLogService,
            AuditLogCleanupService auditLogCleanupService) {
        this.securityPolicyService = securityPolicyService;
        this.auditLogService = auditLogService;
        this.auditLogCleanupService = auditLogCleanupService;
    }

    /**
     * Get current security policy.
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get security policy", description = "Get the current security policy settings")
    public ResponseEntity<SecurityPolicyResponse> getPolicy() {
        SecurityPolicy policy = securityPolicyService.getPolicy();
        return ResponseEntity.ok(SecurityPolicyResponse.fromEntity(policy));
    }

    /**
     * Update security policy.
     */
    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update security policy", description = "Update the security policy settings")
    public ResponseEntity<SecurityPolicyResponse> updatePolicy(
            @Valid @RequestBody SecurityPolicyUpdateRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String username = authentication.getName();

        try {
            SecurityPolicy updated = securityPolicyService.updatePolicy(request, username);

            // Log the change
            auditLogService.logConfigChange(
                    username,
                    "Security policy updated: mfaRequired=" + updated.getMfaRequired() +
                            ", passwordMinLength=" + updated.getPasswordMinLength() +
                            ", passwordRotationDays=" + updated.getPasswordRotationDays() +
                            ", auditLogRetentionDays=" + updated.getAuditLogRetentionDays(),
                    httpRequest);

            logger.info("Security policy updated by {}", username);

            return ResponseEntity.ok(SecurityPolicyResponse.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Trigger manual audit log cleanup.
     */
    @PostMapping("/cleanup-logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Trigger audit log cleanup",
            description = "Manually trigger cleanup of audit logs older than retention period")
    public ResponseEntity<Map<String, Object>> triggerLogCleanup(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String username = authentication.getName();

        int deletedCount = auditLogCleanupService.runCleanupNow();

        auditLogService.logConfigChange(
                username,
                "Manual audit log cleanup triggered. Deleted " + deletedCount + " events.",
                httpRequest);

        return ResponseEntity.ok(Map.of(
                "message", "Audit log cleanup completed",
                "deletedCount", deletedCount,
                "retentionDays", securityPolicyService.getAuditLogRetentionDays()
        ));
    }
}
