/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.OscalDocument;
import gov.nist.oscal.tools.api.entity.OscalModelType;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.OscalDocumentRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.LibraryStorageService;
import gov.nist.oscal.tools.api.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for
 * {@code POST /api/build/oscal-documents/{id}/save-to-library}.
 * Parameterized over all four supported model types (SSP, AP, AR, POAM) —
 * each verifies that the endpoint maps {@link OscalModelType} to the
 * corresponding {@link SourceType} on the persisted {@link LibraryItem}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OscalDocumentSaveToLibraryTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepo;
    @Autowired OscalDocumentRepository docRepo;
    @Autowired LibraryItemRepository libraryItemRepo;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean StorageService storageService;
    @MockitoBean LibraryStorageService libraryStorageService;
    @MockitoBean EmailService emailService;

    @BeforeEach
    void stubStorage() {
        when(storageService.downloadComponent(anyString())).thenReturn("{\"x\":1}");
        when(libraryStorageService.buildBlobPath(anyString(), anyString(), anyString()))
                .thenReturn("itemId/versionId/doc.json");
        when(libraryStorageService.saveLibraryFile(anyString(), anyString(), anyMap())).thenReturn(true);
    }

    static Stream<Arguments> modelTypeMatrix() {
        return Stream.of(
                Arguments.of(OscalModelType.SYSTEM_SECURITY_PLAN, SourceType.SSP, "alice-ssp"),
                Arguments.of(OscalModelType.ASSESSMENT_PLAN, SourceType.AP, "alice-ap"),
                Arguments.of(OscalModelType.ASSESSMENT_RESULTS, SourceType.AR, "alice-ar"),
                Arguments.of(OscalModelType.PLAN_OF_ACTION_AND_MILESTONES, SourceType.POAM, "alice-poam")
        );
    }

    private User makeUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword(passwordEncoder.encode("CorrectH0rse!Batt"));
        u.setEnabled(true);
        u.setGlobalRole(User.GlobalRole.USER);
        return userRepo.save(u);
    }

    private OscalDocument makeDocument(User creator, OscalModelType modelType, String oscalUuid) {
        OscalDocument d = new OscalDocument(
                oscalUuid, modelType, "Seed Doc",
                "build/" + creator.getUsername() + "/doc.json", creator);
        d.setFilename("doc.json");
        d.setFileSize(7L);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        return docRepo.save(d);
    }

    @ParameterizedTest(name = "modelType={0} -> sourceType={1}")
    @MethodSource("modelTypeMatrix")
    @WithMockUser(username = "placeholder") // overridden per-iteration via SecurityContext below
    void firstSave_createsLibraryItem_withMappedSourceType(
            OscalModelType modelType,
            SourceType expectedSourceType,
            String username) throws Exception {

        // Each parameter row uses its own username so iterations don't share data.
        // @WithMockUser sets a single username for the whole test method, so we
        // override the SecurityContext per iteration using a manual auth.
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        username, "password",
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));

        User alice = makeUser(username);
        String oscalUuid = UUID.randomUUID().toString();
        OscalDocument doc = makeDocument(alice, modelType, oscalUuid);

        Map<String, Object> body = new HashMap<>();
        body.put("title", "Saved " + modelType.name());
        body.put("description", "First save");
        body.put("visibility", "PRIVATE");

        mockMvc.perform(post("/api/build/oscal-documents/{id}/save-to-library", doc.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.itemId").exists())
               .andExpect(jsonPath("$.title").value("Saved " + modelType.name()));

        List<LibraryItem> items = libraryItemRepo.findByCreatedBy(alice);
        assertEquals(1, items.size(), "exactly one library item created for " + modelType);

        LibraryItem item = items.get(0);
        assertEquals(expectedSourceType, item.getSourceType(),
                "modelType=" + modelType + " should map to sourceType=" + expectedSourceType);
        assertEquals(UUID.fromString(oscalUuid), item.getSourceId(),
                "sourceId=oscalDocument.oscalUuid for " + modelType);
        assertEquals(alice.getId(), item.getCreatedBy().getId(), "createdBy=" + username);
        assertNotNull(item.getCurrentVersion(), "currentVersion is set for " + modelType);
        assertEquals(1, item.getCurrentVersion().getVersionNumber(),
                "version 1 created for " + modelType);
    }
}
