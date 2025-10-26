package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.ReusableElement;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.ReusableElementRequest;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.ReusableElementService;
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

@WebMvcTest(ReusableElementController.class)
class ReusableElementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReusableElementService elementService;

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

    private ReusableElement createMockElement(Long id, String name, ReusableElement.ElementType type, User user) {
        ReusableElement element = new ReusableElement();
        element.setId(id);
        element.setName(name);
        element.setType(type);
        element.setJsonContent("{\"test\":\"content\"}");
        element.setDescription("Test description");
        element.setShared(false);
        element.setUseCount(0);
        element.setCreatedBy(user);
        element.setCreatedAt(LocalDateTime.now());
        return element;
    }

    private ReusableElementRequest createValidRequest() {
        ReusableElementRequest request = new ReusableElementRequest();
        request.setType("ROLE");
        request.setName("Test Element");
        request.setJsonContent("{\"test\":\"content\"}");
        request.setDescription("Test description");
        request.setIsShared(false);
        return request;
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateElement_success_returnsCreatedElement() throws Exception {
        // Arrange
        ReusableElementRequest request = createValidRequest();
        User mockUser = createMockUser("testuser");
        ReusableElement element = createMockElement(1L, "Test Element", ReusableElement.ElementType.ROLE, mockUser);

        when(elementService.createElement(
                eq(ReusableElement.ElementType.ROLE), eq("Test Element"), anyString(), anyString(),
                eq(false), eq("testuser")))
                .thenReturn(element);

        // Act & Assert
        mockMvc.perform(post("/api/build/elements")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Element"));

        verify(elementService, times(1)).createElement(
                eq(ReusableElement.ElementType.ROLE), eq("Test Element"), anyString(), anyString(),
                eq(false), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateElement_invalidType_returns400() throws Exception {
        // Arrange
        ReusableElementRequest request = createValidRequest();
        request.setType("INVALID_TYPE");

        // Act & Assert
        mockMvc.perform(post("/api/build/elements")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(elementService, never()).createElement(any(), anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateElement_success_returnsUpdatedElement() throws Exception {
        // Arrange
        ReusableElementRequest request = createValidRequest();
        request.setName("Updated Element");

        User mockUser = createMockUser("testuser");
        ReusableElement element = createMockElement(1L, "Updated Element", ReusableElement.ElementType.ROLE, mockUser);

        when(elementService.updateElement(
                eq(1L), eq("Updated Element"), anyString(), anyString(), eq(false), eq("testuser")))
                .thenReturn(element);

        // Act & Assert
        mockMvc.perform(put("/api/build/elements/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Element"));

        verify(elementService, times(1)).updateElement(
                eq(1L), eq("Updated Element"), anyString(), anyString(), eq(false), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateElement_notCreator_returns403() throws Exception {
        // Arrange
        ReusableElementRequest request = createValidRequest();

        when(elementService.updateElement(anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyString()))
                .thenThrow(new RuntimeException("Only the creator can update this element"));

        // Act & Assert
        mockMvc.perform(put("/api/build/elements/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateElement_notFound_returns404() throws Exception {
        // Arrange
        ReusableElementRequest request = createValidRequest();

        when(elementService.updateElement(anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyString()))
                .thenThrow(new RuntimeException("Element not found"));

        // Act & Assert
        mockMvc.perform(put("/api/build/elements/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetElement_success_returnsElement() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        ReusableElement element = createMockElement(1L, "Test Element", ReusableElement.ElementType.ROLE, mockUser);

        when(elementService.getElement(1L)).thenReturn(element);

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Element"));

        verify(elementService, times(1)).getElement(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetElement_notFound_returns404() throws Exception {
        // Arrange
        when(elementService.getElement(999L))
                .thenThrow(new RuntimeException("Element not found"));

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/999"))
                .andExpect(status().isNotFound());

        verify(elementService, times(1)).getElement(999L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetUserElements_success_returnsElements() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        ReusableElement element1 = createMockElement(1L, "Element 1", ReusableElement.ElementType.ROLE, mockUser);
        ReusableElement element2 = createMockElement(2L, "Element 2", ReusableElement.ElementType.PARTY, mockUser);
        List<ReusableElement> elements = Arrays.asList(element1, element2);

        when(elementService.getUserElements("testuser")).thenReturn(elements);

        // Act & Assert
        mockMvc.perform(get("/api/build/elements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Element 1"))
                .andExpect(jsonPath("$[1].name").value("Element 2"));

        verify(elementService, times(1)).getUserElements("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetUserElements_serviceException_returns500() throws Exception {
        // Arrange
        when(elementService.getUserElements("testuser"))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/build/elements"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetElementsByType_success_returnsElements() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        ReusableElement element1 = createMockElement(1L, "Role Element", ReusableElement.ElementType.ROLE, mockUser);
        List<ReusableElement> elements = Arrays.asList(element1);

        when(elementService.getUserElementsByType("testuser", ReusableElement.ElementType.ROLE))
                .thenReturn(elements);

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/type/role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("ROLE"));

        verify(elementService, times(1)).getUserElementsByType("testuser", ReusableElement.ElementType.ROLE);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetElementsByType_invalidType_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/build/elements/type/INVALID_TYPE"))
                .andExpect(status().isBadRequest());

        verify(elementService, never()).getUserElementsByType(anyString(), any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetElementsByType_serviceException_returns500() throws Exception {
        // Arrange
        when(elementService.getUserElementsByType(anyString(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/type/role"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchElements_success_returnsMatchingElements() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        ReusableElement element1 = createMockElement(1L, "Matching Element", ReusableElement.ElementType.ROLE, mockUser);
        List<ReusableElement> elements = Arrays.asList(element1);

        when(elementService.searchElements("testuser", "matching", null))
                .thenReturn(elements);

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/search")
                .param("q", "matching"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Matching Element"));

        verify(elementService, times(1)).searchElements("testuser", "matching", null);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchElements_withTypeFilter_returnsFilteredElements() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        ReusableElement element1 = createMockElement(1L, "Role Element", ReusableElement.ElementType.ROLE, mockUser);
        List<ReusableElement> elements = Arrays.asList(element1);

        when(elementService.searchElements("testuser", "role", ReusableElement.ElementType.ROLE))
                .thenReturn(elements);

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/search")
                .param("q", "role")
                .param("type", "role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(elementService, times(1)).searchElements("testuser", "role", ReusableElement.ElementType.ROLE);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchElements_noQuery_searchesAll() throws Exception {
        // Arrange
        when(elementService.searchElements("testuser", null, null)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/search"))
                .andExpect(status().isOk());

        verify(elementService, times(1)).searchElements("testuser", null, null);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchElements_invalidType_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/build/elements/search")
                .param("q", "test")
                .param("type", "INVALID_TYPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteElement_success_returns200() throws Exception {
        // Arrange
        doNothing().when(elementService).deleteElement(1L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/build/elements/1")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(elementService, times(1)).deleteElement(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteElement_notCreator_returns403() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Only the creator can delete this element"))
                .when(elementService).deleteElement(1L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/build/elements/1")
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(elementService, times(1)).deleteElement(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteElement_notFound_returns404() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Element not found"))
                .when(elementService).deleteElement(999L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/build/elements/999")
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(elementService, times(1)).deleteElement(999L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentElements_success_returnsElements() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        ReusableElement element1 = createMockElement(1L, "Recent Element", ReusableElement.ElementType.ROLE, mockUser);
        List<ReusableElement> elements = Arrays.asList(element1);

        when(elementService.getRecentElements("testuser", 10)).thenReturn(elements);

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/recent")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Recent Element"));

        verify(elementService, times(1)).getRecentElements("testuser", 10);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentElements_defaultLimit_uses10() throws Exception {
        // Arrange
        when(elementService.getRecentElements("testuser", 10)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/recent"))
                .andExpect(status().isOk());

        verify(elementService, times(1)).getRecentElements("testuser", 10);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentElements_serviceException_returns500() throws Exception {
        // Arrange
        when(elementService.getRecentElements(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/recent"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetMostUsedElements_success_returnsElements() throws Exception {
        // Arrange
        User mockUser = createMockUser("testuser");
        ReusableElement element1 = createMockElement(1L, "Popular Element", ReusableElement.ElementType.ROLE, mockUser);
        element1.setUseCount(50);
        List<ReusableElement> elements = Arrays.asList(element1);

        when(elementService.getMostUsedElements("testuser", 10)).thenReturn(elements);

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/most-used")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].useCount").value(50));

        verify(elementService, times(1)).getMostUsedElements("testuser", 10);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetMostUsedElements_defaultLimit_uses10() throws Exception {
        // Arrange
        when(elementService.getMostUsedElements("testuser", 10)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/most-used"))
                .andExpect(status().isOk());

        verify(elementService, times(1)).getMostUsedElements("testuser", 10);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetMostUsedElements_serviceException_returns500() throws Exception {
        // Arrange
        when(elementService.getMostUsedElements(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/most-used"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetStatistics_success_returnsStats() throws Exception {
        // Arrange
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalElements", 25);
        stats.put("roleCount", 10);

        when(elementService.getElementStatistics("testuser")).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.roleCount").value(10));

        verify(elementService, times(1)).getElementStatistics("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetStatistics_serviceException_returns500() throws Exception {
        // Arrange
        when(elementService.getElementStatistics("testuser"))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/build/elements/statistics"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testIncrementUseCount_success_returns200() throws Exception {
        // Arrange
        doNothing().when(elementService).incrementUseCount(1L);

        // Act & Assert
        mockMvc.perform(post("/api/build/elements/1/use")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(elementService, times(1)).incrementUseCount(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testIncrementUseCount_notFound_returns404() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Element not found"))
                .when(elementService).incrementUseCount(999L);

        // Act & Assert
        mockMvc.perform(post("/api/build/elements/999/use")
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(elementService, times(1)).incrementUseCount(999L);
    }

    @Test
    void testCreateElement_unauthenticated_returns401() throws Exception {
        // Arrange
        ReusableElementRequest request = createValidRequest();

        // Act & Assert
        mockMvc.perform(post("/api/build/elements")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(elementService, never()).createElement(any(), anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void testDeleteElement_unauthenticated_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/build/elements/1")
                .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(elementService, never()).deleteElement(anyLong(), anyString());
    }
}
