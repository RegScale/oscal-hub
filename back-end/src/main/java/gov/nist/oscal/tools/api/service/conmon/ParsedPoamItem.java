package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;

import java.time.LocalDate;
import java.util.Map;

/**
 * Parser output for a single POAM item — independent of JPA entities so
 * parsers and the persistence layer can be tested separately.
 */
public record ParsedPoamItem(
        String externalId,
        String title,
        String description,
        ConMonItemStatus status,
        String rawStatus,
        String severity,                 // LOW/MODERATE/HIGH/CRITICAL or null
        String weaknessSource,
        LocalDate scheduledCompletionDate,
        LocalDate actualCompletionDate,
        String pointOfContact,
        String riskRating,
        Map<String, Object> extraProps   // Unmodeled fields, serialized to extra_props_json
) {}
