package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.Catalog;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.CatalogRequest;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.CatalogService;
import gov.nist.oscal.tools.api.service.RateLimitService;
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

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CatalogService catalogService;

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

    private Catalog catalog(long id, String title, User user) {
        Catalog c = new Catalog();
        c.setId(id);
        c.setTitle(title);
        c.setDescription("desc");
        c.setVersion("1.0.0");
        c.setOscalVersion("1.1.3");
        c.setStoragePath("build/testuser/catalog-uuid.json");
        c.setFilename("catalog-uuid.json");
        c.setOscalUuid("uuid-123");
        c.setGroupCount(2);
        c.setControlCount(15);
        c.setParamCount(3);
        c.setCreatedBy(user);
        c.setLastUpdatedBy(user);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return c;
    }

    private CatalogRequest req() {
        CatalogRequest r = new CatalogRequest();
        r.setTitle("T");
        r.setDescription("D");
        r.setVersion("1.0.0");
        r.setOscalVersion("1.1.3");
        r.setFilename("catalog-uuid.json");
        r.setJsonContent("{\"catalog\":{}}");
        r.setOscalUuid("uuid-123");
        r.setGroupCount(2);
        r.setControlCount(15);
        r.setParamCount(3);
        return r;
    }

    @Test
    @WithMockUser(username = "testuser")
    void create_success_returns201() throws Exception {
        Catalog c = catalog(1L, "T", user("testuser"));
        when(catalogService.createCatalog(
                eq("T"), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyInt(), anyInt(), any(), eq("testuser")))
                .thenReturn(c);

        mockMvc.perform(post("/api/build/catalogs")
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
        when(catalogService.createCatalog(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyInt(), anyInt(), any(), anyString()))
                .thenThrow(new RuntimeException("Catalog with UUID x already exists"));

        mockMvc.perform(post("/api/build/catalogs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "testuser")
    void create_otherRuntime_returns400() throws Exception {
        when(catalogService.createCatalog(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyInt(), anyInt(), any(), anyString()))
                .thenThrow(new RuntimeException("bad data"));

        mockMvc.perform(post("/api/build/catalogs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void create_validation_failsWhenTitleMissing() throws Exception {
        CatalogRequest invalid = req();
        invalid.setTitle("");

        mockMvc.perform(post("/api/build/catalogs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(catalogService, never()).createCatalog(
                anyString(), any(), any(), anyString(), anyString(),
                anyString(), any(), any(), any(), any(), any(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void update_success_returnsOk() throws Exception {
        when(catalogService.updateCatalog(
                eq(1L), anyString(), any(), any(), anyString(),
                any(), any(), any(), any(), eq("testuser")))
                .thenReturn(catalog(1L, "Updated", user("testuser")));

        mockMvc.perform(put("/api/build/catalogs/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void update_notCreator_returns403() throws Exception {
        when(catalogService.updateCatalog(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("Only the creator can update this catalog"));

        mockMvc.perform(put("/api/build/catalogs/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void update_notFound_returns404() throws Exception {
        when(catalogService.updateCatalog(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("Catalog not found: 999"));

        mockMvc.perform(put("/api/build/catalogs/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void get_byId_returnsOk() throws Exception {
        when(catalogService.getCatalog(1L)).thenReturn(catalog(1L, "T", user("testuser")));

        mockMvc.perform(get("/api/build/catalogs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "testuser")
    void get_notFound_returns404() throws Exception {
        when(catalogService.getCatalog(999L)).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(get("/api/build/catalogs/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getByUuid_returnsOk() throws Exception {
        when(catalogService.getCatalogByUuid("uuid-123")).thenReturn(catalog(1L, "T", user("testuser")));

        mockMvc.perform(get("/api/build/catalogs/uuid/uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oscalUuid").value("uuid-123"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getContent_returnsContent() throws Exception {
        when(catalogService.getCatalogContent(1L)).thenReturn("{\"catalog\":{}}");

        mockMvc.perform(get("/api/build/catalogs/1/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("{\"catalog\":{}}"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void list_returnsArray() throws Exception {
        when(catalogService.getUserCatalogs("testuser")).thenReturn(
                List.of(catalog(1L, "A", user("testuser")), catalog(2L, "B", user("testuser"))));

        mockMvc.perform(get("/api/build/catalogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(username = "testuser")
    void search_passesQueryToService() throws Exception {
        when(catalogService.searchCatalogs("testuser", "AC"))
                .thenReturn(List.of(catalog(1L, "AC catalog", user("testuser"))));

        mockMvc.perform(get("/api/build/catalogs/search").param("q", "AC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        verify(catalogService).searchCatalogs("testuser", "AC");
    }

    @Test
    @WithMockUser(username = "testuser")
    void delete_success_returns200() throws Exception {
        doNothing().when(catalogService).deleteCatalog(1L, "testuser");

        mockMvc.perform(delete("/api/build/catalogs/1").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void delete_notCreator_returns403() throws Exception {
        doThrow(new RuntimeException("Only the creator can delete this catalog"))
                .when(catalogService).deleteCatalog(eq(1L), anyString());

        mockMvc.perform(delete("/api/build/catalogs/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void statistics_returnsMap() throws Exception {
        when(catalogService.getStatistics("testuser")).thenReturn(Map.of(
                "totalCatalogs", 3,
                "totalControls", 42));

        mockMvc.perform(get("/api/build/catalogs/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCatalogs").value(3))
                .andExpect(jsonPath("$.totalControls").value(42));
    }
}
