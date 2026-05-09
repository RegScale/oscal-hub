package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.TicketPriority;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import gov.nist.oscal.tools.api.entity.TicketType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record TicketAnalyticsResponse(
    Map<TicketStatus, Long> statusCounts,
    Map<TicketType, Long> typeSplit,
    List<WeekBucket> openedPerWeek,
    List<WeekBucket> resolvedPerWeek,
    List<StaleTicket> staleTickets) {

    public record WeekBucket(LocalDate week, long count) {}
    public record StaleTicket(Long id, TicketType type, String title,
                              TicketPriority priority, java.time.LocalDateTime createdAt,
                              long ageDays) {}
}
