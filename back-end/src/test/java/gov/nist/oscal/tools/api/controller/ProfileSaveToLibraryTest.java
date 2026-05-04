/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.entity.Profile;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.LibraryVersionRepository;
import gov.nist.oscal.tools.api.repository.ProfileRepository;
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
 * Integration tests for {@code POST /api/build/profiles/{profileId}/save-to-library}.
 * Verifies idempotent behavior across the create-new and append-version paths.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileSaveToLibraryTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepo;
    @Autowired ProfileRepository profileRepo;
    @Autowired LibraryItemRepository libraryItemRepo;
    @Autowired LibraryVersionRepository libraryVersionRepo;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean StorageService storageService;
    @MockitoBean LibraryStorageService libraryStorageService;
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

    private Profile makeProfile(User creator, String oscalUuid) {
        Profile p = new Profile(oscalUuid, "Seed Profile", "build/" + creator.getUsername() + "/profile.json", creator);
        p.setFilename("profile.json");
        p.setFileSize(7L);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return profileRepo.save(p);
    }

    @Test
    @WithMockUser(username = "alice-prof-1")
    void firstSave_createsLibraryItem_withSourceLink() throws Exception {
        User alice = makeUser("alice-prof-1");
        String oscalUuid = UUID.randomUUID().toString();
        Profile profile = makeProfile(alice, oscalUuid);

        when(storageService.downloadComponent(anyString())).thenReturn("{\"x\":1}");
        when(libraryStorageService.buildBlobPath(anyString(), anyString(), anyString()))
                .thenReturn("itemId/versionId/profile.json");
        when(libraryStorageService.saveLibraryFile(anyString(), anyString(), anyMap())).thenReturn(true);

        Map<String, Object> body = new HashMap<>();
        body.put("title", "Saved Profile");
        body.put("description", "First save");
        body.put("visibility", "PRIVATE");

        mockMvc.perform(post("/api/build/profiles/{id}/save-to-library", profile.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.itemId").exists())
               .andExpect(jsonPath("$.title").value("Saved Profile"));

        List<LibraryItem> items = libraryItemRepo.findByCreatedBy(alice);
        assertEquals(1, items.size(), "exactly one library item created");
        LibraryItem item = items.get(0);
        assertEquals(SourceType.PROFILE, item.getSourceType(), "sourceType=PROFILE");
        assertEquals(UUID.fromString(oscalUuid), item.getSourceId(), "sourceId=profile.oscalUuid");
        assertEquals(alice.getId(), item.getCreatedBy().getId(), "createdBy=alice");
        assertNotNull(item.getCurrentVersion(), "currentVersion is set");
        assertEquals(1, item.getCurrentVersion().getVersionNumber(), "version 1");
    }

    @Test
    @WithMockUser(username = "alice-prof-2")
    void secondSave_appendsNewVersion_toSameItem() throws Exception {
        User alice = makeUser("alice-prof-2");
        String oscalUuid = UUID.randomUUID().toString();
        Profile profile = makeProfile(alice, oscalUuid);

        when(storageService.downloadComponent(anyString())).thenReturn("{\"x\":1}");
        when(libraryStorageService.buildBlobPath(anyString(), anyString(), anyString()))
                .thenReturn("itemId/versionId/profile.json");
        when(libraryStorageService.saveLibraryFile(anyString(), anyString(), anyMap())).thenReturn(true);

        Map<String, Object> body = new HashMap<>();
        body.put("title", "Saved Profile");
        body.put("visibility", "PRIVATE");

        mockMvc.perform(post("/api/build/profiles/{id}/save-to-library", profile.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isCreated());

        mockMvc.perform(post("/api/build/profiles/{id}/save-to-library", profile.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isCreated());

        List<LibraryItem> items = libraryItemRepo.findByCreatedBy(alice);
        assertEquals(1, items.size(), "still only one library item");

        LibraryItem item = items.get(0);
        List<LibraryVersion> versions = libraryVersionRepo.findByLibraryItemOrderByVersionNumberDesc(item);
        assertEquals(2, versions.size(), "two versions persisted");
        assertEquals(2, item.getCurrentVersion().getVersionNumber(), "currentVersion=v2");
    }
}
