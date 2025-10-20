package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.model.AuthorizationTemplateRequest;
import gov.nist.oscal.tools.api.model.AuthorizationTemplateResponse;
import gov.nist.oscal.tools.api.service.AuthorizationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/authorization-templates")
@Tag(name = "Authorization Templates", description = "APIs for managing authorization templates")
public class AuthorizationTemplateController {

    private final AuthorizationTemplateService templateService;

    @Autowired
    public AuthorizationTemplateController(AuthorizationTemplateService templateService) {
        this.templateService = templateService;
    }

    @Operation(
        summary = "Create new authorization template",
        description = "Create a new authorization template with markdown content and variables"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Template created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<AuthorizationTemplateResponse> createTemplate(
            @Valid @RequestBody AuthorizationTemplateRequest request,
            Principal principal) {
        try {
            AuthorizationTemplate template = templateService.createTemplate(
                    request.getName(),
                    request.getContent(),
                    principal.getName()
            );

            Set<String> variables = templateService.extractVariables(template.getContent());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthorizationTemplateResponse(template, variables));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Update authorization template",
        description = "Update an existing authorization template"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template updated successfully"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AuthorizationTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @RequestBody AuthorizationTemplateRequest request,
            Principal principal) {
        try {
            AuthorizationTemplate template = templateService.updateTemplate(
                    id,
                    request.getName(),
                    request.getContent(),
                    principal.getName()
            );

            Set<String> variables = templateService.extractVariables(template.getContent());
            return ResponseEntity.ok(new AuthorizationTemplateResponse(template, variables));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get authorization template by ID",
        description = "Retrieve a specific authorization template"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template found"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AuthorizationTemplateResponse> getTemplate(@PathVariable Long id) {
        try {
            AuthorizationTemplate template = templateService.getTemplate(id);
            Set<String> variables = templateService.extractVariables(template.getContent());
            return ResponseEntity.ok(new AuthorizationTemplateResponse(template, variables));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get all authorization templates",
        description = "Retrieve all authorization templates"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Templates retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<AuthorizationTemplateResponse>> getAllTemplates() {
        try {
            List<AuthorizationTemplate> templates = templateService.getAllTemplates();
            List<AuthorizationTemplateResponse> responses = templates.stream()
                    .map(template -> {
                        Set<String> variables = templateService.extractVariables(template.getContent());
                        return new AuthorizationTemplateResponse(template, variables);
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get recently updated templates",
        description = "Retrieve recently updated authorization templates"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Templates retrieved successfully")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<AuthorizationTemplateResponse>> getRecentlyUpdated(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AuthorizationTemplate> templates = templateService.getRecentlyUpdated(limit);
            List<AuthorizationTemplateResponse> responses = templates.stream()
                    .map(template -> {
                        Set<String> variables = templateService.extractVariables(template.getContent());
                        return new AuthorizationTemplateResponse(template, variables);
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Search authorization templates",
        description = "Search authorization templates by name"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<List<AuthorizationTemplateResponse>> searchTemplates(
            @RequestParam(required = false) String q) {
        try {
            List<AuthorizationTemplate> templates = templateService.searchTemplates(q);
            List<AuthorizationTemplateResponse> responses = templates.stream()
                    .map(template -> {
                        Set<String> variables = templateService.extractVariables(template.getContent());
                        return new AuthorizationTemplateResponse(template, variables);
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Extract variables from template",
        description = "Extract variable names from a template's content"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Variables extracted successfully"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @GetMapping("/{id}/variables")
    public ResponseEntity<Map<String, Set<String>>> getTemplateVariables(@PathVariable Long id) {
        try {
            Set<String> variables = templateService.extractVariablesFromTemplate(id);
            return ResponseEntity.ok(Map.of("variables", variables));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Delete authorization template",
        description = "Delete an authorization template (creator only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the creator"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id, Principal principal) {
        try {
            templateService.deleteTemplate(id, principal.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Only the creator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
