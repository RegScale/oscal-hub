package gov.nist.oscal.tools.api.service.ai.tools;

public interface Tool {
    String name();
    String description();
    String inputSchemaJson();
    ToolResult invoke(ToolCall call);
}
