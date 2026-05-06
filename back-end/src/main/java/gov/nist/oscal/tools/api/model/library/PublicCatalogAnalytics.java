package gov.nist.oscal.tools.api.model.library;

import java.time.LocalDate;
import java.util.List;

/**
 * Wire shape for GET /api/public/catalog/analytics — feeds the charts on
 * the public catalog's Analytics tab.
 */
public record PublicCatalogAnalytics(
        Totals totals,
        List<TypeStat> byType,
        List<TimeBucket> uploadsOverTime,
        List<TimeBucket> downloadsOverTime) {

    public record Totals(
            long totalItems,
            long totalDownloads,
            long contributorCount,
            long organizationCount) {}

    /**
     * One row per OSCAL type. avgRating is 0 when no ratings exist.
     */
    public record TypeStat(
            String oscalType,
            long itemCount,
            double avgDownloads,
            double avgRating) {}

    /**
     * weekStart is the Monday of the ISO week (date_trunc('week', ...)).
     */
    public record TimeBucket(
            LocalDate weekStart,
            long count) {}
}
