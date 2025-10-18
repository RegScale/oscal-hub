package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.CustomRuleRequest;
import gov.nist.oscal.tools.api.model.CustomRuleResponse;
import gov.nist.oscal.tools.api.service.CustomRulesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rules/custom")
@Tag(name = "Custom Rules", description = "Manage custom validation rules")
public class CustomRulesController {

    private static final Logger logger = LoggerFactory.getLogger(CustomRulesController.class);
    private final CustomRulesService customRulesService;

    @Autowired
    public CustomRulesController(CustomRulesService customRulesService) {
        this.customRulesService = customRulesService;
    }

    @GetMapping
    @Operation(summary = "Get all custom rules", description = "Retrieve all custom validation rules")
    public ResponseEntity<List<CustomRuleResponse>> getAllCustomRules() {
        logger.info("GET /api/rules/custom - Getting all custom rules");
        List<CustomRuleResponse> rules = customRulesService.getAllCustomRules();
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get custom rule by ID", description = "Retrieve a specific custom rule by its database ID")
    public ResponseEntity<CustomRuleResponse> getCustomRuleById(@PathVariable Long id) {
        logger.info("GET /api/rules/custom/{} - Getting custom rule by ID", id);
        Optional<CustomRuleResponse> rule = customRulesService.getCustomRuleById(id);
        return rule.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rule/{ruleId}")
    @Operation(summary = "Get custom rule by rule ID", description = "Retrieve a specific custom rule by its unique rule ID")
    public ResponseEntity<CustomRuleResponse> getCustomRuleByRuleId(@PathVariable String ruleId) {
        logger.info("GET /api/rules/custom/rule/{} - Getting custom rule by rule ID", ruleId);
        Optional<CustomRuleResponse> rule = customRulesService.getCustomRuleByRuleId(ruleId);
        return rule.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/enabled")
    @Operation(summary = "Get enabled custom rules", description = "Retrieve all enabled custom validation rules")
    public ResponseEntity<List<CustomRuleResponse>> getEnabledCustomRules() {
        logger.info("GET /api/rules/custom/enabled - Getting enabled custom rules");
        List<CustomRuleResponse> rules = customRulesService.getEnabledCustomRules();
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get custom rules by category", description = "Retrieve custom rules for a specific category")
    public ResponseEntity<List<CustomRuleResponse>> getCustomRulesByCategory(@PathVariable String category) {
        logger.info("GET /api/rules/custom/category/{} - Getting custom rules by category", category);
        List<CustomRuleResponse> rules = customRulesService.getCustomRulesByCategory(category);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/model/{modelType}")
    @Operation(summary = "Get custom rules by model type", description = "Retrieve enabled custom rules for a specific OSCAL model type")
    public ResponseEntity<List<CustomRuleResponse>> getCustomRulesByModelType(@PathVariable String modelType) {
        logger.info("GET /api/rules/custom/model/{} - Getting custom rules by model type", modelType);
        List<CustomRuleResponse> rules = customRulesService.getCustomRulesForModelType(modelType);
        return ResponseEntity.ok(rules);
    }

    @PostMapping
    @Operation(summary = "Create custom rule", description = "Create a new custom validation rule")
    public ResponseEntity<?> createCustomRule(@Valid @RequestBody CustomRuleRequest request) {
        logger.info("POST /api/rules/custom - Creating custom rule: {}", request.getRuleId());
        try {
            CustomRuleResponse created = customRulesService.createCustomRule(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to create custom rule: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update custom rule", description = "Update an existing custom validation rule")
    public ResponseEntity<?> updateCustomRule(@PathVariable Long id, @Valid @RequestBody CustomRuleRequest request) {
        logger.info("PUT /api/rules/custom/{} - Updating custom rule", id);
        try {
            CustomRuleResponse updated = customRulesService.updateCustomRule(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to update custom rule: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete custom rule", description = "Delete a custom validation rule")
    public ResponseEntity<?> deleteCustomRule(@PathVariable Long id) {
        logger.info("DELETE /api/rules/custom/{} - Deleting custom rule", id);
        try {
            customRulesService.deleteCustomRule(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("Failed to delete custom rule: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle rule enabled status", description = "Toggle the enabled/disabled status of a custom rule")
    public ResponseEntity<?> toggleRuleEnabled(@PathVariable Long id) {
        logger.info("PATCH /api/rules/custom/{}/toggle - Toggling rule enabled status", id);
        try {
            CustomRuleResponse toggled = customRulesService.toggleRuleEnabled(id);
            return ResponseEntity.ok(toggled);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to toggle rule status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
