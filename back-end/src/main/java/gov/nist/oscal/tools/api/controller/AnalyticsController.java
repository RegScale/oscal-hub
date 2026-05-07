package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.AnalyticsResponse;
import gov.nist.oscal.tools.api.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Analytics Controller
 * Provides endpoints for the super admin analytics dashboard.
 */
@RestController
@RequestMapping("/api/admin/analytics")
@Tag(name = "Analytics", description = "Analytics and reporting endpoints for super admins")
@Hidden
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Get comprehensive analytics data for the dashboard
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get analytics dashboard data",
               description = "Returns comprehensive analytics including user activity, operations, and organization metrics")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        AnalyticsResponse analytics = analyticsService.getAnalytics();
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get summary statistics only (lighter weight endpoint)
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get summary statistics",
               description = "Returns quick summary stats for dashboard header cards")
    public ResponseEntity<Map<String, Long>> getSummaryStats() {
        Map<String, Long> stats = analyticsService.getSummaryStats();
        return ResponseEntity.ok(stats);
    }
}
