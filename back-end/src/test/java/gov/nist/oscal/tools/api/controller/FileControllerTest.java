package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.model.FileUploadRequest;
import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.OscalModelType;
import gov.nist.oscal.tools.api.model.SavedFile;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.FileStorageService;
import gov.nist.oscal.tools.api.service.FileValidationService;
import gov.nist.oscal.tools.api.service.RateLimitService;
import org.junit.jupiter.api.Disabled;
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
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for FileController file CRUD operations.
 *
 * DISABLED: These tests were written for file CRUD endpoints (list, get, upload, delete)
 * that were never implemented in FileController. The controller only has the org-logos
 * endpoint. These tests should be re-enabled when the file management endpoints are
 * implemented. See GitHub issue for tracking.
 */
@Disabled("File CRUD endpoints not implemented in FileController - only org-logos endpoint exists")
@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private FileValidationService fileValidationService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    // ========== LIST FILES TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testListFiles_success_returnsListOfFiles() throws Exception {
        // Arrange
        SavedFile file1 = new SavedFile();
        file1.setId("file1");
        file1.setFileName("catalog.xml");
        file1.setModelType(OscalModelType.CATALOG);
        file1.setFormat(OscalFormat.XML);
        file1.setUploadedAt(LocalDateTime.now());

        SavedFile file2 = new SavedFile();
        file2.setId("file2");
        file2.setFileName("ssp.json");
        file2.setModelType(OscalModelType.SYSTEM_SECURITY_PLAN);
        file2.setFormat(OscalFormat.JSON);
        file2.setUploadedAt(LocalDateTime.now());

        List<SavedFile> files = Arrays.asList(file1, file2);
        when(fileStorageService.listFiles("testuser")).thenReturn(files);

        // Act & Assert
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("file1"))
                .andExpect(jsonPath("$[0].fileName").value("catalog.xml"))
                .andExpect(jsonPath("$[1].id").value("file2"))
                .andExpect(jsonPath("$[1].fileName").value("ssp.json"));

        verify(fileStorageService, times(1)).listFiles("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testListFiles_emptyList_returnsEmptyArray() throws Exception {
        // Arrange
        when(fileStorageService.listFiles("testuser")).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(fileStorageService, times(1)).listFiles("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testListFiles_serviceThrowsException_returns500() throws Exception {
        // Arrange
        when(fileStorageService.listFiles("testuser"))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isInternalServerError());

        verify(fileStorageService, times(1)).listFiles("testuser");
    }

    // ========== GET FILE TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testGetFile_existingFile_returnsFile() throws Exception {
        // Arrange
        SavedFile file = new SavedFile();
        file.setId("file123");
        file.setFileName("test-catalog.xml");
        file.setModelType(OscalModelType.CATALOG);
        file.setFormat(OscalFormat.XML);
        file.setUploadedAt(LocalDateTime.now());

        when(fileStorageService.getFile("file123", "testuser")).thenReturn(file);

        // Act & Assert
        mockMvc.perform(get("/api/files/file123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("file123"))
                .andExpect(jsonPath("$.fileName").value("test-catalog.xml"))
                .andExpect(jsonPath("$.modelType").value("catalog"));

        verify(fileStorageService, times(1)).getFile("file123", "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetFile_nonExistentFile_returns404() throws Exception {
        // Arrange
        when(fileStorageService.getFile("nonexistent", "testuser"))
                .thenThrow(new RuntimeException("File not found"));

        // Act & Assert
        mockMvc.perform(get("/api/files/nonexistent"))
                .andExpect(status().isNotFound());

        verify(fileStorageService, times(1)).getFile("nonexistent", "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetFile_unauthorizedUser_returns404() throws Exception {
        // Arrange - user tries to access another user's file
        when(fileStorageService.getFile("file123", "testuser"))
                .thenThrow(new RuntimeException("Access denied"));

        // Act & Assert
        mockMvc.perform(get("/api/files/file123"))
                .andExpect(status().isNotFound());

        verify(fileStorageService, times(1)).getFile("file123", "testuser");
    }

    // ========== GET FILE CONTENT TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testGetFileContent_existingFile_returnsContent() throws Exception {
        // Arrange
        String xmlContent = "<?xml version=\"1.0\"?><catalog></catalog>";
        when(fileStorageService.getFileContent("file123", "testuser")).thenReturn(xmlContent);

        // Act & Assert
        mockMvc.perform(get("/api/files/file123/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(xmlContent));

        verify(fileStorageService, times(1)).getFileContent("file123", "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetFileContent_nonExistentFile_returns404() throws Exception {
        // Arrange
        when(fileStorageService.getFileContent("nonexistent", "testuser"))
                .thenThrow(new RuntimeException("File not found"));

        // Act & Assert
        mockMvc.perform(get("/api/files/nonexistent/content"))
                .andExpect(status().isNotFound());

        verify(fileStorageService, times(1)).getFileContent("nonexistent", "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetFileContent_largeFile_returnsContent() throws Exception {
        // Arrange
        String largeContent = "x".repeat(10000); // 10KB content
        when(fileStorageService.getFileContent("largefile", "testuser")).thenReturn(largeContent);

        // Act & Assert
        mockMvc.perform(get("/api/files/largefile/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(largeContent));

        verify(fileStorageService, times(1)).getFileContent("largefile", "testuser");
    }

    // ========== DELETE FILE TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteFile_existingFile_returns200() throws Exception {
        // Arrange
        when(fileStorageService.deleteFile("file123", "testuser")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/files/file123")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(fileStorageService, times(1)).deleteFile("file123", "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteFile_nonExistentFile_returns404() throws Exception {
        // Arrange
        when(fileStorageService.deleteFile("nonexistent", "testuser")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/files/nonexistent")
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(fileStorageService, times(1)).deleteFile("nonexistent", "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteFile_serviceThrowsException_returns500() throws Exception {
        // Arrange
        when(fileStorageService.deleteFile("file123", "testuser"))
                .thenThrow(new RuntimeException("Storage error"));

        // Act & Assert
        mockMvc.perform(delete("/api/files/file123")
                .with(csrf()))
                .andExpect(status().isInternalServerError());

        verify(fileStorageService, times(1)).deleteFile("file123", "testuser");
    }

    // ========== UPLOAD FILE TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testUploadFile_withFileName_returnsUploadedFile() throws Exception {
        // Arrange
        FileUploadRequest request = new FileUploadRequest();
        request.setContent("<?xml version=\"1.0\"?><catalog></catalog>");
        request.setFileName("my-catalog.xml");
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.XML);

        SavedFile savedFile = new SavedFile();
        savedFile.setId("newfile123");
        savedFile.setFileName("my-catalog.xml");
        savedFile.setModelType(OscalModelType.CATALOG);
        savedFile.setFormat(OscalFormat.XML);
        savedFile.setUploadedAt(LocalDateTime.now());

        when(fileStorageService.saveFile(
                anyString(), eq("my-catalog.xml"), eq(OscalModelType.CATALOG),
                eq(OscalFormat.XML), eq("testuser")))
                .thenReturn(savedFile);

        // Act & Assert
        mockMvc.perform(post("/api/files")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("newfile123"))
                .andExpect(jsonPath("$.fileName").value("my-catalog.xml"))
                .andExpect(jsonPath("$.modelType").value("catalog"));

        verify(fileStorageService, times(1)).saveFile(
                anyString(), eq("my-catalog.xml"), eq(OscalModelType.CATALOG),
                eq(OscalFormat.XML), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUploadFile_withoutFileName_generatesFilename() throws Exception {
        // Arrange
        FileUploadRequest request = new FileUploadRequest();
        request.setContent("{\"catalog\": {}}");
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.JSON);
        // fileName is null - should auto-generate

        SavedFile savedFile = new SavedFile();
        savedFile.setId("newfile456");
        savedFile.setFileName("document.json");
        savedFile.setModelType(OscalModelType.CATALOG);
        savedFile.setFormat(OscalFormat.JSON);
        savedFile.setUploadedAt(LocalDateTime.now());

        when(fileStorageService.saveFile(
                anyString(), eq("document.json"), eq(OscalModelType.CATALOG),
                eq(OscalFormat.JSON), eq("testuser")))
                .thenReturn(savedFile);

        // Act & Assert
        mockMvc.perform(post("/api/files")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("newfile456"))
                .andExpect(jsonPath("$.fileName").value("document.json"));

        verify(fileStorageService, times(1)).saveFile(
                anyString(), eq("document.json"), eq(OscalModelType.CATALOG),
                eq(OscalFormat.JSON), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUploadFile_yamlFormat_generatesYamlExtension() throws Exception {
        // Arrange
        FileUploadRequest request = new FileUploadRequest();
        request.setContent("catalog:\n  metadata:\n    title: Test");
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.YAML);

        SavedFile savedFile = new SavedFile();
        savedFile.setId("newfile789");
        savedFile.setFileName("document.yaml");
        savedFile.setModelType(OscalModelType.CATALOG);
        savedFile.setFormat(OscalFormat.YAML);
        savedFile.setUploadedAt(LocalDateTime.now());

        when(fileStorageService.saveFile(
                anyString(), eq("document.yaml"), eq(OscalModelType.CATALOG),
                eq(OscalFormat.YAML), eq("testuser")))
                .thenReturn(savedFile);

        // Act & Assert
        mockMvc.perform(post("/api/files")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("document.yaml"));

        verify(fileStorageService, times(1)).saveFile(
                anyString(), eq("document.yaml"), eq(OscalModelType.CATALOG),
                eq(OscalFormat.YAML), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUploadFile_serviceThrowsException_returns500() throws Exception {
        // Arrange
        FileUploadRequest request = new FileUploadRequest();
        request.setContent("<?xml version=\"1.0\"?><catalog></catalog>");
        request.setFileName("test.xml");
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.XML);

        when(fileStorageService.saveFile(anyString(), anyString(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("Storage full"));

        // Act & Assert
        mockMvc.perform(post("/api/files")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(fileStorageService, times(1)).saveFile(
                anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUploadFile_sspModel_savesSuccessfully() throws Exception {
        // Arrange
        FileUploadRequest request = new FileUploadRequest();
        request.setContent("<system-security-plan></system-security-plan>");
        request.setFileName("my-ssp.xml");
        request.setModelType(OscalModelType.SYSTEM_SECURITY_PLAN);
        request.setFormat(OscalFormat.XML);

        SavedFile savedFile = new SavedFile();
        savedFile.setId("ssp123");
        savedFile.setFileName("my-ssp.xml");
        savedFile.setModelType(OscalModelType.SYSTEM_SECURITY_PLAN);
        savedFile.setFormat(OscalFormat.XML);
        savedFile.setUploadedAt(LocalDateTime.now());

        when(fileStorageService.saveFile(
                anyString(), eq("my-ssp.xml"), eq(OscalModelType.SYSTEM_SECURITY_PLAN),
                eq(OscalFormat.XML), eq("testuser")))
                .thenReturn(savedFile);

        // Act & Assert
        mockMvc.perform(post("/api/files")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelType").value("system-security-plan"));

        verify(fileStorageService, times(1)).saveFile(
                anyString(), eq("my-ssp.xml"), eq(OscalModelType.SYSTEM_SECURITY_PLAN),
                eq(OscalFormat.XML), eq("testuser"));
    }

    // ========== AUTHENTICATION TESTS ==========

    @Test
    void testListFiles_unauthenticated_returns401() throws Exception {
        // Act & Assert - no @WithMockUser annotation
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isUnauthorized());

        verify(fileStorageService, never()).listFiles(anyString());
    }

    @Test
    void testUploadFile_unauthenticated_returns401() throws Exception {
        // Arrange
        FileUploadRequest request = new FileUploadRequest();
        request.setContent("test");
        request.setFileName("test.xml");
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.XML);

        // Act & Assert - no @WithMockUser annotation
        mockMvc.perform(post("/api/files")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(fileStorageService, never()).saveFile(
                anyString(), anyString(), any(), any(), anyString());
    }
}
