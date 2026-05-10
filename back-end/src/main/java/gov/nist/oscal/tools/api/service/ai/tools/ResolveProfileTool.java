package gov.nist.oscal.tools.api.service.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.ProfileResolutionRequest;
import gov.nist.oscal.tools.api.model.ProfileResolutionResult;
import gov.nist.oscal.tools.api.service.ProfileResolutionService;
import org.springframework.stereotype.Component;

@Component
public class ResolveProfileTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ProfileResolutionService resolver;

    public ResolveProfileTool(ProfileResolutionService resolver) {
        this.resolver = resolver;
    }

    @Override
    public String name() { return "resolve_profile"; }

    @Override
    public String description() {
        return "Resolve an OSCAL Profile against its imported catalogs and return the resolved catalog.";
    }

    @Override
    public String inputSchemaJson() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"profileContent\":{\"type\":\"string\"},"
                + "\"format\":{\"type\":\"string\",\"enum\":[\"JSON\",\"XML\",\"YAML\"]}},"
                + "\"required\":[\"profileContent\",\"format\"]}";
    }

    @Override
    public ToolResult invoke(ToolCall call) {
        try {
            JsonNode args = MAPPER.readTree(call.argsJson());
            ProfileResolutionRequest req = new ProfileResolutionRequest();
            req.setProfileContent(args.get("profileContent").asText());
            req.setFormat(OscalFormat.fromString(args.get("format").asText()));

            ProfileResolutionResult result = resolver.resolveProfile(req, "ai");
            if (result.isSuccess()) {
                return ToolResult.ok("resolved",
                        "{\"resolvedCatalog\":" + MAPPER.writeValueAsString(result.getResolvedCatalog()) + "}");
            }
            // ProfileResolutionResult uses getError() for error messages
            return ToolResult.error("resolve_profile failed: " + result.getError());
        } catch (Exception e) {
            return ToolResult.error("resolve_profile error: " + e.getMessage());
        }
    }
}
