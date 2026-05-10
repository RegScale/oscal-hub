package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse;
import gov.nist.oscal.tools.api.repository.ConMonSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConMonAnalyticsServiceTest {

    private final ConMonSnapshotRepository repo = mock(ConMonSnapshotRepository.class);
    private final ConMonAnalyticsService service = new ConMonAnalyticsService(repo);
    private final Authorization auth = new Authorization();

    @Test
    void emptyHistory_yieldsEmptyAnalyticsButValidShape() {
        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of());

        ConMonAnalyticsResponse r = service.forAuthorization(auth);

        assertThat(r.getOpenCountSeries()).isEmpty();
        assertThat(r.getCurrentSeverityBreakdown()).isEmpty();
        assertThat(r.getCurrentStatusBreakdown()).isEmpty();
        assertThat(r.getAgingBuckets()).isEmpty();
        assertThat(r.getMeanTimeToCloseDays()).isNull();
        assertThat(r.getSlaStats()).isNotNull();
        assertThat(r.getSlaStats().openTotal()).isZero();
        assertThat(r.getSlaStats().slaPercent()).isNull();
    }

    @Test
    void timeSeries_isOrderedOldestToNewest_evenThoughRepoReturnsNewestFirst() {
        // Repo gives newest first; analytics should reverse for chart-friendly order.
        ConMonSnapshot oldest = snapshot(1, LocalDateTime.of(2026, 1, 1, 0, 0), 5, 2, 1);
        ConMonSnapshot middle = snapshot(2, LocalDateTime.of(2026, 2, 1, 0, 0), 4, 4, 1);
        ConMonSnapshot newest = snapshot(3, LocalDateTime.of(2026, 3, 1, 0, 0), 3, 6, 1);
        when(repo.findByAuthorizationOrderByUploadedAtDesc(any()))
                .thenReturn(List.of(newest, middle, oldest));

        ConMonAnalyticsResponse r = service.forAuthorization(auth);

        assertThat(r.getOpenCountSeries()).hasSize(3);
        assertThat(r.getOpenCountSeries().get(0).date()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(r.getOpenCountSeries().get(0).open()).isEqualTo(5);
        assertThat(r.getOpenCountSeries().get(2).date()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(r.getOpenCountSeries().get(2).open()).isEqualTo(3);
    }

    @Test
    void severityBreakdown_includesOnlyOpenItems_emitsSegmentsInPriorityOrder() {
        ConMonSnapshot latest = snapshot(1, LocalDateTime.now(), 0, 0, 0);
        // OPEN items across severities
        addItem(latest, "P-1", ConMonItemStatus.OPEN, "CRITICAL", null);
        addItem(latest, "P-2", ConMonItemStatus.OPEN, "CRITICAL", null);
        addItem(latest, "P-3", ConMonItemStatus.OPEN, "HIGH", null);
        addItem(latest, "P-4", ConMonItemStatus.OPEN, "MODERATE", null);
        addItem(latest, "P-5", ConMonItemStatus.OPEN, "LOW", null);
        addItem(latest, "P-6", ConMonItemStatus.OPEN, null, null);
        // CLOSED item should be excluded even though severity is set
        addItem(latest, "P-7", ConMonItemStatus.CLOSED, "CRITICAL", null);

        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of(latest));

        var segments = service.forAuthorization(auth).getCurrentSeverityBreakdown();

        // Order: Critical, High, Moderate, Low, Unspecified
        assertThat(segments).extracting(ConMonAnalyticsResponse.DonutSegment::label)
                .containsExactly("Critical", "High", "Moderate", "Low", "Unspecified");
        assertThat(segments).extracting(ConMonAnalyticsResponse.DonutSegment::count)
                .containsExactly(2, 1, 1, 1, 1);
    }

    @Test
    void severityBreakdown_omitsZeroBuckets() {
        ConMonSnapshot latest = snapshot(1, LocalDateTime.now(), 0, 0, 0);
        addItem(latest, "P-1", ConMonItemStatus.OPEN, "HIGH", null);
        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of(latest));

        var segments = service.forAuthorization(auth).getCurrentSeverityBreakdown();

        assertThat(segments).hasSize(1);
        assertThat(segments.get(0).label()).isEqualTo("High");
    }

    @Test
    void severityBreakdown_unrecognizedSeverityCountsAsUnspecified() {
        ConMonSnapshot latest = snapshot(1, LocalDateTime.now(), 0, 0, 0);
        addItem(latest, "P-1", ConMonItemStatus.OPEN, "low", null); // wrong case
        addItem(latest, "P-2", ConMonItemStatus.OPEN, "EXTREME", null); // not a real bucket
        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of(latest));

        var segments = service.forAuthorization(auth).getCurrentSeverityBreakdown();

        assertThat(segments).hasSize(1);
        assertThat(segments.get(0).label()).isEqualTo("Unspecified");
        assertThat(segments.get(0).count()).isEqualTo(2);
    }

    @Test
    void slaStats_overdueOpenItems_areCountedSeparatelyFromWithin() {
        ConMonSnapshot latest = snapshot(1, LocalDateTime.now(), 0, 0, 0);
        LocalDate today = LocalDate.now();
        addItem(latest, "P-overdue-1", ConMonItemStatus.OPEN, "HIGH", today.minusDays(5));
        addItem(latest, "P-overdue-2", ConMonItemStatus.OPEN, "HIGH", today.minusDays(1));
        addItem(latest, "P-within-1", ConMonItemStatus.OPEN, "HIGH", today.plusDays(10));
        addItem(latest, "P-within-2", ConMonItemStatus.OPEN, "HIGH", today.plusDays(30));
        addItem(latest, "P-within-3", ConMonItemStatus.OPEN, "HIGH", today); // today is NOT before today
        addItem(latest, "P-noDeadline", ConMonItemStatus.OPEN, "HIGH", null);
        // CLOSED item with overdue date should be excluded
        addItem(latest, "P-closed", ConMonItemStatus.CLOSED, "HIGH", today.minusDays(100));

        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of(latest));

        var sla = service.forAuthorization(auth).getSlaStats();

        assertThat(sla.openTotal()).isEqualTo(6);
        assertThat(sla.overdue()).isEqualTo(2);
        assertThat(sla.withinSla()).isEqualTo(3);
        assertThat(sla.withoutDeadline()).isEqualTo(1);
        // 3 within / 5 considered (excludes noDeadline) = 60%
        assertThat(sla.slaPercent()).isCloseTo(60.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void slaStats_dueToday_isWithinSlaNotOverdue() {
        // Boundary check: a deadline of today is not "before" today, so it counts as within SLA.
        ConMonSnapshot latest = snapshot(1, LocalDateTime.now(), 0, 0, 0);
        addItem(latest, "P-today", ConMonItemStatus.OPEN, "HIGH", LocalDate.now());
        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of(latest));

        var sla = service.forAuthorization(auth).getSlaStats();

        assertThat(sla.overdue()).isZero();
        assertThat(sla.withinSla()).isEqualTo(1);
        assertThat(sla.slaPercent()).isEqualTo(100.0);
    }

    @Test
    void slaStats_allOpenItemsHaveNoDeadline_yieldsNullPercent() {
        ConMonSnapshot latest = snapshot(1, LocalDateTime.now(), 0, 0, 0);
        addItem(latest, "P-1", ConMonItemStatus.OPEN, "HIGH", null);
        addItem(latest, "P-2", ConMonItemStatus.OPEN, "HIGH", null);
        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of(latest));

        var sla = service.forAuthorization(auth).getSlaStats();

        // All items have no deadline → percent is undefined per service contract.
        assertThat(sla.slaPercent()).isNull();
        assertThat(sla.openTotal()).isEqualTo(2);
        assertThat(sla.withoutDeadline()).isEqualTo(2);
    }

    @Test
    void slaStats_noOpenItems_yieldsZerosAndNullPercent() {
        ConMonSnapshot latest = snapshot(1, LocalDateTime.now(), 0, 0, 0);
        addItem(latest, "P-closed-1", ConMonItemStatus.CLOSED, null, null);
        addItem(latest, "P-unknown-1", ConMonItemStatus.UNKNOWN, null, null);
        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of(latest));

        var sla = service.forAuthorization(auth).getSlaStats();

        assertThat(sla.openTotal()).isZero();
        assertThat(sla.slaPercent()).isNull();
    }

    @Test
    void slaStats_isComputedFromLatestSnapshotOnly() {
        ConMonSnapshot newest = snapshot(2, LocalDateTime.of(2026, 3, 1, 0, 0), 0, 0, 0);
        ConMonSnapshot older = snapshot(1, LocalDateTime.of(2026, 1, 1, 0, 0), 0, 0, 0);
        addItem(older, "P-old-overdue", ConMonItemStatus.OPEN, "HIGH",
                LocalDate.now().minusDays(50));
        addItem(newest, "P-new-clean", ConMonItemStatus.OPEN, "HIGH",
                LocalDate.now().plusDays(50));

        when(repo.findByAuthorizationOrderByUploadedAtDesc(any()))
                .thenReturn(List.of(newest, older));

        var sla = service.forAuthorization(auth).getSlaStats();

        // Only the latest snapshot's items count — old overdue item should not affect.
        assertThat(sla.openTotal()).isEqualTo(1);
        assertThat(sla.overdue()).isZero();
        assertThat(sla.withinSla()).isEqualTo(1);
    }

    @Test
    void agingBuckets_useScheduledDateAsBaseline_andClassifyByDayOffset() {
        ConMonSnapshot latest = snapshot(1, LocalDateTime.now(), 0, 0, 0);
        LocalDate today = LocalDate.now();
        addItem(latest, "B-15", ConMonItemStatus.OPEN, "HIGH", today.minusDays(15));    // <30
        addItem(latest, "B-45", ConMonItemStatus.OPEN, "HIGH", today.minusDays(45));    // 30-60
        addItem(latest, "B-75", ConMonItemStatus.OPEN, "HIGH", today.minusDays(75));    // 60-90
        addItem(latest, "B-120", ConMonItemStatus.OPEN, "HIGH", today.minusDays(120));  // 90-180
        addItem(latest, "B-200", ConMonItemStatus.OPEN, "HIGH", today.minusDays(200));  // >180
        // CLOSED items should be excluded.
        addItem(latest, "C", ConMonItemStatus.CLOSED, "HIGH", today.minusDays(15));

        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of(latest));

        var buckets = service.forAuthorization(auth).getAgingBuckets();

        assertThat(buckets).extracting(ConMonAnalyticsResponse.AgingBucket::bucket)
                .containsExactly("<30d", "30–60", "60–90", "90–180", ">180");
        assertThat(buckets).extracting(ConMonAnalyticsResponse.AgingBucket::count)
                .containsExactly(1, 1, 1, 1, 1);
    }

    @Test
    void agingBuckets_fallbackToUploadedAt_whenNoScheduledDate() {
        // If an item has no scheduled date the service uses the snapshot uploadedAt
        // as the baseline. With uploadedAt = today, the offset is 0 → <30d bucket.
        ConMonSnapshot latest = snapshot(1, LocalDateTime.now(), 0, 0, 0);
        addItem(latest, "P-no-date", ConMonItemStatus.OPEN, "HIGH", null);

        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of(latest));

        var buckets = service.forAuthorization(auth).getAgingBuckets();
        assertThat(buckets.get(0).count()).isEqualTo(1);
        assertThat(buckets.get(0).bucket()).isEqualTo("<30d");
    }

    @Test
    void meanTimeToClose_averagesAcrossAllSnapshots_skipsItemsMissingDates() {
        ConMonSnapshot s1 = snapshot(1, LocalDateTime.now().minusDays(60), 0, 0, 0);
        ConMonSnapshot s2 = snapshot(2, LocalDateTime.now(), 0, 0, 0);

        // Closed in s1: 10 days late
        ConMonPoamItem c1 = addItem(s1, "C-1", ConMonItemStatus.CLOSED, null,
                LocalDate.of(2026, 1, 1));
        c1.setActualCompletionDate(LocalDate.of(2026, 1, 11));

        // Closed in s2: 20 days late
        ConMonPoamItem c2 = addItem(s2, "C-2", ConMonItemStatus.CLOSED, null,
                LocalDate.of(2026, 2, 1));
        c2.setActualCompletionDate(LocalDate.of(2026, 2, 21));

        // Closed without scheduled date — must be skipped
        ConMonPoamItem c3 = addItem(s2, "C-3", ConMonItemStatus.CLOSED, null, null);
        c3.setActualCompletionDate(LocalDate.of(2026, 2, 21));

        // Open items must be skipped even with both dates set
        ConMonPoamItem o1 = addItem(s2, "O-1", ConMonItemStatus.OPEN, null,
                LocalDate.of(2026, 2, 1));
        o1.setActualCompletionDate(LocalDate.of(2026, 5, 21));

        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of(s2, s1));

        Double mttc = service.forAuthorization(auth).getMeanTimeToCloseDays();
        // (10 + 20) / 2 = 15
        assertThat(mttc).isCloseTo(15.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void meanTimeToClose_whenNoClosedItems_isNull() {
        ConMonSnapshot latest = snapshot(1, LocalDateTime.now(), 0, 0, 0);
        addItem(latest, "P-1", ConMonItemStatus.OPEN, "HIGH", LocalDate.now());
        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of(latest));

        assertThat(service.forAuthorization(auth).getMeanTimeToCloseDays()).isNull();
    }

    @Test
    void currentStatusBreakdown_alwaysReturnsThreeSegments_evenIfZero() {
        ConMonSnapshot latest = snapshot(1, LocalDateTime.now(), 0, 5, 0);
        when(repo.findByAuthorizationOrderByUploadedAtDesc(any())).thenReturn(List.of(latest));

        var donut = service.forAuthorization(auth).getCurrentStatusBreakdown();

        assertThat(donut).extracting(ConMonAnalyticsResponse.DonutSegment::label)
                .containsExactly("Open", "Closed", "Unknown");
        assertThat(donut).extracting(ConMonAnalyticsResponse.DonutSegment::count)
                .containsExactly(0, 5, 0);
    }

    // ---- helpers ----

    private static ConMonSnapshot snapshot(long id, LocalDateTime uploadedAt,
                                           int openCount, int closedCount, int unknownCount) {
        ConMonSnapshot s = new ConMonSnapshot();
        s.setId(id);
        s.setUploadedAt(uploadedAt);
        s.setSummaryOpenCount(openCount);
        s.setSummaryClosedCount(closedCount);
        s.setSummaryUnknownCount(unknownCount);
        s.setItems(new ArrayList<>());
        return s;
    }

    private static ConMonPoamItem addItem(ConMonSnapshot s, String externalId,
                                          ConMonItemStatus status, String severity,
                                          LocalDate scheduledCompletionDate) {
        ConMonPoamItem i = new ConMonPoamItem();
        i.setSnapshot(s);
        i.setExternalId(externalId);
        i.setTitle(externalId);
        i.setStatus(status);
        i.setSeverity(severity);
        i.setScheduledCompletionDate(scheduledCompletionDate);
        s.getItems().add(i);
        return i;
    }
}
