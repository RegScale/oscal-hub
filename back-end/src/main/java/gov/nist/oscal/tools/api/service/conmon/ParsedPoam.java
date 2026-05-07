package gov.nist.oscal.tools.api.service.conmon;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Parser output for an entire POAM document.
 */
public record ParsedPoam(
        String oscalUuid,           // null for FedRAMP_XLSX
        String oscalVersion,        // null for FedRAMP_XLSX
        String metadataTitle,       // optional
        LocalDateTime metadataLastModified, // optional
        List<ParsedPoamItem> items
) {}
