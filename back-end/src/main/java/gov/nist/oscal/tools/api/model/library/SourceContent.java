package gov.nist.oscal.tools.api.model.library;

/**
 * Bytes + metadata loaded from a builder row, ready to write into the library blob.
 * @param bytes serialized content (typically JSON)
 * @param format "json" / "xml" / "yaml"
 * @param filename suggested filename, e.g. "my-catalog-v3.json"
 * @param oscalType "catalog" / "profile" / etc.
 * @param sourceId the builder row's UUID — becomes library_items.source_id
 * @param defaultTitle title to suggest when the user didn't override
 */
public record SourceContent(
        byte[] bytes,
        String format,
        String filename,
        String oscalType,
        java.util.UUID sourceId,
        String defaultTitle
) {}
