package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.service.ValidationRulesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@Tag(name = "Validation Rules", description = "APIs for viewing and managing OSCAL validation rules")
public class ValidationRulesController {

    private final ValidationRulesService validationRulesService;

    @Autowired
    public ValidationRulesController(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Operation(
        summary = "Get all validation rules",
        description = "Retrieves all validation rules with statistics, organized by category. " +
                     "Includes both built-in OSCAL rules and any custom rules that have been defined."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved validation rules")
    })
    @GetMapping
    public ResponseEntity<ValidationRulesResponse> getAllRules() {
        ValidationRulesResponse response = validationRulesService.getAllRules();
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get validation rules for a specific OSCAL model type",
        description = "Retrieves only the validation rules that apply to a specific OSCAL model type " +
                     "(e.g., catalog, profile, system-security-plan). This helps users understand what " +
                     "rules will be checked when validating a particular type of document."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved rules for model type"),
        @ApiResponse(responseCode = "400", description = "Invalid model type specified")
    })
    @GetMapping("/model/{modelType}")
    public ResponseEntity<ValidationRulesResponse> getRulesForModelType(
        @Parameter(description = "OSCAL model type (e.g., catalog, profile, system-security-plan)")
        @PathVariable String modelType
    ) {
        try {
            OscalModelType type = OscalModelType.fromString(modelType);
            ValidationRulesResponse response = validationRulesService.getRulesForModelType(type);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Get all rule categories",
        description = "Retrieves a list of all validation rule categories (e.g., Metadata, Security Controls, " +
                     "Identifiers) with the count of rules in each category. Useful for organizing and " +
                     "filtering rules in the UI."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved rule categories")
    })
    @GetMapping("/categories")
    public ResponseEntity<List<ValidationRuleCategory>> getCategories() {
        List<ValidationRuleCategory> categories = validationRulesService.getCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(
        summary = "Get validation rules statistics",
        description = "Retrieves summary statistics about validation rules including total count, " +
                     "built-in vs custom rules, and distribution across model types and categories."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics")
    })
    @GetMapping("/stats")
    public ResponseEntity<ValidationRulesStats> getStats() {
        ValidationRulesResponse allRules = validationRulesService.getAllRules();
        ValidationRulesStats stats = new ValidationRulesStats();
        stats.setTotalRules(allRules.getTotalRules());
        stats.setBuiltInRules(allRules.getBuiltInRules());
        stats.setCustomRules(allRules.getCustomRules());
        stats.setRulesByModelType(allRules.getRulesByModelType());
        stats.setRulesByCategory(allRules.getRulesByCategory());
        return ResponseEntity.ok(stats);
    }
}
