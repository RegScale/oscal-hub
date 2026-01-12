package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.OperationHistory;
import gov.nist.oscal.tools.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private HistoryService historyService;

    @Mock
    private FileStorageService fileStorageService;

    private ValidationService validationService;

    private static final String VALID_CATALOG_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<catalog xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\" uuid=\"74c8ba1e-5cd4-4ad1-bbfd-d888e2f6c724\">\n" +
        "  <metadata>\n" +
        "    <title>Sample Security Catalog</title>\n" +
        "    <last-modified>2023-01-01T00:00:00.000Z</last-modified>\n" +
        "    <version>1.0</version>\n" +
        "    <oscal-version>1.0.4</oscal-version>\n" +
        "  </metadata>\n" +
        "</catalog>";

    private static final String VALID_CATALOG_JSON =
        "{\n" +
        "  \"catalog\": {\n" +
        "    \"uuid\": \"74c8ba1e-5cd4-4ad1-bbfd-d888e2f6c724\",\n" +
        "    \"metadata\": {\n" +
        "      \"title\": \"Sample Security Catalog\",\n" +
        "      \"last-modified\": \"2023-01-01T00:00:00.000Z\",\n" +
        "      \"version\": \"1.0\",\n" +
        "      \"oscal-version\": \"1.0.4\"\n" +
        "    }\n" +
        "  }\n" +
        "}";

    @BeforeEach
    void setUp() {
        validationService = new ValidationService(historyService, fileStorageService);
    }

    @Test
    void testValidateValidCatalogXml() {
        // Arrange
        ValidationRequest request = new ValidationRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.XML);
        request.setFileName("test-catalog.xml");

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(OscalModelType.CATALOG, result.getModelType());
        assertEquals(OscalFormat.XML, result.getFormat());
        assertTrue(result.getErrors().isEmpty());

        // Verify interactions
        verify(historyService, times(1)).saveOperation(any(OperationHistory.class));
        verify(fileStorageService, times(1)).saveFile(anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void testValidateValidCatalogJson() {
        // Arrange
        ValidationRequest request = new ValidationRequest();
        request.setContent(VALID_CATALOG_JSON);
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.JSON);
        request.setFileName("test-catalog.json");

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(OscalModelType.CATALOG, result.getModelType());
        assertEquals(OscalFormat.JSON, result.getFormat());
        assertTrue(result.getErrors().isEmpty());

        // Verify interactions
        verify(historyService, times(1)).saveOperation(any(OperationHistory.class));
    }

    @Test
    void testValidateMalformedJson() {
        // Arrange
        ValidationRequest request = new ValidationRequest();
        request.setContent("{invalid json}");
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.JSON);

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void testValidateWithDefaultFileName() {
        // Arrange
        ValidationRequest request = new ValidationRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.XML);
        // No fileName set

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());

        // Verify default filename was used
        verify(fileStorageService, times(1)).saveFile(
            anyString(),
            eq("document.xml"),
            any(),
            any(),
            anyString()
        );
    }

    @Test
    void testValidateWhenFileStorageFails() {
        // Arrange
        ValidationRequest request = new ValidationRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.XML);

        doThrow(new RuntimeException("Storage failure"))
            .when(fileStorageService)
            .saveFile(anyString(), anyString(), any(), any(), anyString());

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert - validation should still succeed even if file storage fails
        assertNotNull(result);
        assertTrue(result.isValid());

        // History should still be saved
        verify(historyService, times(1)).saveOperation(any(OperationHistory.class));
    }

    @Test
    void testValidateWhenHistorySaveFails() {
        // Arrange
        ValidationRequest request = new ValidationRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.XML);

        doThrow(new RuntimeException("History save failure"))
            .when(historyService)
            .saveOperation(any(OperationHistory.class));

        // Act - should not throw exception
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert - validation result should still be returned
        assertNotNull(result);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateProfile() {
        // Arrange
        String validProfile =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<profile xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\" uuid=\"12345678-1234-1234-1234-123456789012\">\n" +
            "  <metadata>\n" +
            "    <title>Sample Profile</title>\n" +
            "    <last-modified>2023-01-01T00:00:00.000Z</last-modified>\n" +
            "    <version>1.0</version>\n" +
            "    <oscal-version>1.0.4</oscal-version>\n" +
            "  </metadata>\n" +
            "</profile>";

        ValidationRequest request = new ValidationRequest();
        request.setContent(validProfile);
        request.setModelType(OscalModelType.PROFILE);
        request.setFormat(OscalFormat.XML);

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(OscalModelType.PROFILE, result.getModelType());
    }

    @Test
    void testValidateSystemSecurityPlan() {
        // Arrange
        String validSSP =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<system-security-plan xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\" uuid=\"12345678-1234-1234-1234-123456789012\">\n" +
            "  <metadata>\n" +
            "    <title>Sample SSP</title>\n" +
            "    <last-modified>2023-01-01T00:00:00.000Z</last-modified>\n" +
            "    <version>1.0</version>\n" +
            "    <oscal-version>1.0.4</oscal-version>\n" +
            "  </metadata>\n" +
            "  <import-profile href=\"profile.xml\"/>\n" +
            "  <system-characteristics>\n" +
            "    <system-id identifier-type=\"https://ietf.org/rfc/rfc4122\">12345678-1234-1234-1234-123456789012</system-id>\n" +
            "    <system-name>Test System</system-name>\n" +
            "    <description><p>Test description</p></description>\n" +
            "    <security-sensitivity-level>low</security-sensitivity-level>\n" +
            "    <system-information>\n" +
            "      <information-type>\n" +
            "        <title>System Information Type</title>\n" +
            "        <description><p>Description</p></description>\n" +
            "        <categorization system=\"https://doi.org/10.6028/NIST.SP.800-60v2r1\">\n" +
            "          <information-type-id>C.1.1.1</information-type-id>\n" +
            "        </categorization>\n" +
            "        <confidentiality-impact><base>fips-199-low</base></confidentiality-impact>\n" +
            "        <integrity-impact><base>fips-199-low</base></integrity-impact>\n" +
            "        <availability-impact><base>fips-199-low</base></availability-impact>\n" +
            "      </information-type>\n" +
            "    </system-information>\n" +
            "    <security-impact-level>\n" +
            "      <security-objective-confidentiality>fips-199-low</security-objective-confidentiality>\n" +
            "      <security-objective-integrity>fips-199-low</security-objective-integrity>\n" +
            "      <security-objective-availability>fips-199-low</security-objective-availability>\n" +
            "    </security-impact-level>\n" +
            "    <status state=\"operational\"/>\n" +
            "    <authorization-boundary><description><p>Test boundary</p></description></authorization-boundary>\n" +
            "  </system-characteristics>\n" +
            "  <system-implementation>\n" +
            "    <user uuid=\"12345678-1234-1234-1234-123456789012\">\n" +
            "      <title>Test User</title>\n" +
            "      <role-id>admin</role-id>\n" +
            "    </user>\n" +
            "  </system-implementation>\n" +
            "  <control-implementation>\n" +
            "    <description><p>Control implementation</p></description>\n" +
            "  </control-implementation>\n" +
            "</system-security-plan>";

        ValidationRequest request = new ValidationRequest();
        request.setContent(validSSP);
        request.setModelType(OscalModelType.SYSTEM_SECURITY_PLAN);
        request.setFormat(OscalFormat.XML);

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, result.getModelType());
    }

    @Test
    void testValidateCatalogYaml() {
        // Arrange
        String validCatalogYaml =
            "---\n" +
            "catalog:\n" +
            "  uuid: 74c8ba1e-5cd4-4ad1-bbfd-d888e2f6c724\n" +
            "  metadata:\n" +
            "    title: Sample Security Catalog\n" +
            "    last-modified: 2023-01-01T00:00:00.000Z\n" +
            "    version: '1.0'\n" +
            "    oscal-version: 1.0.4\n";

        ValidationRequest request = new ValidationRequest();
        request.setContent(validCatalogYaml);
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.YAML);
        request.setFileName("test-catalog.yaml");

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(OscalModelType.CATALOG, result.getModelType());
        assertEquals(OscalFormat.YAML, result.getFormat());
    }

    @Test
    void testValidateComponentDefinition() {
        // Arrange
        String validComponentDef =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<component-definition xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\" uuid=\"12345678-1234-1234-1234-123456789012\">\n" +
            "  <metadata>\n" +
            "    <title>Sample Component Definition</title>\n" +
            "    <last-modified>2023-01-01T00:00:00.000Z</last-modified>\n" +
            "    <version>1.0</version>\n" +
            "    <oscal-version>1.0.4</oscal-version>\n" +
            "  </metadata>\n" +
            "</component-definition>";

        ValidationRequest request = new ValidationRequest();
        request.setContent(validComponentDef);
        request.setModelType(OscalModelType.COMPONENT_DEFINITION);
        request.setFormat(OscalFormat.XML);

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(OscalModelType.COMPONENT_DEFINITION, result.getModelType());
    }

    @Test
    void testValidateAssessmentPlan() {
        // Arrange
        String validAP =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<assessment-plan xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\" uuid=\"12345678-1234-1234-1234-123456789012\">\n" +
            "  <metadata>\n" +
            "    <title>Sample Assessment Plan</title>\n" +
            "    <last-modified>2023-01-01T00:00:00.000Z</last-modified>\n" +
            "    <version>1.0</version>\n" +
            "    <oscal-version>1.1.2</oscal-version>\n" +
            "  </metadata>\n" +
            "  <import-ssp href=\"ssp.xml\"/>\n" +
            "  <reviewed-controls>\n" +
            "    <control-selection>\n" +
            "      <include-all/>\n" +
            "    </control-selection>\n" +
            "  </reviewed-controls>\n" +
            "</assessment-plan>";

        ValidationRequest request = new ValidationRequest();
        request.setContent(validAP);
        request.setModelType(OscalModelType.ASSESSMENT_PLAN);
        request.setFormat(OscalFormat.XML);

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(OscalModelType.ASSESSMENT_PLAN, result.getModelType());
    }

    @Test
    void testValidateAssessmentResults() {
        // Arrange
        String validAR =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<assessment-results xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\" uuid=\"12345678-1234-1234-1234-123456789012\">\n" +
            "  <metadata>\n" +
            "    <title>Sample Assessment Results</title>\n" +
            "    <last-modified>2023-01-01T00:00:00.000Z</last-modified>\n" +
            "    <version>1.0</version>\n" +
            "    <oscal-version>1.0.4</oscal-version>\n" +
            "  </metadata>\n" +
            "  <import-ap href=\"ap.xml\"/>\n" +
            "</assessment-results>";

        ValidationRequest request = new ValidationRequest();
        request.setContent(validAR);
        request.setModelType(OscalModelType.ASSESSMENT_RESULTS);
        request.setFormat(OscalFormat.XML);

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(OscalModelType.ASSESSMENT_RESULTS, result.getModelType());
    }

    @Test
    void testValidatePlanOfActionAndMilestones() {
        // Arrange
        String validPOAM =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<plan-of-action-and-milestones xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\" uuid=\"12345678-1234-1234-1234-123456789012\">\n" +
            "  <metadata>\n" +
            "    <title>Sample POAM</title>\n" +
            "    <last-modified>2023-01-01T00:00:00.000Z</last-modified>\n" +
            "    <version>1.0</version>\n" +
            "    <oscal-version>1.0.4</oscal-version>\n" +
            "  </metadata>\n" +
            "</plan-of-action-and-milestones>";

        ValidationRequest request = new ValidationRequest();
        request.setContent(validPOAM);
        request.setModelType(OscalModelType.PLAN_OF_ACTION_AND_MILESTONES);
        request.setFormat(OscalFormat.XML);

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(OscalModelType.PLAN_OF_ACTION_AND_MILESTONES, result.getModelType());
    }

    @Test
    void testValidateWithExistingFileId_SkipsFileSave() {
        // Arrange
        ValidationRequest request = new ValidationRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.XML);
        request.setFileName("test-catalog.xml");
        request.setFileId("existing-file-id-123");  // File already saved

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());

        // Verify file was NOT saved (since fileId is set)
        verify(fileStorageService, never()).saveFile(anyString(), anyString(), any(), any(), anyString());

        // But history should still be saved
        verify(historyService, times(1)).saveOperation(any(OperationHistory.class));
    }

    @Test
    void testValidateWithEmptyFileId_SavesFile() {
        // Arrange
        ValidationRequest request = new ValidationRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.XML);
        request.setFileName("test-catalog.xml");
        request.setFileId("");  // Empty fileId should trigger save

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());

        // Verify file WAS saved (empty fileId counts as null)
        verify(fileStorageService, times(1)).saveFile(anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void testValidateWithWhitespaceFileId_SavesFile() {
        // Arrange
        ValidationRequest request = new ValidationRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.XML);
        request.setFileName("test-catalog.xml");
        request.setFileId("   ");  // Whitespace-only fileId should trigger save

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());

        // Verify file WAS saved (whitespace fileId counts as empty)
        verify(fileStorageService, times(1)).saveFile(anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void testValidateDefaultFileNameForJson() {
        // Arrange
        ValidationRequest request = new ValidationRequest();
        request.setContent(VALID_CATALOG_JSON);
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.JSON);
        // No fileName set

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());

        // Verify default filename was used with .json extension
        verify(fileStorageService, times(1)).saveFile(
            anyString(),
            eq("document.json"),
            any(),
            any(),
            anyString()
        );
    }

    @Test
    void testValidateDefaultFileNameForYaml() {
        // Arrange
        String validCatalogYaml =
            "---\n" +
            "catalog:\n" +
            "  uuid: 74c8ba1e-5cd4-4ad1-bbfd-d888e2f6c724\n" +
            "  metadata:\n" +
            "    title: Sample Catalog\n" +
            "    last-modified: 2023-01-01T00:00:00.000Z\n" +
            "    version: '1.0'\n" +
            "    oscal-version: 1.0.4\n";

        ValidationRequest request = new ValidationRequest();
        request.setContent(validCatalogYaml);
        request.setModelType(OscalModelType.CATALOG);
        request.setFormat(OscalFormat.YAML);
        // No fileName set

        // Act
        ValidationResult result = validationService.validate(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());

        // Verify default filename was used with .yaml extension
        verify(fileStorageService, times(1)).saveFile(
            anyString(),
            eq("document.yaml"),
            any(),
            any(),
            anyString()
        );
    }
}
