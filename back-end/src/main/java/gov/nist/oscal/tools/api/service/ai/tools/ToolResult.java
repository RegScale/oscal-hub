package gov.nist.oscal.tools.api.service.ai.tools;

public record ToolResult(boolean ok, String summary, String contentJson) {
    public static ToolResult ok(String summary) {
        return new ToolResult(true, summary, "{}");
    }
    public static ToolResult ok(String summary, String contentJson) {
        return new ToolResult(true, summary, contentJson);
    }
    public static ToolResult error(String summary) {
        return new ToolResult(false, summary, "{}");
    }
}
