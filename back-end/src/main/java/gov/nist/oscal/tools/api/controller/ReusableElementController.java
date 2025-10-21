package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.ReusableElement;
import gov.nist.oscal.tools.api.model.ReusableElementRequest;
import gov.nist.oscal.tools.api.model.ReusableElementResponse;
import gov.nist.oscal.tools.api.service.ReusableElementService;
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
@RequestMapping("/api/build/elements")
@Tag(name = "Reusable Elements", description = "APIs for managing reusable OSCAL elements")
public class ReusableElementController {

    private final ReusableElementService elementService;

    @Autowired
    public ReusableElementController(ReusableElementService elementService) {
        this.elementService = elementService;
    }

    @Operation(
        summary = "Create new reusable element",
        description = "Create a new reusable OSCAL element (role, party, link, back matter, responsible party)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Element created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<ReusableElementResponse> createElement(
            @Valid @RequestBody ReusableElementRequest request,
            Principal principal) {
        try {
            ReusableElement.ElementType type = ReusableElement.ElementType.valueOf(request.getType());

            ReusableElement element = elementService.createElement(
                    type,
                    request.getName(),
                    request.getJsonContent(),
                    request.getDescription(),
                    request.getIsShared() != null ? request.getIsShared() : false,
                    principal.getName()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ReusableElementResponse.fromEntity(element));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Update reusable element",
        description = "Update an existing reusable element (creator only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Element updated successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the creator"),
        @ApiResponse(responseCode = "404", description = "Element not found")
    })
    @PutMapping("/{elementId}")
    public ResponseEntity<ReusableElementResponse> updateElement(
            @PathVariable Long elementId,
            @RequestBody ReusableElementRequest request,
            Principal principal) {
        try {
            ReusableElement element = elementService.updateElement(
                    elementId,
                    request.getName(),
                    request.getJsonContent(),
                    request.getDescription(),
                    request.getIsShared(),
                    principal.getName()
            );

            return ResponseEntity.ok(ReusableElementResponse.fromEntity(element));
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
        summary = "Get reusable element by ID",
        description = "Retrieve a specific reusable element"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Element found"),
        @ApiResponse(responseCode = "404", description = "Element not found")
    })
    @GetMapping("/{elementId}")
    public ResponseEntity<ReusableElementResponse> getElement(@PathVariable Long elementId) {
        try {
            ReusableElement element = elementService.getElement(elementId);
            return ResponseEntity.ok(ReusableElementResponse.fromEntity(element));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get user's reusable elements",
        description = "Retrieve all reusable elements created by the authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Elements retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<ReusableElementResponse>> getUserElements(Principal principal) {
        try {
            List<ReusableElement> elements = elementService.getUserElements(principal.getName());
            List<ReusableElementResponse> responses = elements.stream()
                    .map(ReusableElementResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get elements by type",
        description = "Retrieve user's reusable elements filtered by type"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Elements retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid element type")
    })
    @GetMapping("/type/{type}")
    public ResponseEntity<List<ReusableElementResponse>> getElementsByType(
            @PathVariable String type,
            Principal principal) {
        try {
            ReusableElement.ElementType elementType = ReusableElement.ElementType.valueOf(type.toUpperCase());
            List<ReusableElement> elements = elementService.getUserElementsByType(principal.getName(), elementType);
            List<ReusableElementResponse> responses = elements.stream()
                    .map(ReusableElementResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Search reusable elements",
        description = "Search user's reusable elements by name and optionally filter by type"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<List<ReusableElementResponse>> searchElements(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            Principal principal) {
        try {
            ReusableElement.ElementType elementType = null;
            if (type != null && !type.isEmpty()) {
                elementType = ReusableElement.ElementType.valueOf(type.toUpperCase());
            }

            List<ReusableElement> elements = elementService.searchElements(principal.getName(), q, elementType);
            List<ReusableElementResponse> responses = elements.stream()
                    .map(ReusableElementResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Delete reusable element",
        description = "Delete a reusable element (creator only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Element deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the creator"),
        @ApiResponse(responseCode = "404", description = "Element not found")
    })
    @DeleteMapping("/{elementId}")
    public ResponseEntity<Void> deleteElement(@PathVariable Long elementId, Principal principal) {
        try {
            elementService.deleteElement(elementId, principal.getName());
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
        summary = "Get recent elements",
        description = "Retrieve user's recently created reusable elements"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent elements retrieved successfully")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<ReusableElementResponse>> getRecentElements(
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        try {
            List<ReusableElement> elements = elementService.getRecentElements(principal.getName(), limit);
            List<ReusableElementResponse> responses = elements.stream()
                    .map(ReusableElementResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get most used elements",
        description = "Retrieve user's most frequently used reusable elements"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Most used elements retrieved successfully")
    })
    @GetMapping("/most-used")
    public ResponseEntity<List<ReusableElementResponse>> getMostUsedElements(
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        try {
            List<ReusableElement> elements = elementService.getMostUsedElements(principal.getName(), limit);
            List<ReusableElementResponse> responses = elements.stream()
                    .map(ReusableElementResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get element statistics",
        description = "Retrieve statistics about user's element library"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(Principal principal) {
        try {
            Map<String, Object> stats = elementService.getElementStatistics(principal.getName());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Increment element use count",
        description = "Increment the usage count when an element is used in a component"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Use count incremented successfully"),
        @ApiResponse(responseCode = "404", description = "Element not found")
    })
    @PostMapping("/{elementId}/use")
    public ResponseEntity<Void> incrementUseCount(@PathVariable Long elementId) {
        try {
            elementService.incrementUseCount(elementId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
