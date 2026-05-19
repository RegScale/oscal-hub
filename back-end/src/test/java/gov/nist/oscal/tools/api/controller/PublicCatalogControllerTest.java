/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.Visibility;
import gov.nist.oscal.tools.api.model.library.PublicItemSummary;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.LibraryVersionRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.LibraryService;
import gov.nist.oscal.tools.api.service.LibraryStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration matrix for the anonymous public catalog endpoints
 * ({@code /api/public/catalog/**}). Mirrors the
 * {@code @SpringBootTest + @AutoConfigureMockMvc + @Transactional} pattern
 * from {@link LibraryVisibilityControllerTest}, with two adaptations:
 *
 * <ol>
 *   <li>{@link LibraryService} is mocked via {@code @MockitoBean}. The
 *       production public-catalog query uses Postgres-only FTS
 *       ({@code to_tsvector @@ plainto_tsquery}) which H2 (the test DB) does
 *       not parse. Mocking the service lets us verify the full controller
 *       contract — security whitelist, parameter binding, JSON shape, HTTP
 *       status codes — without taking a hard dependency on Postgres in CI.
 *       Service-level FTS behaviour belongs in a separate Postgres-backed
 *       suite.</li>
 *   <li>{@link LibraryStorageService} and {@link EmailService} are also
 *       mocked to avoid blob-backend setup and SMTP startup probes.</li>
 * </ol>
 *
 * <p>The seven tests below cover: visibility-pinned listing, keyword routing,
 * type filtering, 200/404 split on detail, download success + counter
 * propagation, view-counter propagation, and anonymous (no-Authorization)
 * access.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicCatalogControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired LibraryItemRepository libraryItemRepo;
    @Autowired LibraryVersionRepository libraryVersionRepo;
    @Autowired UserRepository userRepo;
    @Autowired OrganizationRepository orgRepo;
    @Autowired OrganizationMembershipRepository memRepo;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean EmailService emailService;
    @MockitoBean LibraryStorageService storageService;
    @MockitoBean LibraryService libraryService;

    @BeforeEach
    void stubStorage() {
        when(storageService.getLibraryFileContent(any()))
                .thenReturn("public-catalog-test-content");
    }

    // -------------------------------------------------------------------------
    // Helpers (mirror LibraryVisibilityControllerTest)
    // -------------------------------------------------------------------------

    private Organization makeOrg(String label) {
        Organization o = new Organization();
        o.setName(label + "-" + System.nanoTime());
        o.setActive(true);
        o.setCreatedAt(LocalDateTime.now());
        return orgRepo.save(o);
    }

    private User makeUser(String username, Organization org) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword(passwordEncoder.encode("CorrectH0rse!Batt"));
        u.setEnabled(true);
        u = userRepo.save(u);
        if (org != null) {
            memRepo.save(new OrganizationMembership(u, org, OrganizationRole.USER));
        }
        return u;
    }

    /** Build and persist a library item (PUBLIC items get publishedAt stamps). */
    private LibraryItem makeItem(User creator, Organization org, Visibility v,
                                  String title, String description, String oscalType) {
        LibraryItem item = new LibraryItem(
                UUID.randomUUID().toString(),
                title,
                description,
                oscalType,
                creator);
        item.setOrganization(org);
        item.setVisibility(v);
        if (v == Visibility.PUBLIC) {
            LocalDateTime now = LocalDateTime.now();
            item.setPublishedAt(now);
            item.setLastPublishedAt(now);
        }
        return libraryItemRepo.save(item);
    }

    /** Attach a current LibraryVersion to the item so download paths work. */
    private LibraryItem withCurrentVersion(LibraryItem item, User uploader) {
        LibraryVersion version = new LibraryVersion(
                UUID.randomUUID().toString(),
                item,
                1,
                "test.json",
                "JSON",
                42L,
                "blob/" + item.getItemId() + "/v1/test.json",
                uploader,
                "Initial version");
        version = libraryVersionRepo.save(version);
        item.setCurrentVersion(version);
        return libraryItemRepo.save(item);
    }

    /** Build a PublicItemSummary record from a (real or fake) LibraryItem. */
    private PublicItemSummary summary(LibraryItem item) {
        return new PublicItemSummary(
                item.getItemId(),
                item.getTitle(),
                item.getDescription(),
                item.getOscalType(),
                List.of(),
                item.getCurrentVersion() != null ? item.getCurrentVersion().getVersionNumber() : null,
                item.getPublishedAt(),
                item.getLastPublishedAt(),
                item.getDownloadCount() == null ? 0L : item.getDownloadCount(),
                null,
                0L,
                item.getOrganization() != null ? item.getOrganization().getName() : null,
                item.getOrganization() != null ? item.getOrganization().getLogoUrl() : null);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void listPublicReturnsOnlyPublicItems() throws Exception {
        Organization orgA = makeOrg("orgA");
        User creator = makeUser("creator-list-" + System.nanoTime(), orgA);

        // Seed all three visibilities; the service is responsible for filtering.
        // We stub it to return exactly the PUBLIC one so the controller's
        // page-shape contract is the surface under test.
        makeItem(creator, orgA, Visibility.PRIVATE, "Private Item", "private", "catalog");
        makeItem(creator, orgA, Visibility.ORGANIZATION, "Org Item", "org", "catalog");
        LibraryItem pub = makeItem(creator, orgA, Visibility.PUBLIC,
                "Public Catalog Alpha", "publicly readable", "catalog");

        when(libraryService.searchPublic(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary(pub)), PageRequest.of(0, 24), 1));

        mockMvc.perform(get("/api/public/catalog/items"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content.length()").value(1))
               .andExpect(jsonPath("$.content[0].itemId").value(pub.getItemId()))
               .andExpect(jsonPath("$.content[0].title").value("Public Catalog Alpha"));
    }

    @Test
    void searchByKeywordMatchesTitleAndDescription() throws Exception {
        Organization orgA = makeOrg("orgA");
        User creator = makeUser("creator-search-" + System.nanoTime(), orgA);

        LibraryItem fedramp = makeItem(creator, orgA, Visibility.PUBLIC,
                "FedRAMP Catalog Foo", "FedRAMP baseline content", "catalog");
        // The non-matching item is seeded too — the assertion is that the
        // controller forwards `q=FedRAMP` to the service and serializes only
        // what it gets back (which is the FedRAMP item).
        makeItem(creator, orgA, Visibility.PUBLIC,
                "NIST Profile Bar", "NIST 800-53 derived profile", "profile");

        when(libraryService.searchPublic(eq("FedRAMP"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary(fedramp)), PageRequest.of(0, 24), 1));

        mockMvc.perform(get("/api/public/catalog/items").param("q", "FedRAMP"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content.length()").value(1))
               .andExpect(jsonPath("$.content[0].itemId").value(fedramp.getItemId()));

        verify(libraryService).searchPublic(eq("FedRAMP"), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void filterByTypeRestrictsResults() throws Exception {
        Organization orgA = makeOrg("orgA");
        User creator = makeUser("creator-type-" + System.nanoTime(), orgA);

        LibraryItem cat = makeItem(creator, orgA, Visibility.PUBLIC,
                "Public Catalog C", "catalog content", "catalog");
        makeItem(creator, orgA, Visibility.PUBLIC,
                "Public Profile P", "profile content", "profile");

        when(libraryService.searchPublic(isNull(), eq("catalog"), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary(cat)), PageRequest.of(0, 24), 1));

        mockMvc.perform(get("/api/public/catalog/items").param("type", "catalog"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content.length()").value(1))
               .andExpect(jsonPath("$.content[0].itemId").value(cat.getItemId()))
               .andExpect(jsonPath("$.content[0].oscalType").value("catalog"));

        verify(libraryService).searchPublic(isNull(), eq("catalog"), isNull(), any(Pageable.class));
    }

    @Test
    void getPublicByIdReturns200ForPublic404ForPrivate() throws Exception {
        Organization orgA = makeOrg("orgA");
        User creator = makeUser("creator-detail-" + System.nanoTime(), orgA);

        LibraryItem pub = makeItem(creator, orgA, Visibility.PUBLIC,
                "Detail Public", "detail-public", "catalog");
        LibraryItem priv = makeItem(creator, orgA, Visibility.PRIVATE,
                "Detail Private", "detail-private", "catalog");

        when(libraryService.getPublic(pub.getItemId()))
                .thenReturn(Optional.of(summary(pub)));
        when(libraryService.getPublic(priv.getItemId()))
                .thenReturn(Optional.empty());  // mirrors findPublicByItemId visibility filter

        mockMvc.perform(get("/api/public/catalog/items/{id}", pub.getItemId()))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.itemId").value(pub.getItemId()));

        mockMvc.perform(get("/api/public/catalog/items/{id}", priv.getItemId()))
               .andExpect(status().isNotFound());
    }

    @Test
    void downloadIncrementsDownloadCount() throws Exception {
        Organization orgA = makeOrg("orgA");
        User creator = makeUser("creator-dl-" + System.nanoTime(), orgA);

        LibraryItem pub = makeItem(creator, orgA, Visibility.PUBLIC,
                "Downloadable", "downloadable content", "catalog");
        pub = withCurrentVersion(pub, creator);

        // Service returns the canned content; the production impl is the one
        // that increments downloadCount and persists. We assert here that the
        // controller invokes the service exactly once per HTTP call (so the
        // counter side-effect is wired) and surfaces the bytes.
        // Controller uses the 2-arg overload (itemId, caller); stub that one.
        when(libraryService.getPublicLatestContent(eq(pub.getItemId()), any()))
                .thenReturn(Optional.of(new LibraryService.VersionDownload(
                        "public-catalog-test-content", "test.json", "JSON")));

        // SecurityConfig deliberately requires auth on /content downloads
        // ("anonymous users can discover items but must sign in to download"
        // — see SecurityConfig.java:118-123). Browse + detail are public;
        // bytes are not.
        mockMvc.perform(get("/api/public/catalog/items/{id}/content", pub.getItemId())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .user(creator.getUsername())))
               .andExpect(status().isOk())
               .andExpect(content().bytes("public-catalog-test-content".getBytes()));

        verify(libraryService, times(1)).getPublicLatestContent(eq(pub.getItemId()), any());
    }

    @Test
    void viewIncrementsViewCount() throws Exception {
        Organization orgA = makeOrg("orgA");
        User creator = makeUser("creator-view-" + System.nanoTime(), orgA);

        LibraryItem pub = makeItem(creator, orgA, Visibility.PUBLIC,
                "Viewable", "viewable content", "catalog");

        // The viewCount increment lives inside LibraryService.getPublic. The
        // controller's job is to call it exactly once per detail-fetch — so
        // that's the assertion we can make at this layer.
        when(libraryService.getPublic(pub.getItemId()))
                .thenReturn(Optional.of(summary(pub)));

        mockMvc.perform(get("/api/public/catalog/items/{id}", pub.getItemId()))
               .andExpect(status().isOk());

        verify(libraryService, times(1)).getPublic(pub.getItemId());
    }

    @Test
    void unauthenticatedAccessAllowed() throws Exception {
        // No Authorization header, no @WithMockUser — anonymous browse must
        // succeed. The SecurityConfig whitelist for /api/public/catalog/**
        // is the surface under test.
        Page<PublicItemSummary> empty = new PageImpl<>(List.of(), PageRequest.of(0, 24), 0);
        when(libraryService.searchPublic(any(), any(), any(), any(Pageable.class)))
                .thenReturn(empty);

        mockMvc.perform(get("/api/public/catalog/items"))
               .andExpect(status().isOk());
    }
}
