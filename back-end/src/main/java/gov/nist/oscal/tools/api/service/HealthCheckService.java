package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.config.FileValidationConfig;
import gov.nist.oscal.tools.api.model.health.ComponentHealth;
import gov.nist.oscal.tools.api.model.health.DetailedHealthResponse;
import gov.nist.oscal.tools.api.model.health.DetailedHealthResponse.ApplicationInfo;
import gov.nist.oscal.tools.api.model.health.DetailedHealthResponse.EnvironmentInfo;
import gov.nist.oscal.tools.api.model.health.DetailedHealthResponse.SystemInfo;
import gov.nist.oscal.tools.api.model.health.SimpleHealthResponse;

import gov.nist.secauto.oscal.lib.OscalBindingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Service for performing health checks on various system components.
 */
@Service
public class HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

    private static final double MEMORY_WARNING_THRESHOLD = 0.90; // 90%
    private static final double DISK_WARNING_THRESHOLD = 0.90; // 90%
    private static final long DISK_CRITICAL_THRESHOLD_MB = 100; // 100MB minimum free space
    private static final double CPU_WARNING_THRESHOLD = 0.80; // 80% CPU usage
    private static final double CPU_CRITICAL_THRESHOLD = 0.95; // 95% CPU usage

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private AzureBlobService azureBlobService;

    @Autowired(required = false)
    private GcsStorageService gcsStorageService;

    @Autowired(required = false)
    private ClamAvScannerService clamAvScannerService;

    @Autowired
    private FileValidationConfig fileValidationConfig;

    @Value("${spring.application.name:oscal-cli-api}")
    private String applicationName;

    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    // Configuration values for secrets health check
    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.url:}")
    private String dbUrl;

    @Value("${spring.datasource.username:}")
    private String dbUsername;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Value("${azure.storage.connection-string:}")
    private String azureStorageConnectionString;

    @Value("${aws.s3.region:}")
    private String awsRegion;

    @Value("${siem.enabled:false}")
    private boolean siemEnabled;

    @Value("${siem.webhook-url:}")
    private String siemWebhookUrl;

    /**
     * Get simple health status for public endpoint.
     */
    public SimpleHealthResponse getSimpleHealth() {
        SimpleHealthResponse response = new SimpleHealthResponse();
        response.setStatus(isHealthy() ? "UP" : "DOWN");
        response.setTimestamp(Instant.now().toString());
        response.setVersion(applicationVersion);
        return response;
    }

    /**
     * Get detailed health status for admin dashboard.
     */
    public DetailedHealthResponse getDetailedHealth() {
        DetailedHealthResponse response = new DetailedHealthResponse();

        // Build all health information
        Map<String, ComponentHealth> components = buildComponentsHealth();

        // Determine overall status based on critical components
        boolean databaseUp = "UP".equals(components.get("database").getStatus());
        String overallStatus = databaseUp ? "UP" : "DOWN";

        response.setStatus(overallStatus);
        response.setTimestamp(Instant.now().toString());
        response.setApplication(buildApplicationInfo());
        response.setComponents(components);
        response.setSystem(buildSystemInfo());
        response.setEnvironment(buildEnvironmentInfo());

        return response;
    }

    /**
     * Check if the system is healthy (for simple health check).
     * System is considered healthy if the database is accessible.
     */
    public boolean isHealthy() {
        return isDatabaseHealthy();
    }

    /**
     * Get health status for a specific component.
     */
    public ComponentHealth getComponentHealth(String component) {
        switch (component.toLowerCase()) {
            case "database":
            case "db":
                return checkDatabaseHealth();
            case "storage":
                return checkStorageHealth();
            case "memory":
                return checkMemoryHealth();
            case "cpu":
            case "processor":
                return checkCpuHealth();
            case "diskspace":
            case "disk":
                return checkDiskSpaceHealth();
            case "oscal":
            case "oscallibrary":
                return checkOscalLibraryHealth();
            case "secrets":
            case "config":
            case "configuration":
                return checkSecretsHealth();
            case "clamav":
            case "malware":
            case "antivirus":
            case "virus":
                return checkClamavHealth();
            default:
                return ComponentHealth.builder()
                        .status("UNKNOWN")
                        .message("Unknown component: " + component)
                        .build();
        }
    }

    // ========== Private Helper Methods ==========

    private boolean isDatabaseHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (Exception e) {
            logger.warn("Database health check failed: {}", e.getMessage());
            return false;
        }
    }

    private ApplicationInfo buildApplicationInfo() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        ApplicationInfo info = new ApplicationInfo();
        info.setName(applicationName);
        info.setVersion(applicationVersion);
        info.setProfile(activeProfile);
        info.setUptime(formatDuration(Duration.ofMillis(runtimeMXBean.getUptime())));
        info.setStartTime(Instant.ofEpochMilli(runtimeMXBean.getStartTime()).toString());

        return info;
    }

    private Map<String, ComponentHealth> buildComponentsHealth() {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("database", checkDatabaseHealth());
        components.put("storage", checkStorageHealth());
        components.put("memory", checkMemoryHealth());
        components.put("cpu", checkCpuHealth());
        components.put("diskSpace", checkDiskSpaceHealth());
        components.put("oscalLibrary", checkOscalLibraryHealth());
        components.put("secrets", checkSecretsHealth());
        components.put("clamav", checkClamavHealth());
        return components;
    }

    private ComponentHealth checkDatabaseHealth() {
        long startTime = System.currentTimeMillis();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(5);
            long responseTime = System.currentTimeMillis() - startTime;

            if (valid) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("database", connection.getMetaData().getDatabaseProductName());
                details.put("databaseVersion", connection.getMetaData().getDatabaseProductVersion());
                details.put("driverName", connection.getMetaData().getDriverName());
                details.put("url", connection.getMetaData().getURL());

                return ComponentHealth.builder()
                        .status("UP")
                        .message("Database connection is healthy")
                        .details(details)
                        .responseTimeMs(responseTime)
                        .build();
            } else {
                return ComponentHealth.builder()
                        .status("DOWN")
                        .message("Database connection validation failed")
                        .responseTimeMs(responseTime)
                        .build();
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("error", e.getClass().getSimpleName());
            details.put("errorMessage", e.getMessage());

            return ComponentHealth.builder()
                    .status("DOWN")
                    .message("Database connection failed: " + e.getMessage())
                    .details(details)
                    .responseTimeMs(responseTime)
                    .build();
        }
    }

    private ComponentHealth checkStorageHealth() {
        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> details = new LinkedHashMap<>();

            // Check Azure Blob Storage
            if (azureBlobService != null && azureBlobService.isConfigured()) {
                details.put("provider", "azure_blob_storage");
                details.put("configured", true);
                return ComponentHealth.builder()
                        .status("UP")
                        .message("Azure Blob Storage is configured and available")
                        .details(details)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            // Check GCS Storage
            if (gcsStorageService != null && gcsStorageService.isConfigured()) {
                details.put("provider", "google_cloud_storage");
                details.put("configured", true);
                return ComponentHealth.builder()
                        .status("UP")
                        .message("Google Cloud Storage is configured and available")
                        .details(details)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            // Fallback to local storage
            String localStorageDir = System.getProperty("user.home") + "/.oscal-hub/files";
            File storageDir = new File(localStorageDir);

            details.put("provider", "local_filesystem");
            details.put("path", localStorageDir);
            details.put("exists", storageDir.exists());
            details.put("writable", storageDir.canWrite());

            if (storageDir.exists() && storageDir.canWrite()) {
                return ComponentHealth.builder()
                        .status("UP")
                        .message("Local filesystem storage is available")
                        .details(details)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            } else {
                return ComponentHealth.builder()
                        .status("DEGRADED")
                        .message("Local storage directory may have issues")
                        .details(details)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }
        } catch (Exception e) {
            return ComponentHealth.builder()
                    .status("DOWN")
                    .message("Storage health check failed: " + e.getMessage())
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private ComponentHealth checkMemoryHealth() {
        long startTime = System.currentTimeMillis();

        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long heapCommitted = memoryBean.getHeapMemoryUsage().getCommitted();

            double usagePercent = heapMax > 0 ? (double) heapUsed / heapMax : 0;

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("heapUsedMb", heapUsed / 1024 / 1024);
            details.put("heapMaxMb", heapMax / 1024 / 1024);
            details.put("heapCommittedMb", heapCommitted / 1024 / 1024);
            details.put("usagePercent", Math.round(usagePercent * 100));

            String status;
            String message;

            if (usagePercent >= MEMORY_WARNING_THRESHOLD) {
                status = "DEGRADED";
                message = String.format("Memory usage is high: %.1f%%", usagePercent * 100);
            } else {
                status = "UP";
                message = String.format("Memory usage is healthy: %.1f%%", usagePercent * 100);
            }

            return ComponentHealth.builder()
                    .status(status)
                    .message(message)
                    .details(details)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            return ComponentHealth.builder()
                    .status("UNKNOWN")
                    .message("Failed to check memory: " + e.getMessage())
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private ComponentHealth checkDiskSpaceHealth() {
        long startTime = System.currentTimeMillis();

        try {
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usableSpace = root.getUsableSpace();

            double usagePercent = totalSpace > 0 ? (double) (totalSpace - freeSpace) / totalSpace : 0;
            long freeSpaceMb = freeSpace / 1024 / 1024;

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("totalSpaceGb", totalSpace / 1024 / 1024 / 1024);
            details.put("freeSpaceGb", freeSpace / 1024 / 1024 / 1024);
            details.put("usableSpaceGb", usableSpace / 1024 / 1024 / 1024);
            details.put("usagePercent", Math.round(usagePercent * 100));

            String status;
            String message;

            if (freeSpaceMb < DISK_CRITICAL_THRESHOLD_MB) {
                status = "DOWN";
                message = String.format("Critical: Only %d MB free disk space", freeSpaceMb);
            } else if (usagePercent >= DISK_WARNING_THRESHOLD) {
                status = "DEGRADED";
                message = String.format("Disk usage is high: %.1f%%", usagePercent * 100);
            } else {
                status = "UP";
                message = String.format("Disk usage is healthy: %.1f%%", usagePercent * 100);
            }

            return ComponentHealth.builder()
                    .status(status)
                    .message(message)
                    .details(details)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            return ComponentHealth.builder()
                    .status("UNKNOWN")
                    .message("Failed to check disk space: " + e.getMessage())
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private ComponentHealth checkCpuHealth() {
        long startTime = System.currentTimeMillis();

        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            int availableProcessors = osBean.getAvailableProcessors();
            double systemLoadAverage = osBean.getSystemLoadAverage();

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("availableProcessors", availableProcessors);
            details.put("systemLoadAverage", systemLoadAverage >= 0 ? Math.round(systemLoadAverage * 100.0) / 100.0 : -1);

            // Try to get more detailed CPU info if available (Sun/Oracle JVM)
            double cpuUsage = -1;
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
                cpuUsage = sunOsBean.getCpuLoad();
                if (cpuUsage >= 0) {
                    details.put("cpuUsagePercent", Math.round(cpuUsage * 100));
                }
                details.put("processCpuLoad", Math.round(sunOsBean.getProcessCpuLoad() * 100));
            }

            // Calculate load per processor
            double loadPerProcessor = systemLoadAverage >= 0 ? systemLoadAverage / availableProcessors : -1;
            if (loadPerProcessor >= 0) {
                details.put("loadPerProcessor", Math.round(loadPerProcessor * 100.0) / 100.0);
            }

            String status;
            String message;

            // Determine status based on CPU usage or load average
            double effectiveCpuUsage = cpuUsage >= 0 ? cpuUsage : loadPerProcessor;

            if (effectiveCpuUsage < 0) {
                status = "UP";
                message = String.format("CPU monitoring available (%d processors)", availableProcessors);
            } else if (effectiveCpuUsage >= CPU_CRITICAL_THRESHOLD) {
                status = "DOWN";
                message = String.format("Critical: CPU usage is extremely high: %.1f%%", effectiveCpuUsage * 100);
            } else if (effectiveCpuUsage >= CPU_WARNING_THRESHOLD) {
                status = "DEGRADED";
                message = String.format("CPU usage is high: %.1f%%", effectiveCpuUsage * 100);
            } else {
                status = "UP";
                message = String.format("CPU usage is healthy: %.1f%%", effectiveCpuUsage * 100);
            }

            return ComponentHealth.builder()
                    .status(status)
                    .message(message)
                    .details(details)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            return ComponentHealth.builder()
                    .status("UNKNOWN")
                    .message("Failed to check CPU: " + e.getMessage())
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private ComponentHealth checkSecretsHealth() {
        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> details = new LinkedHashMap<>();
            java.util.List<String> missingRequired = new java.util.ArrayList<>();
            java.util.List<String> missingOptional = new java.util.ArrayList<>();
            java.util.List<String> configuredSecrets = new java.util.ArrayList<>();
            int warningCount = 0;

            // Check required secrets based on profile
            boolean isProd = "prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile);
            boolean isStaging = "staging".equalsIgnoreCase(activeProfile);

            // JWT Secret - always required, check for default value
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                missingRequired.add("JWT_SECRET");
            } else if (jwtSecret.contains("dev-only") || jwtSecret.contains("default") || jwtSecret.length() < 32) {
                if (isProd || isStaging) {
                    missingRequired.add("JWT_SECRET (using insecure default)");
                } else {
                    warningCount++;
                    configuredSecrets.add("JWT_SECRET (dev default)");
                }
            } else {
                configuredSecrets.add("JWT_SECRET");
            }

            // Database credentials - always required
            if (dbUrl == null || dbUrl.isEmpty()) {
                missingRequired.add("DB_URL");
            } else {
                configuredSecrets.add("DB_URL");
            }

            if (dbUsername == null || dbUsername.isEmpty()) {
                missingRequired.add("DB_USERNAME");
            } else {
                configuredSecrets.add("DB_USERNAME");
            }

            if (dbPassword == null || dbPassword.isEmpty()) {
                missingRequired.add("DB_PASSWORD");
            } else {
                configuredSecrets.add("DB_PASSWORD");
            }

            // CORS - required in prod/staging
            if (isProd || isStaging) {
                if (corsAllowedOrigins == null || corsAllowedOrigins.isEmpty() || corsAllowedOrigins.contains("localhost")) {
                    if (corsAllowedOrigins != null && corsAllowedOrigins.contains("localhost")) {
                        warningCount++;
                        configuredSecrets.add("CORS_ALLOWED_ORIGINS (contains localhost)");
                    } else {
                        missingRequired.add("CORS_ALLOWED_ORIGINS");
                    }
                } else {
                    configuredSecrets.add("CORS_ALLOWED_ORIGINS");
                }
            }

            // Cloud Storage - check if either Azure or AWS is configured
            boolean hasCloudStorage = false;
            if (azureStorageConnectionString != null && !azureStorageConnectionString.isEmpty()) {
                configuredSecrets.add("AZURE_STORAGE_CONNECTION_STRING");
                hasCloudStorage = true;
            }
            if (awsRegion != null && !awsRegion.isEmpty()) {
                configuredSecrets.add("AWS_REGION");
                hasCloudStorage = true;
            }
            if (!hasCloudStorage && (isProd || isStaging)) {
                missingOptional.add("Cloud Storage (AZURE_STORAGE_CONNECTION_STRING or AWS_REGION)");
            }

            // SIEM - only required if enabled
            if (siemEnabled) {
                if (siemWebhookUrl == null || siemWebhookUrl.isEmpty()) {
                    missingRequired.add("SIEM_WEBHOOK_URL (SIEM is enabled)");
                } else {
                    configuredSecrets.add("SIEM_WEBHOOK_URL");
                }
            }

            // Build details
            details.put("profile", activeProfile);
            details.put("configuredCount", configuredSecrets.size());
            details.put("missingRequiredCount", missingRequired.size());
            details.put("missingOptionalCount", missingOptional.size());
            details.put("warningCount", warningCount);

            if (!configuredSecrets.isEmpty()) {
                details.put("configured", configuredSecrets);
            }
            if (!missingRequired.isEmpty()) {
                details.put("missingRequired", missingRequired);
            }
            if (!missingOptional.isEmpty()) {
                details.put("missingOptional", missingOptional);
            }

            // Determine status
            String status;
            String message;

            if (!missingRequired.isEmpty()) {
                status = "DOWN";
                message = String.format("Missing %d required configuration(s): %s",
                        missingRequired.size(), String.join(", ", missingRequired));
            } else if (warningCount > 0 || !missingOptional.isEmpty()) {
                status = "DEGRADED";
                if (warningCount > 0) {
                    message = String.format("Configuration has %d warning(s)", warningCount);
                } else {
                    message = String.format("Missing %d optional configuration(s)", missingOptional.size());
                }
            } else {
                status = "UP";
                message = String.format("All %d required secrets/configurations are properly set", configuredSecrets.size());
            }

            return ComponentHealth.builder()
                    .status(status)
                    .message(message)
                    .details(details)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            return ComponentHealth.builder()
                    .status("UNKNOWN")
                    .message("Failed to check secrets: " + e.getMessage())
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private ComponentHealth checkOscalLibraryHealth() {
        long startTime = System.currentTimeMillis();

        try {
            // Try to instantiate the OSCAL binding context
            OscalBindingContext context = OscalBindingContext.instance();

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("bindingContextAvailable", context != null);
            details.put("library", "liboscal-java");

            if (context != null) {
                return ComponentHealth.builder()
                        .status("UP")
                        .message("OSCAL library is available and functional")
                        .details(details)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            } else {
                return ComponentHealth.builder()
                        .status("DOWN")
                        .message("OSCAL binding context is not available")
                        .details(details)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }
        } catch (Exception e) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("error", e.getClass().getSimpleName());
            details.put("errorMessage", e.getMessage());

            return ComponentHealth.builder()
                    .status("DOWN")
                    .message("OSCAL library check failed: " + e.getMessage())
                    .details(details)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private ComponentHealth checkClamavHealth() {
        long startTime = System.currentTimeMillis();

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("enabled", fileValidationConfig.isEnableVirusScanning());

        // If malware scanning is disabled, report as UNKNOWN (not applicable)
        if (!fileValidationConfig.isEnableVirusScanning()) {
            details.put("status", "disabled");
            return ComponentHealth.builder()
                    .status("UNKNOWN")
                    .message("Malware scanning is disabled")
                    .details(details)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // Malware scanning is enabled - check ClamAV availability
        details.put("host", fileValidationConfig.getClamavHost());
        details.put("port", fileValidationConfig.getClamavPort());
        details.put("failOpen", fileValidationConfig.isClamavFailOpen());

        try {
            if (clamAvScannerService == null) {
                return ComponentHealth.builder()
                        .status("DOWN")
                        .message("ClamAV scanner service is not available")
                        .details(details)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            boolean available = clamAvScannerService.isAvailable();
            String version = null;

            if (available) {
                try {
                    version = clamAvScannerService.getVersion();
                    details.put("version", version);
                } catch (Exception e) {
                    logger.debug("Could not retrieve ClamAV version: {}", e.getMessage());
                }
            }

            details.put("connected", available);

            if (available) {
                return ComponentHealth.builder()
                        .status("UP")
                        .message("ClamAV is available" + (version != null ? " (" + version + ")" : ""))
                        .details(details)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            } else {
                // If fail-open is enabled, report as DEGRADED instead of DOWN
                String status = fileValidationConfig.isClamavFailOpen() ? "DEGRADED" : "DOWN";
                String message = fileValidationConfig.isClamavFailOpen()
                        ? "ClamAV unavailable (fail-open mode - uploads allowed without scanning)"
                        : "ClamAV unavailable (fail-closed mode - uploads will be rejected)";

                return ComponentHealth.builder()
                        .status(status)
                        .message(message)
                        .details(details)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }
        } catch (Exception e) {
            details.put("error", e.getClass().getSimpleName());
            details.put("errorMessage", e.getMessage());

            return ComponentHealth.builder()
                    .status("DOWN")
                    .message("ClamAV health check failed: " + e.getMessage())
                    .details(details)
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private SystemInfo buildSystemInfo() {
        SystemInfo info = new SystemInfo();

        // Memory info
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();

        info.setTotalMemoryMb(heapMax / 1024 / 1024);
        info.setUsedMemoryMb(heapUsed / 1024 / 1024);
        info.setFreeMemoryMb((heapMax - heapUsed) / 1024 / 1024);
        info.setMemoryUsagePercent(heapMax > 0 ? Math.round((double) heapUsed / heapMax * 100) : 0);

        // CPU info
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        info.setAvailableProcessors(osBean.getAvailableProcessors());
        info.setSystemLoadAverage(osBean.getSystemLoadAverage());

        // Disk info
        File root = new File("/");
        info.setTotalDiskSpaceGb(root.getTotalSpace() / 1024 / 1024 / 1024);
        info.setFreeDiskSpaceGb(root.getFreeSpace() / 1024 / 1024 / 1024);
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        info.setDiskUsagePercent(totalSpace > 0 ? Math.round((double) (totalSpace - freeSpace) / totalSpace * 100) : 0);

        return info;
    }

    private EnvironmentInfo buildEnvironmentInfo() {
        EnvironmentInfo info = new EnvironmentInfo();

        info.setJavaVersion(System.getProperty("java.version"));
        info.setJavaVendor(System.getProperty("java.vendor"));
        info.setOsName(System.getProperty("os.name"));
        info.setOsVersion(System.getProperty("os.version"));
        info.setOsArch(System.getProperty("os.arch"));
        info.setTimezone(TimeZone.getDefault().getID());

        return info;
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}
