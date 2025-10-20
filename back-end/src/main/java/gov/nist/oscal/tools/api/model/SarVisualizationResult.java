package gov.nist.oscal.tools.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SarVisualizationResult {
    private boolean success;
    private String message;
    private String timestamp;
    private AssessmentInfo assessmentInfo;
    private AssessmentSummary assessmentSummary;
    private Map<String, ControlFamilyAssessment> controlsByFamily = new HashMap<>();
    private List<Finding> findings = new ArrayList<>();
    private List<Observation> observations = new ArrayList<>();
    private List<Risk> risks = new ArrayList<>();

    // Constructors
    public SarVisualizationResult() {
        this.timestamp = Instant.now().toString();
    }

    public SarVisualizationResult(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    // Nested classes for structured data
    public static class AssessmentInfo {
        private String uuid;
        private String title;
        private String description;
        private String version;
        private String oscalVersion;
        private String published;
        private String lastModified;
        private String sspImportHref;

        // Getters and setters
        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
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

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getOscalVersion() {
            return oscalVersion;
        }

        public void setOscalVersion(String oscalVersion) {
            this.oscalVersion = oscalVersion;
        }

        public String getPublished() {
            return published;
        }

        public void setPublished(String published) {
            this.published = published;
        }

        public String getLastModified() {
            return lastModified;
        }

        public void setLastModified(String lastModified) {
            this.lastModified = lastModified;
        }

        public String getSspImportHref() {
            return sspImportHref;
        }

        public void setSspImportHref(String sspImportHref) {
            this.sspImportHref = sspImportHref;
        }
    }

    public static class AssessmentSummary {
        private int totalControlsAssessed;
        private int totalFindings;
        private int totalObservations;
        private int totalRisks;
        private Map<String, Integer> findingsBySeverity = new HashMap<>();
        private Map<String, Integer> observationsByType = new HashMap<>();
        private Map<String, Integer> scoreDistribution = new HashMap<>();
        private Map<String, Integer> risksBySeverity = new HashMap<>();
        private int uniqueFamiliesAssessed;

        // Getters and setters
        public int getTotalControlsAssessed() {
            return totalControlsAssessed;
        }

        public void setTotalControlsAssessed(int totalControlsAssessed) {
            this.totalControlsAssessed = totalControlsAssessed;
        }

        public int getTotalFindings() {
            return totalFindings;
        }

        public void setTotalFindings(int totalFindings) {
            this.totalFindings = totalFindings;
        }

        public int getTotalObservations() {
            return totalObservations;
        }

        public void setTotalObservations(int totalObservations) {
            this.totalObservations = totalObservations;
        }

        public int getTotalRisks() {
            return totalRisks;
        }

        public void setTotalRisks(int totalRisks) {
            this.totalRisks = totalRisks;
        }

        public Map<String, Integer> getFindingsBySeverity() {
            return findingsBySeverity;
        }

        public void setFindingsBySeverity(Map<String, Integer> findingsBySeverity) {
            this.findingsBySeverity = findingsBySeverity;
        }

        public Map<String, Integer> getObservationsByType() {
            return observationsByType;
        }

        public void setObservationsByType(Map<String, Integer> observationsByType) {
            this.observationsByType = observationsByType;
        }

        public Map<String, Integer> getScoreDistribution() {
            return scoreDistribution;
        }

        public void setScoreDistribution(Map<String, Integer> scoreDistribution) {
            this.scoreDistribution = scoreDistribution;
        }

        public Map<String, Integer> getRisksBySeverity() {
            return risksBySeverity;
        }

        public void setRisksBySeverity(Map<String, Integer> risksBySeverity) {
            this.risksBySeverity = risksBySeverity;
        }

        public int getUniqueFamiliesAssessed() {
            return uniqueFamiliesAssessed;
        }

        public void setUniqueFamiliesAssessed(int uniqueFamiliesAssessed) {
            this.uniqueFamiliesAssessed = uniqueFamiliesAssessed;
        }
    }

    public static class ControlFamilyAssessment {
        private String familyId;
        private String familyName;
        private int totalControlsAssessed;
        private int totalFindings;
        private int totalObservations;
        private List<AssessedControl> assessedControls = new ArrayList<>();

        public static class AssessedControl {
            private String controlId;
            private int findingsCount;
            private int observationsCount;
            private String assessmentStatus;

            public AssessedControl() {}

            public AssessedControl(String controlId, int findingsCount, int observationsCount, String assessmentStatus) {
                this.controlId = controlId;
                this.findingsCount = findingsCount;
                this.observationsCount = observationsCount;
                this.assessmentStatus = assessmentStatus;
            }

            public String getControlId() {
                return controlId;
            }

            public void setControlId(String controlId) {
                this.controlId = controlId;
            }

            public int getFindingsCount() {
                return findingsCount;
            }

            public void setFindingsCount(int findingsCount) {
                this.findingsCount = findingsCount;
            }

            public int getObservationsCount() {
                return observationsCount;
            }

            public void setObservationsCount(int observationsCount) {
                this.observationsCount = observationsCount;
            }

            public String getAssessmentStatus() {
                return assessmentStatus;
            }

            public void setAssessmentStatus(String assessmentStatus) {
                this.assessmentStatus = assessmentStatus;
            }
        }

        // Getters and setters
        public String getFamilyId() {
            return familyId;
        }

        public void setFamilyId(String familyId) {
            this.familyId = familyId;
        }

        public String getFamilyName() {
            return familyName;
        }

        public void setFamilyName(String familyName) {
            this.familyName = familyName;
        }

        public int getTotalControlsAssessed() {
            return totalControlsAssessed;
        }

        public void setTotalControlsAssessed(int totalControlsAssessed) {
            this.totalControlsAssessed = totalControlsAssessed;
        }

        public int getTotalFindings() {
            return totalFindings;
        }

        public void setTotalFindings(int totalFindings) {
            this.totalFindings = totalFindings;
        }

        public int getTotalObservations() {
            return totalObservations;
        }

        public void setTotalObservations(int totalObservations) {
            this.totalObservations = totalObservations;
        }

        public List<AssessedControl> getAssessedControls() {
            return assessedControls;
        }

        public void setAssessedControls(List<AssessedControl> assessedControls) {
            this.assessedControls = assessedControls;
        }
    }

    public static class Finding {
        private String uuid;
        private String title;
        private String description;
        private List<String> relatedControls = new ArrayList<>();
        private List<String> relatedObservations = new ArrayList<>();
        private Double score;
        private Double qualityScore;
        private Double completenessScore;

        // Getters and setters
        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
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

        public List<String> getRelatedControls() {
            return relatedControls;
        }

        public void setRelatedControls(List<String> relatedControls) {
            this.relatedControls = relatedControls;
        }

        public List<String> getRelatedObservations() {
            return relatedObservations;
        }

        public void setRelatedObservations(List<String> relatedObservations) {
            this.relatedObservations = relatedObservations;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Double getQualityScore() {
            return qualityScore;
        }

        public void setQualityScore(Double qualityScore) {
            this.qualityScore = qualityScore;
        }

        public Double getCompletenessScore() {
            return completenessScore;
        }

        public void setCompletenessScore(Double completenessScore) {
            this.completenessScore = completenessScore;
        }
    }

    public static class Observation {
        private String uuid;
        private String title;
        private String description;
        private List<String> relatedControls = new ArrayList<>();
        private String observationType;
        private Double overallScore;
        private Double qualityScore;
        private Double completenessScore;

        // Getters and setters
        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
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

        public List<String> getRelatedControls() {
            return relatedControls;
        }

        public void setRelatedControls(List<String> relatedControls) {
            this.relatedControls = relatedControls;
        }

        public String getObservationType() {
            return observationType;
        }

        public void setObservationType(String observationType) {
            this.observationType = observationType;
        }

        public Double getOverallScore() {
            return overallScore;
        }

        public void setOverallScore(Double overallScore) {
            this.overallScore = overallScore;
        }

        public Double getQualityScore() {
            return qualityScore;
        }

        public void setQualityScore(Double qualityScore) {
            this.qualityScore = qualityScore;
        }

        public Double getCompletenessScore() {
            return completenessScore;
        }

        public void setCompletenessScore(Double completenessScore) {
            this.completenessScore = completenessScore;
        }
    }

    public static class Risk {
        private String uuid;
        private String title;
        private String description;
        private String status;
        private List<String> relatedControls = new ArrayList<>();

        // Getters and setters
        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<String> getRelatedControls() {
            return relatedControls;
        }

        public void setRelatedControls(List<String> relatedControls) {
            this.relatedControls = relatedControls;
        }
    }

    // Main class getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public AssessmentInfo getAssessmentInfo() {
        return assessmentInfo;
    }

    public void setAssessmentInfo(AssessmentInfo assessmentInfo) {
        this.assessmentInfo = assessmentInfo;
    }

    public AssessmentSummary getAssessmentSummary() {
        return assessmentSummary;
    }

    public void setAssessmentSummary(AssessmentSummary assessmentSummary) {
        this.assessmentSummary = assessmentSummary;
    }

    public Map<String, ControlFamilyAssessment> getControlsByFamily() {
        return controlsByFamily;
    }

    public void setControlsByFamily(Map<String, ControlFamilyAssessment> controlsByFamily) {
        this.controlsByFamily = controlsByFamily;
    }

    public List<Finding> getFindings() {
        return findings;
    }

    public void setFindings(List<Finding> findings) {
        this.findings = findings;
    }

    public List<Observation> getObservations() {
        return observations;
    }

    public void setObservations(List<Observation> observations) {
        this.observations = observations;
    }

    public List<Risk> getRisks() {
        return risks;
    }

    public void setRisks(List<Risk> risks) {
        this.risks = risks;
    }
}
