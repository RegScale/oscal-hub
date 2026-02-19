package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.AuditEvent;
import gov.nist.oscal.tools.api.model.AuditEventType;
import gov.nist.oscal.tools.api.model.AuditLogStats;
import gov.nist.oscal.tools.api.repository.AuditEventRepository;
import gov.nist.oscal.tools.api.service.SiemForwardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Admin Audit Log Operations.
 * Provides endpoints for viewing, searching, filtering, and exporting audit logs.
 * All endpoints are restricted to SUPER_ADMIN users only.
 */
@RestController
@RequestMapping("/api/admin/logs")
@Tag(name = "Admin Audit Logs", description = "Audit log viewing and management for administrators")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AuditLogController {

    private final AuditEventRepository auditEventRepository;
    private final SiemForwardingService siemForwardingService;

    @Autowired
    public AuditLogController(AuditEventRepository auditEventRepository,
                              SiemForwardingService siemForwardingService) {
        this.auditEventRepository = auditEventRepository;
        this.siemForwardingService = siemForwardingService;
    }

    // ========================================
    // Log Retrieval Endpoints
    // ========================================

    @GetMapping
    @Operation(summary = "Get all audit logs", description = "Retrieve all audit logs with pagination and optional filters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved logs"),
        @ApiResponse(responseCode = "403", description = "Access denied - SUPER_ADMIN required")
    })
    public ResponseEntity<Page<AuditEvent>> getAllLogs(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "50") int size,
            @Parameter(description = "Filter by username") @RequestParam(required = false) String username,
            @Parameter(description = "Filter by IP address") @RequestParam(required = false) String ipAddress,
            @Parameter(description = "Filter by risk level") @RequestParam(required = false) String riskLevel,
            @Parameter(description = "Filter by event type") @RequestParam(required = false) String eventType,
            @Parameter(description = "Start date (ISO DateTime)") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO DateTime)") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        size = Math.min(size, 100); // Cap at 100
        Pageable pageable = PageRequest.of(page, size);

        Page<AuditEvent> result;

        // Apply filters in order of specificity
        if (username != null && !username.isBlank()) {
            if (startDate != null && endDate != null) {
                result = auditEventRepository.findByUsernameAndTimestampBetweenOrderByTimestampDesc(
                    username, startDate, endDate, pageable);
            } else {
                result = auditEventRepository.findByUsernameOrderByTimestampDesc(username, pageable);
            }
        } else if (riskLevel != null && !riskLevel.isBlank()) {
            result = auditEventRepository.findByRiskLevelOrderByTimestampDesc(riskLevel, pageable);
        } else if (eventType != null && !eventType.isBlank()) {
            try {
                AuditEventType type = AuditEventType.valueOf(eventType);
                result = auditEventRepository.findByEventTypeOrderByTimestampDesc(type, pageable);
            } catch (IllegalArgumentException e) {
                result = auditEventRepository.findAllByOrderByTimestampDesc(pageable);
            }
        } else if (ipAddress != null && !ipAddress.isBlank()) {
            result = auditEventRepository.findByIpAddressOrderByTimestampDesc(ipAddress, pageable);
        } else if (startDate != null && endDate != null) {
            result = auditEventRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable);
        } else {
            result = auditEventRepository.findAllByOrderByTimestampDesc(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/raw")
    @Operation(summary = "Get raw access logs", description = "Retrieve API access logs (Authentication and Data Access categories)")
    public ResponseEntity<Page<AuditEvent>> getRawLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String riskLevel) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);

        // Apply additional filters if provided
        if (username != null && !username.isBlank()) {
            return ResponseEntity.ok(auditEventRepository.findRawLogsByUsername(username, pageable));
        } else if (riskLevel != null && !riskLevel.isBlank()) {
            return ResponseEntity.ok(auditEventRepository.findRawLogsByRiskLevel(riskLevel, pageable));
        }
        return ResponseEntity.ok(auditEventRepository.findRawLogs(pageable));
    }

    @GetMapping("/security")
    @Operation(summary = "Get security logs", description = "Retrieve security-related logs (Security, Authorization categories and HIGH risk events)")
    public ResponseEntity<Page<AuditEvent>> getSecurityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String riskLevel) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);

        // Apply additional filters if provided
        if (username != null && !username.isBlank()) {
            return ResponseEntity.ok(auditEventRepository.findSecurityEventsByUsername(username, pageable));
        } else if (riskLevel != null && !riskLevel.isBlank()) {
            return ResponseEntity.ok(auditEventRepository.findSecurityEventsByRiskLevel(riskLevel, pageable));
        }
        return ResponseEntity.ok(auditEventRepository.findAllSecurityEvents(pageable));
    }

    @GetMapping("/errors")
    @Operation(summary = "Get error logs", description = "Retrieve logs with FAILURE or ERROR outcomes")
    public ResponseEntity<Page<AuditEvent>> getErrorLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String riskLevel) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);

        // Apply additional filters if provided
        if (username != null && !username.isBlank()) {
            return ResponseEntity.ok(auditEventRepository.findErrorsByUsername(username, pageable));
        } else if (riskLevel != null && !riskLevel.isBlank()) {
            return ResponseEntity.ok(auditEventRepository.findErrorsByRiskLevel(riskLevel, pageable));
        }
        return ResponseEntity.ok(auditEventRepository.findAllFailedEvents(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search logs", description = "Search logs by keyword in username, resource, URL, or error message")
    public ResponseEntity<Page<AuditEvent>> searchLogs(
            @Parameter(description = "Search keyword") @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditEventRepository.searchByKeyword(q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get log by ID", description = "Retrieve a specific audit log entry by ID")
    public ResponseEntity<AuditEvent> getLogById(@PathVariable Long id) {
        return auditEventRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ========================================
    // Statistics Endpoint
    // ========================================

    @GetMapping("/stats")
    @Operation(summary = "Get log statistics", description = "Retrieve summary statistics for audit logs (cached for 1 minute)")
    @Cacheable(value = "auditStats", key = "'stats'")
    public ResponseEntity<AuditLogStats> getStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        // Total counts
        long totalLogs = auditEventRepository.count();
        long logsToday = auditEventRepository.countEventsSince(startOfToday);
        long securityEventsToday = auditEventRepository.countSecurityEventsSince(startOfToday);
        long errorsToday = auditEventRepository.countErrorsSince(startOfToday);
        long highRiskUnreviewed = auditEventRepository.countUnreviewedHighRiskEvents();

        // Breakdown by category
        Map<String, Long> byCategory = new HashMap<>();
        byCategory.put("Authentication", auditEventRepository.countByCategory("Authentication"));
        byCategory.put("Authorization", auditEventRepository.countByCategory("Authorization"));
        byCategory.put("Data Access", auditEventRepository.countByCategory("Data Access"));
        byCategory.put("Configuration", auditEventRepository.countByCategory("Configuration"));
        byCategory.put("Security", auditEventRepository.countByCategory("Security"));
        byCategory.put("System", auditEventRepository.countByCategory("System"));

        // Breakdown by risk level
        Map<String, Long> byRiskLevel = new HashMap<>();
        byRiskLevel.put("LOW", auditEventRepository.countByRiskLevel("LOW"));
        byRiskLevel.put("MEDIUM", auditEventRepository.countByRiskLevel("MEDIUM"));
        byRiskLevel.put("HIGH", auditEventRepository.countByRiskLevel("HIGH"));

        // Breakdown by outcome
        Map<String, Long> byOutcome = new HashMap<>();
        byOutcome.put("SUCCESS", auditEventRepository.countByOutcome("SUCCESS"));
        byOutcome.put("FAILURE", auditEventRepository.countByOutcome("FAILURE"));
        byOutcome.put("ERROR", auditEventRepository.countByOutcome("ERROR"));

        AuditLogStats stats = new AuditLogStats(
            totalLogs, logsToday, securityEventsToday, errorsToday, highRiskUnreviewed,
            byCategory, byRiskLevel, byOutcome
        );

        return ResponseEntity.ok(stats);
    }

    // ========================================
    // Export Endpoints
    // ========================================

    /**
     * Maximum number of records that can be exported at once.
     * This prevents unbounded exports that could cause memory issues.
     */
    private static final int MAX_EXPORT_RECORDS = 100000;

    @GetMapping("/export/csv")
    @Operation(summary = "Export logs to CSV",
               description = "Export audit logs to CSV format with current filters. Maximum 100,000 records. Uses streaming for memory efficiency.")
    public ResponseEntity<StreamingResponseBody> exportCsv(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Maximum records to export (default 100000)")
            @RequestParam(defaultValue = "100000") int maxRecords) {

        final int limit = Math.min(maxRecords, MAX_EXPORT_RECORDS);

        StreamingResponseBody stream = outputStream -> {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            // CSV Header
            writer.println("ID,Timestamp,Event Type,Category,Username,IP Address,Request URL,HTTP Method,Resource,Action,Outcome,Risk Level,Error Message,Processing Time (ms)");

            int page = 0;
            int pageSize = 1000;
            int totalExported = 0;
            Page<AuditEvent> logPage;

            do {
                Pageable pageable = PageRequest.of(page, pageSize);
                logPage = fetchFilteredLogs(username, riskLevel, startDate, endDate, pageable);

                for (AuditEvent event : logPage.getContent()) {
                    if (totalExported >= limit) break;
                    writer.println(formatCsvRow(event));
                    totalExported++;

                    // Flush periodically for streaming
                    if (totalExported % 1000 == 0) {
                        writer.flush();
                    }
                }

                page++;
            } while (logPage.hasNext() && totalExported < limit);

            writer.flush();
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs-" + LocalDate.now() + ".csv\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(stream);
    }

    @GetMapping("/export/json")
    @Operation(summary = "Export logs to JSON Lines",
               description = "Export audit logs to JSON Lines format (SIEM-friendly). Maximum 100,000 records. Uses streaming for memory efficiency.")
    public ResponseEntity<StreamingResponseBody> exportJson(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Maximum records to export (default 100000)")
            @RequestParam(defaultValue = "100000") int maxRecords) {

        final int limit = Math.min(maxRecords, MAX_EXPORT_RECORDS);

        StreamingResponseBody stream = outputStream -> {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            int page = 0;
            int pageSize = 1000;
            int totalExported = 0;
            Page<AuditEvent> logPage;

            do {
                Pageable pageable = PageRequest.of(page, pageSize);
                logPage = fetchFilteredLogs(username, riskLevel, startDate, endDate, pageable);

                for (AuditEvent event : logPage.getContent()) {
                    if (totalExported >= limit) break;
                    writer.println(formatJsonLine(event));
                    totalExported++;

                    // Flush periodically for streaming
                    if (totalExported % 1000 == 0) {
                        writer.flush();
                    }
                }

                page++;
            } while (logPage.hasNext() && totalExported < limit);

            writer.flush();
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-ndjson"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs-" + LocalDate.now() + ".jsonl\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(stream);
    }

    /**
     * Helper method to fetch filtered logs, avoiding code duplication.
     */
    private Page<AuditEvent> fetchFilteredLogs(String username, String riskLevel,
                                                LocalDateTime startDate, LocalDateTime endDate,
                                                Pageable pageable) {
        if (username != null && !username.isBlank()) {
            if (startDate != null && endDate != null) {
                return auditEventRepository.findByUsernameAndTimestampBetweenOrderByTimestampDesc(
                    username, startDate, endDate, pageable);
            } else {
                return auditEventRepository.findByUsernameOrderByTimestampDesc(username, pageable);
            }
        } else if (riskLevel != null && !riskLevel.isBlank()) {
            return auditEventRepository.findByRiskLevelOrderByTimestampDesc(riskLevel, pageable);
        } else if (startDate != null && endDate != null) {
            return auditEventRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable);
        } else {
            return auditEventRepository.findAllByOrderByTimestampDesc(pageable);
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private String formatCsvRow(AuditEvent event) {
        return String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            event.getId(),
            event.getTimestamp(),
            event.getEventType(),
            escapeCsv(event.getCategory()),
            escapeCsv(event.getUsername()),
            escapeCsv(event.getIpAddress()),
            escapeCsv(event.getRequestUrl()),
            escapeCsv(event.getHttpMethod()),
            escapeCsv(event.getResource()),
            escapeCsv(event.getAction()),
            event.getOutcome(),
            event.getRiskLevel(),
            escapeCsv(event.getErrorMessage()),
            event.getProcessingTimeMs() != null ? event.getProcessingTimeMs() : ""
        );
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatJsonLine(AuditEvent event) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\":").append(event.getId()).append(",");
        json.append("\"timestamp\":\"").append(event.getTimestamp()).append("\",");
        json.append("\"eventType\":\"").append(event.getEventType()).append("\",");
        json.append("\"category\":\"").append(escapeJson(event.getCategory())).append("\",");
        json.append("\"username\":").append(event.getUsername() != null ? "\"" + escapeJson(event.getUsername()) + "\"" : "null").append(",");
        json.append("\"userId\":").append(event.getUserId() != null ? event.getUserId() : "null").append(",");
        json.append("\"ipAddress\":").append(event.getIpAddress() != null ? "\"" + escapeJson(event.getIpAddress()) + "\"" : "null").append(",");
        json.append("\"userAgent\":").append(event.getUserAgent() != null ? "\"" + escapeJson(event.getUserAgent()) + "\"" : "null").append(",");
        json.append("\"requestUrl\":").append(event.getRequestUrl() != null ? "\"" + escapeJson(event.getRequestUrl()) + "\"" : "null").append(",");
        json.append("\"httpMethod\":").append(event.getHttpMethod() != null ? "\"" + escapeJson(event.getHttpMethod()) + "\"" : "null").append(",");
        json.append("\"resource\":").append(event.getResource() != null ? "\"" + escapeJson(event.getResource()) + "\"" : "null").append(",");
        json.append("\"action\":").append(event.getAction() != null ? "\"" + escapeJson(event.getAction()) + "\"" : "null").append(",");
        json.append("\"outcome\":\"").append(event.getOutcome()).append("\",");
        json.append("\"riskLevel\":\"").append(event.getRiskLevel()).append("\",");
        json.append("\"errorMessage\":").append(event.getErrorMessage() != null ? "\"" + escapeJson(event.getErrorMessage()) + "\"" : "null").append(",");
        json.append("\"metadata\":").append(event.getMetadata() != null ? event.getMetadata() : "null").append(",");
        json.append("\"processingTimeMs\":").append(event.getProcessingTimeMs() != null ? event.getProcessingTimeMs() : "null");
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    // ========================================
    // SIEM Integration Endpoints
    // ========================================

    @GetMapping("/siem/status")
    @Operation(summary = "Get SIEM forwarding status", description = "Retrieve the current status of SIEM log forwarding")
    public ResponseEntity<SiemForwardingService.SiemStatus> getSiemStatus() {
        return ResponseEntity.ok(siemForwardingService.getStatus());
    }

    @PostMapping("/siem/test")
    @Operation(summary = "Test SIEM connection", description = "Send a test event to verify SIEM webhook connectivity")
    public ResponseEntity<SiemForwardingService.TestResult> testSiemConnection() {
        SiemForwardingService.TestResult result = siemForwardingService.testConnection();
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(503).body(result);
        }
    }

    @PostMapping("/siem/flush")
    @Operation(summary = "Flush SIEM queue", description = "Manually flush queued events to SIEM")
    public ResponseEntity<Map<String, Object>> flushSiemQueue() {
        int flushedCount = siemForwardingService.manualFlush();
        Map<String, Object> response = new HashMap<>();
        response.put("flushedEvents", flushedCount);
        response.put("message", flushedCount > 0 ? "Events flushed successfully" : "No events to flush");
        return ResponseEntity.ok(response);
    }
}
