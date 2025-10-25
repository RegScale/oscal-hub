package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryTag;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.LibraryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LibraryController.class)
class LibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LibraryService libraryService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    // ========== CREATE LIBRARY ITEM TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testCreateLibraryItem_success_returnsCreated() throws Exception {
        // Arrange
        LibraryItemRequest request = new LibraryItemRequest();
        request.setTitle("Test Catalog");
        request.setDescription("A test catalog");
        request.setOscalType("catalog");
        request.setFileName("test.xml");
        request.setFormat("XML");
        request.setFileContent("<catalog></catalog>");
        request.setTags(new HashSet<>(Arrays.asList("test", "catalog")));

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        LibraryTag tag1 = new LibraryTag("test");
        LibraryTag tag2 = new LibraryTag("catalog");

        LibraryItem mockItem = new LibraryItem();
        mockItem.setId(1L);
        mockItem.setItemId("item-1");
        mockItem.setTitle("Test Catalog");
        mockItem.setOscalType("catalog");
        mockItem.setCreatedBy(mockUser);
        mockItem.setTags(new HashSet<>(Arrays.asList(tag1, tag2)));
        mockItem.setVersions(new HashSet<>());

        when(libraryService.createLibraryItem(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anySet(), eq("testuser")))
                .thenReturn(mockItem);

        // Act & Assert
        mockMvc.perform(post("/api/library")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Catalog"));

        verify(libraryService, times(1)).createLibraryItem(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anySet(), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateLibraryItem_serviceThrowsException_returns400() throws Exception {
        // Arrange
        LibraryItemRequest request = new LibraryItemRequest();
        request.setTitle("Test");
        request.setOscalType("catalog");
        request.setFileName("test.xml");
        request.setFormat("XML");
        request.setFileContent("content");

        when(libraryService.createLibraryItem(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anySet(), anyString()))
                .thenThrow(new RuntimeException("Creation failed"));

        // Act & Assert
        mockMvc.perform(post("/api/library")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== UPDATE LIBRARY ITEM TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateLibraryItem_success_returnsUpdated() throws Exception {
        // Arrange
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated description");
        request.setTags(new HashSet<>(Arrays.asList("updated", "test")));

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        LibraryTag tag1 = new LibraryTag("updated");
        LibraryTag tag2 = new LibraryTag("test");

        LibraryItem mockItem = new LibraryItem();
        mockItem.setId(1L);
        mockItem.setItemId("1");
        mockItem.setTitle("Updated Title");
        mockItem.setCreatedBy(mockUser);
        mockItem.setTags(new HashSet<>(Arrays.asList(tag1, tag2)));
        mockItem.setVersions(new HashSet<>());

        when(libraryService.updateLibraryItem(anyString(), anyString(), anyString(), anySet(), eq("testuser")))
                .thenReturn(mockItem);

        // Act & Assert
        mockMvc.perform(put("/api/library/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateLibraryItem_notFound_returns404() throws Exception {
        // Arrange
        LibraryItemUpdateRequest request = new LibraryItemUpdateRequest();
        request.setTitle("Updated");

        when(libraryService.updateLibraryItem(anyString(), anyString(), anyString(), anySet(), anyString()))
                .thenThrow(new RuntimeException("Item not found"));

        // Act & Assert
        mockMvc.perform(put("/api/library/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ========== ADD VERSION TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testAddVersion_success_returnsCreated() throws Exception {
        // Arrange
        LibraryVersionRequest request = new LibraryVersionRequest();
        request.setFileName("test-v2.xml");
        request.setFormat("XML");
        request.setFileContent("content v2");
        request.setChangeDescription("Updated controls");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        LibraryVersion mockVersion = new LibraryVersion();
        mockVersion.setId(1L);
        mockVersion.setVersionId("version-1");
        mockVersion.setVersionNumber(2);
        mockVersion.setChangeDescription("Updated controls");
        mockVersion.setUploadedBy(mockUser);

        when(libraryService.addVersion(anyString(), anyString(), anyString(), anyString(), anyString(), eq("testuser")))
                .thenReturn(mockVersion);

        // Act & Assert
        mockMvc.perform(post("/api/library/1/versions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(2));
    }

    // ========== GET LIBRARY ITEM TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testGetLibraryItem_success_returnsItem() throws Exception {
        // Arrange
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        LibraryItem mockItem = new LibraryItem();
        mockItem.setId(1L);
        mockItem.setItemId("1");
        mockItem.setTitle("Test Catalog");
        mockItem.setOscalType("catalog");
        mockItem.setCreatedBy(mockUser);
        mockItem.setTags(new HashSet<>());
        mockItem.setVersions(new HashSet<>());

        when(libraryService.getLibraryItem("1")).thenReturn(mockItem);

        // Act & Assert
        mockMvc.perform(get("/api/library/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Catalog"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetLibraryItem_notFound_returns404() throws Exception {
        // Arrange
        when(libraryService.getLibraryItem("999"))
                .thenThrow(new RuntimeException("Item not found"));

        // Act & Assert
        mockMvc.perform(get("/api/library/999"))
                .andExpect(status().isNotFound());
    }

    // ========== GET CONTENT TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testGetLibraryItemContent_success_returnsContent() throws Exception {
        // Arrange
        String content = "<catalog></catalog>";
        when(libraryService.getCurrentVersionContent("1")).thenReturn(content);

        // Act & Assert
        mockMvc.perform(get("/api/library/1/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content));
    }

    // ========== DELETE TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteLibraryItem_success_returns200() throws Exception {
        // Arrange
        doNothing().when(libraryService).deleteLibraryItem("1", "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/library/1")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteLibraryItem_notCreator_returns403() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Only the creator can delete"))
                .when(libraryService).deleteLibraryItem("1", "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/library/1")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== SEARCH TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testSearchLibrary_byKeyword_returnsResults() throws Exception {
        // Arrange
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        LibraryItem item = new LibraryItem();
        item.setId(1L);
        item.setItemId("item-1");
        item.setTitle("NIST Catalog");
        item.setCreatedBy(mockUser);
        item.setTags(new HashSet<>());
        item.setVersions(new HashSet<>());

        when(libraryService.searchLibrary("NIST", null, null))
                .thenReturn(Arrays.asList(item));

        // Act & Assert
        mockMvc.perform(get("/api/library/search")
                .param("q", "NIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("NIST Catalog"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchLibrary_byOscalType_returnsResults() throws Exception {
        // Arrange
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        LibraryItem item = new LibraryItem();
        item.setId(1L);
        item.setItemId("item-1");
        item.setOscalType("catalog");
        item.setCreatedBy(mockUser);
        item.setTags(new HashSet<>());
        item.setVersions(new HashSet<>());

        when(libraryService.searchLibrary(null, "catalog", null))
                .thenReturn(Arrays.asList(item));

        // Act & Assert
        mockMvc.perform(get("/api/library/search")
                .param("oscalType", "catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ========== GET ALL ITEMS TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testGetAllLibraryItems_success_returnsAllItems() throws Exception {
        // Arrange
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        LibraryItem item1 = new LibraryItem();
        item1.setId(1L);
        item1.setItemId("item-1");
        item1.setCreatedBy(mockUser);
        item1.setTags(new HashSet<>());
        item1.setVersions(new HashSet<>());

        LibraryItem item2 = new LibraryItem();
        item2.setId(2L);
        item2.setItemId("item-2");
        item2.setCreatedBy(mockUser);
        item2.setTags(new HashSet<>());
        item2.setVersions(new HashSet<>());

        when(libraryService.getAllLibraryItems())
                .thenReturn(Arrays.asList(item1, item2));

        // Act & Assert
        mockMvc.perform(get("/api/library"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ========== GET BY TYPE TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testGetItemsByType_success_returnsItems() throws Exception {
        // Arrange
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        LibraryItem item = new LibraryItem();
        item.setId(1L);
        item.setItemId("item-1");
        item.setOscalType("catalog");
        item.setCreatedBy(mockUser);
        item.setTags(new HashSet<>());
        item.setVersions(new HashSet<>());

        when(libraryService.getLibraryItemsByOscalType("catalog"))
                .thenReturn(Arrays.asList(item));

        // Act & Assert
        mockMvc.perform(get("/api/library/type/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ========== ANALYTICS TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testGetAnalytics_success_returnsAnalytics() throws Exception {
        // Arrange
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalItems", 100);
        analytics.put("totalDownloads", 500);

        when(libraryService.getAnalytics()).thenReturn(analytics);

        // Act & Assert
        mockMvc.perform(get("/api/library/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(100))
                .andExpect(jsonPath("$.totalDownloads").value(500));
    }

    // ========== TAGS TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testGetAllTags_success_returnsTags() throws Exception {
        // Arrange
        LibraryTag tag1 = new LibraryTag("nist");
        LibraryTag tag2 = new LibraryTag("security");

        when(libraryService.getAllTags())
                .thenReturn(Arrays.asList(tag1, tag2));

        // Act & Assert
        mockMvc.perform(get("/api/library/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("nist"))
                .andExpect(jsonPath("$[1].name").value("security"));
    }

    // ========== AUTHENTICATION TESTS ==========

    @Test
    void testCreateLibraryItem_unauthenticated_returns401() throws Exception {
        // Arrange
        LibraryItemRequest request = new LibraryItemRequest();
        request.setTitle("Test");
        request.setOscalType("catalog");
        request.setFileName("test.xml");
        request.setFormat("XML");
        request.setFileContent("content");

        // Act & Assert
        mockMvc.perform(post("/api/library")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(libraryService, never()).createLibraryItem(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anySet(), anyString());
    }
}
