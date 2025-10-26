package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentDefinitionRequestTest {

    @Test
    void testNoArgsConstructor() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();

        assertNotNull(request);
        assertNull(request.getTitle());
        assertNull(request.getDescription());
        assertNull(request.getVersion());
        assertNull(request.getOscalVersion());
        assertNull(request.getFilename());
        assertNull(request.getJsonContent());
        assertNull(request.getOscalUuid());
        assertNull(request.getComponentCount());
        assertNull(request.getCapabilityCount());
        assertNull(request.getControlCount());
    }

    @Test
    void testAllArgsConstructor() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest(
                "DoD Security Components",
                "Component definitions for DoD security requirements",
                "1.0.0",
                "1.1.2",
                "dod-components.json",
                "{\"component-definition\": {}}"
        );

        assertEquals("DoD Security Components", request.getTitle());
        assertEquals("Component definitions for DoD security requirements", request.getDescription());
        assertEquals("1.0.0", request.getVersion());
        assertEquals("1.1.2", request.getOscalVersion());
        assertEquals("dod-components.json", request.getFilename());
        assertTrue(request.getJsonContent().contains("component-definition"));
    }

    @Test
    void testSetTitle() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setTitle("NIST Component Library");
        assertEquals("NIST Component Library", request.getTitle());
    }

    @Test
    void testSetDescription() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setDescription("Comprehensive library of security components");
        assertEquals("Comprehensive library of security components", request.getDescription());
    }

    @Test
    void testSetVersion() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setVersion("2.1.0");
        assertEquals("2.1.0", request.getVersion());
    }

    @Test
    void testSetOscalVersion() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setOscalVersion("1.0.4");
        assertEquals("1.0.4", request.getOscalVersion());
    }

    @Test
    void testSetFilename() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setFilename("component-def-v2.json");
        assertEquals("component-def-v2.json", request.getFilename());
    }

    @Test
    void testSetJsonContent() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        String jsonContent = "{\"component-definition\": {\"uuid\": \"abc-123\"}}";
        request.setJsonContent(jsonContent);
        assertEquals(jsonContent, request.getJsonContent());
    }

    @Test
    void testSetOscalUuid() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setOscalUuid("550e8400-e29b-41d4-a716-446655440000");
        assertEquals("550e8400-e29b-41d4-a716-446655440000", request.getOscalUuid());
    }

    @Test
    void testSetComponentCount() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setComponentCount(15);
        assertEquals(15, request.getComponentCount());
    }

    @Test
    void testSetCapabilityCount() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setCapabilityCount(42);
        assertEquals(42, request.getCapabilityCount());
    }

    @Test
    void testSetControlCount() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setControlCount(325);
        assertEquals(325, request.getControlCount());
    }

    @Test
    void testSetAllFields() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setTitle("Complete Component Definition");
        request.setDescription("A comprehensive component definition");
        request.setVersion("1.5.2");
        request.setOscalVersion("1.0.4");
        request.setFilename("complete-comp-def.json");
        request.setJsonContent("{\"component-definition\": {\"metadata\": {}}}");
        request.setOscalUuid("123e4567-e89b-12d3-a456-426614174000");
        request.setComponentCount(20);
        request.setCapabilityCount(50);
        request.setControlCount(400);

        assertEquals("Complete Component Definition", request.getTitle());
        assertEquals("A comprehensive component definition", request.getDescription());
        assertEquals("1.5.2", request.getVersion());
        assertEquals("1.0.4", request.getOscalVersion());
        assertEquals("complete-comp-def.json", request.getFilename());
        assertTrue(request.getJsonContent().contains("metadata"));
        assertNotNull(request.getOscalUuid());
        assertEquals(20, request.getComponentCount());
        assertEquals(50, request.getCapabilityCount());
        assertEquals(400, request.getControlCount());
    }

    @Test
    void testSetFieldsToNull() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest(
                "Title",
                "Description",
                "1.0",
                "1.0.0",
                "file.json",
                "{}"
        );

        request.setTitle(null);
        request.setDescription(null);
        request.setVersion(null);
        request.setOscalVersion(null);
        request.setFilename(null);
        request.setJsonContent(null);
        request.setOscalUuid(null);
        request.setComponentCount(null);
        request.setCapabilityCount(null);
        request.setControlCount(null);

        assertNull(request.getTitle());
        assertNull(request.getDescription());
        assertNull(request.getVersion());
        assertNull(request.getOscalVersion());
        assertNull(request.getFilename());
        assertNull(request.getJsonContent());
        assertNull(request.getOscalUuid());
        assertNull(request.getComponentCount());
        assertNull(request.getCapabilityCount());
        assertNull(request.getControlCount());
    }

    @Test
    void testModifyAllFields() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();

        request.setTitle("Old Title");
        request.setVersion("1.0.0");
        request.setComponentCount(10);

        request.setTitle("New Title");
        request.setVersion("2.0.0");
        request.setComponentCount(20);

        assertEquals("New Title", request.getTitle());
        assertEquals("2.0.0", request.getVersion());
        assertEquals(20, request.getComponentCount());
    }

    @Test
    void testWithLongJsonContent() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        StringBuilder longJson = new StringBuilder();
        longJson.append("{\"component-definition\": {\"components\": [");
        for (int i = 0; i < 100; i++) {
            if (i > 0) longJson.append(",");
            longJson.append("{\"uuid\": \"comp-").append(i).append("\"}");
        }
        longJson.append("]}}");

        request.setJsonContent(longJson.toString());
        assertTrue(request.getJsonContent().length() > 1000);
    }

    @Test
    void testWithSpecialCharactersInTitle() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        String titleWithSpecialChars = "Component Def v2.0 (Updated) [2025-Q1] - Final";
        request.setTitle(titleWithSpecialChars);
        assertEquals(titleWithSpecialChars, request.getTitle());
        assertTrue(request.getTitle().contains("(Updated)"));
    }

    @Test
    void testWithEmptyStrings() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest(
                "",
                "",
                "",
                "",
                "",
                ""
        );

        assertEquals("", request.getTitle());
        assertEquals("", request.getDescription());
        assertEquals("", request.getVersion());
        assertEquals("", request.getOscalVersion());
        assertEquals("", request.getFilename());
        assertEquals("", request.getJsonContent());
    }

    @Test
    void testZeroCounts() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setComponentCount(0);
        request.setCapabilityCount(0);
        request.setControlCount(0);

        assertEquals(0, request.getComponentCount());
        assertEquals(0, request.getCapabilityCount());
        assertEquals(0, request.getControlCount());
    }

    @Test
    void testLargeCounts() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setComponentCount(9999);
        request.setCapabilityCount(8888);
        request.setControlCount(7777);

        assertEquals(9999, request.getComponentCount());
        assertEquals(8888, request.getCapabilityCount());
        assertEquals(7777, request.getControlCount());
    }

    @Test
    void testCompleteComponentDefinitionScenario() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest(
                "Production Component Library v2.0",
                "Complete set of validated security components for production systems",
                "2.0.0",
                "1.0.4",
                "prod-components-v2.json",
                "{\"component-definition\": {\"uuid\": \"prod-123\", \"metadata\": {\"title\": \"Production Components\"}}}"
        );

        request.setOscalUuid("prod-123");
        request.setComponentCount(25);
        request.setCapabilityCount(75);
        request.setControlCount(450);

        assertNotNull(request);
        assertTrue(request.getTitle().contains("v2.0"));
        assertTrue(request.getDescription().contains("production"));
        assertEquals("2.0.0", request.getVersion());
        assertEquals("1.0.4", request.getOscalVersion());
        assertTrue(request.getFilename().endsWith(".json"));
        assertTrue(request.getJsonContent().contains("prod-123"));
        assertEquals("prod-123", request.getOscalUuid());
        assertEquals(25, request.getComponentCount());
        assertEquals(75, request.getCapabilityCount());
        assertEquals(450, request.getControlCount());
    }

    @Test
    void testPartialRequest() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        request.setTitle("Minimal Component Definition");
        request.setOscalVersion("1.0.0");
        request.setFilename("minimal.json");
        request.setJsonContent("{}");

        assertEquals("Minimal Component Definition", request.getTitle());
        assertNull(request.getDescription());
        assertNull(request.getVersion());
        assertEquals("1.0.0", request.getOscalVersion());
        assertNull(request.getComponentCount());
    }

    @Test
    void testWithMultilineDescription() {
        ComponentDefinitionRequest request = new ComponentDefinitionRequest();
        String multilineDesc = "Component definition containing:\n- Hardware components\n- Software components\n- Service components";
        request.setDescription(multilineDesc);
        assertTrue(request.getDescription().contains("\n"));
        assertTrue(request.getDescription().contains("Hardware"));
    }
}
