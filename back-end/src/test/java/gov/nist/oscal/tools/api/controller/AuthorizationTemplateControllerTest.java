package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.AuthorizationTemplateRequest;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.AuthorizationTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthorizationTemplateController.class)
class AuthorizationTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthorizationTemplateService templateService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    private User createMockUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setId(1L);
        return user;
    }

    private AuthorizationTemplate createMockTemplate(Long id, String name, String content, User user) {
        AuthorizationTemplate template = new AuthorizationTemplate();
        template.setId(id);
        template.setName(name);
        template.setContent(content);
        template.setCreatedBy(user);
        template.setLastUpdatedBy(user);
        template.setCreatedAt(java.time.LocalDateTime.now());
        template.setLastUpdatedAt(java.time.LocalDateTime.now());
        return template;
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateTemplate_success_returnsCreatedTemplate() throws Exception {
        // Arrange
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        request.setName("Test Template");
        request.setContent("This is a template with {{variable1}} and {{variable2}}");

        User mockUser = createMockUser("testuser");
        AuthorizationTemplate template = createMockTemplate(
                1L, "Test Template", "This is a template with {{variable1}} and {{variable2}}", mockUser);

        Set<String> variables = new HashSet<>(Arrays.asList("variable1", "variable2"));

        when(templateService.createTemplate(eq("Test Template"), anyString(), eq("testuser")))
                .thenReturn(template);
        when(templateService.extractVariables(anyString())).thenReturn(variables);

        // Act & Assert
        mockMvc.perform(post("/api/authorization-templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Template"))
                .andExpect(jsonPath("$.variables.length()").value(2));

        verify(templateService, times(1)).createTemplate(eq("Test Template"), anyString(), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateTemplate_serviceException_returns400() throws Exception {
        // Arrange
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        request.setName("Test Template");
        request.setContent("Content");

        when(templateService.createTemplate(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(post("/api/authorization-templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateTemplate_success_returnsUpdatedTemplate() throws Exception {
        // Arrange
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        request.setName("Updated Template");
        request.setContent("Updated content with {{newVar}}");

        User mockUser = createMockUser("testuser");
        AuthorizationTemplate template = createMockTemplate(
                1L, "Updated Template", "Updated content with {{newVar}}", mockUser);

        Set<String> variables = new HashSet<>(Arrays.asList("newVar"));

        when(templateService.updateTemplate(eq(1L), eq("Updated Template"), anyString(), eq("testuser")))
                .thenReturn(template);
        when(templateService.extractVariables(anyString())).thenReturn(variables);

        // Act & Assert
        mockMvc.perform(put("/api/authorization-templates/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Template"))
                .andExpect(jsonPath("$.variables.length()").value(1));

        verify(templateService, times(1)).updateTemplate(eq(1L), eq("Updated Template"), anyString(), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateTemplate_notFound_returns404() throws Exception {
        // Arrange
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        request.setName("Updated Template");
        request.setContent("Content");

        when(templateService.updateTemplate(eq(999L), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Not found"));

        // Act & Assert
        mockMvc.perform(put("/api/authorization-templates/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetTemplate_success_returnsTemplate() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        AuthorizationTemplate template = createMockTemplate(
                1L, "Test Template", "Content with {{var1}}", mockUser);

        Set<String> variables = new HashSet<>(Arrays.asList("var1"));

        when(templateService.getTemplate(1L)).thenReturn(template);
        when(templateService.extractVariables(anyString())).thenReturn(variables);

        // Act & Assert
        mockMvc.perform(get("/api/authorization-templates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Template"))
                .andExpect(jsonPath("$.variables.length()").value(1));

        verify(templateService, times(1)).getTemplate(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetTemplate_notFound_returns404() throws Exception {
        // Arrange
        when(templateService.getTemplate(999L)).thenThrow(new RuntimeException("Not found"));

        // Act & Assert
        mockMvc.perform(get("/api/authorization-templates/999"))
                .andExpect(status().isNotFound());

        verify(templateService, times(1)).getTemplate(999L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAllTemplates_success_returnsTemplates() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        AuthorizationTemplate template1 = createMockTemplate(
                1L, "Template 1", "Content 1 {{var1}}", mockUser);

        AuthorizationTemplate template2 = createMockTemplate(
                2L, "Template 2", "Content 2 {{var2}}", mockUser);

        List<AuthorizationTemplate> templates = Arrays.asList(template1, template2);

        when(templateService.getAllTemplates()).thenReturn(templates);
        when(templateService.extractVariables(anyString())).thenReturn(new HashSet<>());

        // Act & Assert
        mockMvc.perform(get("/api/authorization-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Template 1"))
                .andExpect(jsonPath("$[1].name").value("Template 2"));

        verify(templateService, times(1)).getAllTemplates();
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAllTemplates_serviceException_returns500() throws Exception {
        // Arrange
        when(templateService.getAllTemplates()).thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(get("/api/authorization-templates"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentlyUpdated_success_returnsTemplates() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        AuthorizationTemplate template1 = createMockTemplate(
                1L, "Recent Template", "Recent content", mockUser);

        List<AuthorizationTemplate> templates = Arrays.asList(template1);

        when(templateService.getRecentlyUpdated(10)).thenReturn(templates);
        when(templateService.extractVariables(anyString())).thenReturn(new HashSet<>());

        // Act & Assert
        mockMvc.perform(get("/api/authorization-templates/recent")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Recent Template"));

        verify(templateService, times(1)).getRecentlyUpdated(10);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentlyUpdated_defaultLimit_uses10() throws Exception {
        // Arrange
        when(templateService.getRecentlyUpdated(10)).thenReturn(Arrays.asList());
        when(templateService.extractVariables(anyString())).thenReturn(new HashSet<>());

        // Act & Assert
        mockMvc.perform(get("/api/authorization-templates/recent"))
                .andExpect(status().isOk());

        verify(templateService, times(1)).getRecentlyUpdated(10);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentlyUpdated_serviceException_returns500() throws Exception {
        // Arrange
        when(templateService.getRecentlyUpdated(anyInt())).thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(get("/api/authorization-templates/recent"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchTemplates_success_returnsMatchingTemplates() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        AuthorizationTemplate template1 = createMockTemplate(
                1L, "Matching Template", "Content", mockUser);

        List<AuthorizationTemplate> templates = Arrays.asList(template1);

        when(templateService.searchTemplates("matching")).thenReturn(templates);
        when(templateService.extractVariables(anyString())).thenReturn(new HashSet<>());

        // Act & Assert
        mockMvc.perform(get("/api/authorization-templates/search")
                .param("q", "matching"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Matching Template"));

        verify(templateService, times(1)).searchTemplates("matching");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchTemplates_noQuery_searchesAll() throws Exception {
        // Arrange
        when(templateService.searchTemplates(null)).thenReturn(Arrays.asList());
        when(templateService.extractVariables(anyString())).thenReturn(new HashSet<>());

        // Act & Assert
        mockMvc.perform(get("/api/authorization-templates/search"))
                .andExpect(status().isOk());

        verify(templateService, times(1)).searchTemplates(null);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchTemplates_serviceException_returns500() throws Exception {
        // Arrange
        when(templateService.searchTemplates(anyString())).thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(get("/api/authorization-templates/search")
                .param("q", "test"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetTemplateVariables_success_returnsVariables() throws Exception {
        // Arrange
        Set<String> variables = new HashSet<>(Arrays.asList("var1", "var2", "var3"));

        when(templateService.extractVariablesFromTemplate(1L)).thenReturn(variables);

        // Act & Assert
        mockMvc.perform(get("/api/authorization-templates/1/variables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variables.length()").value(3));

        verify(templateService, times(1)).extractVariablesFromTemplate(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetTemplateVariables_notFound_returns404() throws Exception {
        // Arrange
        when(templateService.extractVariablesFromTemplate(999L))
                .thenThrow(new RuntimeException("Not found"));

        // Act & Assert
        mockMvc.perform(get("/api/authorization-templates/999/variables"))
                .andExpect(status().isNotFound());

        verify(templateService, times(1)).extractVariablesFromTemplate(999L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteTemplate_success_returns200() throws Exception {
        // Arrange
        doNothing().when(templateService).deleteTemplate(1L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/authorization-templates/1")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(templateService, times(1)).deleteTemplate(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteTemplate_notCreator_returns403() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Only the creator can delete this template"))
                .when(templateService).deleteTemplate(1L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/authorization-templates/1")
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(templateService, times(1)).deleteTemplate(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteTemplate_notFound_returns404() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Template not found"))
                .when(templateService).deleteTemplate(999L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/authorization-templates/999")
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(templateService, times(1)).deleteTemplate(999L, "testuser");
    }

    @Test
    void testCreateTemplate_unauthenticated_returns401() throws Exception {
        // Arrange
        AuthorizationTemplateRequest request = new AuthorizationTemplateRequest();
        request.setName("Test Template");
        request.setContent("Content");

        // Act & Assert
        mockMvc.perform(post("/api/authorization-templates")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(templateService, never()).createTemplate(anyString(), anyString(), anyString());
    }

    @Test
    void testDeleteTemplate_unauthenticated_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/authorization-templates/1")
                .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(templateService, never()).deleteTemplate(anyLong(), anyString());
    }
}
