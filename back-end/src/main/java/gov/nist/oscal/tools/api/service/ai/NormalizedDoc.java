package gov.nist.oscal.tools.api.service.ai;

public record NormalizedDoc(
        String plainText,
        String xhtml,
        String detectedMime,
        String filename,
        int charCount
) { }
