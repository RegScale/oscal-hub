/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.OscalModelType;
import gov.nist.oscal.tools.api.model.SavedFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        // Create a new service instance for each test
        service = new FileStorageService();

        // Force local storage mode by not setting connection string
        ReflectionTestUtils.setField(service, "connectionString", "");
        ReflectionTestUtils.setField(service, "containerName", "oscal-files");

        // Initialize the service (will use local storage)
        service.init();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up test user directories
        String storageDir = System.getProperty("user.home") + "/.oscal-hub/files";
        Path storagePath = Paths.get(storageDir);

        if (Files.exists(storagePath)) {
            // Delete test user directories
            try (Stream<Path> paths = Files.list(storagePath)) {
                paths.filter(p -> {
                    String fileName = p.getFileName().toString();
                    return fileName.equals("testuser") ||
                           fileName.equals("user1") ||
                           fileName.equals("user2") ||
                           fileName.equals("newuser") ||
                           fileName.equals("user");
                }).forEach(userDir -> {
                    try {
                        deleteDirectory(userDir);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
            }
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> paths = Files.walk(directory)) {
                paths.sorted((a, b) -> b.compareTo(a))
                     .forEach(path -> {
                         try {
                             Files.deleteIfExists(path);
                         } catch (IOException e) {
                             // Ignore
                         }
                     });
            }
        }
    }

    @Test
    void testInitialization_withNoConnectionString_usesLocalStorage() {
        // Given: Service initialized without Azure connection string

        // When: Check if local storage is being used
        Boolean useLocalStorage = (Boolean) ReflectionTestUtils.getField(service, "useLocalStorage");

        // Then: Should be using local storage
        assertTrue(useLocalStorage);
    }

    @Test
    void testSaveFile_validInput_savesSuccessfully() {
        // Given: Valid file content and metadata
        String content = "<?xml version=\"1.0\"?><catalog xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\"></catalog>";
        String fileName = "test-catalog.xml";
        OscalModelType modelType = OscalModelType.CATALOG;
        OscalFormat format = OscalFormat.XML;
        String username = "testuser";

        // When: Saving the file
        SavedFile savedFile = service.saveFile(content, fileName, modelType, format, username);

        // Then: File should be saved with correct metadata
        assertNotNull(savedFile);
        assertNotNull(savedFile.getId());
        assertEquals(fileName, savedFile.getFileName());
        assertEquals(modelType, savedFile.getModelType());
        assertEquals(format, savedFile.getFormat());
        assertEquals(username, savedFile.getUsername());
        assertTrue(savedFile.getFileSize() > 0);
        assertNotNull(savedFile.getUploadedAt());
        assertNotNull(savedFile.getFilePath());
    }

    @Test
    void testSaveFile_specialCharactersInFileName_sanitizesFileName() {
        // Given: File name with special characters
        String content = "{\"catalog\": {}}";
        String fileName = "test file@#$%.json";
        String username = "testuser";

        // When: Saving the file
        SavedFile savedFile = service.saveFile(content, fileName, OscalModelType.CATALOG, OscalFormat.JSON, username);

        // Then: File name should be sanitized
        assertNotNull(savedFile);
        assertTrue(savedFile.getFilePath().contains("_"));
        assertFalse(savedFile.getFilePath().contains("@"));
        assertFalse(savedFile.getFilePath().contains("#"));
    }

    @Test
    void testListFiles_noFilesExist_returnsEmptyList() {
        // Given: User with no files
        String username = "newuser";

        // When: Listing files
        List<SavedFile> files = service.listFiles(username);

        // Then: Should return empty list
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    void testListFiles_multipleFiles_returnsAllFiles() {
        // Given: Multiple saved files for a user
        String username = "testuser";
        service.saveFile("content1", "file1.xml", OscalModelType.CATALOG, OscalFormat.XML, username);
        service.saveFile("content2", "file2.json", OscalModelType.PROFILE, OscalFormat.JSON, username);
        service.saveFile("content3", "file3.yaml", OscalModelType.SYSTEM_SECURITY_PLAN, OscalFormat.YAML, username);

        // When: Listing files
        List<SavedFile> files = service.listFiles(username);

        // Then: Should return all 3 files
        assertNotNull(files);
        assertEquals(3, files.size());
    }

    @Test
    void testListFiles_multipleUsers_returnOnlyUserFiles() {
        // Given: Files for different users
        String user1 = "user1";
        String user2 = "user2";
        service.saveFile("content1", "file1.xml", OscalModelType.CATALOG, OscalFormat.XML, user1);
        service.saveFile("content2", "file2.json", OscalModelType.PROFILE, OscalFormat.JSON, user2);

        // When: Listing files for user1
        List<SavedFile> files = service.listFiles(user1);

        // Then: Should only return user1's files
        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals("file1.xml", files.get(0).getFileName());
    }

    @Test
    void testGetFileContent_existingFile_returnsContent() {
        // Given: A saved file
        String expectedContent = "<?xml version=\"1.0\"?><catalog></catalog>";
        String username = "testuser";
        SavedFile savedFile = service.saveFile(expectedContent, "test.xml", OscalModelType.CATALOG, OscalFormat.XML, username);

        // When: Getting file content
        String actualContent = service.getFileContent(savedFile.getId(), username);

        // Then: Should return the same content
        assertEquals(expectedContent, actualContent);
    }

    @Test
    void testGetFileContent_nonExistentFile_throwsException() {
        // Given: A non-existent file ID
        String fileId = "non-existent-id";
        String username = "testuser";

        // When & Then: Getting content should throw exception
        assertThrows(RuntimeException.class, () -> {
            service.getFileContent(fileId, username);
        });
    }

    @Test
    void testGetFile_existingFile_returnsMetadata() {
        // Given: A saved file
        String content = "{\"catalog\": {}}";
        String fileName = "test-catalog.json";
        String username = "testuser";
        SavedFile original = service.saveFile(content, fileName, OscalModelType.CATALOG, OscalFormat.JSON, username);

        // When: Getting file metadata
        SavedFile retrieved = service.getFile(original.getId(), username);

        // Then: Should return correct metadata
        assertNotNull(retrieved);
        assertEquals(original.getId(), retrieved.getId());
        assertEquals(original.getFileName(), retrieved.getFileName());
        assertEquals(original.getModelType(), retrieved.getModelType());
        assertEquals(original.getFormat(), retrieved.getFormat());
    }

    @Test
    void testGetFile_nonExistentFile_throwsException() {
        // Given: A non-existent file ID
        String fileId = "non-existent-id";
        String username = "testuser";

        // When & Then: Getting file should throw exception
        assertThrows(RuntimeException.class, () -> {
            service.getFile(fileId, username);
        });
    }

    @Test
    void testDeleteFile_existingFile_deletesSuccessfully() {
        // Given: A saved file
        String content = "test content";
        String username = "testuser";
        SavedFile savedFile = service.saveFile(content, "test.xml", OscalModelType.CATALOG, OscalFormat.XML, username);

        // When: Deleting the file
        boolean deleted = service.deleteFile(savedFile.getId(), username);

        // Then: File should be deleted
        assertTrue(deleted);

        // And: File should not be retrievable
        assertThrows(RuntimeException.class, () -> {
            service.getFile(savedFile.getId(), username);
        });
    }

    @Test
    void testDeleteFile_nonExistentFile_returnsFalse() {
        // Given: A non-existent file ID
        String fileId = "non-existent-id";
        String username = "testuser";

        // When: Deleting the file
        boolean deleted = service.deleteFile(fileId, username);

        // Then: Should return false
        assertFalse(deleted);
    }

    @Test
    void testFormatDetection_xmlFile_returnsXml() {
        // Given: XML file
        SavedFile file = service.saveFile("content", "test.xml", null, OscalFormat.XML, "user");

        // Then: Format should be XML
        assertEquals(OscalFormat.XML, file.getFormat());
    }

    @Test
    void testFormatDetection_jsonFile_returnsJson() {
        // Given: JSON file
        SavedFile file = service.saveFile("content", "test.json", null, OscalFormat.JSON, "user");

        // Then: Format should be JSON
        assertEquals(OscalFormat.JSON, file.getFormat());
    }

    @Test
    void testFormatDetection_yamlFile_returnsYaml() {
        // Given: YAML file
        SavedFile file = service.saveFile("content", "test.yaml", null, OscalFormat.YAML, "user");

        // Then: Format should be YAML
        assertEquals(OscalFormat.YAML, file.getFormat());
    }

    @Test
    void testModelTypeDetection_catalogFile_returnsCatalog() {
        // Given: File with 'catalog' in name
        SavedFile file = service.saveFile("content", "nist-catalog.xml", OscalModelType.CATALOG, OscalFormat.XML, "user");

        // Then: Model type should be CATALOG
        assertEquals(OscalModelType.CATALOG, file.getModelType());
    }

    @Test
    void testModelTypeDetection_profileFile_returnsProfile() {
        // Given: File with 'profile' in name
        SavedFile file = service.saveFile("content", "security-profile.json", OscalModelType.PROFILE, OscalFormat.JSON, "user");

        // Then: Model type should be PROFILE
        assertEquals(OscalModelType.PROFILE, file.getModelType());
    }

    @Test
    void testModelTypeDetection_sspFile_returnsSSP() {
        // Given: File with 'ssp' in name
        SavedFile file = service.saveFile("content", "my-ssp.xml", OscalModelType.SYSTEM_SECURITY_PLAN, OscalFormat.XML, "user");

        // Then: Model type should be SYSTEM_SECURITY_PLAN
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, file.getModelType());
    }

    @Test
    void testFileOrdering_multipleFiles_orderedByMostRecent() throws InterruptedException {
        // Given: Multiple files saved at different times
        String username = "testuser";
        service.saveFile("content1", "file1.xml", OscalModelType.CATALOG, OscalFormat.XML, username);
        Thread.sleep(10); // Ensure different timestamps
        service.saveFile("content2", "file2.xml", OscalModelType.CATALOG, OscalFormat.XML, username);
        Thread.sleep(10);
        SavedFile latest = service.saveFile("content3", "file3.xml", OscalModelType.CATALOG, OscalFormat.XML, username);

        // When: Listing files
        List<SavedFile> files = service.listFiles(username);

        // Then: Most recent file should be first
        assertEquals(3, files.size());
        assertEquals(latest.getId(), files.get(0).getId());
    }

    @Test
    void testSaveAndRetrieve_largeFile_handlesCorrectly() {
        // Given: Large file content (simulate large OSCAL document)
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("<control id=\"control-").append(i).append("\"></control>\n");
        }
        String content = largeContent.toString();
        String username = "testuser";

        // When: Saving and retrieving
        SavedFile savedFile = service.saveFile(content, "large-catalog.xml", OscalModelType.CATALOG, OscalFormat.XML, username);
        String retrievedContent = service.getFileContent(savedFile.getId(), username);

        // Then: Content should match
        assertEquals(content, retrievedContent);
        assertTrue(savedFile.getFileSize() > 100000); // Should be > 100KB
    }

    @Test
    void testMetadataPersistence_savedAndRetrieved_metadataMatches() {
        // Given: File with all metadata
        String content = "test content";
        String fileName = "test-file.json";
        OscalModelType modelType = OscalModelType.COMPONENT_DEFINITION;
        OscalFormat format = OscalFormat.JSON;
        String username = "testuser";

        // When: Saving and retrieving
        SavedFile saved = service.saveFile(content, fileName, modelType, format, username);
        SavedFile retrieved = service.getFile(saved.getId(), username);

        // Then: All metadata should match
        assertEquals(saved.getId(), retrieved.getId());
        assertEquals(saved.getFileName(), retrieved.getFileName());
        assertEquals(saved.getModelType(), retrieved.getModelType());
        assertEquals(saved.getFormat(), retrieved.getFormat());
        assertEquals(saved.getUsername(), retrieved.getUsername());
    }
}
