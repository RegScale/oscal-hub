package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.health.ComponentHealth;
import gov.nist.oscal.tools.api.model.health.DetailedHealthResponse;
import gov.nist.oscal.tools.api.model.health.SimpleHealthResponse;
import gov.nist.oscal.tools.api.service.HealthCheckService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for health check endpoints.
 * Provides both public endpoints for monitoring and authenticated endpoints for admin dashboards.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Health check and monitoring endpoints")
public class HealthController {

    private final HealthCheckService healthCheckService;

    @Autowired
    public HealthController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    /**
     * Simple health check endpoint for load balancers and basic monitoring.
     * This endpoint is public and does not require authentication.
     */
    @Operation(
        summary = "Simple health check",
        description = "Returns basic health status. Public endpoint for load balancers and monitoring tools."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "System is healthy",
            content = @Content(schema = @Schema(implementation = SimpleHealthResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<SimpleHealthResponse> health() {
        return ResponseEntity.ok(healthCheckService.getSimpleHealth());
    }

    /**
     * Ping endpoint for uptime monitoring tools.
     * Returns simple "OK" or 503 status for external monitoring services.
     * This endpoint is public and does not require authentication.
     */
    @Operation(
        summary = "Ping endpoint",
        description = "Simple ping endpoint for external monitoring tools (UptimeRobot, Pingdom, etc.). Returns 'OK' if healthy, 503 if unhealthy."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "System is healthy",
            content = @Content(schema = @Schema(type = "string", example = "OK"))
        ),
        @ApiResponse(
            responseCode = "503",
            description = "System is unhealthy",
            content = @Content(schema = @Schema(type = "string", example = "UNHEALTHY"))
        )
    })
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        boolean healthy = healthCheckService.isHealthy();
        if (healthy) {
            return ResponseEntity.ok("OK");
        } else {
            return ResponseEntity.status(503).body("UNHEALTHY");
        }
    }

    /**
     * Detailed health check endpoint for admin dashboard.
     * Returns comprehensive health information including all components, system info, and environment.
     * This endpoint requires SUPER_ADMIN authentication.
     */
    @Operation(
        summary = "Detailed health check",
        description = "Returns comprehensive health status including component health, system metrics, and environment information. Requires SUPER_ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Detailed health status",
            content = @Content(schema = @Schema(implementation = DetailedHealthResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - requires SUPER_ADMIN role"
        )
    })
    @GetMapping("/detailed")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DetailedHealthResponse> detailedHealth() {
        return ResponseEntity.ok(healthCheckService.getDetailedHealth());
    }

    /**
     * Individual component health check endpoint.
     * Returns health status for a specific component.
     * This endpoint requires SUPER_ADMIN authentication.
     */
    @Operation(
        summary = "Component health check",
        description = "Returns health status for a specific component. Requires SUPER_ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Component health status",
            content = @Content(schema = @Schema(implementation = ComponentHealth.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Unknown component specified"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - requires SUPER_ADMIN role"
        )
    })
    @GetMapping("/component/{component}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ComponentHealth> componentHealth(
            @Parameter(
                description = "Component name to check (database, storage, memory, diskspace, oscal)",
                example = "database"
            )
            @PathVariable String component) {
        try {
            return ResponseEntity.ok(healthCheckService.getComponentHealth(component));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ComponentHealth.builder()
                    .status("UNKNOWN")
                    .message(e.getMessage())
                    .build()
            );
        }
    }
}
