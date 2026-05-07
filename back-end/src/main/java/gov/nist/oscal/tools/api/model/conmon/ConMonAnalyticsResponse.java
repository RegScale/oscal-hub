package gov.nist.oscal.tools.api.model.conmon;

import java.time.LocalDate;
import java.util.List;

public class ConMonAnalyticsResponse {

    public record TimeSeriesPoint(LocalDate date, int open, int closed, int unknown) {}
    public record SeveritySeriesPoint(LocalDate date, int low, int moderate, int high, int critical) {}
    public record DonutSegment(String label, int count) {}
    public record AgingBucket(String bucket, int count) {}

    private List<TimeSeriesPoint> openCountSeries;
    private List<SeveritySeriesPoint> severitySeriesByDate;
    private List<DonutSegment> currentStatusBreakdown;
    private List<AgingBucket> agingBuckets;
    private Double meanTimeToCloseDays;

    public ConMonAnalyticsResponse() {}

    public List<TimeSeriesPoint> getOpenCountSeries() { return openCountSeries; }
    public void setOpenCountSeries(List<TimeSeriesPoint> l) { this.openCountSeries = l; }
    public List<SeveritySeriesPoint> getSeveritySeriesByDate() { return severitySeriesByDate; }
    public void setSeveritySeriesByDate(List<SeveritySeriesPoint> l) { this.severitySeriesByDate = l; }
    public List<DonutSegment> getCurrentStatusBreakdown() { return currentStatusBreakdown; }
    public void setCurrentStatusBreakdown(List<DonutSegment> l) { this.currentStatusBreakdown = l; }
    public List<AgingBucket> getAgingBuckets() { return agingBuckets; }
    public void setAgingBuckets(List<AgingBucket> l) { this.agingBuckets = l; }
    public Double getMeanTimeToCloseDays() { return meanTimeToCloseDays; }
    public void setMeanTimeToCloseDays(Double d) { this.meanTimeToCloseDays = d; }
}
