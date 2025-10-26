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
class ConversionServiceTest {

    @Mock
    private HistoryService historyService;

    @Mock
    private FileStorageService fileStorageService;

    private ConversionService conversionService;

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
        conversionService = new ConversionService(historyService, fileStorageService);
    }

    @Test
    void testConvertXmlToJson() {
        // Arrange
        ConversionRequest request = new ConversionRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);
        request.setFileName("test-catalog.xml");

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().contains("\"catalog\""));
        assertTrue(result.getContent().contains("\"uuid\""));
        assertEquals(OscalFormat.XML, result.getFromFormat());
        assertEquals(OscalFormat.JSON, result.getToFormat());

        // Verify interactions
        verify(historyService, times(1)).saveOperation(any(OperationHistory.class));
        // Should save both input and converted files
        verify(fileStorageService, times(2)).saveFile(anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void testConvertJsonToXml() {
        // Arrange
        ConversionRequest request = new ConversionRequest();
        request.setContent(VALID_CATALOG_JSON);
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.JSON);
        request.setToFormat(OscalFormat.XML);
        request.setFileName("test-catalog.json");

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().contains("<catalog"));
        assertTrue(result.getContent().contains("uuid=\"74c8ba1e-5cd4-4ad1-bbfd-d888e2f6c724\""));
        assertEquals(OscalFormat.JSON, result.getFromFormat());
        assertEquals(OscalFormat.XML, result.getToFormat());
    }

    @Test
    void testConvertXmlToYaml() {
        // Arrange
        ConversionRequest request = new ConversionRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.YAML);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().contains("catalog:"));
        assertTrue(result.getContent().contains("uuid:"));
    }

    @Test
    void testConvertYamlToXml() {
        // Arrange
        String validYaml =
            "---\n" +
            "catalog:\n" +
            "  uuid: 74c8ba1e-5cd4-4ad1-bbfd-d888e2f6c724\n" +
            "  metadata:\n" +
            "    title: Sample Security Catalog\n" +
            "    last-modified: 2023-01-01T00:00:00.000Z\n" +
            "    version: '1.0'\n" +
            "    oscal-version: 1.0.4\n";

        ConversionRequest request = new ConversionRequest();
        request.setContent(validYaml);
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.YAML);
        request.setToFormat(OscalFormat.XML);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().contains("<catalog"));
    }

    @Test
    void testConvertWithDefaultFileName() {
        // Arrange
        ConversionRequest request = new ConversionRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);
        // No fileName set

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());

        // Verify default filenames were used
        verify(fileStorageService, times(1)).saveFile(
            anyString(),
            eq("document.xml"),
            any(),
            any(),
            anyString()
        );
        verify(fileStorageService, times(1)).saveFile(
            anyString(),
            eq("document.json"),
            any(),
            any(),
            anyString()
        );
    }

    @Test
    void testConvertWhenFileStorageFails() {
        // Arrange
        ConversionRequest request = new ConversionRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);

        doThrow(new RuntimeException("Storage failure"))
            .when(fileStorageService)
            .saveFile(anyString(), anyString(), any(), any(), anyString());

        // Act - should not throw exception
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert - conversion should still succeed
        assertNotNull(result);
        assertTrue(result.isSuccess());

        // History should still be saved
        verify(historyService, times(1)).saveOperation(any(OperationHistory.class));
    }

    @Test
    void testConvertWhenHistorySaveFails() {
        // Arrange
        ConversionRequest request = new ConversionRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);

        doThrow(new RuntimeException("History save failure"))
            .when(historyService)
            .saveOperation(any(OperationHistory.class));

        // Act - should not throw exception
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert - conversion result should still be returned
        assertNotNull(result);
        assertTrue(result.isSuccess());
    }

    @Test
    void testConvertProfile() {
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

        ConversionRequest request = new ConversionRequest();
        request.setContent(validProfile);
        request.setModelType(OscalModelType.PROFILE);
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getContent().contains("\"profile\""));
    }

    @Test
    void testConvertComponentDefinition() {
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

        ConversionRequest request = new ConversionRequest();
        request.setContent(validComponentDef);
        request.setModelType(OscalModelType.COMPONENT_DEFINITION);
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getContent().contains("\"component-definition\""));
    }

    @Test
    void testConvertFileNameExtensionReplacement() {
        // Arrange
        ConversionRequest request = new ConversionRequest();
        request.setContent(VALID_CATALOG_XML);
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);
        request.setFileName("my-catalog.xml");

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());

        // Verify that the output filename has the correct extension
        verify(fileStorageService, times(1)).saveFile(
            anyString(),
            eq("my-catalog.xml"),
            any(),
            any(),
            anyString()
        );
        verify(fileStorageService, times(1)).saveFile(
            anyString(),
            eq("my-catalog.json"),
            any(),
            any(),
            anyString()
        );
    }

    @Test
    void testConvertMalformedJson() {
        // Arrange
        ConversionRequest request = new ConversionRequest();
        request.setContent("{invalid json}");
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.JSON);
        request.setToFormat(OscalFormat.XML);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
    }

    @Test
    void testConvertSameFormat() {
        // Arrange - converting from JSON to JSON
        ConversionRequest request = new ConversionRequest();
        request.setContent(VALID_CATALOG_JSON);
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.JSON);
        request.setToFormat(OscalFormat.JSON);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert - should still work, effectively validating and reformatting
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getContent());
    }

    @Test
    void testConvertSystemSecurityPlan() {
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
            "    </user>\n" +
            "  </system-implementation>\n" +
            "  <control-implementation>\n" +
            "    <description><p>Control implementation</p></description>\n" +
            "  </control-implementation>\n" +
            "</system-security-plan>";

        ConversionRequest request = new ConversionRequest();
        request.setContent(validSSP);
        request.setModelType(OscalModelType.SYSTEM_SECURITY_PLAN);
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getContent().contains("\"system-security-plan\""));
    }

    @Test
    void testConvertAssessmentPlan() {
        // Arrange
        String validAP =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<assessment-plan xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\" uuid=\"12345678-1234-1234-1234-123456789012\">\n" +
            "  <metadata>\n" +
            "    <title>Sample Assessment Plan</title>\n" +
            "    <last-modified>2023-01-01T00:00:00.000Z</last-modified>\n" +
            "    <version>1.0</version>\n" +
            "    <oscal-version>1.0.4</oscal-version>\n" +
            "  </metadata>\n" +
            "  <import-ssp href=\"ssp.xml\"/>\n" +
            "</assessment-plan>";

        ConversionRequest request = new ConversionRequest();
        request.setContent(validAP);
        request.setModelType(OscalModelType.ASSESSMENT_PLAN);
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getContent().contains("\"assessment-plan\""));
    }

    @Test
    void testConvertAssessmentResults() {
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

        ConversionRequest request = new ConversionRequest();
        request.setContent(validAR);
        request.setModelType(OscalModelType.ASSESSMENT_RESULTS);
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getContent().contains("\"assessment-results\""));
    }

    @Test
    void testConvertPlanOfActionAndMilestones() {
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

        ConversionRequest request = new ConversionRequest();
        request.setContent(validPOAM);
        request.setModelType(OscalModelType.PLAN_OF_ACTION_AND_MILESTONES);
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getContent().contains("\"plan-of-action-and-milestones\""));
    }

    @Test
    void testConvertJsonToYaml() {
        // Arrange
        ConversionRequest request = new ConversionRequest();
        request.setContent(VALID_CATALOG_JSON);
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.JSON);
        request.setToFormat(OscalFormat.YAML);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().contains("catalog:"));
        assertTrue(result.getContent().contains("uuid:"));
    }

    @Test
    void testConvertYamlToJson() {
        // Arrange
        String validYaml =
            "---\n" +
            "catalog:\n" +
            "  uuid: 74c8ba1e-5cd4-4ad1-bbfd-d888e2f6c724\n" +
            "  metadata:\n" +
            "    title: Sample Security Catalog\n" +
            "    last-modified: 2023-01-01T00:00:00.000Z\n" +
            "    version: '1.0'\n" +
            "    oscal-version: 1.0.4\n";

        ConversionRequest request = new ConversionRequest();
        request.setContent(validYaml);
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.YAML);
        request.setToFormat(OscalFormat.JSON);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().contains("\"catalog\""));
        assertTrue(result.getContent().contains("\"uuid\""));
    }

    @Test
    void testConvertYamlToYaml() {
        // Arrange - converting from YAML to YAML (effectively reformatting)
        String validYaml =
            "---\n" +
            "catalog:\n" +
            "  uuid: 74c8ba1e-5cd4-4ad1-bbfd-d888e2f6c724\n" +
            "  metadata:\n" +
            "    title: Sample Security Catalog\n" +
            "    last-modified: 2023-01-01T00:00:00.000Z\n" +
            "    version: '1.0'\n" +
            "    oscal-version: 1.0.4\n";

        ConversionRequest request = new ConversionRequest();
        request.setContent(validYaml);
        request.setModelType(OscalModelType.CATALOG);
        request.setFromFormat(OscalFormat.YAML);
        request.setToFormat(OscalFormat.YAML);

        // Act
        ConversionResult result = conversionService.convert(request, "testuser");

        // Assert - should still work, effectively validating and reformatting
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().contains("catalog:"));
    }
}
