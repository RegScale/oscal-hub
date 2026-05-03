package gov.nist.oscal.tools.api.service.ai.wizard;

final class JsonStrings {
    private JsonStrings() { }
    static String quote(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
