/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Artifact;
import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.OperationHistory;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OscalDocument;
import gov.nist.oscal.tools.api.entity.OscalModelType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.Visibility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seeds each leaderboard activity source and verifies the per-user
 * GROUP BY count queries, including time-window cutoffs, PRIVATE
 * library-item exclusion, and the publishedAt -> createdAt fallback.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LeaderboardQueriesTest {

    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    @PersistenceContext
    EntityManager em;

    @Autowired HistoryRepository historyRepository;
    @Autowired LibraryItemRepository libraryItemRepository;
    @Autowired ArtifactRepository artifactRepository;
    @Autowired OscalDocumentRepository oscalDocumentRepository;
    @Autowired AuthorizationRepository authorizationRepository;

    User alice;
    User bob;
    Organization org;
    AuthorizationTemplate template;
    LocalDateTime recent;
    LocalDateTime old;

    @BeforeEach
    void setUp() {
        recent = LocalDateTime.now().minusDays(1);
        old = LocalDateTime.now().minusDays(90);
        alice = newUser("lb-alice");
        bob = newUser("lb-bob");
        org = newOrg("Leaderboard Org");
        template = newTemplate(alice, org);

        // operations: alice 2 recent + 1 old, bob 1 recent
        newOperation(alice, recent);
        newOperation(alice, recent);
        newOperation(alice, old);
        newOperation(bob, recent);

        // library: alice PUBLIC recent, ORGANIZATION old, PRIVATE recent (never
        // counts); bob PUBLIC with null publishedAt (falls back to createdAt)
        newLibraryItem(alice, Visibility.PUBLIC, recent, recent);
        newLibraryItem(alice, Visibility.ORGANIZATION, old, old);
        newLibraryItem(alice, Visibility.PRIVATE, recent, recent);
        newLibraryItem(bob, Visibility.PUBLIC, recent, null);

        // artifacts: alice 1 recent, bob 1 old
        newArtifact(alice, recent);
        newArtifact(bob, old);

        // documents: alice 1 recent
        newDocument(alice, recent);

        // authorizations: alice 1 recent, bob 1 old
        newAuthorization(alice, recent);
        newAuthorization(bob, old);

        em.flush();
        em.clear();
    }

    @Test
    void countsOperationsPerUserWithCutoff() {
        Map<Long, Long> allTime = toMap(historyRepository.countOperationsPerUserSince(EPOCH));
        assertThat(allTime).containsEntry(alice.getId(), 3L).containsEntry(bob.getId(), 1L);

        Map<Long, Long> recent30 = toMap(
                historyRepository.countOperationsPerUserSince(LocalDateTime.now().minusDays(30)));
        assertThat(recent30).containsEntry(alice.getId(), 2L).containsEntry(bob.getId(), 1L);
    }

    @Test
    void countsSharedLibraryItemsExcludingPrivateWithPublishedAtFallback() {
        Map<Long, Long> allTime = toMap(libraryItemRepository.countSharedItemsPerUserSince(EPOCH));
        assertThat(allTime).containsEntry(alice.getId(), 2L).containsEntry(bob.getId(), 1L);

        Map<Long, Long> recent30 = toMap(
                libraryItemRepository.countSharedItemsPerUserSince(LocalDateTime.now().minusDays(30)));
        assertThat(recent30).containsEntry(alice.getId(), 1L).containsEntry(bob.getId(), 1L);
    }

    @Test
    void countsArtifactsPerUserWithCutoff() {
        Map<Long, Long> allTime = toMap(artifactRepository.countCreatedPerUserSince(EPOCH));
        assertThat(allTime).containsEntry(alice.getId(), 1L).containsEntry(bob.getId(), 1L);

        Map<Long, Long> recent30 = toMap(
                artifactRepository.countCreatedPerUserSince(LocalDateTime.now().minusDays(30)));
        assertThat(recent30).containsEntry(alice.getId(), 1L).doesNotContainKey(bob.getId());
    }

    @Test
    void countsDocumentsPerUserWithCutoff() {
        Map<Long, Long> allTime = toMap(oscalDocumentRepository.countCreatedPerUserSince(EPOCH));
        assertThat(allTime).containsEntry(alice.getId(), 1L).doesNotContainKey(bob.getId());
    }

    @Test
    void countsAuthorizationsPerUserWithCutoff() {
        Map<Long, Long> allTime = toMap(authorizationRepository.countCreatedPerUserSince(EPOCH));
        assertThat(allTime).containsEntry(alice.getId(), 1L).containsEntry(bob.getId(), 1L);

        Map<Long, Long> recent30 = toMap(
                authorizationRepository.countCreatedPerUserSince(LocalDateTime.now().minusDays(30)));
        assertThat(recent30).containsEntry(alice.getId(), 1L).doesNotContainKey(bob.getId());
    }

    // --- helpers ---

    private Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private User newUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test");
        u.setPassword("x");
        u.setEnabled(true);
        em.persist(u);
        return u;
    }

    private Organization newOrg(String name) {
        Organization o = new Organization();
        o.setName(name);
        em.persist(o);
        return o;
    }

    private AuthorizationTemplate newTemplate(User creator, Organization organization) {
        AuthorizationTemplate t = new AuthorizationTemplate();
        t.setName("LB Template");
        t.setContent("body");
        t.setCreatedBy(creator);
        t.setCreatedAt(LocalDateTime.now());
        t.setLastUpdatedAt(LocalDateTime.now());
        t.setOrganization(organization);
        em.persist(t);
        return t;
    }

    private void newOperation(User user, LocalDateTime ts) {
        OperationHistory op = new OperationHistory();
        op.setOperationType("VALIDATE");
        op.setFileName("f.json");
        op.setTimestamp(ts);
        op.setSuccess(true);
        op.setUser(user);
        em.persist(op);
    }

    private void newLibraryItem(User user, Visibility vis, LocalDateTime createdAt, LocalDateTime publishedAt) {
        LibraryItem item = new LibraryItem();
        item.setItemId(UUID.randomUUID().toString());
        item.setTitle("Item");
        item.setOscalType("catalog");
        item.setCreatedBy(user);
        item.setCreatedAt(createdAt);
        item.setUpdatedAt(createdAt);
        item.setVisibility(vis);
        item.setPublishedAt(publishedAt);
        em.persist(item);
    }

    private void newArtifact(User user, LocalDateTime createdAt) {
        Artifact a = new Artifact(UUID.randomUUID().toString(), "Artifact", null,
                Artifact.ArtifactVisibility.PRIVATE, user);
        a.setCreatedAt(createdAt);
        a.setUpdatedAt(createdAt);
        em.persist(a);
    }

    private void newDocument(User user, LocalDateTime createdAt) {
        OscalDocument d = new OscalDocument(UUID.randomUUID().toString(),
                OscalModelType.SYSTEM_SECURITY_PLAN, "Doc", "path/doc.json", user);
        d.setFilename("doc.json");
        d.setCreatedAt(createdAt);
        em.persist(d);
    }

    private void newAuthorization(User user, LocalDateTime createdAt) {
        Authorization a = new Authorization();
        a.setName("Auth " + UUID.randomUUID());
        a.setSspItemId("ssp-item");
        a.setTemplate(template);
        a.setAuthorizedBy(user);
        a.setAuthorizedAt(createdAt);
        a.setCreatedAt(createdAt);
        a.setVariableValues(new HashMap<>());
        a.setOrganization(org);
        a.setDateExpired(LocalDate.now().plusYears(1));
        a.setSystemOwner("o");
        a.setSecurityManager("sm");
        a.setAuthorizingOfficial("ao");
        a.setCompletedContent("content");
        em.persist(a);
    }
}
