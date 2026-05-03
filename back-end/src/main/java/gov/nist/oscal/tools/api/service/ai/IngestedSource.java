package gov.nist.oscal.tools.api.service.ai;

public record IngestedSource(
        Kind kind,
        String filename,
        String text,
        byte[] pdfBytes,
        long sizeBytes
) {
    public enum Kind { TEXT, PDF }
}
