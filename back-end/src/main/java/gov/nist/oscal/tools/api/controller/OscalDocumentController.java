package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.OscalDocument;
import gov.nist.oscal.tools.api.entity.OscalModelType;
import gov.nist.oscal.tools.api.model.OscalDocumentRequest;
import gov.nist.oscal.tools.api.model.OscalDocumentResponse;
import gov.nist.oscal.tools.api.service.OscalDocumentService;
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

/**
 * Unified CRUD endpoint for OSCAL documents that share the generic builder
 * (SSP, Assessment Plan, Assessment Results, POA&amp;M).
 */
@RestController
@RequestMapping("/api/build/oscal-documents")
@Tag(name = "OSCAL Documents", description = "Unified CRUD for SSP, Assessment Plan/Results, and POA&M")
public class OscalDocumentController {

    private final OscalDocumentService documentService;

    @Autowired
    public OscalDocumentController(OscalDocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "Create a new OSCAL document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Document created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "UUID already exists")
    })
    @PostMapping
    public ResponseEntity<OscalDocumentResponse> create(@Valid @RequestBody OscalDocumentRequest request, Principal principal) {
        try {
            OscalModelType modelType = OscalModelType.fromSlug(request.getModelType());
            OscalDocument doc = documentService.create(
                    modelType, request.getTitle(), request.getDescription(), request.getVersion(),
                    request.getOscalVersion(), request.getFilename(), request.getJsonContent(),
                    request.getOscalUuid(), request.getStatsJson(), request.getDraft(),
                    principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(OscalDocumentResponse.fromEntity(doc));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Update an existing OSCAL document")
    @PutMapping("/{id}")
    public ResponseEntity<OscalDocumentResponse> update(
            @PathVariable Long id,
            @RequestBody OscalDocumentRequest request,
            Principal principal) {
        try {
            OscalDocument doc = documentService.update(
                    id, request.getTitle(), request.getDescription(), request.getVersion(),
                    request.getJsonContent(), request.getStatsJson(), request.getDraft(),
                    principal.getName());
            return ResponseEntity.ok(OscalDocumentResponse.fromEntity(doc));
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

    @Operation(summary = "Get a document by ID")
    @GetMapping("/{id}")
    public ResponseEntity<OscalDocumentResponse> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(OscalDocumentResponse.fromEntity(documentService.get(id)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get a document by OSCAL UUID")
    @GetMapping("/uuid/{oscalUuid}")
    public ResponseEntity<OscalDocumentResponse> getByUuid(@PathVariable String oscalUuid) {
        try {
            return ResponseEntity.ok(OscalDocumentResponse.fromEntity(documentService.getByUuid(oscalUuid)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get the JSON content of a document")
    @GetMapping("/{id}/content")
    public ResponseEntity<Map<String, String>> getContent(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(Map.of("content", documentService.getContent(id)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "List the user's documents of a given model type")
    @GetMapping
    public ResponseEntity<List<OscalDocumentResponse>> list(
            @RequestParam("modelType") String modelTypeSlug,
            Principal principal) {
        try {
            OscalModelType modelType = OscalModelType.fromSlug(modelTypeSlug);
            List<OscalDocumentResponse> resp = documentService.listByUserAndType(principal.getName(), modelType)
                    .stream().map(OscalDocumentResponse::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Search documents by title/description within a model type")
    @GetMapping("/search")
    public ResponseEntity<List<OscalDocumentResponse>> search(
            @RequestParam("modelType") String modelTypeSlug,
            @RequestParam(required = false) String q,
            Principal principal) {
        try {
            OscalModelType modelType = OscalModelType.fromSlug(modelTypeSlug);
            List<OscalDocumentResponse> resp = documentService.search(principal.getName(), modelType, q)
                    .stream().map(OscalDocumentResponse::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Delete a document (creator only)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        try {
            documentService.delete(id, principal.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Only the creator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.notFound().build();
        }
    }
}
