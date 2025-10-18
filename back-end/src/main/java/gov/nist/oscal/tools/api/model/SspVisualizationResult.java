package gov.nist.oscal.tools.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SspVisualizationResult {
    private boolean success;
    private String message;
    private String timestamp;
    private SystemInfo systemInfo;
    private SecurityCategorization categorization;
    private List<InformationType> informationTypes = new ArrayList<>();
    private List<PersonnelRole> personnel = new ArrayList<>();
    private Map<String, ControlFamilyStatus> controlsByFamily = new HashMap<>();
    private List<Asset> assets = new ArrayList<>();

    // Constructors
    public SspVisualizationResult() {
        this.timestamp = Instant.now().toString();
    }

    public SspVisualizationResult(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    // Nested classes for structured data
    public static class SystemInfo {
        private String uuid;
        private String name;
        private String shortName;
        private String description;
        private String status;
        private List<SystemId> systemIds = new ArrayList<>();

        public static class SystemId {
            private String identifierType;
            private String id;

            public SystemId() {}

            public SystemId(String identifierType, String id) {
                this.identifierType = identifierType;
                this.id = id;
            }

            public String getIdentifierType() {
                return identifierType;
            }

            public void setIdentifierType(String identifierType) {
                this.identifierType = identifierType;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }
        }

        // Getters and setters
        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getShortName() {
            return shortName;
        }

        public void setShortName(String shortName) {
            this.shortName = shortName;
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

        public List<SystemId> getSystemIds() {
            return systemIds;
        }

        public void setSystemIds(List<SystemId> systemIds) {
            this.systemIds = systemIds;
        }
    }

    public static class SecurityCategorization {
        private String confidentiality;
        private String integrity;
        private String availability;
        private String overall;

        // Getters and setters
        public String getConfidentiality() {
            return confidentiality;
        }

        public void setConfidentiality(String confidentiality) {
            this.confidentiality = confidentiality;
        }

        public String getIntegrity() {
            return integrity;
        }

        public void setIntegrity(String integrity) {
            this.integrity = integrity;
        }

        public String getAvailability() {
            return availability;
        }

        public void setAvailability(String availability) {
            this.availability = availability;
        }

        public String getOverall() {
            return overall;
        }

        public void setOverall(String overall) {
            this.overall = overall;
        }
    }

    public static class InformationType {
        private String uuid;
        private String title;
        private String description;
        private List<String> categorizations = new ArrayList<>();
        private ImpactLevel confidentiality;
        private ImpactLevel integrity;
        private ImpactLevel availability;

        public static class ImpactLevel {
            private String base;
            private String selected;

            public ImpactLevel() {}

            public ImpactLevel(String base, String selected) {
                this.base = base;
                this.selected = selected;
            }

            public String getBase() {
                return base;
            }

            public void setBase(String base) {
                this.base = base;
            }

            public String getSelected() {
                return selected;
            }

            public void setSelected(String selected) {
                this.selected = selected;
            }
        }

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

        public List<String> getCategorizations() {
            return categorizations;
        }

        public void setCategorizations(List<String> categorizations) {
            this.categorizations = categorizations;
        }

        public ImpactLevel getConfidentiality() {
            return confidentiality;
        }

        public void setConfidentiality(ImpactLevel confidentiality) {
            this.confidentiality = confidentiality;
        }

        public ImpactLevel getIntegrity() {
            return integrity;
        }

        public void setIntegrity(ImpactLevel integrity) {
            this.integrity = integrity;
        }

        public ImpactLevel getAvailability() {
            return availability;
        }

        public void setAvailability(ImpactLevel availability) {
            this.availability = availability;
        }
    }

    public static class PersonnelRole {
        private String roleId;
        private String roleTitle;
        private String roleShortName;
        private List<Person> assignedPersonnel = new ArrayList<>();

        public static class Person {
            private String uuid;
            private String name;
            private String jobTitle;
            private String type;

            public Person() {}

            public Person(String uuid, String name, String jobTitle, String type) {
                this.uuid = uuid;
                this.name = name;
                this.jobTitle = jobTitle;
                this.type = type;
            }

            public String getUuid() {
                return uuid;
            }

            public void setUuid(String uuid) {
                this.uuid = uuid;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getJobTitle() {
                return jobTitle;
            }

            public void setJobTitle(String jobTitle) {
                this.jobTitle = jobTitle;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }
        }

        // Getters and setters
        public String getRoleId() {
            return roleId;
        }

        public void setRoleId(String roleId) {
            this.roleId = roleId;
        }

        public String getRoleTitle() {
            return roleTitle;
        }

        public void setRoleTitle(String roleTitle) {
            this.roleTitle = roleTitle;
        }

        public String getRoleShortName() {
            return roleShortName;
        }

        public void setRoleShortName(String roleShortName) {
            this.roleShortName = roleShortName;
        }

        public List<Person> getAssignedPersonnel() {
            return assignedPersonnel;
        }

        public void setAssignedPersonnel(List<Person> assignedPersonnel) {
            this.assignedPersonnel = assignedPersonnel;
        }
    }

    public static class ControlFamilyStatus {
        private String familyId;
        private String familyName;
        private int totalControls;
        private Map<String, Integer> statusCounts = new HashMap<>();
        private List<ControlStatus> controls = new ArrayList<>();

        public static class ControlStatus {
            private String controlId;
            private String implementationStatus;
            private String controlOrigination;

            public ControlStatus() {}

            public ControlStatus(String controlId, String implementationStatus, String controlOrigination) {
                this.controlId = controlId;
                this.implementationStatus = implementationStatus;
                this.controlOrigination = controlOrigination;
            }

            public String getControlId() {
                return controlId;
            }

            public void setControlId(String controlId) {
                this.controlId = controlId;
            }

            public String getImplementationStatus() {
                return implementationStatus;
            }

            public void setImplementationStatus(String implementationStatus) {
                this.implementationStatus = implementationStatus;
            }

            public String getControlOrigination() {
                return controlOrigination;
            }

            public void setControlOrigination(String controlOrigination) {
                this.controlOrigination = controlOrigination;
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

        public int getTotalControls() {
            return totalControls;
        }

        public void setTotalControls(int totalControls) {
            this.totalControls = totalControls;
        }

        public Map<String, Integer> getStatusCounts() {
            return statusCounts;
        }

        public void setStatusCounts(Map<String, Integer> statusCounts) {
            this.statusCounts = statusCounts;
        }

        public List<ControlStatus> getControls() {
            return controls;
        }

        public void setControls(List<ControlStatus> controls) {
            this.controls = controls;
        }
    }

    public static class Asset {
        private String uuid;
        private String description;
        private String assetType;
        private String function;
        private String fqdn;
        private String ipv4Address;
        private String ipv6Address;
        private String macAddress;
        private boolean virtual;
        private boolean publicAccess;
        private String softwareName;
        private String softwareVersion;
        private String vendorName;
        private boolean isScanned;

        // Getters and setters
        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAssetType() {
            return assetType;
        }

        public void setAssetType(String assetType) {
            this.assetType = assetType;
        }

        public String getFunction() {
            return function;
        }

        public void setFunction(String function) {
            this.function = function;
        }

        public String getFqdn() {
            return fqdn;
        }

        public void setFqdn(String fqdn) {
            this.fqdn = fqdn;
        }

        public String getIpv4Address() {
            return ipv4Address;
        }

        public void setIpv4Address(String ipv4Address) {
            this.ipv4Address = ipv4Address;
        }

        public String getIpv6Address() {
            return ipv6Address;
        }

        public void setIpv6Address(String ipv6Address) {
            this.ipv6Address = ipv6Address;
        }

        public String getMacAddress() {
            return macAddress;
        }

        public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
        }

        public boolean isVirtual() {
            return virtual;
        }

        public void setVirtual(boolean virtual) {
            this.virtual = virtual;
        }

        public boolean isPublicAccess() {
            return publicAccess;
        }

        public void setPublicAccess(boolean publicAccess) {
            this.publicAccess = publicAccess;
        }

        public String getSoftwareName() {
            return softwareName;
        }

        public void setSoftwareName(String softwareName) {
            this.softwareName = softwareName;
        }

        public String getSoftwareVersion() {
            return softwareVersion;
        }

        public void setSoftwareVersion(String softwareVersion) {
            this.softwareVersion = softwareVersion;
        }

        public String getVendorName() {
            return vendorName;
        }

        public void setVendorName(String vendorName) {
            this.vendorName = vendorName;
        }

        public boolean isScanned() {
            return isScanned;
        }

        public void setScanned(boolean scanned) {
            isScanned = scanned;
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

    public SystemInfo getSystemInfo() {
        return systemInfo;
    }

    public void setSystemInfo(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    public SecurityCategorization getCategorization() {
        return categorization;
    }

    public void setCategorization(SecurityCategorization categorization) {
        this.categorization = categorization;
    }

    public List<InformationType> getInformationTypes() {
        return informationTypes;
    }

    public void setInformationTypes(List<InformationType> informationTypes) {
        this.informationTypes = informationTypes;
    }

    public List<PersonnelRole> getPersonnel() {
        return personnel;
    }

    public void setPersonnel(List<PersonnelRole> personnel) {
        this.personnel = personnel;
    }

    public Map<String, ControlFamilyStatus> getControlsByFamily() {
        return controlsByFamily;
    }

    public void setControlsByFamily(Map<String, ControlFamilyStatus> controlsByFamily) {
        this.controlsByFamily = controlsByFamily;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }
}
