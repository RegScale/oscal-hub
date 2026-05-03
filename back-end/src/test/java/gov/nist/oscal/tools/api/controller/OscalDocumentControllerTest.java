package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.OscalDocument;
import gov.nist.oscal.tools.api.entity.OscalModelType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.OscalDocumentRequest;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.OscalDocumentService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OscalDocumentController.class)
class OscalDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OscalDocumentService documentService;

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

    private OscalDocument doc(long id, String title, OscalModelType type, User user) {
        OscalDocument d = new OscalDocument();
        d.setId(id);
        d.setTitle(title);
        d.setOscalUuid("uuid-" + id);
        d.setModelType(type);
        d.setVersion("1.0.0");
        d.setOscalVersion("1.1.3");
        d.setStoragePath("build/testuser/" + type.slug() + "-uuid.json");
        d.setFilename(type.slug() + "-uuid.json");
        d.setCreatedBy(user);
        d.setLastUpdatedBy(user);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        return d;
    }

    private OscalDocumentRequest req(String modelTypeSlug) {
        OscalDocumentRequest r = new OscalDocumentRequest();
        r.setModelType(modelTypeSlug);
        r.setTitle("T");
        r.setVersion("1.0.0");
        r.setOscalVersion("1.1.3");
        r.setFilename(modelTypeSlug + "-uuid.json");
        r.setJsonContent("{\"" + modelTypeSlug + "\":{}}");
        r.setOscalUuid("uuid-1");
        return r;
    }

    @Test
    @WithMockUser(username = "testuser")
    void create_ssp_returns201() throws Exception {
        when(documentService.create(
                eq(OscalModelType.SYSTEM_SECURITY_PLAN), eq("T"), any(), any(), anyString(),
                anyString(), anyString(), anyString(), any(), any(), eq("testuser")))
                .thenReturn(doc(1L, "T", OscalModelType.SYSTEM_SECURITY_PLAN, user("testuser")));

        mockMvc.perform(post("/api/build/oscal-documents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req("system-security-plan"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modelType").value("system-security-plan"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void create_invalidModelType_returns400() throws Exception {
        OscalDocumentRequest r = req("not-a-real-model");

        mockMvc.perform(post("/api/build/oscal-documents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void create_duplicate_returns409() throws Exception {
        when(documentService.create(
                any(), anyString(), any(), any(), anyString(),
                anyString(), anyString(), any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("Document with UUID x already exists"));

        mockMvc.perform(post("/api/build/oscal-documents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req("system-security-plan"))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "testuser")
    void update_success_returnsOk() throws Exception {
        when(documentService.update(eq(1L), any(), any(), any(), any(), any(), any(), eq("testuser")))
                .thenReturn(doc(1L, "Updated", OscalModelType.ASSESSMENT_PLAN, user("testuser")));

        mockMvc.perform(put("/api/build/oscal-documents/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req("assessment-plan"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void update_notCreator_returns403() throws Exception {
        when(documentService.update(anyLong(), any(), any(), any(), any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("Only the creator can update this document"));

        mockMvc.perform(put("/api/build/oscal-documents/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req("system-security-plan"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void list_byModelType_returnsArray() throws Exception {
        when(documentService.listByUserAndType("testuser", OscalModelType.PLAN_OF_ACTION_AND_MILESTONES))
                .thenReturn(List.of(doc(1L, "POAM A", OscalModelType.PLAN_OF_ACTION_AND_MILESTONES, user("testuser"))));

        mockMvc.perform(get("/api/build/oscal-documents").param("modelType", "plan-of-action-and-milestones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].modelType").value("plan-of-action-and-milestones"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void list_invalidModelType_returns400() throws Exception {
        mockMvc.perform(get("/api/build/oscal-documents").param("modelType", "bogus"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void search_passesParams() throws Exception {
        when(documentService.search("testuser", OscalModelType.ASSESSMENT_RESULTS, "Q3"))
                .thenReturn(List.of(doc(1L, "Q3 results", OscalModelType.ASSESSMENT_RESULTS, user("testuser"))));

        mockMvc.perform(get("/api/build/oscal-documents/search")
                        .param("modelType", "assessment-results")
                        .param("q", "Q3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        verify(documentService).search("testuser", OscalModelType.ASSESSMENT_RESULTS, "Q3");
    }

    @Test
    @WithMockUser(username = "testuser")
    void getContent_returnsContent() throws Exception {
        when(documentService.getContent(1L)).thenReturn("{\"x\":1}");

        mockMvc.perform(get("/api/build/oscal-documents/1/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("{\"x\":1}"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void delete_success_returns200() throws Exception {
        doNothing().when(documentService).delete(1L, "testuser");

        mockMvc.perform(delete("/api/build/oscal-documents/1").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void delete_notCreator_returns403() throws Exception {
        doThrow(new RuntimeException("Only the creator can delete this document"))
                .when(documentService).delete(eq(1L), anyString());

        mockMvc.perform(delete("/api/build/oscal-documents/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getByUuid_returnsOk() throws Exception {
        when(documentService.getByUuid("uuid-1"))
                .thenReturn(doc(1L, "T", OscalModelType.ASSESSMENT_PLAN, user("testuser")));

        mockMvc.perform(get("/api/build/oscal-documents/uuid/uuid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oscalUuid").value("uuid-1"));
    }
}
