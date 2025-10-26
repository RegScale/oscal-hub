package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VisualizationServiceTest {

    @InjectMocks
    private VisualizationService visualizationService;

    @BeforeEach
    void setUp() {
        visualizationService = new VisualizationService();
    }

    // ========== analyzeSSP Tests ==========

    @Test
    void testAnalyzeSSP_success_jsonFormat() {
        String sspJson = "{\n" +
            "  \"system-security-plan\": {\n" +
            "    \"uuid\": \"test-uuid-123\",\n" +
            "    \"metadata\": {\n" +
            "      \"title\": \"Test SSP\",\n" +
            "      \"version\": \"1.0\"\n" +
            "    },\n" +
            "    \"system-characteristics\": {\n" +
            "      \"system-name\": \"Test System\",\n" +
            "      \"system-name-short\": \"TS\",\n" +
            "      \"description\": \"Test description\",\n" +
            "      \"status\": {\n" +
            "        \"state\": \"operational\"\n" +
            "      },\n" +
            "      \"security-impact-level\": {\n" +
            "        \"security-objective-confidentiality\": \"moderate\",\n" +
            "        \"security-objective-integrity\": \"moderate\",\n" +
            "        \"security-objective-availability\": \"low\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setContent(sspJson);
        request.setFormat(OscalFormat.JSON);

        SspVisualizationResult result = visualizationService.analyzeSSP(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("SSP analyzed successfully", result.getMessage());
        assertNotNull(result.getSystemInfo());
        assertEquals("test-uuid-123", result.getSystemInfo().getUuid());
        assertEquals("Test System", result.getSystemInfo().getName());
        assertEquals("TS", result.getSystemInfo().getShortName());
        assertEquals("operational", result.getSystemInfo().getStatus());
        assertNotNull(result.getCategorization());
        assertEquals("moderate", result.getCategorization().getConfidentiality());
        assertEquals("moderate", result.getCategorization().getIntegrity());
        assertEquals("low", result.getCategorization().getAvailability());
    }

    @Test
    void testAnalyzeSSP_invalidDocument_missingRequiredFields() {
        String invalidJson = "{\n" +
            "  \"invalid-root\": {\n" +
            "    \"some-field\": \"value\"\n" +
            "  }\n" +
            "}";

        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setContent(invalidJson);
        request.setFormat(OscalFormat.JSON);

        SspVisualizationResult result = visualizationService.analyzeSSP(request, "testuser");

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Invalid SSP document"));
    }

    @Test
    void testAnalyzeSSP_parseError_invalidJson() {
        String invalidJson = "{ invalid json syntax";

        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setContent(invalidJson);
        request.setFormat(OscalFormat.JSON);

        SspVisualizationResult result = visualizationService.analyzeSSP(request, "testuser");

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().startsWith("Failed to analyze SSP:"));
    }

    @Test
    void testAnalyzeSSP_xmlFormat() {
        String sspXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<system-security-plan xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\">\n" +
            "  <uuid>test-uuid-xml</uuid>\n" +
            "  <metadata>\n" +
            "    <title>XML Test SSP</title>\n" +
            "  </metadata>\n" +
            "  <system-characteristics>\n" +
            "    <system-name>XML System</system-name>\n" +
            "  </system-characteristics>\n" +
            "</system-security-plan>";

        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setContent(sspXml);
        request.setFormat(OscalFormat.XML);

        SspVisualizationResult result = visualizationService.analyzeSSP(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getSystemInfo());
    }

    @Test
    void testAnalyzeSSP_withControlImplementation() {
        String sspJson = "{\n" +
            "  \"system-security-plan\": {\n" +
            "    \"uuid\": \"test-uuid\",\n" +
            "    \"metadata\": {\"title\": \"Test\"},\n" +
            "    \"system-characteristics\": {\n" +
            "      \"system-name\": \"Test System\"\n" +
            "    },\n" +
            "    \"control-implementation\": {\n" +
            "      \"implemented-requirements\": [\n" +
            "        {\n" +
            "          \"control-id\": \"ac-1\",\n" +
            "          \"props\": [\n" +
            "            {\n" +
            "              \"name\": \"implementation-status\",\n" +
            "              \"value\": \"implemented\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"name\": \"control-origination\",\n" +
            "              \"value\": \"system-specific\"\n" +
            "            }\n" +
            "          ]\n" +
            "        },\n" +
            "        {\n" +
            "          \"control-id\": \"ac-2\",\n" +
            "          \"props\": [\n" +
            "            {\n" +
            "              \"name\": \"implementation-status\",\n" +
            "              \"value\": \"planned\"\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";

        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setContent(sspJson);
        request.setFormat(OscalFormat.JSON);

        SspVisualizationResult result = visualizationService.analyzeSSP(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getControlsByFamily());
        assertTrue(result.getControlsByFamily().containsKey("ac"));

        SspVisualizationResult.ControlFamilyStatus acFamily = result.getControlsByFamily().get("ac");
        assertEquals(2, acFamily.getTotalControls());
        assertEquals("Access Control", acFamily.getFamilyName());
        assertEquals(1, acFamily.getStatusCounts().get("implemented"));
        assertEquals(1, acFamily.getStatusCounts().get("planned"));
    }

    @Test
    void testAnalyzeSSP_withPersonnelRoles() {
        String sspJson = "{\n" +
            "  \"system-security-plan\": {\n" +
            "    \"uuid\": \"test-uuid\",\n" +
            "    \"metadata\": {\n" +
            "      \"title\": \"Test\",\n" +
            "      \"roles\": [\n" +
            "        {\n" +
            "          \"id\": \"admin\",\n" +
            "          \"title\": \"Administrator\",\n" +
            "          \"short-name\": \"Admin\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"parties\": [\n" +
            "        {\n" +
            "          \"uuid\": \"party-1\",\n" +
            "          \"type\": \"person\",\n" +
            "          \"name\": \"John Doe\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"responsible-parties\": [\n" +
            "        {\n" +
            "          \"role-id\": \"admin\",\n" +
            "          \"party-uuids\": [\"party-1\"]\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    \"system-characteristics\": {\n" +
            "      \"system-name\": \"Test System\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setContent(sspJson);
        request.setFormat(OscalFormat.JSON);

        SspVisualizationResult result = visualizationService.analyzeSSP(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getPersonnel());
        assertEquals(1, result.getPersonnel().size());

        SspVisualizationResult.PersonnelRole adminRole = result.getPersonnel().get(0);
        assertEquals("admin", adminRole.getRoleId());
        assertEquals("Administrator", adminRole.getRoleTitle());
        assertEquals(1, adminRole.getAssignedPersonnel().size());
        assertEquals("John Doe", adminRole.getAssignedPersonnel().get(0).getName());
    }

    @Test
    void testAnalyzeSSP_withAssets() {
        String sspJson = "{\n" +
            "  \"system-security-plan\": {\n" +
            "    \"uuid\": \"test-uuid\",\n" +
            "    \"metadata\": {\"title\": \"Test\"},\n" +
            "    \"system-characteristics\": {\n" +
            "      \"system-name\": \"Test System\"\n" +
            "    },\n" +
            "    \"system-implementation\": {\n" +
            "      \"inventory-items\": [\n" +
            "        {\n" +
            "          \"uuid\": \"asset-1\",\n" +
            "          \"description\": \"Web Server\",\n" +
            "          \"props\": [\n" +
            "            {\"name\": \"asset-type\", \"value\": \"server\"},\n" +
            "            {\"name\": \"ipv4-address\", \"value\": \"10.0.0.1\"},\n" +
            "            {\"name\": \"fqdn\", \"value\": \"web.example.com\"},\n" +
            "            {\"name\": \"virtual\", \"value\": \"yes\"},\n" +
            "            {\"name\": \"public\", \"value\": \"no\"}\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";

        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setContent(sspJson);
        request.setFormat(OscalFormat.JSON);

        SspVisualizationResult result = visualizationService.analyzeSSP(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getAssets());
        assertEquals(1, result.getAssets().size());

        SspVisualizationResult.Asset asset = result.getAssets().get(0);
        assertEquals("asset-1", asset.getUuid());
        assertEquals("Web Server", asset.getDescription());
        assertEquals("server", asset.getAssetType());
        assertEquals("10.0.0.1", asset.getIpv4Address());
        assertEquals("web.example.com", asset.getFqdn());
        assertTrue(asset.isVirtual());
        assertFalse(asset.isPublicAccess());
    }

    // ========== analyzeProfile Tests ==========

    @Test
    void testAnalyzeProfile_success_jsonFormat() {
        String profileJson = "{\n" +
            "  \"profile\": {\n" +
            "    \"uuid\": \"profile-uuid\",\n" +
            "    \"metadata\": {\n" +
            "      \"title\": \"Test Profile\",\n" +
            "      \"version\": \"1.0\",\n" +
            "      \"oscal-version\": \"1.0.4\"\n" +
            "    },\n" +
            "    \"imports\": [\n" +
            "      {\n" +
            "        \"href\": \"#catalog-ref\",\n" +
            "        \"include-controls\": [\n" +
            "          {\n" +
            "            \"with-ids\": [\"ac-1\", \"ac-2\", \"au-1\"]\n" +
            "          }\n" +
            "        ],\n" +
            "        \"exclude-controls\": [\n" +
            "          {\n" +
            "            \"with-ids\": [\"ac-3\"]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

        ProfileVisualizationRequest request = new ProfileVisualizationRequest();
        request.setContent(profileJson);
        request.setFormat(OscalFormat.JSON);

        ProfileVisualizationResult result = visualizationService.analyzeProfile(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Profile analyzed successfully", result.getMessage());
        assertNotNull(result.getProfileInfo());
        assertEquals("profile-uuid", result.getProfileInfo().getUuid());
        assertEquals("Test Profile", result.getProfileInfo().getTitle());
        assertEquals("1.0", result.getProfileInfo().getVersion());
        assertNotNull(result.getImports());
        assertEquals(1, result.getImports().size());

        ProfileVisualizationResult.ImportInfo importInfo = result.getImports().get(0);
        assertEquals("#catalog-ref", importInfo.getHref());
        assertTrue(importInfo.getIncludeAllIds().contains("ac-1"));
        assertTrue(importInfo.getIncludeAllIds().contains("ac-2"));
        assertTrue(importInfo.getIncludeAllIds().contains("au-1"));
        assertTrue(importInfo.getExcludeIds().contains("ac-3"));

        // Check control summary
        assertNotNull(result.getControlSummary());
        assertEquals(3, result.getControlSummary().getTotalIncludedControls());
        assertEquals(1, result.getControlSummary().getTotalExcludedControls());
        assertEquals(2, result.getControlSummary().getUniqueFamilies()); // ac and au

        // Check controls by family
        assertNotNull(result.getControlsByFamily());
        assertTrue(result.getControlsByFamily().containsKey("ac"));
        assertTrue(result.getControlsByFamily().containsKey("au"));

        ProfileVisualizationResult.ControlFamilyInfo acFamily = result.getControlsByFamily().get("ac");
        assertEquals(2, acFamily.getIncludedCount());
        assertEquals(1, acFamily.getExcludedCount());
    }

    @Test
    void testAnalyzeProfile_withIncludeAll() {
        String profileJson = "{\n" +
            "  \"profile\": {\n" +
            "    \"uuid\": \"profile-uuid\",\n" +
            "    \"metadata\": {\n" +
            "      \"title\": \"Test Profile\"\n" +
            "    },\n" +
            "    \"imports\": [\n" +
            "      {\n" +
            "        \"href\": \"#catalog-ref\",\n" +
            "        \"include-all\": {}\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

        ProfileVisualizationRequest request = new ProfileVisualizationRequest();
        request.setContent(profileJson);
        request.setFormat(OscalFormat.JSON);

        ProfileVisualizationResult result = visualizationService.analyzeProfile(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getImports());
        assertEquals(1, result.getImports().size());

        ProfileVisualizationResult.ImportInfo importInfo = result.getImports().get(0);
        assertTrue(importInfo.getIncludeAllIds().contains("*"));
    }

    @Test
    void testAnalyzeProfile_withModifications() {
        String profileJson = "{\n" +
            "  \"profile\": {\n" +
            "    \"uuid\": \"profile-uuid\",\n" +
            "    \"metadata\": {\"title\": \"Test Profile\"},\n" +
            "    \"imports\": [{\"href\": \"#catalog\"}],\n" +
            "    \"modify\": {\n" +
            "      \"set-parameters\": [\n" +
            "        {\"param-id\": \"param-1\", \"values\": [\"value1\"]},\n" +
            "        {\"param-id\": \"param-2\", \"values\": [\"value2\"]}\n" +
            "      ],\n" +
            "      \"alters\": [\n" +
            "        {\"control-id\": \"ac-1\", \"adds\": []},\n" +
            "        {\"control-id\": \"ac-2\", \"adds\": []}\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";

        ProfileVisualizationRequest request = new ProfileVisualizationRequest();
        request.setContent(profileJson);
        request.setFormat(OscalFormat.JSON);

        ProfileVisualizationResult result = visualizationService.analyzeProfile(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getModificationSummary());
        assertEquals(2, result.getModificationSummary().getTotalSetsParameters());
        assertEquals(2, result.getModificationSummary().getTotalAlters());
        assertTrue(result.getModificationSummary().getModifiedControlIds().contains("ac-1"));
        assertTrue(result.getModificationSummary().getModifiedControlIds().contains("ac-2"));

        // Check control summary includes modification count
        assertNotNull(result.getControlSummary());
        assertEquals(4, result.getControlSummary().getTotalModifications()); // 2 params + 2 alters
    }

    @Test
    void testAnalyzeProfile_invalidDocument() {
        String invalidJson = "{\n" +
            "  \"invalid-root\": {\n" +
            "    \"some-field\": \"value\"\n" +
            "  }\n" +
            "}";

        ProfileVisualizationRequest request = new ProfileVisualizationRequest();
        request.setContent(invalidJson);
        request.setFormat(OscalFormat.JSON);

        ProfileVisualizationResult result = visualizationService.analyzeProfile(request, "testuser");

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Invalid Profile document"));
    }

    @Test
    void testAnalyzeProfile_parseError() {
        String invalidJson = "{ invalid json";

        ProfileVisualizationRequest request = new ProfileVisualizationRequest();
        request.setContent(invalidJson);
        request.setFormat(OscalFormat.JSON);

        ProfileVisualizationResult result = visualizationService.analyzeProfile(request, "testuser");

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().startsWith("Failed to analyze Profile:"));
    }

    // ========== analyzeSAR Tests ==========

    @Test
    void testAnalyzeSAR_success_jsonFormat() {
        String sarJson = "{\n" +
            "  \"assessment-results\": {\n" +
            "    \"uuid\": \"sar-uuid\",\n" +
            "    \"metadata\": {\n" +
            "      \"title\": \"Test SAR\",\n" +
            "      \"version\": \"1.0\",\n" +
            "      \"oscal-version\": \"1.0.4\"\n" +
            "    },\n" +
            "    \"import-ssp\": {\n" +
            "      \"href\": \"#ssp-ref\"\n" +
            "    },\n" +
            "    \"results\": [\n" +
            "      {\n" +
            "        \"uuid\": \"result-1\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

        SarVisualizationRequest request = new SarVisualizationRequest();
        request.setContent(sarJson);
        request.setFormat(OscalFormat.JSON);

        SarVisualizationResult result = visualizationService.analyzeSAR(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("SAR analyzed successfully", result.getMessage());
        assertNotNull(result.getAssessmentInfo());
        assertEquals("sar-uuid", result.getAssessmentInfo().getUuid());
        assertEquals("Test SAR", result.getAssessmentInfo().getTitle());
        assertEquals("#ssp-ref", result.getAssessmentInfo().getSspImportHref());
    }

    @Test
    void testAnalyzeSAR_withObservations() {
        String sarJson = "{\n" +
            "  \"assessment-results\": {\n" +
            "    \"uuid\": \"sar-uuid\",\n" +
            "    \"metadata\": {\"title\": \"Test SAR\"},\n" +
            "    \"import-ssp\": {\"href\": \"#ssp\"},\n" +
            "    \"results\": [\n" +
            "      {\n" +
            "        \"uuid\": \"result-1\",\n" +
            "        \"reviewed-controls\": {\n" +
            "          \"control-selections\": [\n" +
            "            {\n" +
            "              \"include-controls\": [\n" +
            "                {\n" +
            "                  \"statement-ids\": [\"ac-1\", \"ac-2\"]\n" +
            "                }\n" +
            "              ]\n" +
            "            }\n" +
            "          ]\n" +
            "        },\n" +
            "        \"observations\": [\n" +
            "          {\n" +
            "            \"uuid\": \"obs-1\",\n" +
            "            \"title\": \"Test Observation\",\n" +
            "            \"description\": \"Test description\",\n" +
            "            \"types\": [\"finding\"],\n" +
            "            \"props\": [\n" +
            "              {\"name\": \"control-id\", \"value\": \"ac-1\"},\n" +
            "              {\"name\": \"overall-score\", \"value\": \"85.5\"},\n" +
            "              {\"name\": \"quality-score\", \"value\": \"90.0\"},\n" +
            "              {\"name\": \"completeness-score\", \"value\": \"80.0\"}\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

        SarVisualizationRequest request = new SarVisualizationRequest();
        request.setContent(sarJson);
        request.setFormat(OscalFormat.JSON);

        SarVisualizationResult result = visualizationService.analyzeSAR(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getObservations());
        assertEquals(1, result.getObservations().size());

        SarVisualizationResult.Observation obs = result.getObservations().get(0);
        assertEquals("obs-1", obs.getUuid());
        assertEquals("Test Observation", obs.getTitle());
        assertEquals("finding", obs.getObservationType());
        assertTrue(obs.getRelatedControls().contains("ac-1"));
        assertEquals(85.5, obs.getOverallScore(), 0.01);
        assertEquals(90.0, obs.getQualityScore(), 0.01);
        assertEquals(80.0, obs.getCompletenessScore(), 0.01);

        // Check assessment summary
        assertNotNull(result.getAssessmentSummary());
        assertEquals(1, result.getAssessmentSummary().getTotalObservations());
        assertEquals(2, result.getAssessmentSummary().getTotalControlsAssessed());

        // Check score distribution
        assertNotNull(result.getAssessmentSummary().getScoreDistribution());
        assertEquals(1, result.getAssessmentSummary().getScoreDistribution().get("80-90"));
    }

    @Test
    void testAnalyzeSAR_withFindings() {
        String sarJson = "{\n" +
            "  \"assessment-results\": {\n" +
            "    \"uuid\": \"sar-uuid\",\n" +
            "    \"metadata\": {\"title\": \"Test SAR\"},\n" +
            "    \"import-ssp\": {\"href\": \"#ssp\"},\n" +
            "    \"results\": [\n" +
            "      {\n" +
            "        \"uuid\": \"result-1\",\n" +
            "        \"reviewed-controls\": {\n" +
            "          \"control-selections\": [\n" +
            "            {\"include-controls\": [{\"statement-ids\": [\"ac-1\"]}]}\n" +
            "          ]\n" +
            "        },\n" +
            "        \"observations\": [\n" +
            "          {\n" +
            "            \"uuid\": \"obs-1\",\n" +
            "            \"title\": \"Observation\",\n" +
            "            \"description\": \"Test\",\n" +
            "            \"props\": [\n" +
            "              {\"name\": \"overall-score\", \"value\": \"75.0\"}\n" +
            "            ]\n" +
            "          }\n" +
            "        ],\n" +
            "        \"findings\": [\n" +
            "          {\n" +
            "            \"uuid\": \"finding-1\",\n" +
            "            \"title\": \"Test Finding\",\n" +
            "            \"description\": \"Test finding description\",\n" +
            "            \"target\": {\n" +
            "              \"type\": \"objective-id\",\n" +
            "              \"target-id\": \"ac-1\"\n" +
            "            },\n" +
            "            \"related-observations\": [\n" +
            "              {\"observation-uuid\": \"obs-1\"}\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

        SarVisualizationRequest request = new SarVisualizationRequest();
        request.setContent(sarJson);
        request.setFormat(OscalFormat.JSON);

        SarVisualizationResult result = visualizationService.analyzeSAR(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getFindings());
        assertEquals(1, result.getFindings().size());

        SarVisualizationResult.Finding finding = result.getFindings().get(0);
        assertEquals("finding-1", finding.getUuid());
        assertEquals("Test Finding", finding.getTitle());
        assertTrue(finding.getRelatedControls().contains("ac-1"));
        assertTrue(finding.getRelatedObservations().contains("obs-1"));
        assertEquals(75.0, finding.getScore(), 0.01); // Average from related observation

        // Check assessment summary
        assertEquals(1, result.getAssessmentSummary().getTotalFindings());

        // Check control family assessment
        assertTrue(result.getControlsByFamily().containsKey("ac"));
        SarVisualizationResult.ControlFamilyAssessment acFamily = result.getControlsByFamily().get("ac");
        assertEquals(1, acFamily.getTotalFindings());
    }

    @Test
    void testAnalyzeSAR_withRisks() {
        String sarJson = "{\n" +
            "  \"assessment-results\": {\n" +
            "    \"uuid\": \"sar-uuid\",\n" +
            "    \"metadata\": {\"title\": \"Test SAR\"},\n" +
            "    \"import-ssp\": {\"href\": \"#ssp\"},\n" +
            "    \"results\": [\n" +
            "      {\n" +
            "        \"uuid\": \"result-1\",\n" +
            "        \"risks\": [\n" +
            "          {\n" +
            "            \"uuid\": \"risk-1\",\n" +
            "            \"title\": \"Test Risk\",\n" +
            "            \"description\": \"Risk description\",\n" +
            "            \"status\": \"open\",\n" +
            "            \"characterization\": {\n" +
            "              \"facets\": [\n" +
            "                {\n" +
            "                  \"name\": \"control-id\",\n" +
            "                  \"value\": \"ac-1\"\n" +
            "                }\n" +
            "              ]\n" +
            "            }\n" +
            "          },\n" +
            "          {\n" +
            "            \"uuid\": \"risk-2\",\n" +
            "            \"title\": \"Another Risk\",\n" +
            "            \"description\": \"Risk 2\",\n" +
            "            \"status\": \"closed\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

        SarVisualizationRequest request = new SarVisualizationRequest();
        request.setContent(sarJson);
        request.setFormat(OscalFormat.JSON);

        SarVisualizationResult result = visualizationService.analyzeSAR(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getRisks());
        assertEquals(2, result.getRisks().size());

        SarVisualizationResult.Risk risk1 = result.getRisks().get(0);
        assertEquals("risk-1", risk1.getUuid());
        assertEquals("Test Risk", risk1.getTitle());
        assertEquals("open", risk1.getStatus());
        assertTrue(risk1.getRelatedControls().contains("ac-1"));

        // Check assessment summary
        assertEquals(2, result.getAssessmentSummary().getTotalRisks());
        assertEquals(1, result.getAssessmentSummary().getRisksBySeverity().get("open"));
        assertEquals(1, result.getAssessmentSummary().getRisksBySeverity().get("closed"));
    }

    @Test
    void testAnalyzeSAR_invalidDocument() {
        String invalidJson = "{\n" +
            "  \"invalid-root\": {\n" +
            "    \"some-field\": \"value\"\n" +
            "  }\n" +
            "}";

        SarVisualizationRequest request = new SarVisualizationRequest();
        request.setContent(invalidJson);
        request.setFormat(OscalFormat.JSON);

        SarVisualizationResult result = visualizationService.analyzeSAR(request, "testuser");

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Invalid SAR document"));
    }

    @Test
    void testAnalyzeSAR_parseError() {
        String invalidJson = "{ invalid json";

        SarVisualizationRequest request = new SarVisualizationRequest();
        request.setContent(invalidJson);
        request.setFormat(OscalFormat.JSON);

        SarVisualizationResult result = visualizationService.analyzeSAR(request, "testuser");

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().startsWith("Failed to analyze SAR:"));
    }

    // ========== Edge Cases and Format Tests ==========

    @Test
    void testAnalyzeSSP_yamlFormat() {
        String sspYaml = "---\n" +
            "system-security-plan:\n" +
            "  uuid: yaml-uuid\n" +
            "  metadata:\n" +
            "    title: YAML Test SSP\n" +
            "  system-characteristics:\n" +
            "    system-name: YAML System\n";

        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setContent(sspYaml);
        request.setFormat(OscalFormat.YAML);

        SspVisualizationResult result = visualizationService.analyzeSSP(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("yaml-uuid", result.getSystemInfo().getUuid());
        assertEquals("YAML System", result.getSystemInfo().getName());
    }

    @Test
    void testAnalyzeProfile_xmlFormat() {
        String profileXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<profile xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\">\n" +
            "  <uuid>xml-profile-uuid</uuid>\n" +
            "  <metadata>\n" +
            "    <title>XML Test Profile</title>\n" +
            "  </metadata>\n" +
            "</profile>";

        ProfileVisualizationRequest request = new ProfileVisualizationRequest();
        request.setContent(profileXml);
        request.setFormat(OscalFormat.XML);

        ProfileVisualizationResult result = visualizationService.analyzeProfile(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
    }

    @Test
    void testAnalyzeSSP_withInformationTypes() {
        String sspJson = "{\n" +
            "  \"system-security-plan\": {\n" +
            "    \"uuid\": \"test-uuid\",\n" +
            "    \"metadata\": {\"title\": \"Test\"},\n" +
            "    \"system-characteristics\": {\n" +
            "      \"system-name\": \"Test System\",\n" +
            "      \"system-information\": {\n" +
            "        \"information-types\": [\n" +
            "          {\n" +
            "            \"uuid\": \"info-type-1\",\n" +
            "            \"title\": \"Financial Data\",\n" +
            "            \"description\": \"Financial information\",\n" +
            "            \"confidentiality-impact\": {\n" +
            "              \"base\": \"moderate\",\n" +
            "              \"selected\": \"high\"\n" +
            "            },\n" +
            "            \"integrity-impact\": {\n" +
            "              \"base\": \"low\"\n" +
            "            },\n" +
            "            \"availability-impact\": {\n" +
            "              \"base\": \"low\"\n" +
            "            }\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

        SspVisualizationRequest request = new SspVisualizationRequest();
        request.setContent(sspJson);
        request.setFormat(OscalFormat.JSON);

        SspVisualizationResult result = visualizationService.analyzeSSP(request, "testuser");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getInformationTypes());
        assertEquals(1, result.getInformationTypes().size());

        SspVisualizationResult.InformationType infoType = result.getInformationTypes().get(0);
        assertEquals("info-type-1", infoType.getUuid());
        assertEquals("Financial Data", infoType.getTitle());
        assertNotNull(infoType.getConfidentiality());
        assertEquals("moderate", infoType.getConfidentiality().getBase());
        assertEquals("high", infoType.getConfidentiality().getSelected());
        assertEquals("low", infoType.getIntegrity().getBase());
    }
}
