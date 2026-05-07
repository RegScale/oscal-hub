package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse.AgingBucket;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse.DonutSegment;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse.SeveritySeriesPoint;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse.TimeSeriesPoint;
import gov.nist.oscal.tools.api.repository.ConMonSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConMonAnalyticsService {

    private final ConMonSnapshotRepository snapshotRepository;

    public ConMonAnalyticsService(ConMonSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    public ConMonAnalyticsResponse forAuthorization(Authorization authorization) {
        List<ConMonSnapshot> snaps = snapshotRepository.findByAuthorizationOrderByUploadedAtDesc(authorization);
        ConMonAnalyticsResponse r = new ConMonAnalyticsResponse();
        r.setOpenCountSeries(timeSeries(snaps));
        r.setSeveritySeriesByDate(severitySeries(snaps));
        r.setCurrentStatusBreakdown(currentDonut(snaps));
        r.setAgingBuckets(agingBuckets(snaps));
        r.setMeanTimeToCloseDays(meanTimeToClose(snaps));
        return r;
    }

    private List<TimeSeriesPoint> timeSeries(List<ConMonSnapshot> snaps) {
        List<TimeSeriesPoint> out = new ArrayList<>(snaps.size());
        for (int i = snaps.size() - 1; i >= 0; i--) {
            ConMonSnapshot s = snaps.get(i);
            out.add(new TimeSeriesPoint(
                    s.getUploadedAt().toLocalDate(),
                    s.getSummaryOpenCount(),
                    s.getSummaryClosedCount(),
                    s.getSummaryUnknownCount()));
        }
        return out;
    }

    private List<SeveritySeriesPoint> severitySeries(List<ConMonSnapshot> snaps) {
        List<SeveritySeriesPoint> out = new ArrayList<>(snaps.size());
        for (int i = snaps.size() - 1; i >= 0; i--) {
            ConMonSnapshot s = snaps.get(i);
            int low = 0, mod = 0, high = 0, crit = 0;
            for (ConMonPoamItem it : s.getItems()) {
                if (it.getStatus() != ConMonItemStatus.OPEN) continue;
                String sev = it.getSeverity();
                if ("LOW".equals(sev)) low++;
                else if ("MODERATE".equals(sev)) mod++;
                else if ("HIGH".equals(sev)) high++;
                else if ("CRITICAL".equals(sev)) crit++;
            }
            out.add(new SeveritySeriesPoint(s.getUploadedAt().toLocalDate(), low, mod, high, crit));
        }
        return out;
    }

    private List<DonutSegment> currentDonut(List<ConMonSnapshot> snaps) {
        if (snaps.isEmpty()) return List.of();
        ConMonSnapshot latest = snaps.get(0);
        return List.of(
                new DonutSegment("Open", latest.getSummaryOpenCount()),
                new DonutSegment("Closed", latest.getSummaryClosedCount()),
                new DonutSegment("Unknown", latest.getSummaryUnknownCount())
        );
    }

    private List<AgingBucket> agingBuckets(List<ConMonSnapshot> snaps) {
        if (snaps.isEmpty()) return List.of();
        ConMonSnapshot latest = snaps.get(0);
        LocalDate today = LocalDate.now();

        int[] buckets = new int[5]; // <30, 30-60, 60-90, 90-180, >180
        for (ConMonPoamItem it : latest.getItems()) {
            if (it.getStatus() != ConMonItemStatus.OPEN) continue;
            LocalDate baseline = it.getScheduledCompletionDate();
            if (baseline == null) baseline = latest.getUploadedAt().toLocalDate();
            long days = Math.abs(ChronoUnit.DAYS.between(baseline, today));
            if (days < 30) buckets[0]++;
            else if (days < 60) buckets[1]++;
            else if (days < 90) buckets[2]++;
            else if (days < 180) buckets[3]++;
            else buckets[4]++;
        }
        return List.of(
                new AgingBucket("<30d", buckets[0]),
                new AgingBucket("30–60", buckets[1]),
                new AgingBucket("60–90", buckets[2]),
                new AgingBucket("90–180", buckets[3]),
                new AgingBucket(">180", buckets[4])
        );
    }

    /**
     * Best-effort mean time to close: across all snapshots, average days
     * between scheduledCompletionDate and actualCompletionDate on closed items.
     * Returns null if no data.
     */
    private Double meanTimeToClose(List<ConMonSnapshot> snaps) {
        long totalDays = 0;
        long count = 0;
        for (ConMonSnapshot s : snaps) {
            for (ConMonPoamItem it : s.getItems()) {
                if (it.getStatus() != ConMonItemStatus.CLOSED) continue;
                LocalDate sched = it.getScheduledCompletionDate();
                LocalDate actual = it.getActualCompletionDate();
                if (sched == null || actual == null) continue;
                totalDays += Math.abs(ChronoUnit.DAYS.between(sched, actual));
                count++;
            }
        }
        return count == 0 ? null : ((double) totalDays) / count;
    }
}
