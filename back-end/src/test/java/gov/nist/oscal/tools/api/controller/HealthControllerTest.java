/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.model.health.ComponentHealth;
import gov.nist.oscal.tools.api.model.health.DetailedHealthResponse;
import gov.nist.oscal.tools.api.model.health.SimpleHealthResponse;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.HealthCheckService;
import gov.nist.oscal.tools.api.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HealthCheckService healthCheckService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    @MockitoBean
    private SecurityHeadersConfig securityHeadersConfig;

    // ========== GET /api/health Tests ==========

    @Test
    @WithMockUser(username = "testuser")
    void testHealth_returnsSimpleHealthResponse() throws Exception {
        // Given
        SimpleHealthResponse response = new SimpleHealthResponse("UP", "2025-02-16T10:30:00Z", "1.0.0");

        when(healthCheckService.getSimpleHealth()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(healthCheckService).getSimpleHealth();
    }

    @Test
    @WithMockUser(username = "testuser")
    void testHealth_whenDown_returnsDownStatus() throws Exception {
        // Given
        SimpleHealthResponse response = new SimpleHealthResponse("DOWN", "2025-02-16T10:30:00Z", "1.0.0");

        when(healthCheckService.getSimpleHealth()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"));

        verify(healthCheckService).getSimpleHealth();
    }

    // ========== GET /api/health/ping Tests ==========

    @Test
    @WithMockUser(username = "testuser")
    void testPing_whenHealthy_returnsOk() throws Exception {
        // Given
        when(healthCheckService.isHealthy()).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/health/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        verify(healthCheckService).isHealthy();
    }

    @Test
    @WithMockUser(username = "testuser")
    void testPing_whenUnhealthy_returns503() throws Exception {
        // Given
        when(healthCheckService.isHealthy()).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/health/ping"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("UNHEALTHY"));

        verify(healthCheckService).isHealthy();
    }

    // ========== GET /api/health/detailed Tests ==========

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testDetailedHealth_withSuperAdmin_returnsDetailedResponse() throws Exception {
        // Given
        DetailedHealthResponse response = createMockDetailedHealthResponse();

        when(healthCheckService.getDetailedHealth()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/health/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application.name").value("oscal-cli-api"))
                .andExpect(jsonPath("$.application.version").value("1.0.0"))
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.memory.status").value("UP"))
                .andExpect(jsonPath("$.system.availableProcessors").value(8))
                .andExpect(jsonPath("$.environment.javaVersion").value("17.0.2"));

        verify(healthCheckService).getDetailedHealth();
    }

    @Test
    @WithMockUser(username = "regularuser")
    void testDetailedHealth_withoutSuperAdmin_returnsForbidden() throws Exception {
        // When & Then - regular user without SUPER_ADMIN role should get 403
        mockMvc.perform(get("/api/health/detailed"))
                .andExpect(status().isForbidden());

        verify(healthCheckService, never()).getDetailedHealth();
    }

    // ========== GET /api/health/component/{component} Tests ==========

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_database_returnsHealth() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .message("Database connection is healthy")
                .details(Map.of("database", "PostgreSQL", "version", "15.0"))
                .responseTimeMs(15L)
                .build();

        when(healthCheckService.getComponentHealth("database")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Database connection is healthy"))
                .andExpect(jsonPath("$.details.database").value("PostgreSQL"))
                .andExpect(jsonPath("$.responseTimeMs").value(15));

        verify(healthCheckService).getComponentHealth("database");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_memory_returnsHealth() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .message("Memory usage is healthy: 45.0%")
                .details(Map.of("heapUsedMb", 450, "heapMaxMb", 1024, "usagePercent", 45))
                .responseTimeMs(1L)
                .build();

        when(healthCheckService.getComponentHealth("memory")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/memory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details.usagePercent").value(45));

        verify(healthCheckService).getComponentHealth("memory");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_storage_returnsHealth() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .message("Local filesystem storage is available")
                .details(Map.of("provider", "local_filesystem", "writable", true))
                .responseTimeMs(2L)
                .build();

        when(healthCheckService.getComponentHealth("storage")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/storage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details.provider").value("local_filesystem"));

        verify(healthCheckService).getComponentHealth("storage");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_diskSpace_returnsHealth() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .message("Disk usage is healthy: 35.0%")
                .details(Map.of("totalSpaceGb", 500, "freeSpaceGb", 325, "usagePercent", 35))
                .responseTimeMs(3L)
                .build();

        when(healthCheckService.getComponentHealth("diskspace")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/diskspace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details.freeSpaceGb").value(325));

        verify(healthCheckService).getComponentHealth("diskspace");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_oscalLibrary_returnsHealth() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .message("OSCAL library is available and functional")
                .details(Map.of("bindingContextAvailable", true, "library", "liboscal-java"))
                .responseTimeMs(50L)
                .build();

        when(healthCheckService.getComponentHealth("oscal")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/oscal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details.library").value("liboscal-java"));

        verify(healthCheckService).getComponentHealth("oscal");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_unknownComponent_returnsUnknown() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("UNKNOWN")
                .message("Unknown component: foo")
                .build();

        when(healthCheckService.getComponentHealth("foo")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.message").value("Unknown component: foo"));

        verify(healthCheckService).getComponentHealth("foo");
    }

    @Test
    @WithMockUser(username = "regularuser")
    void testComponentHealth_withoutSuperAdmin_returnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/health/component/database"))
                .andExpect(status().isForbidden());

        verify(healthCheckService, never()).getComponentHealth(anyString());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_degradedStatus() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("DEGRADED")
                .message("Memory usage is high: 92.0%")
                .details(Map.of("heapUsedMb", 920, "heapMaxMb", 1024, "usagePercent", 92))
                .responseTimeMs(1L)
                .build();

        when(healthCheckService.getComponentHealth("memory")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/memory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.details.usagePercent").value(92));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_downStatus() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("DOWN")
                .message("Database connection failed: Connection refused")
                .details(Map.of("error", "SQLException", "errorMessage", "Connection refused"))
                .responseTimeMs(5000L)
                .build();

        when(healthCheckService.getComponentHealth("database")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.details.error").value("SQLException"));
    }

    // ========== CPU Component Health Tests ==========

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_cpu_returnsHealth() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .message("CPU usage is healthy: 45.0%")
                .details(Map.of("availableProcessors", 8, "systemLoadAverage", 1.5, "cpuUsagePercent", 45))
                .responseTimeMs(2L)
                .build();

        when(healthCheckService.getComponentHealth("cpu")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/cpu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("CPU usage is healthy: 45.0%"))
                .andExpect(jsonPath("$.details.availableProcessors").value(8))
                .andExpect(jsonPath("$.details.cpuUsagePercent").value(45));

        verify(healthCheckService).getComponentHealth("cpu");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_cpu_degradedStatus() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("DEGRADED")
                .message("CPU usage is high: 85.0%")
                .details(Map.of("availableProcessors", 4, "systemLoadAverage", 3.4, "cpuUsagePercent", 85))
                .responseTimeMs(2L)
                .build();

        when(healthCheckService.getComponentHealth("cpu")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/cpu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.details.cpuUsagePercent").value(85));
    }

    // ========== Secrets Component Health Tests ==========

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_secrets_allConfigured() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .message("All 5 required secrets/configurations are properly set")
                .details(Map.of(
                        "profile", "dev",
                        "configuredCount", 5,
                        "missingRequiredCount", 0,
                        "missingOptionalCount", 0
                ))
                .responseTimeMs(1L)
                .build();

        when(healthCheckService.getComponentHealth("secrets")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/secrets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details.configuredCount").value(5))
                .andExpect(jsonPath("$.details.missingRequiredCount").value(0));

        verify(healthCheckService).getComponentHealth("secrets");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_secrets_missingRequired() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("DOWN")
                .message("Missing 2 required configuration(s): JWT_SECRET, DB_PASSWORD")
                .details(Map.of(
                        "profile", "prod",
                        "configuredCount", 3,
                        "missingRequiredCount", 2,
                        "missingOptionalCount", 0
                ))
                .responseTimeMs(1L)
                .build();

        when(healthCheckService.getComponentHealth("secrets")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/secrets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.details.missingRequiredCount").value(2));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_secrets_degradedWithWarnings() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("DEGRADED")
                .message("Configuration has 1 warning(s)")
                .details(Map.of(
                        "profile", "dev",
                        "configuredCount", 5,
                        "missingRequiredCount", 0,
                        "warningCount", 1
                ))
                .responseTimeMs(1L)
                .build();

        when(healthCheckService.getComponentHealth("secrets")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/secrets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.details.warningCount").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    void testComponentHealth_config_aliasWorksForSecrets() throws Exception {
        // Given
        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .message("All configurations are properly set")
                .details(Map.of("profile", "dev", "configuredCount", 5))
                .responseTimeMs(1L)
                .build();

        when(healthCheckService.getComponentHealth("config")).thenReturn(health);

        // When & Then
        mockMvc.perform(get("/api/health/component/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        verify(healthCheckService).getComponentHealth("config");
    }

    // ========== Helper Methods ==========

    private DetailedHealthResponse createMockDetailedHealthResponse() {
        DetailedHealthResponse response = new DetailedHealthResponse();
        response.setStatus("UP");
        response.setTimestamp("2025-02-16T10:30:00Z");

        // Application info
        DetailedHealthResponse.ApplicationInfo appInfo = new DetailedHealthResponse.ApplicationInfo();
        appInfo.setName("oscal-cli-api");
        appInfo.setVersion("1.0.0");
        appInfo.setProfile("test");
        appInfo.setUptime("2d 5h 30m 15s");
        appInfo.setStartTime("2025-02-14T05:00:00Z");
        response.setApplication(appInfo);

        // Components
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("database", ComponentHealth.builder()
                .status("UP")
                .message("Database connection is healthy")
                .responseTimeMs(15L)
                .build());
        components.put("storage", ComponentHealth.builder()
                .status("UP")
                .message("Local filesystem storage is available")
                .responseTimeMs(2L)
                .build());
        components.put("memory", ComponentHealth.builder()
                .status("UP")
                .message("Memory usage is healthy: 45.0%")
                .responseTimeMs(1L)
                .build());
        components.put("cpu", ComponentHealth.builder()
                .status("UP")
                .message("CPU usage is healthy: 30.0%")
                .responseTimeMs(1L)
                .build());
        components.put("diskSpace", ComponentHealth.builder()
                .status("UP")
                .message("Disk usage is healthy: 35.0%")
                .responseTimeMs(3L)
                .build());
        components.put("oscalLibrary", ComponentHealth.builder()
                .status("UP")
                .message("OSCAL library is available and functional")
                .responseTimeMs(50L)
                .build());
        components.put("secrets", ComponentHealth.builder()
                .status("UP")
                .message("All required secrets/configurations are properly set")
                .responseTimeMs(1L)
                .build());
        response.setComponents(components);

        // System info
        DetailedHealthResponse.SystemInfo sysInfo = new DetailedHealthResponse.SystemInfo();
        sysInfo.setTotalMemoryMb(1024);
        sysInfo.setUsedMemoryMb(450);
        sysInfo.setFreeMemoryMb(574);
        sysInfo.setMemoryUsagePercent(45);
        sysInfo.setAvailableProcessors(8);
        sysInfo.setSystemLoadAverage(1.5);
        sysInfo.setTotalDiskSpaceGb(500);
        sysInfo.setFreeDiskSpaceGb(325);
        sysInfo.setDiskUsagePercent(35);
        response.setSystem(sysInfo);

        // Environment info
        DetailedHealthResponse.EnvironmentInfo envInfo = new DetailedHealthResponse.EnvironmentInfo();
        envInfo.setJavaVersion("17.0.2");
        envInfo.setJavaVendor("Eclipse Adoptium");
        envInfo.setOsName("Linux");
        envInfo.setOsVersion("5.15.0");
        envInfo.setOsArch("amd64");
        envInfo.setTimezone("America/New_York");
        response.setEnvironment(envInfo);

        return response;
    }
}
