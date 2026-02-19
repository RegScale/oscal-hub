package gov.nist.oscal.tools.api.model.security;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents an identified gap in SOC 2 compliance.
 */
@Schema(description = "SOC 2 compliance gap analysis")
public class GapAnalysis {

    @Schema(description = "Gap identifier", example = "GAP-001")
    private String gapId;

    @Schema(description = "Related control ID", example = "CC6.8")
    private String controlId;

    @Schema(description = "Gap title", example = "Multi-Factor Authentication Not Implemented")
    private String title;

    @Schema(description = "Detailed description of the gap")
    private String description;

    @Schema(description = "Severity level")
    private GapSeverity severity;

    @Schema(description = "Recommended remediation")
    private String recommendation;

    @Schema(description = "Estimated effort to remediate")
    private String effort;

    @Schema(description = "Priority ranking", example = "1")
    private int priority;

    public GapAnalysis() {
    }

    public GapAnalysis(String gapId, String controlId, String title, String description,
                       GapSeverity severity, String recommendation, String effort, int priority) {
        this.gapId = gapId;
        this.controlId = controlId;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.recommendation = recommendation;
        this.effort = effort;
        this.priority = priority;
    }

    public String getGapId() {
        return gapId;
    }

    public void setGapId(String gapId) {
        this.gapId = gapId;
    }

    public String getControlId() {
        return controlId;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GapSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(GapSeverity severity) {
        this.severity = severity;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getEffort() {
        return effort;
    }

    public void setEffort(String effort) {
        this.effort = effort;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String gapId;
        private String controlId;
        private String title;
        private String description;
        private GapSeverity severity;
        private String recommendation;
        private String effort;
        private int priority;

        public Builder gapId(String gapId) {
            this.gapId = gapId;
            return this;
        }

        public Builder controlId(String controlId) {
            this.controlId = controlId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder severity(GapSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder recommendation(String recommendation) {
            this.recommendation = recommendation;
            return this;
        }

        public Builder effort(String effort) {
            this.effort = effort;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public GapAnalysis build() {
            return new GapAnalysis(gapId, controlId, title, description, severity, recommendation, effort, priority);
        }
    }
}
