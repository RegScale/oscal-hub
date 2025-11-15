package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import gov.nist.oscal.tools.api.model.AccessRequestResponse;
import gov.nist.oscal.tools.api.model.ReviewAccessRequestRequest;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.UserAccessRequestService;
import gov.nist.oscal.tools.api.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
public class OrgAdminController {

    @Autowired
    private UserAccessRequestService accessRequestService;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private UserRepository userRepository;

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
        description = "Reset a user's password and generate a temporary password. Returns temp password (should be emailed in production). ORG_ADMIN role required."
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
            response.put("message", "Password reset successfully. User must change password on next login.");
            response.put("tempPassword", result.get("tempPassword"));
            response.put("username", result.get("username"));
            response.put("email", result.get("email"));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
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
