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
}
