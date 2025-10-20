package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.ProfileVisualizationRequest;
import gov.nist.oscal.tools.api.model.ProfileVisualizationResult;
import gov.nist.oscal.tools.api.model.SarVisualizationRequest;
import gov.nist.oscal.tools.api.model.SarVisualizationResult;
import gov.nist.oscal.tools.api.model.SspVisualizationRequest;
import gov.nist.oscal.tools.api.model.SspVisualizationResult;
import gov.nist.oscal.tools.api.service.VisualizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/visualization")
@Tag(name = "OSCAL Visualization", description = "APIs for visualizing OSCAL documents")
public class VisualizationController {

    private final VisualizationService visualizationService;

    @Autowired
    public VisualizationController(VisualizationService visualizationService) {
        this.visualizationService = visualizationService;
    }

    @Operation(
        summary = "Analyze System Security Plan for visualization",
        description = "Analyzes an OSCAL SSP document and extracts key information for visualization including categorization, information types, personnel/roles, control status by family, and assets."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "SSP analyzed successfully (check 'success' field in response)")
    })
    @PostMapping("/ssp")
    public ResponseEntity<SspVisualizationResult> visualizeSSP(
        @Valid @RequestBody SspVisualizationRequest request,
        Principal principal
    ) {
        SspVisualizationResult result = visualizationService.analyzeSSP(request, principal.getName());
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Analyze Profile for visualization",
        description = "Analyzes an OSCAL Profile document and extracts key information for visualization including imports, control counts by family, and modifications."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile analyzed successfully (check 'success' field in response)")
    })
    @PostMapping("/profile")
    public ResponseEntity<ProfileVisualizationResult> visualizeProfile(
        @Valid @RequestBody ProfileVisualizationRequest request,
        Principal principal
    ) {
        ProfileVisualizationResult result = visualizationService.analyzeProfile(request, principal.getName());
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Analyze Security Assessment Results for visualization",
        description = "Analyzes an OSCAL Assessment Results document and extracts key information for visualization including controls assessed, findings, observations, and risks."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "SAR analyzed successfully (check 'success' field in response)")
    })
    @PostMapping("/sar")
    public ResponseEntity<SarVisualizationResult> visualizeSAR(
        @Valid @RequestBody SarVisualizationRequest request,
        Principal principal
    ) {
        SarVisualizationResult result = visualizationService.analyzeSAR(request, principal.getName());
        return ResponseEntity.ok(result);
    }
}
