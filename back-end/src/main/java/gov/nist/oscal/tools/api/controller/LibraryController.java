package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryTag;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/library")
@Tag(name = "Library Management", description = "APIs for managing shared OSCAL library")
public class LibraryController {

    private final LibraryService libraryService;

    @Autowired
    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @Operation(
        summary = "Create new library item",
        description = "Upload a new OSCAL file to the shared library with metadata"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Library item created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<LibraryItemResponse> createLibraryItem(
            @Valid @RequestBody LibraryItemRequest request,
            Principal principal) {
        try {
            LibraryItem item = libraryService.createLibraryItem(
                    request.getTitle(),
                    request.getDescription(),
                    request.getOscalType(),
                    request.getFileName(),
                    request.getFormat(),
                    request.getFileContent(),
                    request.getTags(),
                    principal.getName()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(LibraryItemResponse.fromEntity(item));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Update library item metadata",
        description = "Update title, description, and tags of a library item"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Library item updated successfully"),
        @ApiResponse(responseCode = "404", description = "Library item not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{itemId}")
    public ResponseEntity<LibraryItemResponse> updateLibraryItem(
            @PathVariable String itemId,
            @RequestBody LibraryItemUpdateRequest request,
            Principal principal) {
        try {
            LibraryItem item = libraryService.updateLibraryItem(
                    itemId,
                    request.getTitle(),
                    request.getDescription(),
                    request.getTags(),
                    principal.getName()
            );

            return ResponseEntity.ok(LibraryItemResponse.fromEntity(item));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Add new version to library item",
        description = "Upload a new version of an existing library item"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Version added successfully"),
        @ApiResponse(responseCode = "404", description = "Library item not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{itemId}/versions")
    public ResponseEntity<LibraryVersionResponse> addVersion(
            @PathVariable String itemId,
            @Valid @RequestBody LibraryVersionRequest request,
            Principal principal) {
        try {
            LibraryVersion version = libraryService.addVersion(
                    itemId,
                    request.getFileName(),
                    request.getFormat(),
                    request.getFileContent(),
                    request.getChangeDescription(),
                    principal.getName()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(LibraryVersionResponse.fromEntity(version));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get library item by ID",
        description = "Retrieve a specific library item with metadata"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Library item found"),
        @ApiResponse(responseCode = "404", description = "Library item not found")
    })
    @GetMapping("/{itemId}")
    public ResponseEntity<LibraryItemResponse> getLibraryItem(@PathVariable String itemId) {
        try {
            LibraryItem item = libraryService.getLibraryItem(itemId);
            return ResponseEntity.ok(LibraryItemResponse.fromEntity(item));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get library item file content",
        description = "Download the current version file content of a library item"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File content retrieved"),
        @ApiResponse(responseCode = "404", description = "Library item not found")
    })
    @GetMapping("/{itemId}/content")
    public ResponseEntity<Map<String, String>> getLibraryItemContent(@PathVariable String itemId) {
        try {
            String content = libraryService.getCurrentVersionContent(itemId);
            return ResponseEntity.ok(Map.of("content", content));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get version history",
        description = "Retrieve all versions of a library item"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Version history retrieved"),
        @ApiResponse(responseCode = "404", description = "Library item not found")
    })
    @GetMapping("/{itemId}/versions")
    public ResponseEntity<List<LibraryVersionResponse>> getVersionHistory(@PathVariable String itemId) {
        try {
            List<LibraryVersion> versions = libraryService.getVersionHistory(itemId);
            List<LibraryVersionResponse> responses = versions.stream()
                    .map(LibraryVersionResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get specific version content",
        description = "Download the file content of a specific version"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Version content retrieved"),
        @ApiResponse(responseCode = "404", description = "Version not found")
    })
    @GetMapping("/versions/{versionId}/content")
    public ResponseEntity<Map<String, String>> getVersionContent(@PathVariable String versionId) {
        try {
            String content = libraryService.getVersionContent(versionId);
            return ResponseEntity.ok(Map.of("content", content));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Delete library item",
        description = "Delete a library item and all its versions (creator only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Library item deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the creator"),
        @ApiResponse(responseCode = "404", description = "Library item not found")
    })
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteLibraryItem(@PathVariable String itemId, Principal principal) {
        try {
            libraryService.deleteLibraryItem(itemId, principal.getName());
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
        summary = "Search library",
        description = "Search library items by keyword, OSCAL type, or tag"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<List<LibraryItemResponse>> searchLibrary(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String oscalType,
            @RequestParam(required = false) String tag) {
        try {
            List<LibraryItem> items = libraryService.searchLibrary(q, oscalType, tag);
            List<LibraryItemResponse> responses = items.stream()
                    .map(LibraryItemResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get all library items",
        description = "Retrieve all items in the library"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Library items retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<LibraryItemResponse>> getAllLibraryItems() {
        try {
            List<LibraryItem> items = libraryService.getAllLibraryItems();
            List<LibraryItemResponse> responses = items.stream()
                    .map(LibraryItemResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get items by OSCAL type",
        description = "Retrieve all library items of a specific OSCAL type"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Library items retrieved successfully")
    })
    @GetMapping("/type/{oscalType}")
    public ResponseEntity<List<LibraryItemResponse>> getItemsByType(@PathVariable String oscalType) {
        try {
            List<LibraryItem> items = libraryService.getLibraryItemsByOscalType(oscalType);
            List<LibraryItemResponse> responses = items.stream()
                    .map(LibraryItemResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get most popular items",
        description = "Retrieve the most downloaded library items"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Popular items retrieved successfully")
    })
    @GetMapping("/popular")
    public ResponseEntity<List<LibraryItemResponse>> getMostPopular(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<LibraryItem> items = libraryService.getMostPopular(limit);
            List<LibraryItemResponse> responses = items.stream()
                    .map(LibraryItemResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get recently updated items",
        description = "Retrieve recently updated library items"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recently updated items retrieved successfully")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<LibraryItemResponse>> getRecentlyUpdated(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<LibraryItem> items = libraryService.getRecentlyUpdated(limit);
            List<LibraryItemResponse> responses = items.stream()
                    .map(LibraryItemResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get library analytics",
        description = "Retrieve statistics and analytics about the library"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully")
    })
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        try {
            Map<String, Object> analytics = libraryService.getAnalytics();
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get all tags",
        description = "Retrieve all available tags in the library"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tags retrieved successfully")
    })
    @GetMapping("/tags")
    public ResponseEntity<List<Map<String, Object>>> getAllTags() {
        try {
            List<LibraryTag> tags = libraryService.getAllTags();
            List<Map<String, Object>> tagResponses = tags.stream()
                    .map(tag -> Map.of(
                            "name", (Object) tag.getName(),
                            "usageCount", tag.getUsageCount()
                    ))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(tagResponses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get popular tags",
        description = "Retrieve the most popular tags in the library"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Popular tags retrieved successfully")
    })
    @GetMapping("/tags/popular")
    public ResponseEntity<List<Map<String, Object>>> getPopularTags(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<LibraryTag> tags = libraryService.getPopularTags(limit);
            List<Map<String, Object>> tagResponses = tags.stream()
                    .map(tag -> Map.of(
                            "name", (Object) tag.getName(),
                            "usageCount", tag.getUsageCount()
                    ))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(tagResponses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
