package gov.nist.oscal.tools.api.service.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class FetchCatalogTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() { return "fetch_catalog"; }

    @Override
    public String description() {
        return "Fetch a referenced OSCAL catalog (e.g., NIST_SP-800-53_rev5) for grounded reasoning. "
                + "Foundation release: returns a not-loaded marker; populated by per-wizard plans.";
    }

    @Override
    public String inputSchemaJson() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"ref\":{\"type\":\"string\"}},"
                + "\"required\":[\"ref\"]}";
    }

    @Override
    public ToolResult invoke(ToolCall call) {
        try {
            JsonNode args = MAPPER.readTree(call.argsJson());
            String ref = args.get("ref").asText();
            return ToolResult.error("fetch_catalog: '" + ref + "' not loaded in foundation release");
        } catch (Exception e) {
            return ToolResult.error("fetch_catalog error: " + e.getMessage());
        }
    }
}
