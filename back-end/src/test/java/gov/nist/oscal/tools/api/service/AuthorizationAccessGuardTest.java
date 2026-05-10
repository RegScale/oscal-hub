package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.User.GlobalRole;
import gov.nist.oscal.tools.api.exception.InsufficientAuthorizationRoleException;
import gov.nist.oscal.tools.api.repository.AuthorizationGrantRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationAccessGuardTest {

    @Mock AuthorizationGrantRepository grantRepository;
    @Mock OrganizationMembershipRepository membershipRepository;

    @InjectMocks
    AuthorizationAccessGuard guard;

    Organization orgA;
    User creator;
    User otherInOrg;
    User outOfOrg;
    User superAdmin;
    User orgAdmin;
    Authorization auth;

    @BeforeEach
    void setUp() {
        orgA = new Organization(); orgA.setId(100L); orgA.setName("A");

        creator = newUser(1L, "alice");
        otherInOrg = newUser(2L, "bob");
        outOfOrg = newUser(3L, "carol");
        superAdmin = newUser(4L, "root"); superAdmin.setGlobalRole(GlobalRole.SUPER_ADMIN);
        orgAdmin = newUser(5L, "admin");

        auth = new Authorization();
        auth.setId(50L);
        auth.setOrganization(orgA);
        auth.setAuthorizedBy(creator);
    }

    private User newUser(Long id, String name) {
        User u = new User();
        u.setId(id);
        u.setUsername(name);
        u.setGlobalRole(GlobalRole.USER);
        return u;
    }

    private OrganizationMembership membership(User u, Organization o, OrganizationRole role, MembershipStatus status) {
        OrganizationMembership m = new OrganizationMembership();
        m.setId(1L);
        m.setUser(u);
        m.setOrganization(o);
        m.setRole(role);
        m.setStatus(status);
        return m;
    }

    private AuthorizationGrant grant(User u, AuthorizationRole role) {
        AuthorizationGrant g = new AuthorizationGrant();
        g.setAuthorization(auth);
        g.setUser(u);
        g.setRole(role);
        g.setGrantedBy(creator);
        return g;
    }

    private void inOrg(User u, OrganizationRole role) {
        when(membershipRepository.findByUserAndStatus(u, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership(u, orgA, role, MembershipStatus.ACTIVE)));
    }

    @Test
    void effectiveRole_superAdmin_returnsOwnerRegardlessOfMembership() {
        // No membership stub needed — SUPER_ADMIN short-circuits before hitting the repo
        AuthorizationRole result = guard.effectiveRole(auth, superAdmin);

        assertThat(result).isEqualTo(AuthorizationRole.OWNER);
    }

    @Test
    void effectiveRole_orgAdminOfThisOrg_returnsOwner() {
        inOrg(orgAdmin, OrganizationRole.ORG_ADMIN);

        AuthorizationRole result = guard.effectiveRole(auth, orgAdmin);

        assertThat(result).isEqualTo(AuthorizationRole.OWNER);
    }

    @Test
    void effectiveRole_creator_returnsOwner() {
        inOrg(creator, OrganizationRole.USER);

        AuthorizationRole result = guard.effectiveRole(auth, creator);

        assertThat(result).isEqualTo(AuthorizationRole.OWNER);
    }

    @Test
    void effectiveRole_userWithExplicitGrant_returnsThatRole() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.EDITOR)));

        AuthorizationRole result = guard.effectiveRole(auth, otherInOrg);

        assertThat(result).isEqualTo(AuthorizationRole.EDITOR);
    }

    @Test
    void effectiveRole_userWithNoGrantButShareWithOrgSet_returnsDefaultRole() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg)).thenReturn(Optional.empty());
        auth.setShareWithOrgDefaultRole(AuthorizationRole.VIEWER);

        AuthorizationRole result = guard.effectiveRole(auth, otherInOrg);

        assertThat(result).isEqualTo(AuthorizationRole.VIEWER);
    }

    @Test
    void effectiveRole_userInOrgButNoGrantOrShareDefault_returnsNull() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg)).thenReturn(Optional.empty());
        // shareWithOrgDefaultRole is null

        AuthorizationRole result = guard.effectiveRole(auth, otherInOrg);

        assertThat(result).isNull();
    }

    @Test
    void effectiveRole_outOfOrgUser_returnsNull() {
        when(membershipRepository.findByUserAndStatus(outOfOrg, MembershipStatus.ACTIVE))
                .thenReturn(List.of()); // no membership in any org

        AuthorizationRole result = guard.effectiveRole(auth, outOfOrg);

        assertThat(result).isNull();
    }

    @Test
    void effectiveRole_userWithGrantAndAlsoSharedOrg_explicitGrantWins() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.CONTRIBUTOR)));
        auth.setShareWithOrgDefaultRole(AuthorizationRole.VIEWER);

        AuthorizationRole result = guard.effectiveRole(auth, otherInOrg);

        // CONTRIBUTOR is more privileged than VIEWER — keeps the grant
        assertThat(result).isEqualTo(AuthorizationRole.CONTRIBUTOR);
    }

    // --- requireXxx behavior ---

    @Test
    void requireWriteDetails_owner_passes() {
        inOrg(creator, OrganizationRole.USER);

        assertThatNoException().isThrownBy(() -> guard.requireWriteDetails(auth, creator));
    }

    @Test
    void requireWriteDetails_editor_passes() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.EDITOR)));

        assertThatNoException().isThrownBy(() -> guard.requireWriteDetails(auth, otherInOrg));
    }

    @Test
    void requireWriteDetails_contributor_throws403() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.CONTRIBUTOR)));

        assertThatThrownBy(() -> guard.requireWriteDetails(auth, otherInOrg))
                .isInstanceOf(InsufficientAuthorizationRoleException.class);
    }

    @Test
    void requireManageGrants_orgAdmin_passes() {
        inOrg(orgAdmin, OrganizationRole.ORG_ADMIN);

        assertThatNoException().isThrownBy(() -> guard.requireManageGrants(auth, orgAdmin));
    }

    @Test
    void requireManageGrants_editor_throws403() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.EDITOR)));

        assertThatThrownBy(() -> guard.requireManageGrants(auth, otherInOrg))
                .isInstanceOf(InsufficientAuthorizationRoleException.class);
    }

    @Test
    void requireDelete_creator_passes() {
        inOrg(creator, OrganizationRole.USER);

        assertThatNoException().isThrownBy(() -> guard.requireDelete(auth, creator));
    }

    @Test
    void requireDelete_editor_throws403() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.EDITOR)));

        assertThatThrownBy(() -> guard.requireDelete(auth, otherInOrg))
                .isInstanceOf(InsufficientAuthorizationRoleException.class);
    }

    @Test
    void requireDeleteOwnedItem_contributorOwnsItem_passes() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.CONTRIBUTOR)));

        assertThatNoException().isThrownBy(() ->
                guard.requireDeleteOwnedItem(auth, otherInOrg, otherInOrg.getId()));
    }

    @Test
    void requireDeleteOwnedItem_contributorOtherUsersItem_throws403() {
        inOrg(otherInOrg, OrganizationRole.USER);
        when(grantRepository.findByAuthorizationAndUser(auth, otherInOrg))
                .thenReturn(Optional.of(grant(otherInOrg, AuthorizationRole.CONTRIBUTOR)));

        assertThatThrownBy(() ->
                guard.requireDeleteOwnedItem(auth, otherInOrg, 99L))
                .isInstanceOf(InsufficientAuthorizationRoleException.class);
    }

    @Test
    void requireDeleteOwnedItem_owner_passesEvenForOtherUsersItem() {
        inOrg(creator, OrganizationRole.USER);

        assertThatNoException().isThrownBy(() ->
                guard.requireDeleteOwnedItem(auth, creator, 99L));
    }
}
