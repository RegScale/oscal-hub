package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;

import java.util.List;
import java.util.Set;

/**
 * Hybrid status derivation per spec:
 *   1. status keyword on poam-item → mapped (FedRAMP-style values)
 *   2. linked findings/risks → roll up
 *   3. otherwise → UNKNOWN
 */
public final class PoamStatusDeriver {

    private static final Set<String> CLOSED_KEYWORDS = Set.of(
            "completed", "closed", "false-positive", "false positive", "not-applicable", "not applicable");
    private static final Set<String> OPEN_KEYWORDS = Set.of(
            "ongoing", "open", "risk-accepted", "risk accepted",
            "operational-requirement", "operational requirement", "pending");

    public record DerivedStatus(ConMonItemStatus status, String rawStatus) {}

    private PoamStatusDeriver() {}

    public static DerivedStatus derive(String statusKeyword, List<String> findingStatuses) {
        if (statusKeyword != null && !statusKeyword.isBlank()) {
            String normalized = statusKeyword.trim().toLowerCase();
            if (CLOSED_KEYWORDS.contains(normalized)) return new DerivedStatus(ConMonItemStatus.CLOSED, statusKeyword);
            if (OPEN_KEYWORDS.contains(normalized)) return new DerivedStatus(ConMonItemStatus.OPEN, statusKeyword);
            return new DerivedStatus(ConMonItemStatus.UNKNOWN, statusKeyword);
        }
        if (findingStatuses != null && !findingStatuses.isEmpty()) {
            boolean anyOpen = findingStatuses.stream().anyMatch(s -> {
                if (s == null) return false;
                String n = s.trim().toLowerCase();
                return OPEN_KEYWORDS.contains(n);
            });
            boolean anyKnown = findingStatuses.stream().anyMatch(s -> {
                if (s == null) return false;
                String n = s.trim().toLowerCase();
                return OPEN_KEYWORDS.contains(n) || CLOSED_KEYWORDS.contains(n);
            });
            if (anyOpen) return new DerivedStatus(ConMonItemStatus.OPEN, null);
            if (anyKnown) return new DerivedStatus(ConMonItemStatus.CLOSED, null);
        }
        return new DerivedStatus(ConMonItemStatus.UNKNOWN, null);
    }
}
