package gov.nist.oscal.tools.api.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockConstruction;

/**
 * Comprehensive test suite for LibraryStorageService.
 * Tests both Azure Blob Storage and local file storage modes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LibraryStorageServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobProperties blobProperties;

    @InjectMocks
    private LibraryStorageService libraryStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set upload directory to temp directory
        ReflectionTestUtils.setField(libraryStorageService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(libraryStorageService, "libraryContainerName", "oscal-library");
    }

    // ==================== Initialization Tests ====================

    @Test
    void testInit_withValidConnectionString_createsContainer() {
        // Use a properly formatted Azure connection string (even though fake)
        String fakeConnectionString = "DefaultEndpointsProtocol=https;AccountName=devstoreaccount1;" +
                "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
                "BlobEndpoint=https://devstoreaccount1.blob.core.windows.net/";

        ReflectionTestUtils.setField(libraryStorageService, "connectionString", fakeConnectionString);

        try (var mockedBuilder = mockConstruction(com.azure.storage.blob.BlobServiceClientBuilder.class,
                (mock, context) -> {
                    when(mock.connectionString(anyString())).thenReturn(mock);
                    when(mock.buildClient()).thenReturn(blobServiceClient);
                })) {

            when(blobServiceClient.getBlobContainerClient("oscal-library")).thenReturn(containerClient);
            when(containerClient.exists()).thenReturn(false);

            libraryStorageService.init();

            verify(containerClient).create();
            assertFalse((boolean) ReflectionTestUtils.getField(libraryStorageService, "useLocalStorage"));
        }
    }

    @Test
    void testInit_withExistingContainer_doesNotCreate() {
        String fakeConnectionString = "DefaultEndpointsProtocol=https;AccountName=devstoreaccount1;" +
                "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
                "BlobEndpoint=https://devstoreaccount1.blob.core.windows.net/";

        ReflectionTestUtils.setField(libraryStorageService, "connectionString", fakeConnectionString);

        try (var mockedBuilder = mockConstruction(com.azure.storage.blob.BlobServiceClientBuilder.class,
                (mock, context) -> {
                    when(mock.connectionString(anyString())).thenReturn(mock);
                    when(mock.buildClient()).thenReturn(blobServiceClient);
                })) {

            when(blobServiceClient.getBlobContainerClient("oscal-library")).thenReturn(containerClient);
            when(containerClient.exists()).thenReturn(true);

            libraryStorageService.init();

            verify(containerClient, never()).create();
            assertFalse((boolean) ReflectionTestUtils.getField(libraryStorageService, "useLocalStorage"));
        }
    }

    @Test
    void testInit_withNullConnectionString_usesLocalStorage() {
        ReflectionTestUtils.setField(libraryStorageService, "connectionString", null);

        libraryStorageService.init();

        assertTrue((boolean) ReflectionTestUtils.getField(libraryStorageService, "useLocalStorage"));
        verify(blobServiceClient, never()).getBlobContainerClient(anyString());

        // Verify local library path was created
        Path localLibraryPath = (Path) ReflectionTestUtils.getField(libraryStorageService, "localLibraryPath");
        assertNotNull(localLibraryPath);
        assertTrue(Files.exists(localLibraryPath));
    }

    @Test
    void testInit_withEmptyConnectionString_usesLocalStorage() {
        ReflectionTestUtils.setField(libraryStorageService, "connectionString", "");

        libraryStorageService.init();

        assertTrue((boolean) ReflectionTestUtils.getField(libraryStorageService, "useLocalStorage"));
        verify(blobServiceClient, never()).getBlobContainerClient(anyString());
    }

    @Test
    void testInit_withAzureFailure_throwsException() {
        ReflectionTestUtils.setField(libraryStorageService, "connectionString", "valid-connection-string");

        when(blobServiceClient.getBlobContainerClient("oscal-library"))
                .thenThrow(new RuntimeException("Azure connection failed"));

        assertThrows(RuntimeException.class, () -> libraryStorageService.init());
    }

    // ==================== Local Storage Mode Tests ====================

    @Test
    void testSaveLibraryFile_localStorageMode_success() throws IOException {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", true);
        Path localLibraryPath = tempDir.resolve("library");
        Files.createDirectories(localLibraryPath);
        ReflectionTestUtils.setField(libraryStorageService, "localLibraryPath", localLibraryPath);

        String content = "{ \"catalog\": { \"uuid\": \"test\" } }";
        String blobPath = "item123/v1/catalog.json";

        boolean result = libraryStorageService.saveLibraryFile(content, blobPath, null);

        assertTrue(result);

        // Verify file was created
        Path expectedPath = localLibraryPath.resolve(blobPath);
        assertTrue(Files.exists(expectedPath));
        assertEquals(content, Files.readString(expectedPath));
    }

    @Test
    void testSaveLibraryFile_localStorageMode_createsDirectories() throws IOException {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", true);
        Path localLibraryPath = tempDir.resolve("library");
        Files.createDirectories(localLibraryPath);
        ReflectionTestUtils.setField(libraryStorageService, "localLibraryPath", localLibraryPath);

        String content = "test content";
        String blobPath = "item456/v2/nested/file.json";

        boolean result = libraryStorageService.saveLibraryFile(content, blobPath, null);

        assertTrue(result);

        Path expectedPath = localLibraryPath.resolve(blobPath);
        assertTrue(Files.exists(expectedPath));
        assertTrue(Files.exists(expectedPath.getParent()));
    }

    @Test
    void testGetLibraryFileContent_localStorageMode_success() throws IOException {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", true);
        Path localLibraryPath = tempDir.resolve("library");
        Files.createDirectories(localLibraryPath);
        ReflectionTestUtils.setField(libraryStorageService, "localLibraryPath", localLibraryPath);

        // Create test file
        String expectedContent = "test file content";
        String blobPath = "item123/v1/test.json";
        Path filePath = localLibraryPath.resolve(blobPath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, expectedContent);

        String result = libraryStorageService.getLibraryFileContent(blobPath);

        assertEquals(expectedContent, result);
    }

    @Test
    void testGetLibraryFileContent_localStorageMode_fileNotFound_throwsException() {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", true);
        Path localLibraryPath = tempDir.resolve("library");
        ReflectionTestUtils.setField(libraryStorageService, "localLibraryPath", localLibraryPath);

        assertThrows(RuntimeException.class, () ->
                libraryStorageService.getLibraryFileContent("item123/v1/nonexistent.json")
        );
    }

    // ==================== Azure Storage Mode Tests ====================

    @Test
    void testSaveLibraryFile_azureStorage_success() {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", false);
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);

        String content = "{ \"catalog\": { \"uuid\": \"test\" } }";
        String blobPath = "item123/v1/catalog.json";

        boolean result = libraryStorageService.saveLibraryFile(content, blobPath, null);

        assertTrue(result);
        verify(containerClient).getBlobClient(blobPath);
        verify(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), eq(true));
        verify(blobClient, never()).setMetadata(any());
    }

    @Test
    void testSaveLibraryFile_azureStorage_withMetadata_setsMetadata() {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", false);
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);

        String content = "test content";
        String blobPath = "item123/v1/file.json";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("itemId", "item123");
        metadata.put("versionId", "v1");

        boolean result = libraryStorageService.saveLibraryFile(content, blobPath, metadata);

        assertTrue(result);
        verify(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), eq(true));
        verify(blobClient).setMetadata(metadata);
    }

    @Test
    void testSaveLibraryFile_azureStorage_uploadFails_throwsException() {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", false);
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        doThrow(new RuntimeException("Upload failed"))
                .when(blobClient).upload(any(ByteArrayInputStream.class), anyLong(), eq(true));

        assertThrows(RuntimeException.class, () ->
                libraryStorageService.saveLibraryFile("content", "item123/v1/file.json", null)
        );
    }

    @Test
    void testSaveLibraryFile_azureStorage_notConfigured_throwsException() {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", false);
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", null);

        assertThrows(RuntimeException.class, () ->
                libraryStorageService.saveLibraryFile("content", "item123/v1/file.json", null)
        );
    }

    @Test
    void testGetLibraryFileContent_azureStorage_success() {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", false);
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);

        String expectedContent = "test content from azure";
        when(blobClient.downloadContent()).thenReturn(BinaryData.fromString(expectedContent));

        String result = libraryStorageService.getLibraryFileContent("item123/v1/test.json");

        assertEquals(expectedContent, result);
        verify(containerClient).getBlobClient("item123/v1/test.json");
    }

    @Test
    void testGetLibraryFileContent_azureStorage_blobNotFound_throwsException() {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", false);
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                libraryStorageService.getLibraryFileContent("item123/v1/nonexistent.json")
        );
    }

    @Test
    void testGetLibraryFileContent_azureStorage_notConfigured_throwsException() {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", false);
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", null);

        assertThrows(RuntimeException.class, () ->
                libraryStorageService.getLibraryFileContent("item123/v1/file.json")
        );
    }

    @Test
    void testDeleteLibraryFile_azureStorage_success() {
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);

        boolean result = libraryStorageService.deleteLibraryFile("item123/v1/test.json");

        assertTrue(result);
        verify(blobClient).delete();
    }

    @Test
    void testDeleteLibraryFile_azureStorage_blobNotFound_returnsFalse() {
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        boolean result = libraryStorageService.deleteLibraryFile("item123/v1/nonexistent.json");

        assertFalse(result);
        verify(blobClient, never()).delete();
    }

    @Test
    void testDeleteLibraryFile_azureStorage_notConfigured_throwsException() {
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", null);

        assertThrows(RuntimeException.class, () ->
                libraryStorageService.deleteLibraryFile("item123/v1/file.json")
        );
    }

    @Test
    void testDeleteLibraryFile_azureStorage_deleteFails_returnsFalse() {
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        doThrow(new RuntimeException("Delete failed")).when(blobClient).delete();

        boolean result = libraryStorageService.deleteLibraryFile("item123/v1/test.json");

        assertFalse(result);
    }

    @Test
    void testFileExists_azureStorage_fileExists_returnsTrue() {
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);

        boolean result = libraryStorageService.fileExists("item123/v1/test.json");

        assertTrue(result);
    }

    @Test
    void testFileExists_azureStorage_fileNotFound_returnsFalse() {
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        boolean result = libraryStorageService.fileExists("item123/v1/nonexistent.json");

        assertFalse(result);
    }

    @Test
    void testFileExists_azureStorage_notConfigured_returnsFalse() {
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", null);

        boolean result = libraryStorageService.fileExists("item123/v1/file.json");

        assertFalse(result);
    }

    @Test
    void testFileExists_azureStorage_checkFails_returnsFalse() {
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.exists()).thenThrow(new RuntimeException("Check failed"));

        boolean result = libraryStorageService.fileExists("item123/v1/test.json");

        assertFalse(result);
    }

    @Test
    void testGetFileSize_azureStorage_success() {
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getBlobSize()).thenReturn(1024L);

        long result = libraryStorageService.getFileSize("item123/v1/test.json");

        assertEquals(1024L, result);
    }

    @Test
    void testGetFileSize_azureStorage_notConfigured_throwsException() {
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", null);

        assertThrows(RuntimeException.class, () ->
                libraryStorageService.getFileSize("item123/v1/file.json")
        );
    }

    @Test
    void testGetFileSize_azureStorage_getFails_throwsException() {
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobClient.getProperties()).thenThrow(new RuntimeException("Get properties failed"));

        assertThrows(RuntimeException.class, () ->
                libraryStorageService.getFileSize("item123/v1/test.json")
        );
    }

    // ==================== Utility Method Tests ====================

    @Test
    void testBuildBlobPath_normalFilename() {
        String result = libraryStorageService.buildBlobPath("item123", "v1", "catalog.json");

        assertEquals("item123/v1/catalog.json", result);
    }

    @Test
    void testBuildBlobPath_withSpecialCharacters_sanitized() {
        String result = libraryStorageService.buildBlobPath("item-456", "v2", "my catalog@#$.json");

        assertEquals("item-456/v2/my_catalog___.json", result);
        assertFalse(result.contains("@"));
        assertFalse(result.contains("#"));
        assertFalse(result.contains("$"));
        assertFalse(result.contains(" "));
    }

    @Test
    void testBuildBlobPath_preservesValidCharacters() {
        String result = libraryStorageService.buildBlobPath("item123", "v1", "catalog-v2_final.json");

        assertTrue(result.contains("catalog-v2_final.json"));
        assertTrue(result.contains("-"));
        assertTrue(result.contains("_"));
        assertTrue(result.contains("."));
    }

    @Test
    void testIsConfigured_azureMode_returnsTrue() {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", false);
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", containerClient);

        boolean result = libraryStorageService.isConfigured();

        assertTrue(result);
    }

    @Test
    void testIsConfigured_localMode_returnsTrue() {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", true);
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", null);

        boolean result = libraryStorageService.isConfigured();

        assertTrue(result);
    }

    @Test
    void testIsConfigured_notInitialized_returnsFalse() {
        ReflectionTestUtils.setField(libraryStorageService, "useLocalStorage", false);
        ReflectionTestUtils.setField(libraryStorageService, "containerClient", null);

        boolean result = libraryStorageService.isConfigured();

        assertFalse(result);
    }
}
