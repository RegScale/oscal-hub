package gov.nist.oscal.tools.api.service.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.OscalModelType;
import gov.nist.oscal.tools.api.model.ValidationError;
import gov.nist.oscal.tools.api.model.ValidationRequest;
import gov.nist.oscal.tools.api.model.ValidationResult;
import gov.nist.oscal.tools.api.service.ValidationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ValidateOscalTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ValidationService validationService;

    public ValidateOscalTool(ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public String name() { return "validate_oscal"; }

    @Override
    public String description() {
        return "Validate OSCAL content against the schema and constraints. "
                + "Returns valid=true or a list of error messages.";
    }

    @Override
    public String inputSchemaJson() {
        return "{\"type\":\"object\","
                + "\"properties\":{"
                + "\"content\":{\"type\":\"string\"},"
                + "\"format\":{\"type\":\"string\",\"enum\":[\"JSON\",\"XML\",\"YAML\"]},"
                + "\"modelType\":{\"type\":\"string\"}"
                + "},\"required\":[\"content\",\"format\",\"modelType\"]}";
    }

    @Override
    public ToolResult invoke(ToolCall call) {
        try {
            JsonNode args = MAPPER.readTree(call.argsJson());
            ValidationRequest req = new ValidationRequest();
            req.setContent(args.get("content").asText());
            req.setFormat(OscalFormat.fromString(args.get("format").asText()));
            req.setModelType(OscalModelType.fromString(args.get("modelType").asText()));

            ValidationResult result = validationService.validate(req, "ai");
            if (result.isValid()) {
                return ToolResult.ok("valid");
            }
            List<ValidationError> errors = result.getErrors();
            String errorMsg = (errors == null || errors.isEmpty())
                    ? "invalid"
                    : errors.stream().map(ValidationError::getMessage).collect(Collectors.joining("; "));
            return ToolResult.error("invalid: " + errorMsg);
        } catch (Exception e) {
            return ToolResult.error("validate_oscal error: " + e.getMessage());
        }
    }
}
