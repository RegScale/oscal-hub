package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import gov.nist.oscal.tools.api.model.AccessRequestResponse;
import gov.nist.oscal.tools.api.model.OrganizationResponse;
import gov.nist.oscal.tools.api.model.ReviewAccessRequestRequest;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.OrgAnalyticsService;
import gov.nist.oscal.tools.api.service.OrganizationService;
import gov.nist.oscal.tools.api.service.UserAccessRequestService;
import gov.nist.oscal.tools.api.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for organization administrators
 * Provides endpoints for managing access requests and users within an organization
 */
@RestController
@RequestMapping("/api/org-admin")
@Tag(name = "Organization Admin", description = "APIs for organization administrators to manage users and access requests")
@SuppressWarnings("unused")
public class OrgAdminController {

    private static final Logger log = LoggerFactory.getLogger(OrgAdminController.class);

    @Autowired
    private gov.nist.oscal.tools.api.telemetry.TelemetryService telemetryService;

    @Autowired
    private UserAccessRequestService accessRequestService;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgAnalyticsService orgAnalyticsService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    // ========================================================================
    // Access Request Management
    // ========================================================================

    @Operation(
        summary = "Get pending access requests",
        description = "Retrieve all pending access requests for the admin's organization. ORG_ADMIN role required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Access requests retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Organization ID required"),
        @ApiResponse(responseCode = "403", description = "Access denied - ORG_ADMIN role required")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/access-requests")
    public ResponseEntity<?> getPendingAccessRequests(@RequestParam Long organizationId) {
        try {
            List<UserAccessRequest> requests = accessRequestService.getPendingRequests(organizationId);
            List<AccessRequestResponse> response = requests.stream()
                    .map(AccessRequestResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Get all access requests",
        description = "Retrieve all access requests (pending, approved, rejected) for the organization. ORG_ADMIN role required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Access requests retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Organization ID required"),
        @ApiResponse(responseCode = "403", description = "Access denied - ORG_ADMIN role required")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/access-requests/all")
    public ResponseEntity<?> getAllAccessRequests(@RequestParam Long organizationId) {
        try {
            List<UserAccessRequest> requests = accessRequestService.getAllRequests(organizationId);
            List<AccessRequestResponse> response = requests.stream()
                    .map(AccessRequestResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Approve access request",
        description = "Approve a pending access request. Creates user account if needed. ORG_ADMIN role required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Request approved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or request already processed"),
        @ApiResponse(responseCode = "403", description = "Access denied - ORG_ADMIN role required")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/access-requests/{id}/approve")
    public ResponseEntity<?> approveAccessRequest(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReviewAccessRequestRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Get current user ID (would need to inject UserRepository or service)
            // For now, we'll pass 0 and update this in Phase 4 with proper JWT handling
            Long reviewerId = getCurrentUserId(username);

            String notes = request != null ? request.getNotes() : null;
            UserAccessRequest approvedRequest = accessRequestService.approveRequest(id, reviewerId, notes);

            try {
                telemetryService.emit(gov.nist.oscal.tools.api.telemetry.EventNames.ACCESS_REQUEST_APPROVED, Map.of(
                        "request_id", String.valueOf(id),
                        "organization_id", approvedRequest.getOrganization() != null
                                ? String.valueOf(approvedRequest.getOrganization().getId()) : ""));
            } catch (Exception telEx) {
                log.debug("Telemetry emit failed (non-fatal): {}", telEx.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Access request approved successfully");
            response.put("request", new AccessRequestResponse(approvedRequest));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Reject access request",
        description = "Reject a pending access request with optional notes. ORG_ADMIN role required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Request rejected successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or request already processed"),
        @ApiResponse(responseCode = "403", description = "Access denied - ORG_ADMIN role required")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/access-requests/{id}/reject")
    public ResponseEntity<?> rejectAccessRequest(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReviewAccessRequestRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            Long reviewerId = getCurrentUserId(username);

            String notes = request != null ? request.getNotes() : null;
            UserAccessRequest rejectedRequest = accessRequestService.rejectRequest(id, reviewerId, notes);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Access request rejected");
            response.put("request", new AccessRequestResponse(rejectedRequest));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ========================================================================
    // User Management
    // ========================================================================

    @Operation(
        summary = "Get organization users",
        description = "Retrieve all users in the admin's organization. ORG_ADMIN role required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Organization ID required"),
        @ApiResponse(responseCode = "403", description = "Access denied - ORG_ADMIN role required")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<?> getOrganizationUsers(@RequestParam Long organizationId) {
        try {
            List<OrganizationMembership> memberships = userManagementService.getOrganizationUsers(organizationId);
            List<Map<String, Object>> users = memberships.stream()
                    .map(m -> {
                        Map<String, Object> user = new HashMap<>();
                        user.put("userId", m.getUser().getId());
                        user.put("username", m.getUser().getUsername());
                        user.put("email", m.getUser().getEmail());
                        user.put("role", m.getRole().toString());
                        user.put("status", m.getStatus().toString());
                        user.put("joinedAt", m.getJoinedAt());
                        user.put("updatedAt", m.getUpdatedAt());
                        return user;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(users);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Lock user account",
        description = "Temporarily lock a user's access to the organization. ORG_ADMIN role required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User locked successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or user already locked"),
        @ApiResponse(responseCode = "403", description = "Access denied - ORG_ADMIN role required")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/users/{userId}/lock")
    public ResponseEntity<?> lockUser(
            @PathVariable Long userId,
            @RequestParam Long organizationId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long adminId = getCurrentUserId(authentication.getName());

            OrganizationMembership membership = userManagementService.lockUser(userId, organizationId, adminId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User locked successfully");
            response.put("userId", userId);
            response.put("status", membership.getStatus().toString());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Unlock user account",
        description = "Unlock a previously locked user account. ORG_ADMIN role required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User unlocked successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Access denied - ORG_ADMIN role required")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/users/{userId}/unlock")
    public ResponseEntity<?> unlockUser(
            @PathVariable Long userId,
            @RequestParam Long organizationId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long adminId = getCurrentUserId(authentication.getName());

            OrganizationMembership membership = userManagementService.unlockUser(userId, organizationId, adminId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User unlocked successfully");
            response.put("userId", userId);
            response.put("status", membership.getStatus().toString());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Deactivate user account",
        description = "Permanently deactivate a user's access to the organization. ORG_ADMIN role required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or user already deactivated"),
        @ApiResponse(responseCode = "403", description = "Access denied - ORG_ADMIN role required")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/users/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(
            @PathVariable Long userId,
            @RequestParam Long organizationId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long adminId = getCurrentUserId(authentication.getName());

            OrganizationMembership membership = userManagementService.deactivateUser(userId, organizationId, adminId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User deactivated successfully");
            response.put("userId", userId);
            response.put("status", membership.getStatus().toString());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Reactivate user account",
        description = "Reactivate a previously deactivated user account. ORG_ADMIN role required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User reactivated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Access denied - ORG_ADMIN role required")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/users/{userId}/reactivate")
    public ResponseEntity<?> reactivateUser(
            @PathVariable Long userId,
            @RequestParam Long organizationId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long adminId = getCurrentUserId(authentication.getName());

            OrganizationMembership membership = userManagementService.reactivateUser(userId, organizationId, adminId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User reactivated successfully");
            response.put("userId", userId);
            response.put("status", membership.getStatus().toString());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Reset user password",
        description = "Reset a user's password. A single-use temporary password is generated and emailed to the user; it is never returned in the API response. ORG_ADMIN role required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Access denied - ORG_ADMIN role required")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<?> resetPassword(
            @PathVariable Long userId,
            @RequestParam Long organizationId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long adminId = getCurrentUserId(authentication.getName());

            Map<String, String> result = userManagementService.resetPassword(userId, organizationId, adminId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password reset. A temporary password has been emailed to the user; they must change it on next login.");
            response.put("username", result.get("username"));
            response.put("email", result.get("email"));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ========================================================================
    // Analytics
    // ========================================================================

    @Operation(
        summary = "Get analytics summary",
        description = "Retrieve a summary of organization analytics including member count, pending requests, logins and operations this month. ORG_ADMIN role required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics summary retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Organization not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - ORG_ADMIN role required")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/analytics/summary")
    public ResponseEntity<?> getAnalyticsSummary(@RequestParam Long organizationId) {
        try {
            Map<String, Object> summary = orgAnalyticsService.getAnalyticsSummary(organizationId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Failed to get analytics summary for org {}: {}", organizationId, e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Get full analytics",
        description = "Retrieve full organization analytics including active users, logins, operations, daily trends, operation breakdown, and top users. ORG_ADMIN role required."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Organization not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - ORG_ADMIN role required")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(@RequestParam Long organizationId) {
        try {
            Map<String, Object> analytics = orgAnalyticsService.getFullAnalytics(organizationId);
            return ResponseEntity.ok(analytics);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ========================================================================
    // Organization profile (name + description)
    // ========================================================================

    @Operation(
        summary = "Get organization profile",
        description = "Return the organization's profile (name, description, logo). ORG_ADMIN of the target org or SUPER_ADMIN."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization profile retrieved"),
        @ApiResponse(responseCode = "403", description = "Access denied - not an ORG_ADMIN of this organization"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/organizations/{organizationId}")
    public ResponseEntity<?> getOrganizationProfile(@PathVariable Long organizationId) {
        User caller = currentUser();
        if (!isOrgAdminOf(caller, organizationId)) {
            return forbidden();
        }
        try {
            Organization org = organizationService.getOrganization(organizationId);
            return ResponseEntity.ok(new OrganizationResponse(org));
        } catch (RuntimeException e) {
            return notFound(e.getMessage());
        }
    }

    @Operation(
        summary = "Update organization profile",
        description = "Update the organization's name and/or description. ORG_ADMIN of the target org or SUPER_ADMIN."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization profile updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request (e.g. name already taken)"),
        @ApiResponse(responseCode = "403", description = "Access denied - not an ORG_ADMIN of this organization"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PatchMapping("/organizations/{organizationId}")
    public ResponseEntity<?> updateOrganizationProfile(
            @PathVariable Long organizationId,
            @Valid @RequestBody UpdateOrgProfileRequest request) {
        User caller = currentUser();
        if (!isOrgAdminOf(caller, organizationId)) {
            return forbidden();
        }
        try {
            // updateOrganization treats nulls as "leave alone", which matches PATCH semantics.
            // We never let an ORG_ADMIN flip `active` from here — that's a platform-admin operation.
            Organization org = organizationService.updateOrganization(
                    organizationId,
                    request.name(),
                    request.description(),
                    null);
            return ResponseEntity.ok(new OrganizationResponse(org));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** PATCH body for {@link #updateOrganizationProfile}. Both fields optional; nulls mean "leave alone". */
    public record UpdateOrgProfileRequest(String name, String description) {}

    @Operation(
        summary = "Upload organization logo",
        description = "Upload a logo (PNG/JPG/SVG, max 2MB) for the organization. ORG_ADMIN of the target org or SUPER_ADMIN."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logo uploaded"),
        @ApiResponse(responseCode = "400", description = "Invalid file or file too large"),
        @ApiResponse(responseCode = "403", description = "Access denied - not an ORG_ADMIN of this organization"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/organizations/{organizationId}/logo")
    public ResponseEntity<?> uploadOrganizationLogo(
            @PathVariable Long organizationId,
            @RequestParam("file") MultipartFile file) {
        User caller = currentUser();
        if (!isOrgAdminOf(caller, organizationId)) {
            return forbidden();
        }
        try {
            String logoUrl = organizationService.uploadLogo(organizationId, file);
            Map<String, String> response = new HashMap<>();
            response.put("logoUrl", logoUrl);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload logo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        } catch (RuntimeException e) {
            return notFound(e.getMessage());
        }
    }

    @Operation(
        summary = "Delete organization logo",
        description = "Remove the logo from the organization. ORG_ADMIN of the target org or SUPER_ADMIN."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Logo deleted"),
        @ApiResponse(responseCode = "403", description = "Access denied - not an ORG_ADMIN of this organization"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/organizations/{organizationId}/logo")
    public ResponseEntity<?> deleteOrganizationLogo(@PathVariable Long organizationId) {
        User caller = currentUser();
        if (!isOrgAdminOf(caller, organizationId)) {
            return forbidden();
        }
        try {
            organizationService.deleteLogo(organizationId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return notFound(e.getMessage());
        }
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * True if the caller is a SUPER_ADMIN (platform-level bypass) or an ACTIVE
     * ORG_ADMIN member of the target organization.
     */
    private boolean isOrgAdminOf(User user, Long organizationId) {
        if (user.getGlobalRole() == User.GlobalRole.SUPER_ADMIN) return true;
        return membershipRepository.findByUserIdAndOrganizationId(user.getId(), organizationId)
                .filter(m -> m.getStatus() == OrganizationMembership.MembershipStatus.ACTIVE
                          && m.getRole() == OrganizationMembership.OrganizationRole.ORG_ADMIN)
                .isPresent();
    }

    private ResponseEntity<?> forbidden() {
        Map<String, String> body = new HashMap<>();
        body.put("error", "You are not an organization admin of this organization");
        return ResponseEntity.status(403).body(body);
    }

    private ResponseEntity<?> notFound(String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", message == null ? "Not found" : message);
        return ResponseEntity.status(404).body(body);
    }

    /**
     * Helper method to get current user ID from username
     * TODO: This will be improved in Phase 4 when we add userId to JWT claims
     */
    private Long getCurrentUserId(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return user.getId();
    }
}
