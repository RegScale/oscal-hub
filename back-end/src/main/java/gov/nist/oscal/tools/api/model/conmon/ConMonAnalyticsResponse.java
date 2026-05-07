package gov.nist.oscal.tools.api.model.conmon;

import java.time.LocalDate;
import java.util.List;

public class ConMonAnalyticsResponse {

    public record TimeSeriesPoint(LocalDate date, int open, int closed, int unknown) {}
    public record DonutSegment(String label, int count) {}
    public record AgingBucket(String bucket, int count) {}
    public record SlaStats(
            int openTotal,
            int withinSla,
            int overdue,
            int withoutDeadline,
            Double slaPercent  // null if openTotal == 0
    ) {}

    private List<TimeSeriesPoint> openCountSeries;
    private List<DonutSegment> currentSeverityBreakdown;
    private List<DonutSegment> currentStatusBreakdown;
    private List<AgingBucket> agingBuckets;
    private Double meanTimeToCloseDays;
    private SlaStats slaStats;

    public ConMonAnalyticsResponse() {}

    public List<TimeSeriesPoint> getOpenCountSeries() { return openCountSeries; }
    public void setOpenCountSeries(List<TimeSeriesPoint> l) { this.openCountSeries = l; }
    public List<DonutSegment> getCurrentSeverityBreakdown() { return currentSeverityBreakdown; }
    public void setCurrentSeverityBreakdown(List<DonutSegment> l) { this.currentSeverityBreakdown = l; }
    public List<DonutSegment> getCurrentStatusBreakdown() { return currentStatusBreakdown; }
    public void setCurrentStatusBreakdown(List<DonutSegment> l) { this.currentStatusBreakdown = l; }
    public List<AgingBucket> getAgingBuckets() { return agingBuckets; }
    public void setAgingBuckets(List<AgingBucket> l) { this.agingBuckets = l; }
    public Double getMeanTimeToCloseDays() { return meanTimeToCloseDays; }
    public void setMeanTimeToCloseDays(Double d) { this.meanTimeToCloseDays = d; }
    public SlaStats getSlaStats() { return slaStats; }
    public void setSlaStats(SlaStats s) { this.slaStats = s; }
}
