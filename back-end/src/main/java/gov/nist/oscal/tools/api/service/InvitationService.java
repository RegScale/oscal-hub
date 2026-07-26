package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.Invitation;
import gov.nist.oscal.tools.api.entity.Invitation.Status;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.InvitationExpiredException;
import gov.nist.oscal.tools.api.exception.InvitationNotFoundException;
import gov.nist.oscal.tools.api.exception.UserAlreadyMemberException;
import gov.nist.oscal.tools.api.model.AuditEventType;
import gov.nist.oscal.tools.api.repository.InvitationRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationService {

    private static final Logger logger = LoggerFactory.getLogger(InvitationService.class);

    @Autowired private InvitationRepository invRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private OrganizationRepository orgRepo;
    @Autowired private OrganizationMembershipRepository memRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private PasswordValidationService passwordValidationService;
    @Autowired private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public Invitation createInvitation(Long orgId, String email, Invitation.Role role, User inviter) {
        Organization org = orgRepo.findById(orgId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + orgId));

        // Already an active member? Emails are not unique — check every account
        // sharing this email (an Optional lookup crashes on duplicates).
        for (User existingUser : userRepo.findAllByEmailIgnoreCase(email)) {
            Optional<OrganizationMembership> existingMembership =
                memRepo.findByUserIdAndOrganizationId(existingUser.getId(), orgId);
            if (existingMembership.isPresent()
                && existingMembership.get().getStatus() == OrganizationMembership.MembershipStatus.ACTIVE) {
                throw new UserAlreadyMemberException(email);
            }
        }

        // Revoke prior PENDING invitations for same email + org
        List<Invitation> priors = invRepo.findByEmailAndOrganizationIdAndStatus(email, orgId, Status.PENDING);
        for (Invitation prior : priors) {
            prior.setStatus(Status.REVOKED);
            invRepo.save(prior);
        }

        Invitation inv = new Invitation();
        inv.setEmail(email);
        inv.setOrganization(org);
        inv.setInvitedBy(inviter);
        inv.setRole(role);
        // status, token, createdAt, expiresAt set by @PrePersist
        inv = invRepo.save(inv);

        try {
            emailService.sendInvitation(inv, inviter, org);
            inv.setEmailSent(true);
        } catch (Exception e) {
            logger.warn("Failed to send invitation email for invitation {}: {}", inv.getId(), e.getMessage());
            inv.setEmailSent(false);
        }
        inv = invRepo.save(inv);

        Map<String, Object> meta = new HashMap<>();
        meta.put("invitationId", inv.getId());
        meta.put("email", email);
        meta.put("organizationId", org.getId());
        auditLogService.logEvent(AuditEventType.INVITATION_CREATED, inviter.getUsername(), inviter.getId(),
            "SUCCESS", "INVITATION", "CREATE", meta);

        return inv;
    }

    /**
     * Accept an invitation.
     *
     * @param authenticatedUser the signed-in caller, or null for anonymous accepts.
     *        When present, the invitation binds to THIS account. When absent and
     *        the invitation email matches an existing account, acceptance is
     *        refused — possession of the emailed link must never yield a session
     *        for an existing account (that was an account-takeover vector).
     */
    @Transactional
    public User acceptInvitation(String token, String username, String password, User authenticatedUser) {
        Invitation inv = invRepo.findByToken(token)
            .orElseThrow(() -> new InvitationNotFoundException(token));

        // Idempotency: a double-click or client retry after a successful accept
        // re-sends the same token. Return the accepted user instead of failing
        // with "no longer valid".
        if (inv.getStatus() == Status.ACCEPTED && inv.getAcceptedBy() != null) {
            if (authenticatedUser != null
                    && authenticatedUser.getId().equals(inv.getAcceptedBy().getId())) {
                return inv.getAcceptedBy();
            }
            if (username != null && username.equals(inv.getAcceptedBy().getUsername())) {
                return inv.getAcceptedBy();
            }
        }

        if (inv.getStatus() != Status.PENDING) {
            throw new InvitationExpiredException("Invitation no longer valid");
        }
        if (inv.getExpiresAt().isBefore(LocalDateTime.now())) {
            inv.setStatus(Status.EXPIRED);
            invRepo.save(inv);
            throw new InvitationExpiredException("Invitation has expired");
        }

        User user;
        if (authenticatedUser != null) {
            // Signed-in accept: bind the caller's own account. The email may
            // legitimately differ (personal vs work address) — the join is
            // attributed to the real username and visible to org admins.
            user = authenticatedUser;
            if (!inv.getEmail().equalsIgnoreCase(user.getEmail())) {
                logger.info("Invitation {} addressed to {} accepted by signed-in user {} ({})",
                    inv.getId(), inv.getEmail(), user.getUsername(), user.getEmail());
            }
        } else if (!userRepo.findAllByEmailIgnoreCase(inv.getEmail()).isEmpty()) {
            // Anonymous accept for an email that already has an account:
            // require sign-in. Handing out a session here would let anyone
            // holding the link take over the existing account.
            throw new IllegalArgumentException(
                "An account already exists for this email address. Please sign in to your "
                + "account first, then open the invitation link again.");
        } else {
            String newUsername = username == null ? null : username.trim();
            if (newUsername == null || newUsername.isBlank() || password == null || password.isBlank()) {
                throw new IllegalArgumentException(
                    "username and password are required for first-time invite acceptance");
            }
            // Pre-check so the common case is a clear message instead of a
            // DB constraint violation (the unique constraint still backstops races).
            // Case-insensitive: "Iorga" and "iorga" are the same identity.
            if (userRepo.existsByUsernameIgnoreCase(newUsername)) {
                throw new IllegalArgumentException(
                    "That username is already taken. Please choose another.");
            }
            try {
                passwordValidationService.validatePassword(password, newUsername);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Password does not meet complexity requirements: " + e.getMessage());
            }
            User u = new User();
            u.setEmail(inv.getEmail());
            u.setUsername(newUsername);
            u.setPassword(passwordEncoder.encode(password));
            u.setEnabled(true);
            u.setPasswordChangedAt(LocalDateTime.now());
            u.setFailedLoginAttempts(0);
            user = userRepo.save(u);
            // New account created via invitation — register in the marketing CRM
            // after commit (CrmSyncListener). Consent disclosed on the accept form.
            eventPublisher.publishEvent(new gov.nist.oscal.tools.api.crm.CrmEvents.ContactRegistered(
                user.getId(), "invitation"));
        }

        // Add membership, or repair an inactive one. An admin re-inviting a
        // DEACTIVATED member is an explicit signal to reactivate — previously the
        // invitation was consumed while the membership stayed DEACTIVATED, leaving
        // the user locked out with no way to retry.
        OrganizationMembership.OrganizationRole role =
            OrganizationMembership.OrganizationRole.valueOf(inv.getRole().name());
        Optional<OrganizationMembership> existing =
            memRepo.findByUserIdAndOrganizationId(user.getId(), inv.getOrganization().getId());
        if (existing.isPresent()) {
            OrganizationMembership membership = existing.get();
            if (membership.getStatus() == OrganizationMembership.MembershipStatus.LOCKED) {
                throw new IllegalArgumentException(
                    "Your membership in this organization is locked. "
                    + "Please contact your organization admin.");
            }
            if (membership.getStatus() == OrganizationMembership.MembershipStatus.DEACTIVATED) {
                membership.setStatus(OrganizationMembership.MembershipStatus.ACTIVE);
                membership.setRole(role);
                memRepo.save(membership);
                logger.info("Reactivated membership of user {} in org {} via invitation {}",
                    user.getUsername(), inv.getOrganization().getId(), inv.getId());
            }
        } else {
            memRepo.save(new OrganizationMembership(user, inv.getOrganization(), role));
        }

        inv.setStatus(Status.ACCEPTED);
        inv.setAcceptedAt(LocalDateTime.now());
        inv.setAcceptedBy(user);
        invRepo.save(inv);

        Map<String, Object> meta = new HashMap<>();
        meta.put("invitationId", inv.getId());
        meta.put("organizationId", inv.getOrganization().getId());
        auditLogService.logEvent(AuditEventType.INVITATION_ACCEPTED, user.getUsername(), user.getId(),
            "SUCCESS", "INVITATION", "ACCEPT", meta);
        return user;
    }

    @Transactional
    public void revokeInvitation(Long invitationId, User actor) {
        Invitation inv = invRepo.findById(invitationId)
            .orElseThrow(() -> new InvitationNotFoundException(String.valueOf(invitationId)));
        if (inv.getStatus() == Status.PENDING) {
            inv.setStatus(Status.REVOKED);
            invRepo.save(inv);
            Map<String, Object> meta = new HashMap<>();
            meta.put("invitationId", invitationId);
            auditLogService.logEvent(AuditEventType.INVITATION_REVOKED, actor.getUsername(), actor.getId(),
                "SUCCESS", "INVITATION", "REVOKE", meta);
        }
    }

    /**
     * Re-send an invitation email. Works for PENDING and EXPIRED invitations:
     * the token is regenerated (invalidating any copy of the old link), the
     * expiry window restarts, and an EXPIRED invitation returns to PENDING.
     *
     * @throws IllegalArgumentException for ACCEPTED/REVOKED invitations
     */
    @Transactional
    public Invitation resendInvitation(Long invitationId, User actor) {
        Invitation inv = invRepo.findById(invitationId)
            .orElseThrow(() -> new InvitationNotFoundException(String.valueOf(invitationId)));

        if (inv.getStatus() == Status.ACCEPTED) {
            throw new IllegalArgumentException("This invitation was already accepted.");
        }
        if (inv.getStatus() == Status.REVOKED) {
            throw new IllegalArgumentException(
                "This invitation was revoked. Create a new invitation instead.");
        }

        inv.setToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        inv.setExpiresAt(LocalDateTime.now().plusDays(7));
        inv.setStatus(Status.PENDING);

        try {
            emailService.sendInvitation(inv, actor, inv.getOrganization());
            inv.setEmailSent(true);
        } catch (Exception e) {
            logger.warn("Failed to re-send invitation email for invitation {}: {}", inv.getId(), e.getMessage());
            inv.setEmailSent(false);
        }
        inv = invRepo.save(inv);

        Map<String, Object> meta = new HashMap<>();
        meta.put("invitationId", inv.getId());
        meta.put("email", inv.getEmail());
        meta.put("organizationId", inv.getOrganization().getId());
        auditLogService.logEvent(AuditEventType.INVITATION_CREATED, actor.getUsername(), actor.getId(),
            "SUCCESS", "INVITATION", "RESEND", meta);

        return inv;
    }

    public List<Invitation> listForOrganization(Long orgId, Status status) {
        return invRepo.findByOrganizationIdAndStatus(orgId, status);
    }

    public Invitation findByToken(String token) {
        return invRepo.findByToken(token)
            .orElseThrow(() -> new InvitationNotFoundException(token));
    }
}
