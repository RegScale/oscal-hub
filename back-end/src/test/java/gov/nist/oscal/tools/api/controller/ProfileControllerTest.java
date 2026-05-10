package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.Profile;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.ProfileRequest;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.ProfileService;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.service.library.LibraryIngestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private LibraryIngestService libraryIngestService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    @MockitoBean
    private SecurityHeadersConfig securityHeadersConfig;

    private User user(String username) {
        User u = new User();
        u.setId(1L);
        u.setUsername(username);
        return u;
    }

    private Profile profile(long id, String title, User user) {
        Profile p = new Profile();
        p.setId(id);
        p.setTitle(title);
        p.setDescription("desc");
        p.setVersion("1.0.0");
        p.setOscalVersion("1.1.3");
        p.setStoragePath("build/testuser/profile-uuid.json");
        p.setFilename("profile-uuid.json");
        p.setOscalUuid("uuid-123");
        p.setImportCount(2);
        p.setControlCount(50);
        p.setAlterCount(5);
        p.setCreatedBy(user);
        p.setLastUpdatedBy(user);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    private ProfileRequest req() {
        ProfileRequest r = new ProfileRequest();
        r.setTitle("T");
        r.setDescription("D");
        r.setVersion("1.0.0");
        r.setOscalVersion("1.1.3");
        r.setFilename("profile-uuid.json");
        r.setJsonContent("{\"profile\":{}}");
        r.setOscalUuid("uuid-123");
        r.setImportCount(2);
        r.setControlCount(50);
        r.setAlterCount(5);
        return r;
    }

    @Test
    @WithMockUser(username = "testuser")
    void create_success_returns201() throws Exception {
        when(profileService.createProfile(
                eq("T"), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyInt(), anyInt(), any(), eq("testuser")))
                .thenReturn(profile(1L, "T", user("testuser")));

        mockMvc.perform(post("/api/build/profiles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("T"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void create_duplicate_returns409() throws Exception {
        when(profileService.createProfile(
                anyString(), any(), any(), anyString(), anyString(),
                anyString(), any(), any(), any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("Profile with UUID x already exists"));

        mockMvc.perform(post("/api/build/profiles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "testuser")
    void create_otherRuntime_returns400() throws Exception {
        when(profileService.createProfile(
                anyString(), any(), any(), anyString(), anyString(),
                anyString(), any(), any(), any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("bad data"));

        mockMvc.perform(post("/api/build/profiles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void update_success_returnsOk() throws Exception {
        when(profileService.updateProfile(
                eq(1L), anyString(), any(), any(), anyString(),
                any(), any(), any(), any(), eq("testuser")))
                .thenReturn(profile(1L, "Updated", user("testuser")));

        mockMvc.perform(put("/api/build/profiles/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void update_notCreator_returns403() throws Exception {
        when(profileService.updateProfile(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("Only the creator can update this profile"));

        mockMvc.perform(put("/api/build/profiles/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void get_byId_returnsOk() throws Exception {
        when(profileService.getProfile(1L)).thenReturn(profile(1L, "T", user("testuser")));

        mockMvc.perform(get("/api/build/profiles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "testuser")
    void get_notFound_returns404() throws Exception {
        when(profileService.getProfile(999L)).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(get("/api/build/profiles/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getByUuid_returnsOk() throws Exception {
        when(profileService.getProfileByUuid("uuid-123")).thenReturn(profile(1L, "T", user("testuser")));

        mockMvc.perform(get("/api/build/profiles/uuid/uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oscalUuid").value("uuid-123"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getContent_returnsContent() throws Exception {
        when(profileService.getProfileContent(1L)).thenReturn("{\"profile\":{}}");

        mockMvc.perform(get("/api/build/profiles/1/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("{\"profile\":{}}"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void list_returnsArray() throws Exception {
        when(profileService.getUserProfiles("testuser")).thenReturn(
                List.of(profile(1L, "A", user("testuser"))));

        mockMvc.perform(get("/api/build/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "testuser")
    void search_passesQueryToService() throws Exception {
        when(profileService.searchProfiles("testuser", "moderate"))
                .thenReturn(List.of(profile(1L, "Mod", user("testuser"))));

        mockMvc.perform(get("/api/build/profiles/search").param("q", "moderate"))
                .andExpect(status().isOk());
        verify(profileService).searchProfiles("testuser", "moderate");
    }

    @Test
    @WithMockUser(username = "testuser")
    void delete_success_returns200() throws Exception {
        doNothing().when(profileService).deleteProfile(1L, "testuser");

        mockMvc.perform(delete("/api/build/profiles/1").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void delete_notCreator_returns403() throws Exception {
        doThrow(new RuntimeException("Only the creator can delete this profile"))
                .when(profileService).deleteProfile(eq(1L), anyString());

        mockMvc.perform(delete("/api/build/profiles/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void statistics_returnsMap() throws Exception {
        when(profileService.getStatistics("testuser")).thenReturn(Map.of(
                "totalProfiles", 4,
                "totalImports", 8));

        mockMvc.perform(get("/api/build/profiles/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProfiles").value(4));
    }
}
