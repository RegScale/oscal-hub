/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.Catalog;
import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.CatalogRepository;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.LibraryVersionRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.LibraryStorageService;
import gov.nist.oscal.tools.api.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code POST /api/build/catalogs/{catalogId}/save-to-library}.
 * Verifies idempotent behavior: first call creates a {@link LibraryItem} +
 * version 1; second call appends a new version to the same item.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogSaveToLibraryTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepo;
    @Autowired CatalogRepository catalogRepo;
    @Autowired LibraryItemRepository libraryItemRepo;
    @Autowired LibraryVersionRepository libraryVersionRepo;
    @Autowired PasswordEncoder passwordEncoder;

    // Storage layers are mocked so the test doesn't need real Azure/local files.
    @MockitoBean StorageService storageService;
    @MockitoBean LibraryStorageService libraryStorageService;

    // EmailService is mocked to avoid SMTP probes during context init.
    @MockitoBean EmailService emailService;

    private User makeUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword(passwordEncoder.encode("CorrectH0rse!Batt"));
        u.setEnabled(true);
        u.setGlobalRole(User.GlobalRole.USER);
        return userRepo.save(u);
    }

    private Catalog makeCatalog(User creator, String oscalUuid) {
        Catalog c = new Catalog(oscalUuid, "Seed Catalog", "build/" + creator.getUsername() + "/cat.json", creator);
        c.setFilename("cat.json");
        c.setFileSize(7L);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return catalogRepo.save(c);
    }

    @Test
    @WithMockUser(username = "alice-cat-1")
    void firstSave_createsLibraryItem_withSourceLink() throws Exception {
        User alice = makeUser("alice-cat-1");
        String oscalUuid = UUID.randomUUID().toString();
        Catalog catalog = makeCatalog(alice, oscalUuid);

        // Resolver downloads catalog content via StorageService.
        when(storageService.downloadComponent(anyString())).thenReturn("{\"x\":1}");
        // Library blob write succeeds (returning true).
        when(libraryStorageService.buildBlobPath(anyString(), anyString(), anyString()))
                .thenReturn("itemId/versionId/cat.json");
        when(libraryStorageService.saveLibraryFile(anyString(), anyString(), anyMap())).thenReturn(true);

        Map<String, Object> body = new HashMap<>();
        body.put("title", "Saved Catalog");
        body.put("description", "First save");
        body.put("visibility", "PRIVATE");

        mockMvc.perform(post("/api/build/catalogs/{id}/save-to-library", catalog.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.itemId").exists())
               .andExpect(jsonPath("$.title").value("Saved Catalog"));

        List<LibraryItem> items = libraryItemRepo.findByCreatedBy(alice);
        assertEquals(1, items.size(), "exactly one library item created");
        LibraryItem item = items.get(0);
        assertEquals(SourceType.CATALOG, item.getSourceType(), "sourceType=CATALOG");
        assertEquals(UUID.fromString(oscalUuid), item.getSourceId(), "sourceId=catalog.oscalUuid");
        assertEquals(alice.getId(), item.getCreatedBy().getId(), "createdBy=alice");
        assertNotNull(item.getCurrentVersion(), "currentVersion is set");
        assertEquals(1, item.getCurrentVersion().getVersionNumber(), "version 1");
    }

    @Test
    @WithMockUser(username = "alice-cat-2")
    void secondSave_appendsNewVersion_toSameItem() throws Exception {
        User alice = makeUser("alice-cat-2");
        String oscalUuid = UUID.randomUUID().toString();
        Catalog catalog = makeCatalog(alice, oscalUuid);

        when(storageService.downloadComponent(anyString())).thenReturn("{\"x\":1}");
        when(libraryStorageService.buildBlobPath(anyString(), anyString(), anyString()))
                .thenReturn("itemId/versionId/cat.json");
        when(libraryStorageService.saveLibraryFile(anyString(), anyString(), anyMap())).thenReturn(true);

        Map<String, Object> body = new HashMap<>();
        body.put("title", "Saved Catalog");
        body.put("visibility", "PRIVATE");

        // First save.
        mockMvc.perform(post("/api/build/catalogs/{id}/save-to-library", catalog.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isCreated());

        // Second save (idempotent: should append a new version, not create a new item).
        mockMvc.perform(post("/api/build/catalogs/{id}/save-to-library", catalog.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isCreated());

        List<LibraryItem> items = libraryItemRepo.findByCreatedBy(alice);
        assertEquals(1, items.size(), "still only one library item — second save appended");

        LibraryItem item = items.get(0);
        List<LibraryVersion> versions = libraryVersionRepo.findByLibraryItemOrderByVersionNumberDesc(item);
        assertEquals(2, versions.size(), "two versions persisted");
        assertEquals(2, item.getCurrentVersion().getVersionNumber(), "currentVersion=v2");
    }
}
