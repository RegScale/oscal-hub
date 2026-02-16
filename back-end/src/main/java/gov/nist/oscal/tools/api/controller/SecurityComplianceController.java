package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.security.ComplianceSummary;
import gov.nist.oscal.tools.api.model.security.GapAnalysis;
import gov.nist.oscal.tools.api.model.security.Soc2Control;
import gov.nist.oscal.tools.api.service.SecurityComplianceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for SOC 2 security compliance endpoints.
 * Provides compliance summary, control status, and gap analysis for admin dashboards.
 * All endpoints require SUPER_ADMIN authentication.
 */
@RestController
@RequestMapping("/api/admin/security")
@Tag(name = "Security Compliance", description = "SOC 2 security compliance endpoints")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SecurityComplianceController {

    private final SecurityComplianceService securityComplianceService;

    @Autowired
    public SecurityComplianceController(SecurityComplianceService securityComplianceService) {
        this.securityComplianceService = securityComplianceService;
    }

    /**
     * Get compliance summary with overall statistics.
     */
    @Operation(
        summary = "Get compliance summary",
        description = "Returns overall SOC 2 compliance statistics including control counts by status and category. Requires SUPER_ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Compliance summary",
            content = @Content(schema = @Schema(implementation = ComplianceSummary.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - requires SUPER_ADMIN role"
        )
    })
    @GetMapping("/compliance-summary")
    public ResponseEntity<ComplianceSummary> getComplianceSummary() {
        return ResponseEntity.ok(securityComplianceService.getComplianceSummary());
    }

    /**
     * Get all SOC 2 controls.
     */
    @Operation(
        summary = "Get all controls",
        description = "Returns all SOC 2 controls with their implementation status. Requires SUPER_ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of all controls",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Soc2Control.class)))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - requires SUPER_ADMIN role"
        )
    })
    @GetMapping("/controls")
    public ResponseEntity<List<Soc2Control>> getAllControls() {
        return ResponseEntity.ok(securityComplianceService.getAllControls());
    }

    /**
     * Get controls filtered by category.
     */
    @Operation(
        summary = "Get controls by category",
        description = "Returns SOC 2 controls filtered by Trust Service Criteria category. Requires SUPER_ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of controls in the specified category",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Soc2Control.class)))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid category code"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - requires SUPER_ADMIN role"
        )
    })
    @GetMapping("/controls/{category}")
    public ResponseEntity<List<Soc2Control>> getControlsByCategory(
            @Parameter(
                description = "Category code (CC6, CC7, CC8, CC9, DATA, AUDIT)",
                example = "CC6"
            )
            @PathVariable String category) {
        List<Soc2Control> controls = securityComplianceService.getControlsByCategory(category);
        if (controls.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(controls);
    }

    /**
     * Get all identified gaps with recommendations.
     */
    @Operation(
        summary = "Get gap analysis",
        description = "Returns all identified compliance gaps with severity, recommendations, and priority. Requires SUPER_ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of compliance gaps",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = GapAnalysis.class)))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - requires SUPER_ADMIN role"
        )
    })
    @GetMapping("/gaps")
    public ResponseEntity<List<GapAnalysis>> getGapAnalysis() {
        return ResponseEntity.ok(securityComplianceService.getGapAnalysis());
    }
}
