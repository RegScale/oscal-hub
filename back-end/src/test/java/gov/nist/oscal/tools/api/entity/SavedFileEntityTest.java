package gov.nist.oscal.tools.api.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SavedFileEntityTest {

    @Test
    void testNoArgsConstructor() {
        SavedFileEntity entity = new SavedFileEntity();

        assertNotNull(entity);
        assertNotNull(entity.getUploadedAt()); // uploadedAt is set in constructor
        assertNull(entity.getId());
        assertNull(entity.getFileId());
        assertNull(entity.getFileName());
        assertNull(entity.getModelType());
        assertNull(entity.getFormat());
        assertNull(entity.getFileSize());
        assertNull(entity.getFilePath());
        assertNull(entity.getUser());
    }

    @Test
    void testAllArgsConstructor() {
        User user = new User();
        user.setUsername("testuser");

        String fileId = "file-uuid-123";
        String fileName = "catalog.json";
        String modelType = "catalog";
        String format = "JSON";
        Long fileSize = 1024L;
        String filePath = "/uploads/catalog.json";

        SavedFileEntity entity = new SavedFileEntity(
            fileId, fileName, modelType, format, fileSize, filePath, user);

        assertNotNull(entity);
        assertEquals(fileId, entity.getFileId());
        assertEquals(fileName, entity.getFileName());
        assertEquals(modelType, entity.getModelType());
        assertEquals(format, entity.getFormat());
        assertEquals(fileSize, entity.getFileSize());
        assertEquals(filePath, entity.getFilePath());
        assertEquals(user, entity.getUser());
        assertNotNull(entity.getUploadedAt());
    }

    @Test
    void testIdGetterAndSetter() {
        SavedFileEntity entity = new SavedFileEntity();

        Long id = 100L;
        entity.setId(id);

        assertEquals(id, entity.getId());
    }

    @Test
    void testFileIdGetterAndSetter() {
        SavedFileEntity entity = new SavedFileEntity();

        String fileId = "uuid-456";
        entity.setFileId(fileId);

        assertEquals(fileId, entity.getFileId());
    }

    @Test
    void testFileNameGetterAndSetter() {
        SavedFileEntity entity = new SavedFileEntity();

        String fileName = "profile.xml";
        entity.setFileName(fileName);

        assertEquals(fileName, entity.getFileName());
    }

    @Test
    void testModelTypeGetterAndSetter() {
        SavedFileEntity entity = new SavedFileEntity();

        String modelType = "profile";
        entity.setModelType(modelType);

        assertEquals(modelType, entity.getModelType());
    }

    @Test
    void testFormatGetterAndSetter() {
        SavedFileEntity entity = new SavedFileEntity();

        String format = "XML";
        entity.setFormat(format);

        assertEquals(format, entity.getFormat());
    }

    @Test
    void testFileSizeGetterAndSetter() {
        SavedFileEntity entity = new SavedFileEntity();

        Long fileSize = 2048L;
        entity.setFileSize(fileSize);

        assertEquals(fileSize, entity.getFileSize());
    }

    @Test
    void testUploadedAtGetterAndSetter() {
        SavedFileEntity entity = new SavedFileEntity();

        LocalDateTime uploadedAt = LocalDateTime.of(2024, 1, 15, 10, 30);
        entity.setUploadedAt(uploadedAt);

        assertEquals(uploadedAt, entity.getUploadedAt());
    }

    @Test
    void testFilePathGetterAndSetter() {
        SavedFileEntity entity = new SavedFileEntity();

        String filePath = "/var/data/uploads/ssp.yaml";
        entity.setFilePath(filePath);

        assertEquals(filePath, entity.getFilePath());
    }

    @Test
    void testUserGetterAndSetter() {
        SavedFileEntity entity = new SavedFileEntity();

        User user = new User();
        user.setUsername("admin");
        user.setEmail("admin@example.com");

        entity.setUser(user);

        assertEquals(user, entity.getUser());
        assertEquals("admin", entity.getUser().getUsername());
        assertEquals("admin@example.com", entity.getUser().getEmail());
    }

    @Test
    void testUploadedAtSetInConstructor() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        SavedFileEntity entity = new SavedFileEntity();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertNotNull(entity.getUploadedAt());
        assertTrue(entity.getUploadedAt().isAfter(before) || entity.getUploadedAt().isEqual(before));
        assertTrue(entity.getUploadedAt().isBefore(after) || entity.getUploadedAt().isEqual(after));
    }

    @Test
    void testAllArgsConstructorSetsUploadedAt() {
        User user = new User();

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        SavedFileEntity entity = new SavedFileEntity(
            "id", "name", "type", "format", 100L, "/path", user);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertNotNull(entity.getUploadedAt());
        assertTrue(entity.getUploadedAt().isAfter(before) || entity.getUploadedAt().isEqual(before));
        assertTrue(entity.getUploadedAt().isBefore(after) || entity.getUploadedAt().isEqual(after));
    }

    @Test
    void testSettersWithNullValues() {
        SavedFileEntity entity = new SavedFileEntity(
            "id", "name", "type", "format", 100L, "/path", new User());

        entity.setFileId(null);
        entity.setFileName(null);
        entity.setModelType(null);
        entity.setFormat(null);
        entity.setFileSize(null);
        entity.setFilePath(null);
        entity.setUser(null);

        assertNull(entity.getFileId());
        assertNull(entity.getFileName());
        assertNull(entity.getModelType());
        assertNull(entity.getFormat());
        assertNull(entity.getFileSize());
        assertNull(entity.getFilePath());
        assertNull(entity.getUser());
    }

    @Test
    void testMultipleFormats() {
        SavedFileEntity jsonEntity = new SavedFileEntity();
        jsonEntity.setFormat("JSON");
        assertEquals("JSON", jsonEntity.getFormat());

        SavedFileEntity xmlEntity = new SavedFileEntity();
        xmlEntity.setFormat("XML");
        assertEquals("XML", xmlEntity.getFormat());

        SavedFileEntity yamlEntity = new SavedFileEntity();
        yamlEntity.setFormat("YAML");
        assertEquals("YAML", yamlEntity.getFormat());
    }

    @Test
    void testMultipleModelTypes() {
        String[] modelTypes = {"catalog", "profile", "ssp", "component-definition",
                               "assessment-plan", "assessment-results", "poam"};

        for (String modelType : modelTypes) {
            SavedFileEntity entity = new SavedFileEntity();
            entity.setModelType(modelType);
            assertEquals(modelType, entity.getModelType());
        }
    }
}
