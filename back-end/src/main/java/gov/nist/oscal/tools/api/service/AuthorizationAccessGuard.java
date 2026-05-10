package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.User.GlobalRole;
import gov.nist.oscal.tools.api.exception.InsufficientAuthorizationRoleException;
import gov.nist.oscal.tools.api.repository.AuthorizationGrantRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Resolves a user's effective role on a specific authorization, applying
 * SUPER_ADMIN and ORG_ADMIN bypasses, the creator's implicit OWNER status,
 * any explicit AuthorizationGrant rows, and the share-with-org default.
 *
 * Out-of-org users always resolve to {@code null} (no role at all).
 */
@Service
public class AuthorizationAccessGuard {

    private final AuthorizationGrantRepository grantRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public AuthorizationAccessGuard(AuthorizationGrantRepository grantRepository,
                                    OrganizationMembershipRepository membershipRepository) {
        this.grantRepository = grantRepository;
        this.membershipRepository = membershipRepository;
    }

    public AuthorizationRole effectiveRole(Authorization authorization, User user) {
        if (user.getGlobalRole() == GlobalRole.SUPER_ADMIN) {
            return AuthorizationRole.OWNER;
        }

        Optional<OrganizationMembership> membership = activeMembershipInOrg(user, authorization.getOrganization().getId());
        if (membership.isEmpty()) {
            return null;
        }

        if (membership.get().getRole() == OrganizationRole.ORG_ADMIN) {
            return AuthorizationRole.OWNER;
        }

        if (authorization.getAuthorizedBy() != null
                && user.getId().equals(authorization.getAuthorizedBy().getId())) {
            return AuthorizationRole.OWNER;
        }

        AuthorizationRole grantRole = grantRepository.findByAuthorizationAndUser(authorization, user)
                .map(AuthorizationGrant::getRole)
                .orElse(null);
        AuthorizationRole shareRole = authorization.getShareWithOrgDefaultRole();

        return moreSenior(grantRole, shareRole);
    }

    public void requireRead(Authorization authorization, User user) {
        if (effectiveRole(authorization, user) == null) {
            throw new InsufficientAuthorizationRoleException("none", "VIEWER");
        }
    }

    public void requireWriteDetails(Authorization authorization, User user) {
        AuthorizationRole role = effectiveRole(authorization, user);
        if (!isAtLeast(role, AuthorizationRole.EDITOR)) {
            throw new InsufficientAuthorizationRoleException(roleName(role), "EDITOR");
        }
    }

    public void requireUploadConMon(Authorization authorization, User user) {
        AuthorizationRole role = effectiveRole(authorization, user);
        if (!isAtLeast(role, AuthorizationRole.CONTRIBUTOR)) {
            throw new InsufficientAuthorizationRoleException(roleName(role), "CONTRIBUTOR");
        }
    }

    public void requireUploadDocument(Authorization authorization, User user) {
        AuthorizationRole role = effectiveRole(authorization, user);
        if (!isAtLeast(role, AuthorizationRole.CONTRIBUTOR)) {
            throw new InsufficientAuthorizationRoleException(roleName(role), "CONTRIBUTOR");
        }
    }

    public void requireDeleteOwnedItem(Authorization authorization, User user, Long ownerUserId) {
        AuthorizationRole role = effectiveRole(authorization, user);
        if (isAtLeast(role, AuthorizationRole.EDITOR)) {
            return;
        }
        if (role == AuthorizationRole.CONTRIBUTOR && user.getId().equals(ownerUserId)) {
            return;
        }
        throw new InsufficientAuthorizationRoleException(roleName(role), "EDITOR or CONTRIBUTOR-of-own-item");
    }

    public void requireManageGrants(Authorization authorization, User user) {
        AuthorizationRole role = effectiveRole(authorization, user);
        if (role != AuthorizationRole.OWNER) {
            throw new InsufficientAuthorizationRoleException(roleName(role), "OWNER");
        }
    }

    public void requireDelete(Authorization authorization, User user) {
        AuthorizationRole role = effectiveRole(authorization, user);
        if (role != AuthorizationRole.OWNER) {
            throw new InsufficientAuthorizationRoleException(roleName(role), "OWNER");
        }
    }

    private Optional<OrganizationMembership> activeMembershipInOrg(User user, Long orgId) {
        List<OrganizationMembership> memberships =
                membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE);
        return memberships.stream()
                .filter(m -> orgId.equals(m.getOrganization().getId()))
                .findFirst();
    }

    private static boolean isAtLeast(AuthorizationRole have, AuthorizationRole need) {
        if (have == null) return false;
        return seniority(have) >= seniority(need);
    }

    private static int seniority(AuthorizationRole role) {
        return switch (role) {
            case OWNER -> 4;
            case EDITOR -> 3;
            case CONTRIBUTOR -> 2;
            case VIEWER -> 1;
        };
    }

    private static AuthorizationRole moreSenior(AuthorizationRole a, AuthorizationRole b) {
        if (a == null) return b;
        if (b == null) return a;
        return seniority(a) >= seniority(b) ? a : b;
    }

    private static String roleName(AuthorizationRole role) {
        return role == null ? "none" : role.name();
    }
}
