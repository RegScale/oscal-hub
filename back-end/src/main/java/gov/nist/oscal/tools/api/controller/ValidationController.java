package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.service.BatchOperationService;
import gov.nist.oscal.tools.api.service.ConversionService;
import gov.nist.oscal.tools.api.service.ProfileResolutionService;
import gov.nist.oscal.tools.api.service.ValidationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api")
@Tag(name = "OSCAL Operations", description = "APIs for OSCAL document validation, conversion, and profile resolution")
public class ValidationController {

    private final ValidationService validationService;
    private final ConversionService conversionService;
    private final ProfileResolutionService profileResolutionService;
    private final BatchOperationService batchOperationService;

    @Autowired
    public ValidationController(
        ValidationService validationService,
        ConversionService conversionService,
        ProfileResolutionService profileResolutionService,
        BatchOperationService batchOperationService
    ) {
        this.validationService = validationService;
        this.conversionService = conversionService;
        this.profileResolutionService = profileResolutionService;
        this.batchOperationService = batchOperationService;
    }

    @Operation(
        summary = "Validate OSCAL document",
        description = "Validates an OSCAL document against its schema. Supports JSON, XML, and YAML formats for all OSCAL model types (Catalog, Profile, Component Definition, SSP, Assessment Plan, Assessment Results, POA&M)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed (check 'valid' field in response)")
    })
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validate(@Valid @RequestBody ValidationRequest request, Principal principal) {
        ValidationResult result = validationService.validate(request, principal.getName());
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Convert OSCAL document format",
        description = "Converts an OSCAL document from one format to another. Supports conversion between JSON, XML, and YAML formats for all OSCAL model types."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Conversion completed (check 'success' field in response)")
    })
    @PostMapping("/convert")
    public ResponseEntity<ConversionResult> convert(@Valid @RequestBody ConversionRequest request, Principal principal) {
        ConversionResult result = conversionService.convert(request, principal.getName());
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Resolve OSCAL profile",
        description = "Resolves an OSCAL profile by applying imports and modifications. Note: Full resolution with external catalog fetching is not yet implemented."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile resolution attempted (check 'success' field in response)")
    })
    @PostMapping("/profile/resolve")
    public ResponseEntity<ProfileResolutionResult> resolveProfile(@Valid @RequestBody ProfileResolutionRequest request, Principal principal) {
        ProfileResolutionResult result = profileResolutionService.resolveProfile(request, principal.getName());
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Process batch operation",
        description = "Process multiple OSCAL files in a batch operation (validate or convert). Returns an operation ID for tracking progress. Maximum 10 files per batch."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch operation started")
    })
    @PostMapping("/batch")
    public ResponseEntity<BatchOperationResult> processBatch(@Valid @RequestBody BatchOperationRequest request, Principal principal) {
        BatchOperationResult result = batchOperationService.processBatch(request, principal.getName());
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Get batch operation status",
        description = "Get the status and results of a batch operation by its operation ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch operation found"),
        @ApiResponse(responseCode = "404", description = "Operation not found")
    })
    @GetMapping("/batch/{operationId}")
    public ResponseEntity<BatchOperationResult> getBatchResult(@PathVariable String operationId) {
        BatchOperationResult result = batchOperationService.getBatchResult(operationId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Health check",
        description = "Simple health check endpoint to verify the API is running"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "API is healthy")
    })
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OSCAL CLI API is running");
    }
}
