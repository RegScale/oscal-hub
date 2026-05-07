package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.AuthorizationNotFoundException;
import gov.nist.oscal.tools.api.repository.AuthorizationRepository;
import gov.nist.oscal.tools.api.repository.AuthorizationTemplateRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceOrgIsolationTest {

    @Mock AuthorizationRepository authRepo;
    @Mock AuthorizationTemplateRepository templateRepo;
    @Mock UserRepository userRepo;
    @Mock AuthorizationOrgContext orgContext;
    @Mock AuthorizationAccessGuard accessGuard;

    @InjectMocks
    AuthorizationService service;

    Organization orgA;
    Organization orgB;
    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        orgA = new Organization(); orgA.setId(100L); orgA.setName("A");
        orgB = new Organization(); orgB.setId(101L); orgB.setName("B");

        alice = new User(); alice.setId(1L); alice.setUsername("alice");
        bob = new User(); bob.setId(2L); bob.setUsername("bob");
    }

    private Authorization auth(Long id, Organization org) {
        Authorization a = new Authorization();
        a.setId(id);
        a.setName("auth-" + id);
        a.setOrganization(org);
        a.setAuthorizedBy(alice);
        a.setAuthorizedAt(LocalDateTime.now());
        a.setCreatedAt(LocalDateTime.now());
        a.setVariableValues(new HashMap<>());
        return a;
    }

    @Test
    void getAllAuthorizations_filtersByCurrentUserOrg() {
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(orgContext.requirePrimaryOrganization(alice)).thenReturn(orgA);
        when(authRepo.findByOrganization(orgA)).thenReturn(List.of(auth(1L, orgA), auth(2L, orgA)));
        // Guard returns OWNER for alice on every authorization in orgA
        when(accessGuard.effectiveRole(any(Authorization.class), eq(alice)))
                .thenReturn(AuthorizationRole.OWNER);

        List<Authorization> result = service.getAllAuthorizationsForUser("alice");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Authorization::getOrganization).containsOnly(orgA);
    }

    @Test
    void getAuthorization_inUserOrg_returnsRow() {
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(orgContext.requirePrimaryOrganization(alice)).thenReturn(orgA);
        Authorization a = auth(1L, orgA);
        when(authRepo.findByIdAndOrganization(1L, orgA)).thenReturn(Optional.of(a));
        // Alice is the creator — access guard returns OWNER so the access check passes.
        when(accessGuard.effectiveRole(a, alice)).thenReturn(AuthorizationRole.OWNER);

        Authorization result = service.getAuthorizationForUser(1L, "alice");

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getAuthorization_outsideUserOrg_throws404() {
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(orgContext.requirePrimaryOrganization(bob)).thenReturn(orgB);
        when(authRepo.findByIdAndOrganization(1L, orgB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAuthorizationForUser(1L, "bob"))
                .isInstanceOf(AuthorizationNotFoundException.class);
    }

    @Test
    void deleteAuthorization_outsideUserOrg_throws404() {
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(orgContext.requirePrimaryOrganization(bob)).thenReturn(orgB);
        when(authRepo.findByIdAndOrganization(1L, orgB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAuthorization(1L, "bob"))
                .isInstanceOf(AuthorizationNotFoundException.class);
    }

    @Test
    void createAuthorization_setsOrgFromCurrentUserPrimary() {
        AuthorizationTemplate t = new AuthorizationTemplate();
        t.setId(50L);
        t.setOrganization(orgA);
        t.setContent("body");

        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(orgContext.requirePrimaryOrganization(alice)).thenReturn(orgA);
        when(templateRepo.findByIdAndOrganization(50L, orgA)).thenReturn(Optional.of(t));
        when(authRepo.save(any(Authorization.class))).thenAnswer(inv -> inv.getArgument(0));

        Authorization created = service.createAuthorization(
                "name", "ssp1", null, 50L, new HashMap<>(), "alice",
                null, "2027-01-01", "owner", "sm", "ao", null, List.of());

        assertThat(created.getOrganization()).isEqualTo(orgA);
    }

    @Test
    void createAuthorization_templateOutsideUserOrg_throws() {
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(orgContext.requirePrimaryOrganization(bob)).thenReturn(orgB);
        when(templateRepo.findByIdAndOrganization(50L, orgB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAuthorization(
                "name", "ssp1", null, 50L, new HashMap<>(), "bob",
                null, "2027-01-01", "owner", "sm", "ao", null, List.of()))
                .isInstanceOf(AuthorizationNotFoundException.class);
    }
}
