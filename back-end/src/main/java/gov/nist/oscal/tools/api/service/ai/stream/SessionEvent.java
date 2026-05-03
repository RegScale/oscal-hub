package gov.nist.oscal.tools.api.service.ai.stream;

public record SessionEvent(Type type, String dataJson) {
    public enum Type {
        SESSION_STARTED,
        PROGRESS,
        TOOL_CALL,
        TOOL_RESULT,
        AWAITING_INPUT,
        CHUNK,
        PARTIAL_DOCUMENT,
        COMPLETE,
        ERROR
    }

    public static SessionEvent progress(String message) {
        return new SessionEvent(Type.PROGRESS, "{\"message\":\"" + escape(message) + "\"}");
    }
    public static SessionEvent complete(String documentJson) {
        return new SessionEvent(Type.COMPLETE, "{\"document\":" + documentJson + "}");
    }
    public static SessionEvent error(String code, String message) {
        return new SessionEvent(Type.ERROR,
                "{\"code\":\"" + escape(code) + "\",\"message\":\"" + escape(message) + "\"}");
    }
    public static SessionEvent chunk(String text) {
        return new SessionEvent(Type.CHUNK, "{\"text\":\"" + escape(text) + "\"}");
    }
    public static SessionEvent toolCall(String tool, String argsSummary) {
        return new SessionEvent(Type.TOOL_CALL,
                "{\"tool\":\"" + escape(tool) + "\",\"args\":\"" + escape(argsSummary) + "\"}");
    }
    public static SessionEvent toolResult(String tool, boolean ok, String summary) {
        return new SessionEvent(Type.TOOL_RESULT,
                "{\"tool\":\"" + escape(tool) + "\",\"ok\":" + ok + ",\"summary\":\"" + escape(summary) + "\"}");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
