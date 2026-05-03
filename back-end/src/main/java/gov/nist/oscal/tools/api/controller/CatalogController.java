package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Catalog;
import gov.nist.oscal.tools.api.model.CatalogRequest;
import gov.nist.oscal.tools.api.model.CatalogResponse;
import gov.nist.oscal.tools.api.service.CatalogService;
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
@RequestMapping("/api/build/catalogs")
@Tag(name = "Catalogs", description = "APIs for managing OSCAL catalogs created in the builder")
public class CatalogController {

    private final CatalogService catalogService;

    @Autowired
    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "Create new catalog")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Catalog created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "UUID already exists")
    })
    @PostMapping
    public ResponseEntity<CatalogResponse> create(@Valid @RequestBody CatalogRequest request, Principal principal) {
        try {
            Catalog catalog = catalogService.createCatalog(
                    request.getTitle(), request.getDescription(), request.getVersion(),
                    request.getOscalVersion(), request.getFilename(), request.getJsonContent(),
                    request.getOscalUuid(), request.getGroupCount(), request.getControlCount(),
                    request.getParamCount(), request.getDraft(), principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(CatalogResponse.fromEntity(catalog));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Update catalog")
    @PutMapping("/{catalogId}")
    public ResponseEntity<CatalogResponse> update(
            @PathVariable Long catalogId,
            @RequestBody CatalogRequest request,
            Principal principal) {
        try {
            Catalog catalog = catalogService.updateCatalog(
                    catalogId, request.getTitle(), request.getDescription(), request.getVersion(),
                    request.getJsonContent(), request.getGroupCount(), request.getControlCount(),
                    request.getParamCount(), request.getDraft(), principal.getName());
            return ResponseEntity.ok(CatalogResponse.fromEntity(catalog));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Only the creator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get catalog metadata by ID")
    @GetMapping("/{catalogId}")
    public ResponseEntity<CatalogResponse> get(@PathVariable Long catalogId) {
        try {
            return ResponseEntity.ok(CatalogResponse.fromEntity(catalogService.getCatalog(catalogId)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get catalog by OSCAL UUID")
    @GetMapping("/uuid/{oscalUuid}")
    public ResponseEntity<CatalogResponse> getByUuid(@PathVariable String oscalUuid) {
        try {
            return ResponseEntity.ok(CatalogResponse.fromEntity(catalogService.getCatalogByUuid(oscalUuid)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get catalog JSON content")
    @GetMapping("/{catalogId}/content")
    public ResponseEntity<Map<String, String>> getContent(@PathVariable Long catalogId) {
        try {
            return ResponseEntity.ok(Map.of("content", catalogService.getCatalogContent(catalogId)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "List user's catalogs")
    @GetMapping
    public ResponseEntity<List<CatalogResponse>> list(Principal principal) {
        try {
            List<CatalogResponse> resp = catalogService.getUserCatalogs(principal.getName())
                    .stream().map(CatalogResponse::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Search catalogs")
    @GetMapping("/search")
    public ResponseEntity<List<CatalogResponse>> search(@RequestParam(required = false) String q, Principal principal) {
        try {
            List<CatalogResponse> resp = catalogService.searchCatalogs(principal.getName(), q)
                    .stream().map(CatalogResponse::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Delete catalog (creator only)")
    @DeleteMapping("/{catalogId}")
    public ResponseEntity<Void> delete(@PathVariable Long catalogId, Principal principal) {
        try {
            catalogService.deleteCatalog(catalogId, principal.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Only the creator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get catalog statistics")
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> stats(Principal principal) {
        try {
            return ResponseEntity.ok(catalogService.getStatistics(principal.getName()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
