package gov.nist.oscal.tools.api.service.ai.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the foundation-release AI tools that intentionally return
 * "not loaded" / "no document context" markers so wizards can register
 * them safely. Verifies that contracts (name, schema, error handling)
 * are stable, since wizard prompts depend on tool name + argument shape.
 */
class AiPlaceholderToolsTest {

    // ---------- LookupControlTool ----------

    @Test
    void lookupControl_namedAndSchemaContainsRequiredFields() {
        LookupControlTool t = new LookupControlTool();
        assertThat(t.name()).isEqualTo("lookup_control");
        assertThat(t.inputSchemaJson())
                .contains("controlId")
                .contains("catalogRef")
                .contains("\"required\":[\"controlId\",\"catalogRef\"]");
    }

    @Test
    void lookupControl_returnsNotLoadedMarkerWithBothInputs() {
        LookupControlTool t = new LookupControlTool();
        ToolResult r = t.invoke(new ToolCall("lookup_control",
                "{\"controlId\":\"ac-1\",\"catalogRef\":\"NIST_SP-800-53_rev5\"}"));
        assertThat(r.ok()).isFalse();
        assertThat(r.summary()).contains("ac-1").contains("NIST_SP-800-53_rev5");
    }

    @Test
    void lookupControl_invalidJson_returnsErrorNotCrash() {
        LookupControlTool t = new LookupControlTool();
        ToolResult r = t.invoke(new ToolCall("lookup_control", "{not json"));
        assertThat(r.ok()).isFalse();
        assertThat(r.summary()).startsWith("lookup_control error:");
    }

    @Test
    void lookupControl_missingRequiredField_returnsErrorNotCrash() {
        LookupControlTool t = new LookupControlTool();
        ToolResult r = t.invoke(new ToolCall("lookup_control",
                "{\"controlId\":\"ac-1\"}"));
        assertThat(r.ok()).isFalse();
    }

    // ---------- FetchCatalogTool ----------

    @Test
    void fetchCatalog_namedAndSchemaRequiresRef() {
        FetchCatalogTool t = new FetchCatalogTool();
        assertThat(t.name()).isEqualTo("fetch_catalog");
        assertThat(t.inputSchemaJson()).contains("\"required\":[\"ref\"]");
    }

    @Test
    void fetchCatalog_returnsNotLoadedMarkerWithRefEchoed() {
        FetchCatalogTool t = new FetchCatalogTool();
        ToolResult r = t.invoke(new ToolCall("fetch_catalog",
                "{\"ref\":\"NIST_SP-800-53_rev5\"}"));
        assertThat(r.ok()).isFalse();
        assertThat(r.summary()).contains("NIST_SP-800-53_rev5");
    }

    @Test
    void fetchCatalog_invalidJson_returnsErrorNotCrash() {
        FetchCatalogTool t = new FetchCatalogTool();
        ToolResult r = t.invoke(new ToolCall("fetch_catalog", "garbage"));
        assertThat(r.ok()).isFalse();
        assertThat(r.summary()).startsWith("fetch_catalog error:");
    }

    // ---------- ReadCurrentDocSectionTool ----------

    @Test
    void readCurrentDocSection_namedAndSchemaRequiresJsonPath() {
        ReadCurrentDocSectionTool t = new ReadCurrentDocSectionTool();
        assertThat(t.name()).isEqualTo("read_current_document_section");
        assertThat(t.inputSchemaJson()).contains("\"required\":[\"jsonPath\"]");
    }

    @Test
    void readCurrentDocSection_returnsOkWithNullValue_whenNoBuilderContext() {
        ReadCurrentDocSectionTool t = new ReadCurrentDocSectionTool();
        ToolResult r = t.invoke(new ToolCall("read_current_document_section",
                "{\"jsonPath\":\"$.metadata.title\"}"));
        // Foundation-release contract: ok() so wizards can register the tool
        // without a wired builder source; returns explicit null in contentJson.
        assertThat(r.ok()).isTrue();
        assertThat(r.contentJson()).contains("\"value\":null");
        assertThat(r.contentJson()).contains("$.metadata.title");
    }

    @Test
    void readCurrentDocSection_invalidJson_returnsErrorNotCrash() {
        ReadCurrentDocSectionTool t = new ReadCurrentDocSectionTool();
        ToolResult r = t.invoke(new ToolCall("read_current_document_section",
                "{\"jsonPath\":}"));
        assertThat(r.ok()).isFalse();
        assertThat(r.summary()).startsWith("read_current_document_section error:");
    }
}
