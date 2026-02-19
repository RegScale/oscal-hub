package gov.nist.oscal.tools.api.model.health;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Detailed health response containing comprehensive system health information.
 * Used by the admin health dashboard.
 */
@Schema(description = "Detailed health check response with component status and system information")
public class DetailedHealthResponse {

    @Schema(description = "Overall health status", example = "UP")
    private String status;

    @Schema(description = "Timestamp of health check", example = "2024-01-15T10:30:00Z")
    private String timestamp;

    @Schema(description = "Application information")
    private ApplicationInfo application;

    @Schema(description = "Health status of individual components")
    private Map<String, ComponentHealth> components;

    @Schema(description = "System resource information")
    private SystemInfo system;

    @Schema(description = "Environment information")
    private EnvironmentInfo environment;

    public DetailedHealthResponse() {
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public ApplicationInfo getApplication() {
        return application;
    }

    public void setApplication(ApplicationInfo application) {
        this.application = application;
    }

    public Map<String, ComponentHealth> getComponents() {
        return components;
    }

    public void setComponents(Map<String, ComponentHealth> components) {
        this.components = components;
    }

    public SystemInfo getSystem() {
        return system;
    }

    public void setSystem(SystemInfo system) {
        this.system = system;
    }

    public EnvironmentInfo getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentInfo environment) {
        this.environment = environment;
    }

    /**
     * Application metadata information.
     */
    @Schema(description = "Application metadata")
    public static class ApplicationInfo {

        @Schema(description = "Application name", example = "oscal-cli-api")
        private String name;

        @Schema(description = "Application version", example = "1.0.0")
        private String version;

        @Schema(description = "Active Spring profile", example = "dev")
        private String profile;

        @Schema(description = "Application uptime", example = "PT2H30M15S")
        private String uptime;

        @Schema(description = "Application start time", example = "2024-01-15T08:00:00Z")
        private String startTime;

        public ApplicationInfo() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public String getUptime() {
            return uptime;
        }

        public void setUptime(String uptime) {
            this.uptime = uptime;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }
    }

    /**
     * System resource information.
     */
    @Schema(description = "System resource metrics")
    public static class SystemInfo {

        @Schema(description = "Total memory in MB", example = "4096")
        private long totalMemoryMb;

        @Schema(description = "Used memory in MB", example = "2048")
        private long usedMemoryMb;

        @Schema(description = "Free memory in MB", example = "2048")
        private long freeMemoryMb;

        @Schema(description = "Memory usage percentage", example = "50.0")
        private double memoryUsagePercent;

        @Schema(description = "Number of available processors", example = "8")
        private int availableProcessors;

        @Schema(description = "System load average (1 minute)", example = "1.5")
        private double systemLoadAverage;

        @Schema(description = "Total disk space in GB", example = "500")
        private long totalDiskSpaceGb;

        @Schema(description = "Free disk space in GB", example = "250")
        private long freeDiskSpaceGb;

        @Schema(description = "Disk usage percentage", example = "50.0")
        private double diskUsagePercent;

        public SystemInfo() {
        }

        public long getTotalMemoryMb() {
            return totalMemoryMb;
        }

        public void setTotalMemoryMb(long totalMemoryMb) {
            this.totalMemoryMb = totalMemoryMb;
        }

        public long getUsedMemoryMb() {
            return usedMemoryMb;
        }

        public void setUsedMemoryMb(long usedMemoryMb) {
            this.usedMemoryMb = usedMemoryMb;
        }

        public long getFreeMemoryMb() {
            return freeMemoryMb;
        }

        public void setFreeMemoryMb(long freeMemoryMb) {
            this.freeMemoryMb = freeMemoryMb;
        }

        public double getMemoryUsagePercent() {
            return memoryUsagePercent;
        }

        public void setMemoryUsagePercent(double memoryUsagePercent) {
            this.memoryUsagePercent = memoryUsagePercent;
        }

        public int getAvailableProcessors() {
            return availableProcessors;
        }

        public void setAvailableProcessors(int availableProcessors) {
            this.availableProcessors = availableProcessors;
        }

        public double getSystemLoadAverage() {
            return systemLoadAverage;
        }

        public void setSystemLoadAverage(double systemLoadAverage) {
            this.systemLoadAverage = systemLoadAverage;
        }

        public long getTotalDiskSpaceGb() {
            return totalDiskSpaceGb;
        }

        public void setTotalDiskSpaceGb(long totalDiskSpaceGb) {
            this.totalDiskSpaceGb = totalDiskSpaceGb;
        }

        public long getFreeDiskSpaceGb() {
            return freeDiskSpaceGb;
        }

        public void setFreeDiskSpaceGb(long freeDiskSpaceGb) {
            this.freeDiskSpaceGb = freeDiskSpaceGb;
        }

        public double getDiskUsagePercent() {
            return diskUsagePercent;
        }

        public void setDiskUsagePercent(double diskUsagePercent) {
            this.diskUsagePercent = diskUsagePercent;
        }
    }

    /**
     * Environment information.
     */
    @Schema(description = "Runtime environment information")
    public static class EnvironmentInfo {

        @Schema(description = "Java version", example = "17.0.2")
        private String javaVersion;

        @Schema(description = "Java vendor", example = "Eclipse Adoptium")
        private String javaVendor;

        @Schema(description = "Operating system name", example = "Linux")
        private String osName;

        @Schema(description = "Operating system version", example = "5.15.0")
        private String osVersion;

        @Schema(description = "Operating system architecture", example = "amd64")
        private String osArch;

        @Schema(description = "System timezone", example = "America/New_York")
        private String timezone;

        public EnvironmentInfo() {
        }

        public String getJavaVersion() {
            return javaVersion;
        }

        public void setJavaVersion(String javaVersion) {
            this.javaVersion = javaVersion;
        }

        public String getJavaVendor() {
            return javaVendor;
        }

        public void setJavaVendor(String javaVendor) {
            this.javaVendor = javaVendor;
        }

        public String getOsName() {
            return osName;
        }

        public void setOsName(String osName) {
            this.osName = osName;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

        public String getOsArch() {
            return osArch;
        }

        public void setOsArch(String osArch) {
            this.osArch = osArch;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }
    }
}
