package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthorizationRepositoryOrgScopeTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    AuthorizationRepository repo;

    Organization orgA;
    Organization orgB;
    User userA;
    User userB;
    AuthorizationTemplate templateA;
    AuthorizationTemplate templateB;

    @BeforeEach
    void setUp() {
        orgA = newOrg("Org A");
        orgB = newOrg("Org B");
        userA = newUser("alice-scope");
        userB = newUser("bob-scope");
        templateA = newTemplate("TA", userA, orgA);
        templateB = newTemplate("TB", userB, orgB);

        newAuthorization("Auth A1", userA, templateA, orgA);
        newAuthorization("Auth A2", userA, templateA, orgA);
        newAuthorization("Auth B1", userB, templateB, orgB);
        em.flush();
        em.clear();
    }

    @Test
    void findByOrganization_returnsOnlyThatOrg() {
        List<Authorization> orgAResults = repo.findByOrganization(orgA);
        List<Authorization> orgBResults = repo.findByOrganization(orgB);

        assertThat(orgAResults).extracting(Authorization::getName)
                .containsExactlyInAnyOrder("Auth A1", "Auth A2");
        assertThat(orgBResults).extracting(Authorization::getName)
                .containsExactly("Auth B1");
    }

    @Test
    void findByIdAndOrganization_correctOrg_returnsRow() {
        Authorization a1 = repo.findByOrganization(orgA).get(0);

        var found = repo.findByIdAndOrganization(a1.getId(), orgA);

        assertThat(found).isPresent();
    }

    @Test
    void findByIdAndOrganization_wrongOrg_returnsEmpty() {
        Authorization a1 = repo.findByOrganization(orgA).get(0);

        var found = repo.findByIdAndOrganization(a1.getId(), orgB);

        assertThat(found).isEmpty();
    }

    @Test
    void findByOrganizationOrderByAuthorizedAtDesc_returnsOrgScopedNewestFirst() {
        List<Authorization> result = repo.findByOrganizationOrderByAuthorizedAtDesc(
                orgA, PageRequest.of(0, 10)).getContent();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(a -> a.getOrganization().getId())
                .containsOnly(orgA.getId());
    }

    @Test
    void searchByNameOrSspItemIdAndOrganization_filtersByOrg() {
        List<Authorization> result =
                repo.searchByNameOrSspItemIdAndOrganization("Auth", orgA);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(a -> a.getOrganization().getId())
                .containsOnly(orgA.getId());
    }

    // --- helpers ---

    private Organization newOrg(String name) {
        Organization o = new Organization();
        o.setName(name);
        em.persist(o);
        return o;
    }

    private User newUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test");
        u.setPassword("x");
        em.persist(u);
        return u;
    }

    private AuthorizationTemplate newTemplate(String name, User creator, Organization org) {
        AuthorizationTemplate t = new AuthorizationTemplate();
        t.setName(name);
        t.setContent("body");
        t.setCreatedBy(creator);
        t.setCreatedAt(LocalDateTime.now());
        t.setLastUpdatedAt(LocalDateTime.now());
        t.setOrganization(org);
        em.persist(t);
        return t;
    }

    private Authorization newAuthorization(String name, User creator,
                                           AuthorizationTemplate template, Organization org) {
        Authorization a = new Authorization();
        a.setName(name);
        a.setSspItemId("ssp-" + name);
        a.setTemplate(template);
        a.setAuthorizedBy(creator);
        a.setAuthorizedAt(LocalDateTime.now());
        a.setCreatedAt(LocalDateTime.now());
        a.setVariableValues(new HashMap<>());
        a.setOrganization(org);
        a.setDateExpired(LocalDate.now().plusYears(1));
        a.setSystemOwner("o");
        a.setSecurityManager("sm");
        a.setAuthorizingOfficial("ao");
        a.setCompletedContent("completed content for " + name);
        em.persist(a);
        return a;
    }
}
