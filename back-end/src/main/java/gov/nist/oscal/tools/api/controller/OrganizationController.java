package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import gov.nist.oscal.tools.api.model.AccessRequestResponse;
import gov.nist.oscal.tools.api.model.AddMemberRequest;
import gov.nist.oscal.tools.api.model.AssignAdminRequest;
import gov.nist.oscal.tools.api.model.OrganizationRequest;
import gov.nist.oscal.tools.api.model.OrganizationResponse;
import gov.nist.oscal.tools.api.model.OrganizationSummaryResponse;
import gov.nist.oscal.tools.api.model.UpdateMemberRoleRequest;
import gov.nist.oscal.tools.api.repository.AuditEventRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserAccessRequestRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.OrganizationService;
import gov.nist.oscal.tools.api.service.UserAccessRequestService;
import gov.nist.oscal.tools.api.service.UserManagementService;
import io.swagger.v3.oas.annotations.Hidden;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
@Hidden
public class OrganizationController {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationController.class);

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private UserAccessRequestService userAccessRequestService;

    @Autowired
    private UserAccessRequestRepository userAccessRequestRepository;

    @Autowired
    private OrganizationMembershipRepository organizationMembershipRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private UserManagementService userManagementService;

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
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> response = users.stream()
                .map(u -> {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", u.getId());
                    user.put("username", u.getUsername());
                    user.put("email", u.getEmail());
                    user.put("firstName", u.getFirstName());
                    user.put("lastName", u.getLastName());
                    user.put("globalRole", u.getGlobalRole().toString());
                    user.put("enabled", u.getEnabled());

                    List<Map<String, Object>> orgs = organizationMembershipRepository.findByUser(u).stream()
                            .map(m -> {
                                Map<String, Object> org = new HashMap<>();
                                org.put("id", m.getOrganization().getId());
                                org.put("name", m.getOrganization().getName());
                                org.put("role", m.getRole().toString());
                                return org;
                            })
                            .collect(Collectors.toList());
                    user.put("organizations", orgs);

                    return user;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Archive user", description = "Disable a user account so they can no longer log in. Super Admin only.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/users/{userId}/archive")
    public ResponseEntity<?> archiveUser(@PathVariable Long userId) {
        try {
            Long adminId = currentAdminId();
            if (adminId != null && adminId.equals(userId)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "You cannot archive your own account");
                return ResponseEntity.badRequest().body(error);
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            user.setEnabled(false);
            userRepository.save(user);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User archived. They can no longer log in.");
            response.put("username", user.getUsername());
            response.put("enabled", false);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Unarchive user", description = "Re-enable an archived user account. Super Admin only.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/users/{userId}/unarchive")
    public ResponseEntity<?> unarchiveUser(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            user.setEnabled(true);
            userRepository.save(user);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User unarchived. They can log in again.");
            response.put("username", user.getUsername());
            response.put("enabled", true);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Reset user password (admin)",
            description = "Reset a user's password. Body fields: password (optional, auto-generated if omitted), notify (default true; if false, the plaintext password is returned in the response so the admin can deliver it out-of-band). Super Admin only.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long userId, @RequestBody(required = false) Map<String, Object> body) {
        try {
            Long adminId = currentAdminId();
            if (adminId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Could not resolve current admin user");
                return ResponseEntity.status(401).body(error);
            }
            String customPassword = body != null && body.get("password") != null ? body.get("password").toString() : null;
            boolean notify = body == null || body.get("notify") == null || Boolean.parseBoolean(body.get("notify").toString());

            Map<String, String> result = userManagementService.resetPasswordByAdmin(userId, adminId, customPassword, notify);
            Map<String, Object> response = new HashMap<>();
            response.put("username", result.get("username"));
            response.put("email", result.get("email"));
            if (notify) {
                response.put("message", "Password reset. A temporary password has been emailed to the user; they must change it on next login.");
                response.put("notified", true);
            } else {
                response.put("message", "Password reset. Deliver the password to the user securely; they will be required to change it on next login.");
                response.put("notified", false);
                response.put("password", result.get("password"));
            }
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "User analytics", description = "Aggregated user signups/logins/activity for the admin dashboard. Super Admin only.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/users/analytics")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getUserAnalytics() {
        long t0 = System.currentTimeMillis();
        logger.info("getUserAnalytics: starting");
        final int monthsBack = 12;
        final int staleDays = 90;

        // ---- Build calendar of last 12 months (oldest → newest, inclusive of current month) ----
        YearMonth currentMonth = YearMonth.now();
        List<String> monthKeys = new ArrayList<>(monthsBack);
        for (int i = monthsBack - 1; i >= 0; i--) {
            monthKeys.add(currentMonth.minusMonths(i).toString()); // YYYY-MM
        }
        LocalDateTime since12mo = currentMonth.minusMonths(monthsBack - 1).atDay(1).atStartOfDay();

        // ---- New users by month ----
        logger.info("getUserAnalytics: querying countNewUsersByMonth ({} ms)", System.currentTimeMillis() - t0);
        Map<String, Long> newUsersBucket = new LinkedHashMap<>();
        monthKeys.forEach(k -> newUsersBucket.put(k, 0L));
        for (Object[] row : userRepository.countNewUsersByMonth(since12mo)) {
            String ym = String.valueOf(row[0]);
            long count = ((Number) row[1]).longValue();
            newUsersBucket.put(ym, count);
        }

        // ---- Logins by month ----
        logger.info("getUserAnalytics: querying countLoginsByMonth ({} ms)", System.currentTimeMillis() - t0);
        Map<String, Long> loginsBucket = new LinkedHashMap<>();
        monthKeys.forEach(k -> loginsBucket.put(k, 0L));
        for (Object[] row : auditEventRepository.countLoginsByMonth(since12mo)) {
            String ym = String.valueOf(row[0]);
            long count = ((Number) row[1]).longValue();
            loginsBucket.put(ym, count);
        }

        // ---- Most active organizations (last 90 days, single native aggregation) ----
        logger.info("getUserAnalytics: querying topOrganizationsByEventCount ({} ms)", System.currentTimeMillis() - t0);
        LocalDateTime since90d = LocalDate.now().minusDays(staleDays).atStartOfDay();
        List<Map<String, Object>> topOrgs = auditEventRepository
                .topOrganizationsByEventCount(since90d)
                .stream()
                .limit(5)
                .map(row -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", row[0]);
                    r.put("name", row[1]);
                    r.put("eventCount", ((Number) row[2]).longValue());
                    return r;
                })
                .collect(Collectors.toList());

        // ---- % stale users (no login in last 90 days) ----
        logger.info("getUserAnalytics: querying stale-user counts ({} ms)", System.currentTimeMillis() - t0);
        long totalUsers = userRepository.count();
        long staleUsers = userRepository.countStaleUsers(since90d);
        double stalePct = totalUsers == 0 ? 0.0 : Math.round((staleUsers * 1000.0 / totalUsers)) / 10.0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("newUsersByMonth", newUsersBucket.entrySet().stream()
                .map(e -> Map.of("month", e.getKey(), "count", e.getValue()))
                .collect(Collectors.toList()));
        response.put("loginsByMonth", loginsBucket.entrySet().stream()
                .map(e -> Map.of("month", e.getKey(), "count", e.getValue()))
                .collect(Collectors.toList()));
        response.put("topActiveOrganizations", topOrgs);
        Map<String, Object> staleSummary = new LinkedHashMap<>();
        staleSummary.put("totalUsers", totalUsers);
        staleSummary.put("staleUsers", staleUsers);
        staleSummary.put("percentage", stalePct);
        staleSummary.put("windowDays", staleDays);
        response.put("staleUsers", staleSummary);

        logger.info("getUserAnalytics: done in {} ms", System.currentTimeMillis() - t0);
        return ResponseEntity.ok(response);
    }

    private Long currentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userRepository.findByUsername(auth.getName())
                .map(User::getId)
                .orElse(null);
    }

    @Operation(
        summary = "Create new user",
        description = "Create a new user with specified global role. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or username/email already exists"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            String globalRoleStr = request.getOrDefault("globalRole", "USER");

            // Validate required fields
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username is required");
            }
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("Password is required");
            }

            // Check if username already exists
            if (userRepository.existsByUsername(username)) {
                throw new RuntimeException("Username already exists");
            }

            // Check if email already exists
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("Email already exists");
            }

            // Parse global role
            User.GlobalRole globalRole;
            try {
                globalRole = User.GlobalRole.valueOf(globalRoleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid global role. Must be 'USER' or 'SUPER_ADMIN'");
            }

            // Create new user
            User user = new User();
            user.setUsername(username.trim());
            user.setEmail(email.trim());
            user.setPassword(passwordEncoder.encode(password));
            user.setEnabled(true);
            user.setGlobalRole(globalRole);
            user.setPasswordChangedAt(java.time.LocalDateTime.now());
            user.setFailedLoginAttempts(0);

            // Save user
            user = userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User created successfully");
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("globalRole", user.getGlobalRole().toString());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
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

        // One GROUP BY query for every org's ACTIVE member count — no N+1.
        Map<Long, Integer> activeMemberCounts = organizationMembershipRepository
                .countMembersByOrganizationAndStatus(OrganizationMembership.MembershipStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

        List<OrganizationResponse> response = organizations.stream()
                .map(org -> {
                    OrganizationResponse r = new OrganizationResponse(org);
                    r.setMemberCount(activeMemberCounts.getOrDefault(org.getId(), 0));
                    return r;
                })
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

    @Operation(
        summary = "Get organizations summary",
        description = "Retrieve all organizations with member counts and pending access request counts. Super Admin only. Uses optimized batch queries to prevent N+1 issues."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organizations summary retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/organizations/summary")
    public ResponseEntity<List<OrganizationSummaryResponse>> getOrganizationsSummary() {
        List<Organization> organizations = organizationService.getAllOrganizations();

        // Batch fetch all member counts in single query (prevents N+1)
        Map<Long, Long> memberCountMap = organizationMembershipRepository.countMembersByOrganization().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // Batch fetch all pending request counts in single query (prevents N+1)
        Map<Long, Long> pendingCountMap = userAccessRequestRepository.countPendingByOrganization().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        List<OrganizationSummaryResponse> response = organizations.stream()
                .map(org -> new OrganizationSummaryResponse(
                        org.getId(),
                        org.getName(),
                        memberCountMap.getOrDefault(org.getId(), 0L).intValue(),
                        pendingCountMap.getOrDefault(org.getId(), 0L)
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get all pending access requests",
        description = "Retrieve all pending access requests across all organizations. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Access requests retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/access-requests")
    public ResponseEntity<List<AccessRequestResponse>> getAllPendingAccessRequests() {
        List<UserAccessRequest> requests = userAccessRequestRepository.findAllPending();
        List<AccessRequestResponse> response = requests.stream()
                .map(AccessRequestResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Approve access request",
        description = "Approve a pending access request. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Access request approved successfully"),
        @ApiResponse(responseCode = "400", description = "Request already processed or invalid"),
        @ApiResponse(responseCode = "404", description = "Access request not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/access-requests/{id}/approve")
    public ResponseEntity<?> approveAccessRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails userDetails) {
        try {
            User reviewer = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Reviewer not found"));
            String notes = body != null ? body.get("notes") : null;
            UserAccessRequest request = userAccessRequestService.approveRequest(id, reviewer.getId(), notes);
            return ResponseEntity.ok(new AccessRequestResponse(request));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(
        summary = "Reject access request",
        description = "Reject a pending access request. Super Admin only."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Access request rejected successfully"),
        @ApiResponse(responseCode = "400", description = "Request already processed or invalid"),
        @ApiResponse(responseCode = "404", description = "Access request not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Super Admin role required")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/access-requests/{id}/reject")
    public ResponseEntity<?> rejectAccessRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails userDetails) {
        try {
            User reviewer = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Reviewer not found"));
            String notes = body != null ? body.get("notes") : null;
            UserAccessRequest request = userAccessRequestService.rejectRequest(id, reviewer.getId(), notes);
            return ResponseEntity.ok(new AccessRequestResponse(request));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
