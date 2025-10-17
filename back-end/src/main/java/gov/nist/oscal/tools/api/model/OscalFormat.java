package gov.nist.oscal.tools.api.model;

public enum OscalFormat {
    XML,
    JSON,
    YAML;

    public static OscalFormat fromString(String format) {
        if (format == null) {
            return null;
        }
        return valueOf(format.toUpperCase());
    }
}
