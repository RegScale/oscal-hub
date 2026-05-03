package gov.nist.oscal.tools.api.service.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class LookupControlTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() { return "lookup_control"; }

    @Override
    public String description() {
        return "Look up a control by ID (e.g., 'ac-1') in a referenced catalog. "
                + "Returns the control's statement, parts, and parameters.";
    }

    @Override
    public String inputSchemaJson() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"controlId\":{\"type\":\"string\"},"
                + "\"catalogRef\":{\"type\":\"string\",\"description\":\"e.g., 'NIST_SP-800-53_rev5'\"}},"
                + "\"required\":[\"controlId\",\"catalogRef\"]}";
    }

    @Override
    public ToolResult invoke(ToolCall call) {
        try {
            JsonNode args = MAPPER.readTree(call.argsJson());
            String controlId = args.get("controlId").asText();
            String catalogRef = args.get("catalogRef").asText();
            // TODO(post-foundation): bridge to the catalog cache / fetch_catalog. For foundation,
            // return a not-found marker so wizards can still call this without crashing.
            return ToolResult.error("lookup_control: catalog '" + catalogRef
                    + "' not loaded; control " + controlId + " unavailable");
        } catch (Exception e) {
            return ToolResult.error("lookup_control error: " + e.getMessage());
        }
    }
}
