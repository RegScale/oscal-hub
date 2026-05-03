package gov.nist.oscal.tools.api.service.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.ConversionRequest;
import gov.nist.oscal.tools.api.model.ConversionResult;
import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.OscalModelType;
import gov.nist.oscal.tools.api.service.ConversionService;
import org.springframework.stereotype.Component;

@Component
public class ConvertFormatTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ConversionService conversionService;

    public ConvertFormatTool(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public String name() { return "convert_format"; }

    @Override
    public String description() { return "Convert OSCAL content between JSON, XML, and YAML."; }

    @Override
    public String inputSchemaJson() {
        return "{\"type\":\"object\","
                + "\"properties\":{"
                + "\"content\":{\"type\":\"string\"},"
                + "\"from\":{\"type\":\"string\",\"enum\":[\"JSON\",\"XML\",\"YAML\"]},"
                + "\"to\":{\"type\":\"string\",\"enum\":[\"JSON\",\"XML\",\"YAML\"]},"
                + "\"modelType\":{\"type\":\"string\"}"
                + "},\"required\":[\"content\",\"from\",\"to\",\"modelType\"]}";
    }

    @Override
    public ToolResult invoke(ToolCall call) {
        try {
            JsonNode args = MAPPER.readTree(call.argsJson());
            ConversionRequest req = new ConversionRequest();
            req.setContent(args.get("content").asText());
            req.setFromFormat(OscalFormat.fromString(args.get("from").asText()));
            req.setToFormat(OscalFormat.fromString(args.get("to").asText()));
            req.setModelType(OscalModelType.fromString(args.get("modelType").asText()));

            ConversionResult result = conversionService.convert(req, "ai");
            if (!result.isSuccess()) {
                return ToolResult.error("convert_format failed: " + result.getError());
            }
            return ToolResult.ok("converted",
                    "{\"content\":" + MAPPER.writeValueAsString(result.getContent()) + "}");
        } catch (Exception e) {
            return ToolResult.error("convert_format error: " + e.getMessage());
        }
    }
}
