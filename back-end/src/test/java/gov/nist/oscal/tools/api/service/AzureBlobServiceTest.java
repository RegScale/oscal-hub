package gov.nist.oscal.tools.api.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.BlobProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AzureBlobServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobProperties blobProperties;

    @InjectMocks
    private AzureBlobService azureBlobService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Reset the service for each test
        azureBlobService = new AzureBlobService();
    }

    // ========== Initialization Tests ==========

    @Test
    void testInit_withValidConnectionString_success() {
        // Mock the builder chain
        try (MockedConstruction<BlobServiceClientBuilder> builderMock = mockConstruction(
                BlobServiceClientBuilder.class,
                (mock, context) -> {
                    when(mock.connectionString(anyString())).thenReturn(mock);
                    when(mock.buildClient()).thenReturn(blobServiceClient);
                })) {

            when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(containerClient);
            when(containerClient.exists()).thenReturn(true);

            ReflectionTestUtils.setField(azureBlobService, "connectionString", "DefaultEndpointsProtocol=https;AccountName=test;AccountKey=test123==;EndpointSuffix=core.windows.net");
            ReflectionTestUtils.setField(azureBlobService, "buildContainerName", "oscal-build-storage");
            ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
            ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());

            azureBlobService.init();

            assertTrue(azureBlobService.isConfigured());
        }
    }

    @Test
    void testInit_withValidConnectionString_createsContainer() {
        // Mock the builder chain
        try (MockedConstruction<BlobServiceClientBuilder> builderMock = mockConstruction(
                BlobServiceClientBuilder.class,
                (mock, context) -> {
                    when(mock.connectionString(anyString())).thenReturn(mock);
                    when(mock.buildClient()).thenReturn(blobServiceClient);
                })) {

            when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(containerClient);
            when(containerClient.exists()).thenReturn(false);

            ReflectionTestUtils.setField(azureBlobService, "connectionString", "DefaultEndpointsProtocol=https;AccountName=test;AccountKey=test123==;EndpointSuffix=core.windows.net");
            ReflectionTestUtils.setField(azureBlobService, "buildContainerName", "oscal-build-storage");
            ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
            ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());

            azureBlobService.init();

            verify(containerClient).create();
            assertTrue(azureBlobService.isConfigured());
        }
    }

    @Test
    void testInit_withNullConnectionString_usesLocalStorage() {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());

        azureBlobService.init();

        assertTrue(azureBlobService.isConfigured());
        // Verify local storage directory was created
        Path localBuildPath = tempDir.resolve("build");
        assertTrue(Files.exists(localBuildPath));
    }

    @Test
    void testInit_withEmptyConnectionString_usesLocalStorage() {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", "   ");
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());

        azureBlobService.init();

        assertTrue(azureBlobService.isConfigured());
    }

    @Test
    void testInit_azureInitializationFails_throwsException() {
        try (MockedConstruction<BlobServiceClientBuilder> builderMock = mockConstruction(
                BlobServiceClientBuilder.class,
                (mock, context) -> {
                    when(mock.connectionString(anyString())).thenReturn(mock);
                    when(mock.buildClient()).thenThrow(new RuntimeException("Connection failed"));
                })) {

            ReflectionTestUtils.setField(azureBlobService, "connectionString", "invalid-connection-string");
            ReflectionTestUtils.setField(azureBlobService, "buildContainerName", "oscal-build-storage");
            ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
            ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());

            assertThrows(RuntimeException.class, () -> azureBlobService.init());
        }
    }

    // ========== Local Storage Mode Tests ==========

    @Test
    void testUploadComponent_localStorage_success() {
        // Initialize with local storage
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        String jsonContent = "{\"component-definition\": {\"uuid\": \"test-123\"}}";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("author", "testuser");

        String result = azureBlobService.uploadComponent("testuser", "component.json", jsonContent, metadata);

        assertEquals("build/testuser/component.json", result);
        Path expectedPath = tempDir.resolve("build/testuser/component.json");
        assertTrue(Files.exists(expectedPath));
    }

    @Test
    void testDownloadComponent_localStorage_success() throws IOException {
        // Initialize with local storage
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        // Create test file
        Path userDir = tempDir.resolve("build/testuser");
        Files.createDirectories(userDir);
        String testContent = "{\"test\": \"data\"}";
        Files.writeString(userDir.resolve("test.json"), testContent);

        String result = azureBlobService.downloadComponent("build/testuser/test.json");

        assertEquals(testContent, result);
    }

    @Test
    void testDownloadComponent_localStorage_fileNotFound_throwsException() {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        assertThrows(RuntimeException.class, () ->
                azureBlobService.downloadComponent("build/testuser/nonexistent.json")
        );
    }

    @Test
    void testListUserComponents_localStorage_success() throws IOException {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        // Create test files
        Path userDir = tempDir.resolve("build/testuser");
        Files.createDirectories(userDir);
        Files.writeString(userDir.resolve("component1.json"), "{}");
        Files.writeString(userDir.resolve("component2.json"), "{}");

        List<String> results = azureBlobService.listUserComponents("testuser");

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(path -> path.contains("component1.json")));
        assertTrue(results.stream().anyMatch(path -> path.contains("component2.json")));
    }

    @Test
    void testListUserComponents_localStorage_noFiles_returnsEmpty() {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        List<String> results = azureBlobService.listUserComponents("newuser");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testDeleteComponent_localStorage_success() throws IOException {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        // Create test file
        Path userDir = tempDir.resolve("build/testuser");
        Files.createDirectories(userDir);
        Path testFile = userDir.resolve("delete-me.json");
        Files.writeString(testFile, "{}");

        boolean result = azureBlobService.deleteComponent("build/testuser/delete-me.json");

        assertTrue(result);
        assertFalse(Files.exists(testFile));
    }

    @Test
    void testDeleteComponent_localStorage_fileNotFound_returnsFalse() {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        boolean result = azureBlobService.deleteComponent("build/testuser/nonexistent.json");

        assertFalse(result);
    }

    @Test
    void testComponentExists_localStorage_fileExists() throws IOException {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        // Create test file
        Path userDir = tempDir.resolve("build/testuser");
        Files.createDirectories(userDir);
        Files.writeString(userDir.resolve("exists.json"), "{}");

        boolean result = azureBlobService.componentExists("build/testuser/exists.json");

        assertTrue(result);
    }

    @Test
    void testComponentExists_localStorage_fileNotFound() {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        boolean result = azureBlobService.componentExists("build/testuser/nonexistent.json");

        assertFalse(result);
    }

    @Test
    void testGetFileSize_localStorage_success() throws IOException {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        // Create test file
        Path userDir = tempDir.resolve("build/testuser");
        Files.createDirectories(userDir);
        String content = "{\"test\": \"data\"}";
        Files.writeString(userDir.resolve("sized.json"), content);

        long size = azureBlobService.getFileSize("build/testuser/sized.json");

        assertTrue(size > 0);
        assertEquals(content.length(), size);
    }

    @Test
    void testGetFileSize_localStorage_fileNotFound_returnsZero() {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        long size = azureBlobService.getFileSize("build/testuser/nonexistent.json");

        assertEquals(0, size);
    }

    // ========== Azure Storage Mode Tests ==========

    @Test
    void testUploadComponent_azureStorage_success() {
        // Set up Azure mode
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);

        String jsonContent = "{\"component-definition\": {\"uuid\": \"test-123\"}}";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("author", "testuser");

        String result = azureBlobService.uploadComponent("testuser", "component.json", jsonContent, metadata);

        assertEquals("build/testuser/component.json", result);
        verify(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), eq(true));
        verify(blobClient).setMetadata(metadata);
    }

    @Test
    void testUploadComponent_azureStorage_withoutMetadata() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);

        String result = azureBlobService.uploadComponent("testuser", "component.json", "{}", null);

        assertEquals("build/testuser/component.json", result);
        verify(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), eq(true));
        verify(blobClient, never()).setMetadata(any());
    }

    @Test
    void testUploadComponent_azureStorage_notConfigured_throwsException() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", null);

        assertThrows(RuntimeException.class, () ->
                azureBlobService.uploadComponent("testuser", "component.json", "{}", null)
        );
    }

    @Test
    void testUploadComponent_azureStorage_uploadFails_throwsException() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        doThrow(new RuntimeException("Upload failed"))
                .when(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), eq(true));

        assertThrows(RuntimeException.class, () ->
                azureBlobService.uploadComponent("testuser", "component.json", "{}", null)
        );
    }

    @Test
    void testDownloadComponent_azureStorage_success() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.downloadContent()).thenReturn(BinaryData.fromString("{\"test\": \"data\"}"));

        String result = azureBlobService.downloadComponent("build/testuser/test.json");

        assertEquals("{\"test\": \"data\"}", result);
    }

    @Test
    void testDownloadComponent_azureStorage_notFound_throwsException() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                azureBlobService.downloadComponent("build/testuser/nonexistent.json")
        );
    }

    @Test
    void testDownloadComponent_azureStorage_notConfigured_throwsException() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", null);

        assertThrows(RuntimeException.class, () ->
                azureBlobService.downloadComponent("build/testuser/test.json")
        );
    }

    @Test
    void testListUserComponents_azureStorage_success() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");

        // Create mock BlobItems
        BlobItem item1 = mock(BlobItem.class);
        when(item1.getName()).thenReturn("build/testuser/component1.json");
        when(item1.isPrefix()).thenReturn(false);

        BlobItem item2 = mock(BlobItem.class);
        when(item2.getName()).thenReturn("build/testuser/component2.json");
        when(item2.isPrefix()).thenReturn(false);

        // Mock PagedIterable directly
        @SuppressWarnings("unchecked")
        com.azure.core.http.rest.PagedIterable<BlobItem> pagedIterable = mock(com.azure.core.http.rest.PagedIterable.class);
        when(pagedIterable.iterator()).thenReturn(List.of(item1, item2).iterator());
        when(containerClient.listBlobsByHierarchy(anyString())).thenReturn(pagedIterable);

        List<String> results = azureBlobService.listUserComponents("testuser");

        assertEquals(2, results.size());
        assertTrue(results.contains("build/testuser/component1.json"));
        assertTrue(results.contains("build/testuser/component2.json"));
    }

    @Test
    void testListUserComponents_azureStorage_notConfigured_throwsException() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", null);

        assertThrows(RuntimeException.class, () ->
                azureBlobService.listUserComponents("testuser")
        );
    }

    @Test
    void testDeleteComponent_azureStorage_success() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);

        boolean result = azureBlobService.deleteComponent("build/testuser/delete-me.json");

        assertTrue(result);
        verify(blobClient).delete();
    }

    @Test
    void testDeleteComponent_azureStorage_notFound_returnsFalse() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        boolean result = azureBlobService.deleteComponent("build/testuser/nonexistent.json");

        assertFalse(result);
        verify(blobClient, never()).delete();
    }

    @Test
    void testDeleteComponent_azureStorage_notConfigured_throwsException() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", null);

        assertThrows(RuntimeException.class, () ->
                azureBlobService.deleteComponent("build/testuser/test.json")
        );
    }

    @Test
    void testDeleteComponent_azureStorage_deleteFails_returnsFalse() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        doThrow(new RuntimeException("Delete failed")).when(blobClient).delete();

        boolean result = azureBlobService.deleteComponent("build/testuser/test.json");

        assertFalse(result);
    }

    @Test
    void testComponentExists_azureStorage_exists() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);

        boolean result = azureBlobService.componentExists("build/testuser/exists.json");

        assertTrue(result);
    }

    @Test
    void testComponentExists_azureStorage_notFound() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        boolean result = azureBlobService.componentExists("build/testuser/nonexistent.json");

        assertFalse(result);
    }

    @Test
    void testComponentExists_azureStorage_notConfigured_returnsFalse() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", null);

        boolean result = azureBlobService.componentExists("build/testuser/test.json");

        assertFalse(result);
    }

    @Test
    void testComponentExists_azureStorage_checkFails_returnsFalse() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenThrow(new RuntimeException("Check failed"));

        boolean result = azureBlobService.componentExists("build/testuser/test.json");

        assertFalse(result);
    }

    @Test
    void testGetFileSize_azureStorage_success() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getBlobSize()).thenReturn(1024L);

        long size = azureBlobService.getFileSize("build/testuser/sized.json");

        assertEquals(1024L, size);
    }

    @Test
    void testGetFileSize_azureStorage_notConfigured_throwsException() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", null);

        assertThrows(RuntimeException.class, () ->
                azureBlobService.getFileSize("build/testuser/test.json")
        );
    }

    @Test
    void testGetFileSize_azureStorage_getFails_throwsException() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getProperties()).thenThrow(new RuntimeException("Get properties failed"));

        assertThrows(RuntimeException.class, () ->
                azureBlobService.getFileSize("build/testuser/test.json")
        );
    }

    // ========== Path Building and Utility Tests ==========

    @Test
    void testBuildBlobPath_success() {
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");

        String result = azureBlobService.buildBlobPath("testuser", "component.json");

        assertEquals("build/testuser/component.json", result);
    }

    @Test
    void testBuildBlobPath_sanitizesFilename() {
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");

        String result = azureBlobService.buildBlobPath("testuser", "my component@file!.json");

        assertEquals("build/testuser/my_component_file_.json", result);
    }

    @Test
    void testIsConfigured_withAzureStorage() {
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);

        assertTrue(azureBlobService.isConfigured());
    }

    @Test
    void testIsConfigured_withLocalStorage() {
        ReflectionTestUtils.setField(azureBlobService, "containerClient", null);
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", true);

        assertTrue(azureBlobService.isConfigured());
    }

    @Test
    void testIsConfigured_notConfigured() {
        ReflectionTestUtils.setField(azureBlobService, "containerClient", null);
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);

        assertFalse(azureBlobService.isConfigured());
    }

    // ========== Additional Error Handling Tests ==========

    @Test
    void testDownloadComponent_azureStorage_downloadFails_throwsException() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.downloadContent()).thenThrow(new RuntimeException("Download failed"));

        assertThrows(RuntimeException.class, () ->
                azureBlobService.downloadComponent("build/testuser/test.json")
        );
    }

    @Test
    void testListUserComponents_azureStorage_listFails_throwsException() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");

        when(containerClient.listBlobsByHierarchy(anyString()))
                .thenThrow(new RuntimeException("List failed"));

        assertThrows(RuntimeException.class, () ->
                azureBlobService.listUserComponents("testuser")
        );
    }

    @Test
    void testListUserComponents_localStorage_ioException_throwsException() throws IOException {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        // Create a directory that will cause an IOException when walking
        Path userDir = tempDir.resolve("build/problematic-user");
        Files.createDirectories(userDir);
        Path subDir = userDir.resolve("subdir");
        Files.createDirectories(subDir);

        // Create a file in the subdirectory and then make it inaccessible
        Path problemFile = subDir.resolve("problem.json");
        Files.writeString(problemFile, "{}");

        // Set permissions to cause an IOException during walk
        // Note: This might not work on all OS/filesystems, but will cover the error path on Unix-like systems
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            subDir.toFile().setReadable(false);

            try {
                assertThrows(RuntimeException.class, () ->
                        azureBlobService.listUserComponents("problematic-user")
                );
            } finally {
                // Restore permissions for cleanup
                subDir.toFile().setReadable(true);
            }
        }
    }

    @Test
    void testGetFromLocalStorage_ioException_throwsException() throws IOException {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        // Create a directory where we expect a file
        Path userDir = tempDir.resolve("build/testuser");
        Files.createDirectories(userDir);
        Path dirInsteadOfFile = userDir.resolve("directory-not-file");
        Files.createDirectories(dirInsteadOfFile);

        // Trying to read a directory as a file should throw an exception
        assertThrows(RuntimeException.class, () ->
                azureBlobService.downloadComponent("build/testuser/directory-not-file")
        );
    }

    @Test
    void testUploadComponent_localStorage_withMetadata() {
        // Initialize with local storage
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        String jsonContent = "{\"component-definition\": {\"uuid\": \"test-456\"}}";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("version", "1.0");
        metadata.put("type", "software");

        // Local storage ignores metadata, but should still work
        String result = azureBlobService.uploadComponent("testuser2", "component2.json", jsonContent, metadata);

        assertEquals("build/testuser2/component2.json", result);
        Path expectedPath = tempDir.resolve("build/testuser2/component2.json");
        assertTrue(Files.exists(expectedPath));
    }

    @Test
    void testListUserComponents_azureStorage_withPrefixItems() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");

        // Create mock BlobItems - mix of files and directories (prefixes)
        BlobItem item1 = mock(BlobItem.class);
        when(item1.getName()).thenReturn("build/testuser/component1.json");
        when(item1.isPrefix()).thenReturn(false);

        BlobItem dirItem = mock(BlobItem.class);
        when(dirItem.getName()).thenReturn("build/testuser/subdirectory/");
        when(dirItem.isPrefix()).thenReturn(true);  // This is a directory/prefix

        BlobItem item2 = mock(BlobItem.class);
        when(item2.getName()).thenReturn("build/testuser/component2.json");
        when(item2.isPrefix()).thenReturn(false);

        // Mock PagedIterable
        @SuppressWarnings("unchecked")
        com.azure.core.http.rest.PagedIterable<BlobItem> pagedIterable = mock(com.azure.core.http.rest.PagedIterable.class);
        when(pagedIterable.iterator()).thenReturn(List.of(item1, dirItem, item2).iterator());
        when(containerClient.listBlobsByHierarchy(anyString())).thenReturn(pagedIterable);

        List<String> results = azureBlobService.listUserComponents("testuser");

        // Should only include files, not prefixes (directories)
        assertEquals(2, results.size());
        assertTrue(results.contains("build/testuser/component1.json"));
        assertTrue(results.contains("build/testuser/component2.json"));
    }

    @Test
    void testUploadComponent_azureStorage_withEmptyMetadata() {
        ReflectionTestUtils.setField(azureBlobService, "useLocalStorage", false);
        ReflectionTestUtils.setField(azureBlobService, "containerClient", containerClient);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);

        Map<String, String> emptyMetadata = new HashMap<>();
        String result = azureBlobService.uploadComponent("testuser", "component.json", "{}", emptyMetadata);

        assertEquals("build/testuser/component.json", result);
        verify(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), eq(true));
        verify(blobClient, never()).setMetadata(any());
    }

    @Test
    void testDeleteComponent_localStorage_ioException_returnsFalse() throws IOException {
        ReflectionTestUtils.setField(azureBlobService, "connectionString", null);
        ReflectionTestUtils.setField(azureBlobService, "buildFolder", "build");
        ReflectionTestUtils.setField(azureBlobService, "uploadDir", tempDir.toString());
        azureBlobService.init();

        // Create a directory instead of a file to trigger IOException on delete
        Path userDir = tempDir.resolve("build/testuser");
        Files.createDirectories(userDir);
        Path dirPath = userDir.resolve("directory-cant-delete");
        Files.createDirectories(dirPath);
        // Put a file inside to make it non-empty
        Files.writeString(dirPath.resolve("file-inside.txt"), "content");

        // Trying to delete a non-empty directory as if it were a file should fail
        boolean result = azureBlobService.deleteComponent("build/testuser/directory-cant-delete");

        assertFalse(result);
    }
}
