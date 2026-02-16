package gov.nist.oscal.tools.api.model.security;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/**
 * Summary of SOC 2 compliance status.
 */
@Schema(description = "SOC 2 compliance summary")
public class ComplianceSummary {

    @Schema(description = "Total number of controls assessed", example = "20")
    private int totalControls;

    @Schema(description = "Number of fully implemented controls", example = "15")
    private int implementedControls;

    @Schema(description = "Number of partially implemented controls", example = "3")
    private int partialControls;

    @Schema(description = "Number of gaps (not implemented)", example = "2")
    private int gapControls;

    @Schema(description = "Overall compliance percentage", example = "82.5")
    private double compliancePercentage;

    @Schema(description = "Timestamp of the assessment")
    private Instant assessmentDate;

    @Schema(description = "Controls count by category")
    private Map<String, CategorySummary> byCategory;

    public ComplianceSummary() {
    }

    public ComplianceSummary(int totalControls, int implementedControls, int partialControls,
                             int gapControls, double compliancePercentage, Instant assessmentDate,
                             Map<String, CategorySummary> byCategory) {
        this.totalControls = totalControls;
        this.implementedControls = implementedControls;
        this.partialControls = partialControls;
        this.gapControls = gapControls;
        this.compliancePercentage = compliancePercentage;
        this.assessmentDate = assessmentDate;
        this.byCategory = byCategory;
    }

    public int getTotalControls() {
        return totalControls;
    }

    public void setTotalControls(int totalControls) {
        this.totalControls = totalControls;
    }

    public int getImplementedControls() {
        return implementedControls;
    }

    public void setImplementedControls(int implementedControls) {
        this.implementedControls = implementedControls;
    }

    public int getPartialControls() {
        return partialControls;
    }

    public void setPartialControls(int partialControls) {
        this.partialControls = partialControls;
    }

    public int getGapControls() {
        return gapControls;
    }

    public void setGapControls(int gapControls) {
        this.gapControls = gapControls;
    }

    public double getCompliancePercentage() {
        return compliancePercentage;
    }

    public void setCompliancePercentage(double compliancePercentage) {
        this.compliancePercentage = compliancePercentage;
    }

    public Instant getAssessmentDate() {
        return assessmentDate;
    }

    public void setAssessmentDate(Instant assessmentDate) {
        this.assessmentDate = assessmentDate;
    }

    public Map<String, CategorySummary> getByCategory() {
        return byCategory;
    }

    public void setByCategory(Map<String, CategorySummary> byCategory) {
        this.byCategory = byCategory;
    }

    /**
     * Summary for a single category.
     */
    @Schema(description = "Summary for a control category")
    public static class CategorySummary {
        @Schema(description = "Category display name")
        private String displayName;

        @Schema(description = "Total controls in category")
        private int total;

        @Schema(description = "Implemented controls in category")
        private int implemented;

        @Schema(description = "Partial controls in category")
        private int partial;

        @Schema(description = "Gap controls in category")
        private int gaps;

        public CategorySummary() {
        }

        public CategorySummary(String displayName, int total, int implemented, int partial, int gaps) {
            this.displayName = displayName;
            this.total = total;
            this.implemented = implemented;
            this.partial = partial;
            this.gaps = gaps;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getImplemented() {
            return implemented;
        }

        public void setImplemented(int implemented) {
            this.implemented = implemented;
        }

        public int getPartial() {
            return partial;
        }

        public void setPartial(int partial) {
            this.partial = partial;
        }

        public int getGaps() {
            return gaps;
        }

        public void setGaps(int gaps) {
            this.gaps = gaps;
        }
    }
}
