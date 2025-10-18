package gov.nist.oscal.tools.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileVisualizationResult {
    private boolean success;
    private String message;
    private String timestamp;
    private ProfileInfo profileInfo;
    private List<ImportInfo> imports = new ArrayList<>();
    private ControlSummary controlSummary;
    private Map<String, ControlFamilyInfo> controlsByFamily = new HashMap<>();
    private ModificationSummary modificationSummary;

    // Constructors
    public ProfileVisualizationResult() {
        this.timestamp = Instant.now().toString();
    }

    public ProfileVisualizationResult(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    // Nested classes for structured data
    public static class ProfileInfo {
        private String uuid;
        private String title;
        private String version;
        private String oscalVersion;
        private String lastModified;
        private String published;

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

        public String getLastModified() {
            return lastModified;
        }

        public void setLastModified(String lastModified) {
            this.lastModified = lastModified;
        }

        public String getPublished() {
            return published;
        }

        public void setPublished(String published) {
            this.published = published;
        }
    }

    public static class ImportInfo {
        private String href;
        private List<String> includeAllIds = new ArrayList<>();
        private List<String> excludeIds = new ArrayList<>();
        private int estimatedControlCount;

        // Getters and setters
        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public List<String> getIncludeAllIds() {
            return includeAllIds;
        }

        public void setIncludeAllIds(List<String> includeAllIds) {
            this.includeAllIds = includeAllIds;
        }

        public List<String> getExcludeIds() {
            return excludeIds;
        }

        public void setExcludeIds(List<String> excludeIds) {
            this.excludeIds = excludeIds;
        }

        public int getEstimatedControlCount() {
            return estimatedControlCount;
        }

        public void setEstimatedControlCount(int estimatedControlCount) {
            this.estimatedControlCount = estimatedControlCount;
        }
    }

    public static class ControlSummary {
        private int totalIncludedControls;
        private int totalExcludedControls;
        private int totalModifications;
        private int uniqueFamilies;

        // Getters and setters
        public int getTotalIncludedControls() {
            return totalIncludedControls;
        }

        public void setTotalIncludedControls(int totalIncludedControls) {
            this.totalIncludedControls = totalIncludedControls;
        }

        public int getTotalExcludedControls() {
            return totalExcludedControls;
        }

        public void setTotalExcludedControls(int totalExcludedControls) {
            this.totalExcludedControls = totalExcludedControls;
        }

        public int getTotalModifications() {
            return totalModifications;
        }

        public void setTotalModifications(int totalModifications) {
            this.totalModifications = totalModifications;
        }

        public int getUniqueFamilies() {
            return uniqueFamilies;
        }

        public void setUniqueFamilies(int uniqueFamilies) {
            this.uniqueFamilies = uniqueFamilies;
        }
    }

    public static class ControlFamilyInfo {
        private String familyId;
        private String familyName;
        private int includedCount;
        private int excludedCount;
        private List<String> includedControls = new ArrayList<>();
        private List<String> excludedControls = new ArrayList<>();

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

        public int getIncludedCount() {
            return includedCount;
        }

        public void setIncludedCount(int includedCount) {
            this.includedCount = includedCount;
        }

        public int getExcludedCount() {
            return excludedCount;
        }

        public void setExcludedCount(int excludedCount) {
            this.excludedCount = excludedCount;
        }

        public List<String> getIncludedControls() {
            return includedControls;
        }

        public void setIncludedControls(List<String> includedControls) {
            this.includedControls = includedControls;
        }

        public List<String> getExcludedControls() {
            return excludedControls;
        }

        public void setExcludedControls(List<String> excludedControls) {
            this.excludedControls = excludedControls;
        }
    }

    public static class ModificationSummary {
        private int totalSetsParameters;
        private int totalAlters;
        private List<String> modifiedControlIds = new ArrayList<>();

        // Getters and setters
        public int getTotalSetsParameters() {
            return totalSetsParameters;
        }

        public void setTotalSetsParameters(int totalSetsParameters) {
            this.totalSetsParameters = totalSetsParameters;
        }

        public int getTotalAlters() {
            return totalAlters;
        }

        public void setTotalAlters(int totalAlters) {
            this.totalAlters = totalAlters;
        }

        public List<String> getModifiedControlIds() {
            return modifiedControlIds;
        }

        public void setModifiedControlIds(List<String> modifiedControlIds) {
            this.modifiedControlIds = modifiedControlIds;
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

    public ProfileInfo getProfileInfo() {
        return profileInfo;
    }

    public void setProfileInfo(ProfileInfo profileInfo) {
        this.profileInfo = profileInfo;
    }

    public List<ImportInfo> getImports() {
        return imports;
    }

    public void setImports(List<ImportInfo> imports) {
        this.imports = imports;
    }

    public ControlSummary getControlSummary() {
        return controlSummary;
    }

    public void setControlSummary(ControlSummary controlSummary) {
        this.controlSummary = controlSummary;
    }

    public Map<String, ControlFamilyInfo> getControlsByFamily() {
        return controlsByFamily;
    }

    public void setControlsByFamily(Map<String, ControlFamilyInfo> controlsByFamily) {
        this.controlsByFamily = controlsByFamily;
    }

    public ModificationSummary getModificationSummary() {
        return modificationSummary;
    }

    public void setModificationSummary(ModificationSummary modificationSummary) {
        this.modificationSummary = modificationSummary;
    }
}
