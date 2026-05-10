package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.NoActiveOrganizationException;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationOrgContextTest {

    @Mock
    OrganizationMembershipRepository membershipRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    AuthorizationOrgContext orgContext;

    private User user;
    private Organization orgA;
    private Organization orgB;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
        user.setUsername("alice");

        orgA = new Organization();
        orgA.setId(100L);

        orgB = new Organization();
        orgB.setId(101L);
    }

    private OrganizationMembership membership(Long id, Organization org, MembershipStatus status) {
        OrganizationMembership m = new OrganizationMembership();
        m.setId(id);
        m.setUser(user);
        m.setOrganization(org);
        m.setRole(OrganizationRole.USER);
        m.setStatus(status);
        return m;
    }

    @Test
    void requirePrimaryOrganization_singleActiveMembership_returnsThatOrg() {
        when(membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership(1L, orgA, MembershipStatus.ACTIVE)));

        Organization result = orgContext.requirePrimaryOrganization(user);

        assertThat(result).isEqualTo(orgA);
    }

    @Test
    void requirePrimaryOrganization_multipleActiveMemberships_returnsLowestId() {
        when(membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE))
                .thenReturn(List.of(
                        membership(7L, orgB, MembershipStatus.ACTIVE),
                        membership(3L, orgA, MembershipStatus.ACTIVE)));

        Organization result = orgContext.requirePrimaryOrganization(user);

        assertThat(result).isEqualTo(orgA);
    }

    @Test
    void requirePrimaryOrganization_noActiveMembership_throws() {
        when(membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE))
                .thenReturn(List.of());

        assertThatThrownBy(() -> orgContext.requirePrimaryOrganization(user))
                .isInstanceOf(NoActiveOrganizationException.class)
                .hasMessageContaining("alice");
    }

    @Test
    void requirePrimaryOrganization_byUsername_resolvesUserThenOrg() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership(1L, orgA, MembershipStatus.ACTIVE)));

        Organization result = orgContext.requirePrimaryOrganization("alice");

        assertThat(result).isEqualTo(orgA);
    }

    @Test
    void requirePrimaryOrganization_byUsername_userNotFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orgContext.requirePrimaryOrganization("ghost"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost");
    }
}
