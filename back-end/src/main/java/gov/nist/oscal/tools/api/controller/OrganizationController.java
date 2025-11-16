package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.AddMemberRequest;
import gov.nist.oscal.tools.api.model.AssignAdminRequest;
import gov.nist.oscal.tools.api.model.OrganizationRequest;
import gov.nist.oscal.tools.api.model.OrganizationResponse;
import gov.nist.oscal.tools.api.model.UpdateMemberRoleRequest;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for organization management (Super Admin only)
 * Provides CRUD operations for organizations and admin assignment
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Organization Management", description = "Super Admin APIs for managing organizations")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserRepository userRepository;

    @Operation(
        summary = "Get all users",
        description = "Retrieve all users in the system. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> response = users.stream()
                .map(u -> {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", u.getId());
                    user.put("username", u.getUsername());
                    user.put("email", u.getEmail());
                    return user;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get all organizations",
        description = "Retrieve all organizations in the system (active and inactive). Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organizations retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizations() {
        List<Organization> organizations = organizationService.getAllOrganizations();
        List<OrganizationResponse> response = organizations.stream()
                .map(OrganizationResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get organization by ID",
        description = "Retrieve detailed information about a specific organization. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Organization not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/organizations/{id}")
    public ResponseEntity<?> getOrganization(@PathVariable Long id) {
        try {
            Organization organization = organizationService.getOrganization(id);
            OrganizationResponse response = new OrganizationResponse(organization);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    @Operation(
        summary = "Create new organization",
        description = "Create a new organization with name, description, and optional logo URL. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or organization name already exists"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/organizations")
    public ResponseEntity<?> createOrganization(@Valid @RequestBody OrganizationRequest request) {
        try {
            Organization organization = organizationService.createOrganization(
                    request.getName(),
                    request.getDescription(),
                    request.getLogoUrl()
            );
            OrganizationResponse response = new OrganizationResponse(organization);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Update organization",
        description = "Update organization details (name, description, active status). Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Organization not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/organizations/{id}")
    public ResponseEntity<?> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationRequest request) {
        try {
            Organization organization = organizationService.updateOrganization(
                    id,
                    request.getName(),
                    request.getDescription(),
                    request.getActive()
            );
            OrganizationResponse response = new OrganizationResponse(organization);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Deactivate organization",
        description = "Soft delete - marks organization as inactive. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Organization not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/organizations/{id}")
    public ResponseEntity<?> deactivateOrganization(@PathVariable Long id) {
        try {
            Organization organization = organizationService.deactivateOrganization(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Organization deactivated successfully");
            response.put("organization", new OrganizationResponse(organization));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    @Operation(
        summary = "Upload organization logo",
        description = "Upload a logo for the organization (PNG, JPG, SVG max 2MB). Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logo uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or file too large"),
        @ApiResponse(responseCode = "404", description = "Organization not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/organizations/{id}/logo")
    public ResponseEntity<?> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            String logoUrl = organizationService.uploadLogo(id, file);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logo uploaded successfully");
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
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    @Operation(
        summary = "Delete organization logo",
        description = "Remove the logo from an organization. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logo deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Organization not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/organizations/{id}/logo")
    public ResponseEntity<?> deleteLogo(@PathVariable Long id) {
        try {
            organizationService.deleteLogo(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logo deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    @Operation(
        summary = "Assign organization administrator",
        description = "Assign a user as administrator of an organization (ORG_ADMIN role). Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Administrator assigned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Organization or user not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/organizations/{id}/admins")
    public ResponseEntity<?> assignOrganizationAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AssignAdminRequest request) {
        try {
            OrganizationMembership membership = organizationService.assignOrganizationAdmin(
                    id,
                    request.getUserId()
            );
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Administrator assigned successfully");
            response.put("organizationId", membership.getOrganization().getId());
            response.put("userId", membership.getUser().getId());
            response.put("username", membership.getUser().getUsername());
            response.put("role", membership.getRole().toString());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Get organization members",
        description = "Retrieve all members of an organization. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Members retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Organization not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/organizations/{id}/members")
    public ResponseEntity<?> getOrganizationMembers(@PathVariable Long id) {
        try {
            List<OrganizationMembership> memberships = organizationService.getOrganizationMembers(id);
            List<Map<String, Object>> members = memberships.stream()
                    .map(m -> {
                        Map<String, Object> member = new HashMap<>();
                        member.put("membershipId", m.getId());
                        member.put("userId", m.getUser().getId());
                        member.put("username", m.getUser().getUsername());
                        member.put("email", m.getUser().getEmail());
                        member.put("role", m.getRole().toString());
                        member.put("status", m.getStatus().toString());
                        member.put("joinedAt", m.getJoinedAt());
                        return member;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(members);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    @Operation(
        summary = "Add member to organization",
        description = "Add a new member to the organization with specified role. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or user already a member"),
        @ApiResponse(responseCode = "404", description = "Organization or user not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/organizations/{id}/members")
    public ResponseEntity<?> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request) {
        try {
            OrganizationRole role = OrganizationRole.valueOf(request.getRole().toUpperCase());
            OrganizationMembership membership = organizationService.addUserToOrganization(
                    id,
                    request.getUserId(),
                    role
            );
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Member added successfully");
            response.put("membershipId", membership.getId());
            response.put("userId", membership.getUser().getId());
            response.put("username", membership.getUser().getUsername());
            response.put("role", membership.getRole().toString());
            response.put("status", membership.getStatus().toString());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid role. Must be 'USER' or 'ORG_ADMIN'");
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Update member role",
        description = "Update a member's role in the organization. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member role updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid role"),
        @ApiResponse(responseCode = "404", description = "Membership not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/organizations/{id}/members/{membershipId}")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long membershipId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        try {
            OrganizationRole role = OrganizationRole.valueOf(request.getRole().toUpperCase());
            OrganizationMembership membership = organizationService.updateMemberRole(membershipId, role);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Member role updated successfully");
            response.put("membershipId", membership.getId());
            response.put("role", membership.getRole().toString());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid role. Must be 'USER' or 'ORG_ADMIN'");
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    @Operation(
        summary = "Remove member from organization",
        description = "Remove a member from the organization. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member removed successfully"),
        @ApiResponse(responseCode = "404", description = "Membership not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/organizations/{id}/members/{membershipId}")
    public ResponseEntity<?> removeMember(
            @PathVariable Long id,
            @PathVariable Long membershipId) {
        try {
            organizationService.removeMember(membershipId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Member removed successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }
}
