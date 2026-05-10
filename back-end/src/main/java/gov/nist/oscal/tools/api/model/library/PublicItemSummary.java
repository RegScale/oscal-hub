package gov.nist.oscal.tools.api.model.library;

import gov.nist.oscal.tools.api.entity.LibraryItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record PublicItemSummary(
        String itemId,
        String title,
        String description,
        String oscalType,
        List<String> tags,
        Integer currentVersionNumber,
        LocalDateTime publishedAt,
        LocalDateTime lastPublishedAt,
        Long downloadCount,
        Double averageRating,
        Long totalRatings) {

    public static PublicItemSummary fromEntity(LibraryItem item, Double averageRating, Long totalRatings) {
        return new PublicItemSummary(
            item.getItemId(),
            item.getTitle(),
            item.getDescription(),
            item.getOscalType(),
            item.getTags() == null ? List.of()
                : item.getTags().stream().map(t -> t.getName()).collect(Collectors.toList()),
            item.getCurrentVersion() != null ? item.getCurrentVersion().getVersionNumber() : null,
            item.getPublishedAt(),
            item.getLastPublishedAt(),
            item.getDownloadCount(),
            averageRating,
            totalRatings);
    }
}
