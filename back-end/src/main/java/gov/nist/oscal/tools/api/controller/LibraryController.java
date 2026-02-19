package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.service.LibraryCommentService;
import gov.nist.oscal.tools.api.service.LibraryRatingService;
import gov.nist.oscal.tools.api.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/library")
@Tag(name = "Library Management", description = "APIs for managing shared OSCAL library")
public class LibraryController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LibraryController.class);

    private final LibraryService libraryService;
    private final LibraryRatingService ratingService;
    private final LibraryCommentService commentService;

    @Autowired
    public LibraryController(LibraryService libraryService,
                            LibraryRatingService ratingService,
                            LibraryCommentService commentService) {
        this.libraryService = libraryService;
        this.ratingService = ratingService;
        this.commentService = commentService;
    }

    /**
     * Enhance a LibraryItemResponse with rating and comment data
     */
    private LibraryItemResponse enhanceWithRatingAndComments(LibraryItemResponse response) {
        try {
            RatingResponse ratingStats = ratingService.getRatingStats(response.getItemId(), null);
            response.setAverageRating(ratingStats.getAverageRating());
            response.setTotalRatings(ratingStats.getTotalRatings());
        } catch (Exception e) {
            logger.warn("Could not load rating stats for item {}", response.getItemId());
            response.setAverageRating(0.0);
            response.setTotalRatings(0L);
        }

        try {
            Long commentCount = commentService.getCommentCount(response.getItemId());
            response.setCommentCount(commentCount);
        } catch (Exception e) {
            logger.warn("Could not load comment count for item {}", response.getItemId());
            response.setCommentCount(0L);
        }

        return response;
    }

    /**
     * Enhance a list of LibraryItemResponses with rating and comment data (batch)
     */
    private List<LibraryItemResponse> enhanceListWithRatingAndComments(List<LibraryItemResponse> responses) {
        if (responses.isEmpty()) {
            return responses;
        }

        List<String> itemIds = responses.stream()
                .map(LibraryItemResponse::getItemId)
                .collect(Collectors.toList());

        // Batch fetch rating stats
        Map<String, RatingResponse> ratingStats = ratingService.getBatchRatingStats(itemIds);

        // Batch fetch comment counts
        Map<String, Long> commentCounts = commentService.getBatchCommentCounts(itemIds);

        // Enhance each response
        for (LibraryItemResponse response : responses) {
            RatingResponse rating = ratingStats.get(response.getItemId());
            if (rating != null) {
                response.setAverageRating(rating.getAverageRating());
                response.setTotalRatings(rating.getTotalRatings());
            } else {
                response.setAverageRating(0.0);
                response.setTotalRatings(0L);
            }

            Long commentCount = commentCounts.get(response.getItemId());
            response.setCommentCount(commentCount != null ? commentCount : 0L);
        }

        return responses;
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
            LibraryItemResponse response = LibraryItemResponse.fromEntity(item);
            return ResponseEntity.ok(enhanceWithRatingAndComments(response));
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
        summary = "Search library (paginated)",
        description = "Search library items by keyword, OSCAL type, or tag with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<PageResponse<LibraryItemResponse>> searchLibrary(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String oscalType,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            size = Math.min(size, 100);

            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<LibraryItem> itemPage = libraryService.searchLibraryPaged(q, oscalType, tag, pageable);
            PageResponse<LibraryItemResponse> response = PageResponse.of(itemPage, LibraryItemResponse::fromEntity);
            response.setContent(enhanceListWithRatingAndComments(response.getContent()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get all library items (paginated)",
        description = "Retrieve all items in the library with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Library items retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PageResponse<LibraryItemResponse>> getAllLibraryItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            size = Math.min(size, 100);

            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<LibraryItem> itemPage = libraryService.getAllLibraryItemsPaged(pageable);
            PageResponse<LibraryItemResponse> response = PageResponse.of(itemPage, LibraryItemResponse::fromEntity);
            response.setContent(enhanceListWithRatingAndComments(response.getContent()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching all library items", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get items by OSCAL type (paginated)",
        description = "Retrieve library items of a specific OSCAL type with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Library items retrieved successfully")
    })
    @GetMapping("/type/{oscalType}")
    public ResponseEntity<PageResponse<LibraryItemResponse>> getItemsByType(
            @PathVariable String oscalType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            size = Math.min(size, 100);

            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<LibraryItem> itemPage = libraryService.getLibraryItemsByOscalTypePaged(oscalType, pageable);
            PageResponse<LibraryItemResponse> response = PageResponse.of(itemPage, LibraryItemResponse::fromEntity);
            response.setContent(enhanceListWithRatingAndComments(response.getContent()));

            return ResponseEntity.ok(response);
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
            return ResponseEntity.ok(enhanceListWithRatingAndComments(responses));
        } catch (Exception e) {
            logger.error("Error fetching popular library items", e);
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
            return ResponseEntity.ok(enhanceListWithRatingAndComments(responses));
        } catch (Exception e) {
            logger.error("Error fetching recent library items", e);
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
            List<Map<String, Object>> tags = libraryService.getAllTags();
            return ResponseEntity.ok(tags);
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
            List<Map<String, Object>> tags = libraryService.getPopularTags(limit);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== RATING ENDPOINTS ====================

    @Operation(
        summary = "Rate a library item",
        description = "Create or update your rating for a library item (1-5 stars)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rating submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid rating value"),
        @ApiResponse(responseCode = "404", description = "Library item not found")
    })
    @PostMapping("/{itemId}/ratings")
    public ResponseEntity<RatingResponse> rateItem(
            @PathVariable String itemId,
            @Valid @RequestBody RatingRequest request,
            Principal principal) {
        try {
            logger.info("Rating request for item {} by user {}", itemId, principal.getName());
            RatingResponse response = ratingService.rateItem(itemId, request.getRating(), principal.getName());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error rating item {}: {}", itemId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Get rating statistics for a library item",
        description = "Returns average rating, total ratings, and the current user's rating"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rating statistics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Library item not found")
    })
    @GetMapping("/{itemId}/ratings")
    public ResponseEntity<RatingResponse> getRatings(
            @PathVariable String itemId,
            Principal principal) {
        try {
            String username = principal != null ? principal.getName() : null;
            RatingResponse response = ratingService.getRatingStats(itemId, username);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error getting ratings for item {}: {}", itemId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Remove your rating from a library item",
        description = "Deletes the current user's rating for the specified library item"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Rating removed successfully"),
        @ApiResponse(responseCode = "404", description = "Library item or rating not found")
    })
    @DeleteMapping("/{itemId}/ratings")
    public ResponseEntity<Void> deleteRating(
            @PathVariable String itemId,
            Principal principal) {
        try {
            logger.info("Delete rating request for item {} by user {}", itemId, principal.getName());
            ratingService.deleteRating(itemId, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting rating for item {}: {}", itemId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== COMMENT ENDPOINTS ====================

    @Operation(
        summary = "Add a comment to a library item",
        description = "Create a new comment or reply to an existing comment"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Comment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid comment content"),
        @ApiResponse(responseCode = "404", description = "Library item or parent comment not found")
    })
    @PostMapping("/{itemId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable String itemId,
            @Valid @RequestBody CommentRequest request,
            Principal principal) {
        try {
            logger.info("Create comment request for item {} by user {}", itemId, principal.getName());
            CommentResponse response = commentService.createComment(
                    itemId,
                    request.getContent(),
                    request.getParentCommentId(),
                    principal.getName()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Error creating comment on item {}: {}", itemId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Get all comments for a library item",
        description = "Returns threaded comments with nested replies"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comments retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Library item not found")
    })
    @GetMapping("/{itemId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable String itemId) {
        try {
            List<CommentResponse> comments = commentService.getComments(itemId);
            return ResponseEntity.ok(comments);
        } catch (RuntimeException e) {
            logger.error("Error getting comments for item {}: {}", itemId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get comment count for a library item",
        description = "Returns the total number of comments (including replies)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comment count retrieved successfully")
    })
    @GetMapping("/{itemId}/comments/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable String itemId) {
        Long count = commentService.getCommentCount(itemId);
        return ResponseEntity.ok(count);
    }

    @Operation(
        summary = "Update a comment",
        description = "Edit the content of a comment. Only the comment owner can edit."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comment updated successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to edit this comment"),
        @ApiResponse(responseCode = "404", description = "Comment not found")
    })
    @PutMapping("/{itemId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable String itemId,
            @PathVariable String commentId,
            @Valid @RequestBody CommentRequest request,
            Principal principal) {
        try {
            logger.info("Update comment {} request by user {}", commentId, principal.getName());
            CommentResponse response = commentService.updateComment(
                    commentId,
                    request.getContent(),
                    principal.getName()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error updating comment {}: {}", commentId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Delete a comment",
        description = "Soft delete a comment. Only the comment owner can delete."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to delete this comment"),
        @ApiResponse(responseCode = "404", description = "Comment not found")
    })
    @DeleteMapping("/{itemId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String itemId,
            @PathVariable String commentId,
            Principal principal) {
        try {
            logger.info("Delete comment {} request by user {}", commentId, principal.getName());
            commentService.deleteComment(commentId, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting comment {}: {}", commentId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (e.getMessage().contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }
}
