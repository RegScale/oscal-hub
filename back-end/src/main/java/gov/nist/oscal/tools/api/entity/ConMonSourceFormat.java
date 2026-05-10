package gov.nist.oscal.tools.api.entity;

public enum ConMonSourceFormat {
    OSCAL_JSON,
    OSCAL_XML,
    OSCAL_YAML,
    FEDRAMP_XLSX;

    public static ConMonSourceFormat fromFilename(String filename) {
        if (filename == null) return null;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".json")) return OSCAL_JSON;
        if (lower.endsWith(".xml")) return OSCAL_XML;
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return OSCAL_YAML;
        if (lower.endsWith(".xlsx")) return FEDRAMP_XLSX;
        return null;
    }
}
