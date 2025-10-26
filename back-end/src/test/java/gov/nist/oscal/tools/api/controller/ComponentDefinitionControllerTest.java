package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.ComponentDefinition;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.ComponentDefinitionRequest;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.ComponentDefinitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ComponentDefinitionController.class)
class ComponentDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComponentDefinitionService componentService;

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

    private ComponentDefinition createMockComponent(Long id, String title, User user) {
        ComponentDefinition component = new ComponentDefinition();
        component.setId(id);
        component.setTitle(title);
        component.setDescription("Test description");
        component.setVersion("1.0.0");
        component.setOscalVersion("1.0.0");
        component.setFilename("test.json");
        component.setAzureBlobPath("build/testuser/test.json");
        component.setOscalUuid("uuid-123");
        component.setComponentCount(5);
        component.setCapabilityCount(3);
        component.setControlCount(10);
        component.setCreatedBy(user);
        component.setCreatedAt(LocalDateTime.now());
        return component;
    }

    private ComponentDefinitionRequest createValidRequest() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setTitle("Test Component");
        request.setDescription("Test description");
        request.setVersion("1.0.0");
        request.setOscalVersion("1.0.0");
        request.setFilename("test.json");
        request.setJsonContent("{\"test\":\"content\"}");
        request.setOscalUuid("uuid-123");
        request.setComponentCount(5);
        request.setCapabilityCount(3);
        request.setControlCount(10);
        return request;
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateComponent_success_returnsCreatedComponent() throws Exception {
        // Arrange
        ComponentDefinitionRequest request = createValidRequest();
        User mockUser = createMockUser("testuser");
        ComponentDefinition component = createMockComponent(1L, "Test Component", mockUser);

        when(componentService.createComponentDefinition(
                eq("Test Component"), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyInt(), anyInt(), eq("testuser")))
                .thenReturn(component);

        // Act & Assert
        mockMvc.perform(post("/api/build/components")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Component"));

        verify(componentService, times(1)).createComponentDefinition(
                eq("Test Component"), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyInt(), anyInt(), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateComponent_alreadyExists_returns409() throws Exception {
        // Arrange
        ComponentDefinitionRequest request = createValidRequest();

        when(componentService.createComponentDefinition(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("Component already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/build/components")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateComponent_serviceException_returns400() throws Exception {
        // Arrange
        ComponentDefinitionRequest request = createValidRequest();

        when(componentService.createComponentDefinition(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("Invalid data"));

        // Act & Assert
        mockMvc.perform(post("/api/build/components")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateComponent_success_returnsUpdatedComponent() throws Exception {
        // Arrange
        ComponentDefinitionRequest request = createValidRequest();
        request.setTitle("Updated Component");

        User mockUser = createMockUser("testuser");
        ComponentDefinition component = createMockComponent(1L, "Updated Component", mockUser);

        when(componentService.updateComponentDefinition(
                eq(1L), eq("Updated Component"), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), eq("testuser")))
                .thenReturn(component);

        // Act & Assert
        mockMvc.perform(put("/api/build/components/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Component"));

        verify(componentService, times(1)).updateComponentDefinition(
                eq(1L), eq("Updated Component"), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateComponent_notCreator_returns403() throws Exception {
        // Arrange
        ComponentDefinitionRequest request = createValidRequest();

        when(componentService.updateComponentDefinition(
                anyLong(), anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("Only the creator can update this component"));

        // Act & Assert
        mockMvc.perform(put("/api/build/components/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateComponent_notFound_returns404() throws Exception {
        // Arrange
        ComponentDefinitionRequest request = createValidRequest();

        when(componentService.updateComponentDefinition(
                anyLong(), anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("Component not found"));

        // Act & Assert
        mockMvc.perform(put("/api/build/components/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetComponent_success_returnsComponent() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        ComponentDefinition component = createMockComponent(1L, "Test Component", mockUser);

        when(componentService.getComponentDefinition(1L)).thenReturn(component);

        // Act & Assert
        mockMvc.perform(get("/api/build/components/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Component"));

        verify(componentService, times(1)).getComponentDefinition(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetComponent_notFound_returns404() throws Exception {
        // Arrange
        when(componentService.getComponentDefinition(999L))
                .thenThrow(new RuntimeException("Component not found"));

        // Act & Assert
        mockMvc.perform(get("/api/build/components/999"))
                .andExpect(status().isNotFound());

        verify(componentService, times(1)).getComponentDefinition(999L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetComponentByUuid_success_returnsComponent() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        ComponentDefinition component = createMockComponent(1L, "Test Component", mockUser);

        when(componentService.getComponentDefinitionByUuid("uuid-123")).thenReturn(component);

        // Act & Assert
        mockMvc.perform(get("/api/build/components/uuid/uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oscalUuid").value("uuid-123"));

        verify(componentService, times(1)).getComponentDefinitionByUuid("uuid-123");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetComponentByUuid_notFound_returns404() throws Exception {
        // Arrange
        when(componentService.getComponentDefinitionByUuid("nonexistent"))
                .thenThrow(new RuntimeException("Component not found"));

        // Act & Assert
        mockMvc.perform(get("/api/build/components/uuid/nonexistent"))
                .andExpect(status().isNotFound());

        verify(componentService, times(1)).getComponentDefinitionByUuid("nonexistent");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetComponentContent_success_returnsContent() throws Exception {
        // Arrange
        when(componentService.getComponentContent(1L)).thenReturn("{\"test\":\"content\"}");

        // Act & Assert
        mockMvc.perform(get("/api/build/components/1/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("{\"test\":\"content\"}"));

        verify(componentService, times(1)).getComponentContent(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetComponentContent_notFound_returns404() throws Exception {
        // Arrange
        when(componentService.getComponentContent(999L))
                .thenThrow(new RuntimeException("Component not found"));

        // Act & Assert
        mockMvc.perform(get("/api/build/components/999/content"))
                .andExpect(status().isNotFound());

        verify(componentService, times(1)).getComponentContent(999L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetUserComponents_success_returnsComponents() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        ComponentDefinition component1 = createMockComponent(1L, "Component 1", mockUser);
        ComponentDefinition component2 = createMockComponent(2L, "Component 2", mockUser);
        List<ComponentDefinition> components = Arrays.asList(component1, component2);

        when(componentService.getUserComponents("testuser")).thenReturn(components);

        // Act & Assert
        mockMvc.perform(get("/api/build/components"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Component 1"))
                .andExpect(jsonPath("$[1].title").value("Component 2"));

        verify(componentService, times(1)).getUserComponents("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetUserComponents_serviceException_returns500() throws Exception {
        // Arrange
        when(componentService.getUserComponents("testuser"))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/build/components"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentComponents_success_returnsComponents() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        ComponentDefinition component1 = createMockComponent(1L, "Recent Component", mockUser);
        List<ComponentDefinition> components = Arrays.asList(component1);

        when(componentService.getRecentComponents("testuser", 10)).thenReturn(components);

        // Act & Assert
        mockMvc.perform(get("/api/build/components/recent")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Recent Component"));

        verify(componentService, times(1)).getRecentComponents("testuser", 10);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentComponents_defaultLimit_uses10() throws Exception {
        // Arrange
        when(componentService.getRecentComponents("testuser", 10)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/build/components/recent"))
                .andExpect(status().isOk());

        verify(componentService, times(1)).getRecentComponents("testuser", 10);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentComponents_serviceException_returns500() throws Exception {
        // Arrange
        when(componentService.getRecentComponents(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/build/components/recent"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchComponents_success_returnsMatchingComponents() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        ComponentDefinition component1 = createMockComponent(1L, "Matching Component", mockUser);
        List<ComponentDefinition> components = Arrays.asList(component1);

        when(componentService.searchComponents("testuser", "matching")).thenReturn(components);

        // Act & Assert
        mockMvc.perform(get("/api/build/components/search")
                .param("q", "matching"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Matching Component"));

        verify(componentService, times(1)).searchComponents("testuser", "matching");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchComponents_noQuery_searchesAll() throws Exception {
        // Arrange
        when(componentService.searchComponents("testuser", null)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/build/components/search"))
                .andExpect(status().isOk());

        verify(componentService, times(1)).searchComponents("testuser", null);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchComponents_serviceException_returns500() throws Exception {
        // Arrange
        when(componentService.searchComponents(anyString(), anyString()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/build/components/search")
                .param("q", "test"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteComponent_success_returns200() throws Exception {
        // Arrange
        doNothing().when(componentService).deleteComponentDefinition(1L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/build/components/1")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(componentService, times(1)).deleteComponentDefinition(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteComponent_notCreator_returns403() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Only the creator can delete this component"))
                .when(componentService).deleteComponentDefinition(1L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/build/components/1")
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(componentService, times(1)).deleteComponentDefinition(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteComponent_notFound_returns404() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Component not found"))
                .when(componentService).deleteComponentDefinition(999L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/build/components/999")
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(componentService, times(1)).deleteComponentDefinition(999L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetStatistics_success_returnsStats() throws Exception {
        // Arrange
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalComponents", 10);
        stats.put("totalControls", 50);

        when(componentService.getComponentStatistics("testuser")).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/build/components/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalComponents").value(10))
                .andExpect(jsonPath("$.totalControls").value(50));

        verify(componentService, times(1)).getComponentStatistics("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetStatistics_serviceException_returns500() throws Exception {
        // Arrange
        when(componentService.getComponentStatistics("testuser"))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/build/components/statistics"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCheckComponentExists_success_returnsTrue() throws Exception {
        // Arrange
        when(componentService.componentExists(1L)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/build/components/1/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        verify(componentService, times(1)).componentExists(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCheckComponentExists_success_returnsFalse() throws Exception {
        // Arrange
        when(componentService.componentExists(999L)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/build/components/999/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));

        verify(componentService, times(1)).componentExists(999L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCheckComponentExists_serviceException_returns500() throws Exception {
        // Arrange
        when(componentService.componentExists(anyLong()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/build/components/1/exists"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testCreateComponent_unauthenticated_returns401() throws Exception {
        // Arrange
        ComponentDefinitionRequest request = createValidRequest();

        // Act & Assert
        mockMvc.perform(post("/api/build/components")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(componentService, never()).createComponentDefinition(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void testDeleteComponent_unauthenticated_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/build/components/1")
                .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(componentService, never()).deleteComponentDefinition(anyLong(), anyString());
    }
}
