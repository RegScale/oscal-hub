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

    @Transactional
    public Invitation createInvitation(Long orgId, String email, Invitation.Role role, User inviter) {
        Organization org = orgRepo.findById(orgId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + orgId));

        // Already an active member?
        Optional<User> existingUser = userRepo.findByEmailIgnoreCase(email);
        if (existingUser.isPresent()) {
            Optional<OrganizationMembership> existingMembership =
                memRepo.findByUserIdAndOrganizationId(existingUser.get().getId(), orgId);
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
        } catch (Exception e) {
            logger.warn("Failed to send invitation email for invitation {}: {}", inv.getId(), e.getMessage());
        }

        Map<String, Object> meta = new HashMap<>();
        meta.put("invitationId", inv.getId());
        meta.put("email", email);
        meta.put("organizationId", org.getId());
        auditLogService.logEvent(AuditEventType.INVITATION_CREATED, inviter.getUsername(), inviter.getId(),
            "SUCCESS", "INVITATION", "CREATE", meta);

        return inv;
    }

    @Transactional
    public User acceptInvitation(String token, String username, String password) {
        Invitation inv = invRepo.findByToken(token)
            .orElseThrow(() -> new InvitationNotFoundException(token));

        if (inv.getStatus() != Status.PENDING) {
            throw new InvitationExpiredException("Invitation no longer valid");
        }
        if (inv.getExpiresAt().isBefore(LocalDateTime.now())) {
            inv.setStatus(Status.EXPIRED);
            invRepo.save(inv);
            throw new InvitationExpiredException("Invitation has expired");
        }

        // Find or create user
        User user = userRepo.findByEmailIgnoreCase(inv.getEmail()).orElseGet(() -> {
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                throw new IllegalArgumentException(
                    "username and password are required for first-time invite acceptance");
            }
            try {
                passwordValidationService.validatePassword(password, username);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Password does not meet complexity requirements: " + e.getMessage());
            }
            User u = new User();
            u.setEmail(inv.getEmail());
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode(password));
            u.setEnabled(true);
            u.setPasswordChangedAt(LocalDateTime.now());
            u.setFailedLoginAttempts(0);
            return userRepo.save(u);
        });

        // Add membership if not already present
        Optional<OrganizationMembership> existing =
            memRepo.findByUserIdAndOrganizationId(user.getId(), inv.getOrganization().getId());
        if (existing.isEmpty()) {
            OrganizationMembership.OrganizationRole role =
                OrganizationMembership.OrganizationRole.valueOf(inv.getRole().name());
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

    public List<Invitation> listForOrganization(Long orgId, Status status) {
        return invRepo.findByOrganizationIdAndStatus(orgId, status);
    }

    public Invitation findByToken(String token) {
        return invRepo.findByToken(token)
            .orElseThrow(() -> new InvitationNotFoundException(token));
    }
}
