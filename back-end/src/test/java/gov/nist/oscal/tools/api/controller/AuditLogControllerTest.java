package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.AuditEvent;
import gov.nist.oscal.tools.api.model.AuditEventType;
import gov.nist.oscal.tools.api.repository.AuditEventRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.service.SiemForwardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

@WebMvcTest(AuditLogController.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuditEventRepository auditEventRepository;

    @MockitoBean
    private SiemForwardingService siemForwardingService;

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

    private AuditEvent testEvent1;
    private AuditEvent testEvent2;

    @BeforeEach
    void setUp() {
        testEvent1 = new AuditEvent(AuditEventType.AUTH_LOGIN_SUCCESS, "testuser", "SUCCESS");
        testEvent1.setId(1L);
        testEvent1.setIpAddress("192.168.1.1");
        testEvent1.setTimestamp(LocalDateTime.now());

        testEvent2 = new AuditEvent(AuditEventType.DATA_FILE_UPLOAD, "admin", "SUCCESS");
        testEvent2.setId(2L);
        testEvent2.setIpAddress("192.168.1.2");
        testEvent2.setResource("test-file.json");
        testEvent2.setTimestamp(LocalDateTime.now());
    }

    // ========================================
    // Get All Logs Tests
    // ========================================

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetAllLogs_superAdmin_success() throws Exception {
        Page<AuditEvent> page = new PageImpl<>(Arrays.asList(testEvent1, testEvent2));
        when(auditEventRepository.findAllByOrderByTimestampDesc(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/logs")
                .param("page", "0")
                .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].eventType").value("AUTH_LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.content[1].eventType").value("DATA_FILE_UPLOAD"));

        verify(auditEventRepository, times(1)).findAllByOrderByTimestampDesc(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetAllLogs_withUsernameFilter_returnsFilteredResults() throws Exception {
        Page<AuditEvent> page = new PageImpl<>(Arrays.asList(testEvent1));
        when(auditEventRepository.findByUsernameOrderByTimestampDesc(eq("testuser"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/logs")
                .param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username").value("testuser"));

        verify(auditEventRepository, times(1)).findByUsernameOrderByTimestampDesc(eq("testuser"), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetAllLogs_withRiskLevelFilter_returnsFilteredResults() throws Exception {
        testEvent1.setRiskLevel("HIGH");
        Page<AuditEvent> page = new PageImpl<>(Arrays.asList(testEvent1));
        when(auditEventRepository.findByRiskLevelOrderByTimestampDesc(eq("HIGH"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/logs")
                .param("riskLevel", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(auditEventRepository, times(1)).findByRiskLevelOrderByTimestampDesc(eq("HIGH"), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "regularuser")
    void testGetAllLogs_nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/logs"))
                .andExpect(status().isForbidden());

        verify(auditEventRepository, never()).findAllByOrderByTimestampDesc(any(Pageable.class));
    }

    @Test
    void testGetAllLogs_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/logs"))
                .andExpect(status().isUnauthorized());

        verify(auditEventRepository, never()).findAllByOrderByTimestampDesc(any(Pageable.class));
    }

    // ========================================
    // Raw Logs Tests
    // ========================================

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetRawLogs_superAdmin_success() throws Exception {
        Page<AuditEvent> page = new PageImpl<>(Arrays.asList(testEvent1));
        when(auditEventRepository.findRawLogs(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/logs/raw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(auditEventRepository, times(1)).findRawLogs(any(Pageable.class));
    }

    // ========================================
    // Security Logs Tests
    // ========================================

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetSecurityLogs_superAdmin_success() throws Exception {
        Page<AuditEvent> page = new PageImpl<>(Arrays.asList(testEvent1));
        when(auditEventRepository.findAllSecurityEvents(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/logs/security"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(auditEventRepository, times(1)).findAllSecurityEvents(any(Pageable.class));
    }

    // ========================================
    // Error Logs Tests
    // ========================================

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetErrorLogs_superAdmin_success() throws Exception {
        AuditEvent errorEvent = new AuditEvent(AuditEventType.SYSTEM_ERROR, "system", "ERROR");
        errorEvent.setId(3L);
        errorEvent.setErrorMessage("Something went wrong");

        Page<AuditEvent> page = new PageImpl<>(Arrays.asList(errorEvent));
        when(auditEventRepository.findAllFailedEvents(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/logs/errors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].outcome").value("ERROR"));

        verify(auditEventRepository, times(1)).findAllFailedEvents(any(Pageable.class));
    }

    // ========================================
    // Search Tests
    // ========================================

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testSearchLogs_superAdmin_success() throws Exception {
        Page<AuditEvent> page = new PageImpl<>(Arrays.asList(testEvent1));
        when(auditEventRepository.searchByKeyword(eq("testuser"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/logs/search")
                .param("q", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(auditEventRepository, times(1)).searchByKeyword(eq("testuser"), any(Pageable.class));
    }

    // ========================================
    // Get by ID Tests
    // ========================================

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetLogById_found_returnsLog() throws Exception {
        when(auditEventRepository.findById(1L)).thenReturn(Optional.of(testEvent1));

        mockMvc.perform(get("/api/admin/logs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.eventType").value("AUTH_LOGIN_SUCCESS"));

        verify(auditEventRepository, times(1)).findById(1L);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetLogById_notFound_returns404() throws Exception {
        when(auditEventRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/logs/999"))
                .andExpect(status().isNotFound());

        verify(auditEventRepository, times(1)).findById(999L);
    }

    // ========================================
    // Statistics Tests
    // ========================================

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetStats_superAdmin_success() throws Exception {
        when(auditEventRepository.count()).thenReturn(1000L);
        when(auditEventRepository.countEventsSince(any(LocalDateTime.class))).thenReturn(50L);
        when(auditEventRepository.countSecurityEventsSince(any(LocalDateTime.class))).thenReturn(10L);
        when(auditEventRepository.countErrorsSince(any(LocalDateTime.class))).thenReturn(5L);
        when(auditEventRepository.countUnreviewedHighRiskEvents()).thenReturn(3L);
        when(auditEventRepository.countByCategory(anyString())).thenReturn(100L);
        when(auditEventRepository.countByRiskLevel(anyString())).thenReturn(200L);
        when(auditEventRepository.countByOutcome(anyString())).thenReturn(300L);

        mockMvc.perform(get("/api/admin/logs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogs").value(1000))
                .andExpect(jsonPath("$.logsToday").value(50))
                .andExpect(jsonPath("$.securityEventsToday").value(10))
                .andExpect(jsonPath("$.errorsToday").value(5))
                .andExpect(jsonPath("$.highRiskUnreviewed").value(3));

        verify(auditEventRepository, times(1)).count();
    }

    // ========================================
    // Export Tests
    // ========================================

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testExportCsv_superAdmin_success() throws Exception {
        Page<AuditEvent> page = new PageImpl<>(Arrays.asList(testEvent1, testEvent2));
        when(auditEventRepository.findAllByOrderByTimestampDesc(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/logs/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("audit-logs-")));

        verify(auditEventRepository, atLeastOnce()).findAllByOrderByTimestampDesc(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testExportJson_superAdmin_success() throws Exception {
        Page<AuditEvent> page = new PageImpl<>(Arrays.asList(testEvent1, testEvent2));
        when(auditEventRepository.findAllByOrderByTimestampDesc(any(Pageable.class))).thenReturn(page);

        // exportJson returns a StreamingResponseBody. Spring MVC starts the
        // response asynchronously, so we have to dispatch the async result
        // before the StreamingResponseBody body actually runs and the mock
        // is invoked. Without asyncDispatch the verify() below would race.
        var mvcResult = mockMvc.perform(get("/api/admin/logs/export/json"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/x-ndjson"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("audit-logs-")));

        verify(auditEventRepository, atLeastOnce()).findAllByOrderByTimestampDesc(any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "regularuser")
    void testExportCsv_nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/logs/export/csv"))
                .andExpect(status().isForbidden());

        verify(auditEventRepository, never()).findAllByOrderByTimestampDesc(any(Pageable.class));
    }

    // ========================================
    // SIEM Integration Tests
    // ========================================

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetSiemStatus_superAdmin_success() throws Exception {
        SiemForwardingService.SiemStatus status = new SiemForwardingService.SiemStatus(
                true, true, 1000L, 5L, 50L, 1L, 10,
                LocalDateTime.now(), null, null, "***configured***", "json"
        );
        when(siemForwardingService.getStatus()).thenReturn(status);

        mockMvc.perform(get("/api/admin/logs/siem/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.healthy").value(true))
                .andExpect(jsonPath("$.eventsForwarded").value(1000))
                .andExpect(jsonPath("$.format").value("json"));

        verify(siemForwardingService, times(1)).getStatus();
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testTestSiemConnection_success_returns200() throws Exception {
        SiemForwardingService.TestResult result = new SiemForwardingService.TestResult(true, "Connection successful");
        when(siemForwardingService.testConnection()).thenReturn(result);

        mockMvc.perform(post("/api/admin/logs/siem/test")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Connection successful"));

        verify(siemForwardingService, times(1)).testConnection();
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testTestSiemConnection_failure_returns503() throws Exception {
        SiemForwardingService.TestResult result = new SiemForwardingService.TestResult(false, "Connection refused");
        when(siemForwardingService.testConnection()).thenReturn(result);

        mockMvc.perform(post("/api/admin/logs/siem/test")
                .with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Connection refused"));

        verify(siemForwardingService, times(1)).testConnection();
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testFlushSiemQueue_success() throws Exception {
        when(siemForwardingService.manualFlush()).thenReturn(25);

        mockMvc.perform(post("/api/admin/logs/siem/flush")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flushedEvents").value(25))
                .andExpect(jsonPath("$.message").value("Events flushed successfully"));

        verify(siemForwardingService, times(1)).manualFlush();
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testFlushSiemQueue_emptyQueue() throws Exception {
        when(siemForwardingService.manualFlush()).thenReturn(0);

        mockMvc.perform(post("/api/admin/logs/siem/flush")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flushedEvents").value(0))
                .andExpect(jsonPath("$.message").value("No events to flush"));

        verify(siemForwardingService, times(1)).manualFlush();
    }

    @Test
    @WithMockUser(username = "regularuser")
    void testSiemEndpoints_nonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/logs/siem/status"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/logs/siem/test")
                .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/logs/siem/flush")
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(siemForwardingService, never()).getStatus();
        verify(siemForwardingService, never()).testConnection();
        verify(siemForwardingService, never()).manualFlush();
    }

    // ========================================
    // Page Size Limit Tests
    // ========================================

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testGetAllLogs_pageSizeExceedsMax_cappedAt100() throws Exception {
        Page<AuditEvent> page = new PageImpl<>(Arrays.asList(testEvent1));
        when(auditEventRepository.findAllByOrderByTimestampDesc(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/logs")
                .param("page", "0")
                .param("size", "500"))  // Request 500, should be capped at 100
                .andExpect(status().isOk());

        // Verify that a pageable with size 100 was used (not 500)
        verify(auditEventRepository, times(1)).findAllByOrderByTimestampDesc(argThat(pageable ->
            pageable.getPageSize() == 100
        ));
    }
}
