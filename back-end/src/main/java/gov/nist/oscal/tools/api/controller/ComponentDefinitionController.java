package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.ComponentDefinition;
import gov.nist.oscal.tools.api.model.ComponentDefinitionRequest;
import gov.nist.oscal.tools.api.model.ComponentDefinitionResponse;
import gov.nist.oscal.tools.api.service.ComponentDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/build/components")
@Tag(name = "Component Definitions", description = "APIs for managing OSCAL component definitions")
public class ComponentDefinitionController {

    private final ComponentDefinitionService componentService;

    @Autowired
    public ComponentDefinitionController(ComponentDefinitionService componentService) {
        this.componentService = componentService;
    }

    @Operation(
        summary = "Create new component definition",
        description = "Create a new OSCAL component definition"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Component definition created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<ComponentDefinitionResponse> createComponent(
            @Valid @RequestBody ComponentDefinitionRequest request,
            Principal principal) {
        try {
            ComponentDefinition component = componentService.createComponentDefinition(
                    request.getTitle(),
                    request.getDescription(),
                    request.getVersion(),
                    request.getOscalVersion(),
                    request.getFilename(),
                    request.getJsonContent(),
                    request.getOscalUuid(),
                    request.getComponentCount(),
                    request.getControlCount(),
                    principal.getName()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ComponentDefinitionResponse.fromEntity(component));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Update component definition",
        description = "Update an existing component definition (creator only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Component updated successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the creator"),
        @ApiResponse(responseCode = "404", description = "Component not found")
    })
    @PutMapping("/{componentId}")
    public ResponseEntity<ComponentDefinitionResponse> updateComponent(
            @PathVariable Long componentId,
            @RequestBody ComponentDefinitionRequest request,
            Principal principal) {
        try {
            ComponentDefinition component = componentService.updateComponentDefinition(
                    componentId,
                    request.getTitle(),
                    request.getDescription(),
                    request.getVersion(),
                    request.getJsonContent(),
                    request.getComponentCount(),
                    request.getControlCount(),
                    principal.getName()
            );

            return ResponseEntity.ok(ComponentDefinitionResponse.fromEntity(component));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Only the creator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get component definition by ID",
        description = "Retrieve a specific component definition"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Component found"),
        @ApiResponse(responseCode = "404", description = "Component not found")
    })
    @GetMapping("/{componentId}")
    public ResponseEntity<ComponentDefinitionResponse> getComponent(@PathVariable Long componentId) {
        try {
            ComponentDefinition component = componentService.getComponentDefinition(componentId);
            return ResponseEntity.ok(ComponentDefinitionResponse.fromEntity(component));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get component definition by OSCAL UUID",
        description = "Retrieve a component definition by its OSCAL UUID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Component found"),
        @ApiResponse(responseCode = "404", description = "Component not found")
    })
    @GetMapping("/uuid/{oscalUuid}")
    public ResponseEntity<ComponentDefinitionResponse> getComponentByUuid(@PathVariable String oscalUuid) {
        try {
            ComponentDefinition component = componentService.getComponentDefinitionByUuid(oscalUuid);
            return ResponseEntity.ok(ComponentDefinitionResponse.fromEntity(component));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get component JSON content",
        description = "Download the JSON content of a component definition"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Content retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Component not found")
    })
    @GetMapping("/{componentId}/content")
    public ResponseEntity<Map<String, String>> getComponentContent(@PathVariable Long componentId) {
        try {
            String content = componentService.getComponentContent(componentId);
            return ResponseEntity.ok(Map.of("content", content));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get user's component definitions",
        description = "Retrieve all component definitions created by the authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Components retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<ComponentDefinitionResponse>> getUserComponents(Principal principal) {
        try {
            List<ComponentDefinition> components = componentService.getUserComponents(principal.getName());
            List<ComponentDefinitionResponse> responses = components.stream()
                    .map(ComponentDefinitionResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get recent component definitions",
        description = "Retrieve user's recently created component definitions"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent components retrieved successfully")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<ComponentDefinitionResponse>> getRecentComponents(
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        try {
            List<ComponentDefinition> components = componentService.getRecentComponents(principal.getName(), limit);
            List<ComponentDefinitionResponse> responses = components.stream()
                    .map(ComponentDefinitionResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Search component definitions",
        description = "Search user's component definitions by title or description"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<List<ComponentDefinitionResponse>> searchComponents(
            @RequestParam(required = false) String q,
            Principal principal) {
        try {
            List<ComponentDefinition> components = componentService.searchComponents(principal.getName(), q);
            List<ComponentDefinitionResponse> responses = components.stream()
                    .map(ComponentDefinitionResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Delete component definition",
        description = "Delete a component definition (creator only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Component deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the creator"),
        @ApiResponse(responseCode = "404", description = "Component not found")
    })
    @DeleteMapping("/{componentId}")
    public ResponseEntity<Void> deleteComponent(@PathVariable Long componentId, Principal principal) {
        try {
            componentService.deleteComponentDefinition(componentId, principal.getName());
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

    @Operation(
        summary = "Get component statistics",
        description = "Retrieve statistics about user's component definitions"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(Principal principal) {
        try {
            Map<String, Object> stats = componentService.getComponentStatistics(principal.getName());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Check if component exists",
        description = "Check if a component definition exists by ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check completed")
    })
    @GetMapping("/{componentId}/exists")
    public ResponseEntity<Map<String, Boolean>> checkComponentExists(@PathVariable Long componentId) {
        try {
            boolean exists = componentService.componentExists(componentId);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
