package gov.nist.oscal.tools.api.service.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ReadCurrentDocSectionTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() { return "read_current_document_section"; }

    @Override
    public String description() {
        return "Builder Author Assist only: read a JSONPath section of the document the user is editing.";
    }

    @Override
    public String inputSchemaJson() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"jsonPath\":{\"type\":\"string\"}},"
                + "\"required\":[\"jsonPath\"]}";
    }

    @Override
    public ToolResult invoke(ToolCall call) {
        try {
            JsonNode args = MAPPER.readTree(call.argsJson());
            String path = args.get("jsonPath").asText();
            // The Author Assist plan wires this to the current builder doc via a request-scoped bean.
            // For foundation, return empty so wizards can register the tool without a wired source.
            return ToolResult.ok("no document context attached",
                    "{\"path\":\"" + path + "\",\"value\":null}");
        } catch (Exception e) {
            return ToolResult.error("read_current_document_section error: " + e.getMessage());
        }
    }
}
