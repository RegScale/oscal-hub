package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SavedFileTest {

    @Test
    void testNoArgsConstructor() {
        SavedFile savedFile = new SavedFile();

        assertNotNull(savedFile);
        assertNull(savedFile.getId());
        assertNull(savedFile.getFileName());
        assertNull(savedFile.getModelType());
        assertNull(savedFile.getFormat());
        assertEquals(0, savedFile.getFileSize());
        assertNull(savedFile.getUploadedAt());
        assertNull(savedFile.getFilePath());
        assertNull(savedFile.getUsername());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime uploadTime = LocalDateTime.of(2025, 1, 15, 10, 30);

        SavedFile savedFile = new SavedFile(
                "file-12345",
                "system-security-plan.json",
                OscalModelType.SYSTEM_SECURITY_PLAN,
                OscalFormat.JSON,
                1024L,
                uploadTime,
                "/uploads/files/file-12345.json"
        );

        assertEquals("file-12345", savedFile.getId());
        assertEquals("system-security-plan.json", savedFile.getFileName());
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, savedFile.getModelType());
        assertEquals(OscalFormat.JSON, savedFile.getFormat());
        assertEquals(1024L, savedFile.getFileSize());
        assertEquals(uploadTime, savedFile.getUploadedAt());
        assertEquals("/uploads/files/file-12345.json", savedFile.getFilePath());
    }

    @Test
    void testAllArgsConstructorWithCatalog() {
        SavedFile savedFile = new SavedFile(
                "catalog-001",
                "nist-800-53.xml",
                OscalModelType.CATALOG,
                OscalFormat.XML,
                51200L,
                LocalDateTime.now(),
                "/uploads/catalogs/nist-800-53.xml"
        );

        assertEquals("catalog-001", savedFile.getId());
        assertEquals(OscalModelType.CATALOG, savedFile.getModelType());
        assertEquals(OscalFormat.XML, savedFile.getFormat());
        assertEquals(51200L, savedFile.getFileSize());
    }

    @Test
    void testSetId() {
        SavedFile savedFile = new SavedFile();
        savedFile.setId("file-99999");
        assertEquals("file-99999", savedFile.getId());
    }

    @Test
    void testSetFileName() {
        SavedFile savedFile = new SavedFile();
        savedFile.setFileName("assessment-results.yaml");
        assertEquals("assessment-results.yaml", savedFile.getFileName());
    }

    @Test
    void testSetModelType() {
        SavedFile savedFile = new SavedFile();
        savedFile.setModelType(OscalModelType.ASSESSMENT_RESULTS);
        assertEquals(OscalModelType.ASSESSMENT_RESULTS, savedFile.getModelType());
    }

    @Test
    void testSetFormat() {
        SavedFile savedFile = new SavedFile();
        savedFile.setFormat(OscalFormat.YAML);
        assertEquals(OscalFormat.YAML, savedFile.getFormat());
    }

    @Test
    void testSetFileSize() {
        SavedFile savedFile = new SavedFile();
        savedFile.setFileSize(2048L);
        assertEquals(2048L, savedFile.getFileSize());
    }

    @Test
    void testSetUploadedAt() {
        SavedFile savedFile = new SavedFile();
        LocalDateTime timestamp = LocalDateTime.of(2025, 2, 20, 14, 45);
        savedFile.setUploadedAt(timestamp);
        assertEquals(timestamp, savedFile.getUploadedAt());
    }

    @Test
    void testSetFilePath() {
        SavedFile savedFile = new SavedFile();
        savedFile.setFilePath("/var/uploads/oscal/profile-123.json");
        assertEquals("/var/uploads/oscal/profile-123.json", savedFile.getFilePath());
    }

    @Test
    void testSetUsername() {
        SavedFile savedFile = new SavedFile();
        savedFile.setUsername("john.doe");
        assertEquals("john.doe", savedFile.getUsername());
    }

    @Test
    void testWithAllOscalModelTypes() {
        OscalModelType[] types = {
                OscalModelType.CATALOG,
                OscalModelType.PROFILE,
                OscalModelType.COMPONENT_DEFINITION,
                OscalModelType.SYSTEM_SECURITY_PLAN,
                OscalModelType.ASSESSMENT_PLAN,
                OscalModelType.ASSESSMENT_RESULTS,
                OscalModelType.PLAN_OF_ACTION_AND_MILESTONES
        };

        for (OscalModelType type : types) {
            SavedFile savedFile = new SavedFile();
            savedFile.setModelType(type);
            assertEquals(type, savedFile.getModelType());
        }
    }

    @Test
    void testWithAllOscalFormats() {
        OscalFormat[] formats = {
                OscalFormat.JSON,
                OscalFormat.XML,
                OscalFormat.YAML
        };

        for (OscalFormat format : formats) {
            SavedFile savedFile = new SavedFile();
            savedFile.setFormat(format);
            assertEquals(format, savedFile.getFormat());
        }
    }

    @Test
    void testWithZeroFileSize() {
        SavedFile savedFile = new SavedFile();
        savedFile.setFileSize(0L);
        assertEquals(0L, savedFile.getFileSize());
    }

    @Test
    void testWithLargeFileSize() {
        SavedFile savedFile = new SavedFile();
        long largeSize = 1024L * 1024L * 100L; // 100 MB
        savedFile.setFileSize(largeSize);
        assertEquals(largeSize, savedFile.getFileSize());
    }

    @Test
    void testWithVeryLargeFileSize() {
        SavedFile savedFile = new SavedFile();
        long veryLargeSize = 1024L * 1024L * 1024L; // 1 GB
        savedFile.setFileSize(veryLargeSize);
        assertEquals(veryLargeSize, savedFile.getFileSize());
    }

    @Test
    void testWithEmptyStrings() {
        SavedFile savedFile = new SavedFile();
        savedFile.setId("");
        savedFile.setFileName("");
        savedFile.setFilePath("");
        savedFile.setUsername("");

        assertEquals("", savedFile.getId());
        assertEquals("", savedFile.getFileName());
        assertEquals("", savedFile.getFilePath());
        assertEquals("", savedFile.getUsername());
    }

    @Test
    void testSetAllFieldsToNull() {
        SavedFile savedFile = new SavedFile();
        savedFile.setId("id");
        savedFile.setFileName("file");
        savedFile.setModelType(OscalModelType.CATALOG);
        savedFile.setFormat(OscalFormat.JSON);
        savedFile.setFileSize(100L);
        savedFile.setUploadedAt(LocalDateTime.now());
        savedFile.setFilePath("path");
        savedFile.setUsername("user");

        savedFile.setId(null);
        savedFile.setFileName(null);
        savedFile.setModelType(null);
        savedFile.setFormat(null);
        savedFile.setFileSize(0L);
        savedFile.setUploadedAt(null);
        savedFile.setFilePath(null);
        savedFile.setUsername(null);

        assertNull(savedFile.getId());
        assertNull(savedFile.getFileName());
        assertNull(savedFile.getModelType());
        assertNull(savedFile.getFormat());
        assertEquals(0L, savedFile.getFileSize());
        assertNull(savedFile.getUploadedAt());
        assertNull(savedFile.getFilePath());
        assertNull(savedFile.getUsername());
    }

    @Test
    void testModifyAllFields() {
        SavedFile savedFile = new SavedFile();

        savedFile.setId("old-id");
        savedFile.setFileName("old-file.json");
        savedFile.setModelType(OscalModelType.CATALOG);
        savedFile.setFormat(OscalFormat.JSON);
        savedFile.setFileSize(100L);
        savedFile.setUploadedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
        savedFile.setFilePath("/old/path");
        savedFile.setUsername("old.user");

        savedFile.setId("new-id");
        savedFile.setFileName("new-file.xml");
        savedFile.setModelType(OscalModelType.PROFILE);
        savedFile.setFormat(OscalFormat.XML);
        savedFile.setFileSize(200L);
        savedFile.setUploadedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        savedFile.setFilePath("/new/path");
        savedFile.setUsername("new.user");

        assertEquals("new-id", savedFile.getId());
        assertEquals("new-file.xml", savedFile.getFileName());
        assertEquals(OscalModelType.PROFILE, savedFile.getModelType());
        assertEquals(OscalFormat.XML, savedFile.getFormat());
        assertEquals(200L, savedFile.getFileSize());
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), savedFile.getUploadedAt());
        assertEquals("/new/path", savedFile.getFilePath());
        assertEquals("new.user", savedFile.getUsername());
    }

    @Test
    void testWithSpecialCharactersInFileName() {
        SavedFile savedFile = new SavedFile();
        String fileName = "file-name_v2.0 (draft) [NIST-800-53].json";
        savedFile.setFileName(fileName);
        assertEquals(fileName, savedFile.getFileName());
    }

    @Test
    void testWithComplexFilePath() {
        SavedFile savedFile = new SavedFile();
        String path = "/var/lib/oscal-cli/uploads/2025/01/15/user123/file-abc-def-123.json";
        savedFile.setFilePath(path);
        assertEquals(path, savedFile.getFilePath());
    }

    @Test
    void testCompleteSavedFileScenario() {
        LocalDateTime uploadTime = LocalDateTime.of(2025, 3, 10, 9, 15);

        SavedFile savedFile = new SavedFile(
                "ssp-prod-001",
                "production-system-ssp.yaml",
                OscalModelType.SYSTEM_SECURITY_PLAN,
                OscalFormat.YAML,
                8192L,
                uploadTime,
                "/uploads/2025/03/ssp-prod-001.yaml"
        );

        savedFile.setUsername("security.admin");

        assertNotNull(savedFile);
        assertTrue(savedFile.getId().startsWith("ssp"));
        assertTrue(savedFile.getFileName().contains("production"));
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, savedFile.getModelType());
        assertEquals(OscalFormat.YAML, savedFile.getFormat());
        assertTrue(savedFile.getFileSize() > 0);
        assertNotNull(savedFile.getUploadedAt());
        assertTrue(savedFile.getFilePath().endsWith(".yaml"));
        assertEquals("security.admin", savedFile.getUsername());
    }
}
