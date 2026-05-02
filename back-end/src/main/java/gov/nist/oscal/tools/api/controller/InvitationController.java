package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Invitation;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.InvitationExpiredException;
import gov.nist.oscal.tools.api.exception.InvitationNotFoundException;
import gov.nist.oscal.tools.api.exception.UserAlreadyMemberException;
import gov.nist.oscal.tools.api.model.AcceptInvitationRequest;
import gov.nist.oscal.tools.api.model.CreateInvitationRequest;
import gov.nist.oscal.tools.api.model.InvitationResponse;
import gov.nist.oscal.tools.api.repository.InvitationRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Invitations", description = "APIs for managing organization invitations")
@SuppressWarnings("unused")
public class InvitationController {

    private static final Logger log = LoggerFactory.getLogger(InvitationController.class);

    @Autowired private InvitationService invitationService;
    @Autowired private UserRepository userRepo;
    @Autowired private OrganizationMembershipRepository memRepo;
    @Autowired private InvitationRepository invRepo;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserDetailsService userDetailsService;

    // ========================================================================
    // ORG-ADMIN endpoints — mirror OrgAdminController's @PreAuthorize pattern
    // ========================================================================

    @Operation(summary = "Create invitation",
        description = "Create an invitation for a user to join an organization. ORG_ADMIN role required.")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/api/org-admin/invitations")
    public ResponseEntity<?> create(@Valid @RequestBody CreateInvitationRequest req, Authentication auth) {
        User inviter = userRepo.findByUsername(auth.getName())
            .orElseThrow(() -> new IllegalStateException("authenticated user not found"));

        if (!isOrgAdmin(inviter, req.getOrganizationId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "FORBIDDEN", "message", "You are not an admin of that organization."));
        }

        try {
            Invitation inv = invitationService.createInvitation(
                req.getOrganizationId(), req.getEmail(), req.getRole(), inviter);
            return ResponseEntity.ok(InvitationResponse.from(inv));
        } catch (UserAlreadyMemberException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "ALREADY_MEMBER", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "List invitations",
        description = "List invitations for an organization filtered by status. ORG_ADMIN role required.")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/api/org-admin/invitations")
    public ResponseEntity<?> list(@RequestParam Long organizationId,
                                   @RequestParam(defaultValue = "PENDING") Invitation.Status status,
                                   Authentication auth) {
        User caller = userRepo.findByUsername(auth.getName())
            .orElseThrow(() -> new IllegalStateException("authenticated user not found"));

        if (!isOrgAdmin(caller, organizationId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "FORBIDDEN", "message", "You are not an admin of that organization."));
        }

        List<InvitationResponse> result = invitationService.listForOrganization(organizationId, status).stream()
            .map(InvitationResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Revoke invitation",
        description = "Revoke a pending invitation by ID. ORG_ADMIN role required.")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/api/org-admin/invitations/{id}")
    public ResponseEntity<?> revoke(@PathVariable Long id, Authentication auth) {
        User actor = userRepo.findByUsername(auth.getName())
            .orElseThrow(() -> new IllegalStateException("authenticated user not found"));

        try {
            Invitation inv = invRepo.findById(id)
                .orElseThrow(() -> new InvitationNotFoundException(String.valueOf(id)));

            if (!isOrgAdmin(actor, inv.getOrganization().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "FORBIDDEN", "message", "You are not an admin of that organization."));
            }

            invitationService.revokeInvitation(id, actor);
            return ResponseEntity.noContent().build();
        } catch (InvitationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "INVITATION_NOT_FOUND"));
        }
    }

    // ========================================================================
    // PUBLIC endpoints — no auth required (token is the authority)
    // ========================================================================

    @Operation(summary = "View invitation by token",
        description = "View details of a pending invitation by its token. No authentication required.")
    @GetMapping("/api/invitations/{token}")
    public ResponseEntity<?> view(@PathVariable String token) {
        try {
            Invitation inv = invitationService.findByToken(token);
            if (inv.getStatus() != Invitation.Status.PENDING
                || inv.getExpiresAt().isBefore(LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", "INVITATION_EXPIRED",
                                 "message", "This invitation is no longer valid."));
            }
            return ResponseEntity.ok(InvitationResponse.from(inv));
        } catch (InvitationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "INVITATION_NOT_FOUND"));
        }
    }

    @Operation(summary = "Accept invitation by token",
        description = "Accept an invitation. For new users, provide username and password in the request body. "
            + "For existing users, the account is located by the invitation email. No authentication required.")
    @PostMapping("/api/invitations/{token}/accept")
    public ResponseEntity<?> accept(@PathVariable String token,
                                     @RequestBody(required = false) AcceptInvitationRequest body) {
        try {
            String username = body == null ? null : body.getUsername();
            String password = body == null ? null : body.getPassword();
            User accepted = invitationService.acceptInvitation(token, username, password);
            UserDetails userDetails = userDetailsService.loadUserByUsername(accepted.getUsername());
            String jwt = jwtUtil.generateToken(userDetails);
            return ResponseEntity.ok(Map.of(
                "token", jwt,
                "userId", accepted.getId(),
                "username", accepted.getUsername()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "VALIDATION", "message", e.getMessage()));
        } catch (InvitationExpiredException e) {
            return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of("error", "INVITATION_EXPIRED", "message", e.getMessage()));
        } catch (InvitationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "INVITATION_NOT_FOUND"));
        }
    }

    // ========================================================================
    // Private helpers
    // ========================================================================

    /**
     * Returns true if the user is a SUPER_ADMIN (platform-level bypass) or is an
     * ACTIVE ORG_ADMIN member of the specified organization.
     */
    private boolean isOrgAdmin(User user, Long organizationId) {
        if (user.getGlobalRole() == User.GlobalRole.SUPER_ADMIN) return true;
        return memRepo.findByUserIdAndOrganizationId(user.getId(), organizationId)
            .filter(m -> m.getStatus() == OrganizationMembership.MembershipStatus.ACTIVE
                      && m.getRole() == OrganizationMembership.OrganizationRole.ORG_ADMIN)
            .isPresent();
    }
}
