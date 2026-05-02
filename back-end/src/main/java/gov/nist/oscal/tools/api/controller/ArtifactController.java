package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Artifact;
import gov.nist.oscal.tools.api.entity.Artifact.ArtifactVisibility;
import gov.nist.oscal.tools.api.entity.ArtifactVersion;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.model.PageResponse;
import gov.nist.oscal.tools.api.service.ArtifactCommentService;
import gov.nist.oscal.tools.api.service.ArtifactRatingService;
import gov.nist.oscal.tools.api.service.ArtifactService;
import gov.nist.oscal.tools.api.telemetry.EventNames;
import gov.nist.oscal.tools.api.telemetry.TelemetryService;
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
@RequestMapping("/api/artifacts")
@Tag(name = "Artifact Management", description = "APIs for managing Markdown artifact templates")
public class ArtifactController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ArtifactController.class);

    private final ArtifactService artifactService;
    private final ArtifactRatingService ratingService;
    private final ArtifactCommentService commentService;
    private final TelemetryService telemetryService;

    @Autowired
    public ArtifactController(ArtifactService artifactService,
                             ArtifactRatingService ratingService,
                             ArtifactCommentService commentService,
                             TelemetryService telemetryService) {
        this.artifactService = artifactService;
        this.ratingService = ratingService;
        this.commentService = commentService;
        this.telemetryService = telemetryService;
    }

    /**
     * Enhance an ArtifactResponse with rating and comment data
     */
    private ArtifactResponse enhanceWithRatingAndComments(ArtifactResponse response, String username) {
        try {
            RatingResponse ratingStats = ratingService.getRatingStats(response.getArtifactId(), username);
            response.setAverageRating(ratingStats.getAverageRating());
            response.setTotalRatings(ratingStats.getTotalRatings());
        } catch (Exception e) {
            logger.warn("Could not load rating stats for artifact {}", response.getArtifactId());
            response.setAverageRating(0.0);
            response.setTotalRatings(0L);
        }

        try {
            Long commentCount = commentService.getCommentCount(response.getArtifactId());
            response.setCommentCount(commentCount);
        } catch (Exception e) {
            logger.warn("Could not load comment count for artifact {}", response.getArtifactId());
            response.setCommentCount(0L);
        }

        return response;
    }

    /**
     * Enhance a list of ArtifactResponses with rating and comment data (batch)
     */
    private List<ArtifactResponse> enhanceListWithRatingAndComments(List<ArtifactResponse> responses) {
        if (responses.isEmpty()) {
            return responses;
        }

        List<String> artifactIds = responses.stream()
                .map(ArtifactResponse::getArtifactId)
                .collect(Collectors.toList());

        // Batch fetch rating stats
        Map<String, RatingResponse> ratingStats = ratingService.getBatchRatingStats(artifactIds);

        // Batch fetch comment counts
        Map<String, Long> commentCounts = commentService.getBatchCommentCounts(artifactIds);

        // Enhance each response
        for (ArtifactResponse response : responses) {
            RatingResponse rating = ratingStats.get(response.getArtifactId());
            if (rating != null) {
                response.setAverageRating(rating.getAverageRating());
                response.setTotalRatings(rating.getTotalRatings());
            } else {
                response.setAverageRating(0.0);
                response.setTotalRatings(0L);
            }

            Long commentCount = commentCounts.get(response.getArtifactId());
            response.setCommentCount(commentCount != null ? commentCount : 0L);
        }

        return responses;
    }

    // ==================== CRUD ENDPOINTS ====================

    @Operation(
        summary = "Create new artifact",
        description = "Create a new Markdown artifact template with metadata and visibility"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Artifact created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<ArtifactResponse> createArtifact(
            @Valid @RequestBody ArtifactRequest request,
            Principal principal) {
        try {
            Artifact artifact = artifactService.createArtifact(
                    request.getTitle(),
                    request.getDescription(),
                    request.getVisibility(),
                    request.getOrganizationId(),
                    request.getContent(),
                    request.getTags(),
                    principal.getName()
            );

            try {
                telemetryService.emit(EventNames.ARTIFACT_UPLOADED, Map.of(
                        "artifact_id", artifact.getArtifactId() != null ? artifact.getArtifactId() : "",
                        "visibility", request.getVisibility() != null ? request.getVisibility().name() : "",
                        "bytes", (long) (request.getContent() != null ? request.getContent().length() : 0)
                ));
            } catch (Exception telEx) {
                logger.debug("Telemetry emit failed (non-fatal): {}", telEx.getMessage());
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ArtifactResponse.fromEntity(artifact));
        } catch (RuntimeException e) {
            logger.error("Error creating artifact: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Update artifact metadata",
        description = "Update title, description, visibility, and tags of an artifact (owner only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Artifact updated successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to update this artifact"),
        @ApiResponse(responseCode = "404", description = "Artifact not found")
    })
    @PutMapping("/{artifactId}")
    public ResponseEntity<ArtifactResponse> updateArtifact(
            @PathVariable String artifactId,
            @RequestBody ArtifactUpdateRequest request,
            Principal principal) {
        try {
            Artifact artifact = artifactService.updateArtifact(
                    artifactId,
                    request.getTitle(),
                    request.getDescription(),
                    request.getVisibility(),
                    request.getOrganizationId(),
                    request.getTags(),
                    principal.getName()
            );

            return ResponseEntity.ok(ArtifactResponse.fromEntity(artifact));
        } catch (RuntimeException e) {
            logger.error("Error updating artifact {}: {}", artifactId, e.getMessage());
            if (e.getMessage().contains("Only the creator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get artifact by ID",
        description = "Retrieve a specific artifact with metadata (visibility checked)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Artifact found"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Artifact not found")
    })
    @GetMapping("/{artifactId}")
    public ResponseEntity<ArtifactResponse> getArtifact(
            @PathVariable String artifactId,
            Principal principal) {
        try {
            Artifact artifact = artifactService.getArtifact(artifactId, principal.getName());
            ArtifactResponse response = ArtifactResponse.fromEntity(artifact);
            return ResponseEntity.ok(enhanceWithRatingAndComments(response, principal.getName()));
        } catch (RuntimeException e) {
            logger.error("Error getting artifact {}: {}", artifactId, e.getMessage());
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Delete artifact",
        description = "Delete an artifact and all its versions (owner only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Artifact deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to delete this artifact"),
        @ApiResponse(responseCode = "404", description = "Artifact not found")
    })
    @DeleteMapping("/{artifactId}")
    public ResponseEntity<Void> deleteArtifact(
            @PathVariable String artifactId,
            Principal principal) {
        try {
            artifactService.deleteArtifact(artifactId, principal.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting artifact {}: {}", artifactId, e.getMessage());
            if (e.getMessage().contains("Only the creator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== CONTENT ENDPOINTS ====================

    @Operation(
        summary = "Get artifact content",
        description = "Download the current version Markdown content of an artifact"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Content retrieved"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Artifact not found")
    })
    @GetMapping("/{artifactId}/content")
    public ResponseEntity<Map<String, String>> getArtifactContent(
            @PathVariable String artifactId,
            Principal principal) {
        try {
            String content = artifactService.getCurrentVersionContent(artifactId, principal.getName());
            try {
                telemetryService.emit(EventNames.ARTIFACT_DOWNLOADED, Map.of(
                        "artifact_id", artifactId,
                        "bytes", (long) (content != null ? content.length() : 0)
                ));
            } catch (Exception telEx) {
                logger.debug("Telemetry emit failed (non-fatal): {}", telEx.getMessage());
            }
            return ResponseEntity.ok(Map.of("content", content));
        } catch (RuntimeException e) {
            logger.error("Error getting artifact content {}: {}", artifactId, e.getMessage());
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Add new version to artifact",
        description = "Add a new version with updated Markdown content"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Version added successfully"),
        @ApiResponse(responseCode = "404", description = "Artifact not found")
    })
    @PostMapping("/{artifactId}/versions")
    public ResponseEntity<ArtifactVersionResponse> addVersion(
            @PathVariable String artifactId,
            @Valid @RequestBody ArtifactVersionRequest request,
            Principal principal) {
        try {
            ArtifactVersion version = artifactService.addVersion(
                    artifactId,
                    request.getContent(),
                    request.getChangeDescription(),
                    principal.getName()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ArtifactVersionResponse.fromEntity(version));
        } catch (Exception e) {
            logger.error("Error adding version to artifact {}: {}", artifactId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get version history",
        description = "Retrieve all versions of an artifact"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Version history retrieved"),
        @ApiResponse(responseCode = "404", description = "Artifact not found")
    })
    @GetMapping("/{artifactId}/versions")
    public ResponseEntity<List<ArtifactVersionResponse>> getVersionHistory(@PathVariable String artifactId) {
        try {
            List<ArtifactVersion> versions = artifactService.getVersionHistory(artifactId);
            List<ArtifactVersionResponse> responses = versions.stream()
                    .map(ArtifactVersionResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error getting version history for artifact {}: {}", artifactId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get specific version content",
        description = "Download the Markdown content of a specific version"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Version content retrieved"),
        @ApiResponse(responseCode = "404", description = "Version not found")
    })
    @GetMapping("/versions/{versionId}/content")
    public ResponseEntity<Map<String, String>> getVersionContent(@PathVariable String versionId) {
        try {
            String content = artifactService.getVersionContent(versionId);
            return ResponseEntity.ok(Map.of("content", content));
        } catch (Exception e) {
            logger.error("Error getting version content {}: {}", versionId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== BROWSE ENDPOINTS ====================

    @Operation(
        summary = "Get all visible artifacts (paginated)",
        description = "Retrieve all artifacts visible to the current user (own + public + organization) with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Artifacts retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PageResponse<ArtifactResponse>> getAllArtifacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Principal principal) {
        try {
            // Enforce max page size
            size = Math.min(size, 100);

            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Artifact> artifactPage = artifactService.getAllVisibleArtifactsPaged(principal.getName(), pageable);
            PageResponse<ArtifactResponse> response = PageResponse.of(artifactPage, ArtifactResponse::fromEntity);

            // Enhance with ratings and comments
            response.setContent(enhanceListWithRatingAndComments(response.getContent()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching all artifacts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get my artifacts (paginated)",
        description = "Retrieve artifacts created by the current user with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Artifacts retrieved successfully")
    })
    @GetMapping("/my")
    public ResponseEntity<PageResponse<ArtifactResponse>> getMyArtifacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Principal principal) {
        try {
            size = Math.min(size, 100);

            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Artifact> artifactPage = artifactService.getMyArtifactsPaged(principal.getName(), pageable);
            PageResponse<ArtifactResponse> response = PageResponse.of(artifactPage, ArtifactResponse::fromEntity);
            response.setContent(enhanceListWithRatingAndComments(response.getContent()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching user's artifacts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get public artifacts (paginated)",
        description = "Retrieve all public artifacts with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Artifacts retrieved successfully")
    })
    @GetMapping("/public")
    public ResponseEntity<PageResponse<ArtifactResponse>> getPublicArtifacts(
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

            Page<Artifact> artifactPage = artifactService.getPublicArtifactsPaged(pageable);
            PageResponse<ArtifactResponse> response = PageResponse.of(artifactPage, ArtifactResponse::fromEntity);
            response.setContent(enhanceListWithRatingAndComments(response.getContent()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching public artifacts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get organization artifacts (paginated)",
        description = "Retrieve artifacts shared within a specific organization with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Artifacts retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "User not a member of this organization")
    })
    @GetMapping("/organization/{orgId}")
    public ResponseEntity<PageResponse<ArtifactResponse>> getOrganizationArtifacts(
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Principal principal) {
        try {
            size = Math.min(size, 100);

            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Artifact> artifactPage = artifactService.getOrganizationArtifactsPaged(orgId, principal.getName(), pageable);
            PageResponse<ArtifactResponse> response = PageResponse.of(artifactPage, ArtifactResponse::fromEntity);
            response.setContent(enhanceListWithRatingAndComments(response.getContent()));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error fetching organization artifacts: {}", e.getMessage());
            if (e.getMessage().contains("does not belong")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== SEARCH ENDPOINTS ====================

    @Operation(
        summary = "Search artifacts (paginated)",
        description = "Search artifacts by keyword, visibility, or tag with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<PageResponse<ArtifactResponse>> searchArtifacts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ArtifactVisibility visibility,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Principal principal) {
        try {
            size = Math.min(size, 100);

            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Artifact> artifactPage = artifactService.searchArtifactsPaged(q, visibility, tag, principal.getName(), pageable);
            PageResponse<ArtifactResponse> response = PageResponse.of(artifactPage, ArtifactResponse::fromEntity);
            response.setContent(enhanceListWithRatingAndComments(response.getContent()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error searching artifacts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get most popular artifacts",
        description = "Retrieve the most downloaded artifacts"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Popular artifacts retrieved successfully")
    })
    @GetMapping("/popular")
    public ResponseEntity<List<ArtifactResponse>> getMostPopular(
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        try {
            List<Artifact> artifacts = artifactService.getMostPopular(limit, principal.getName());
            List<ArtifactResponse> responses = artifacts.stream()
                    .map(ArtifactResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(enhanceListWithRatingAndComments(responses));
        } catch (Exception e) {
            logger.error("Error fetching popular artifacts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get recently updated artifacts",
        description = "Retrieve recently updated artifacts"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recent artifacts retrieved successfully")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<ArtifactResponse>> getRecentlyUpdated(
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        try {
            List<Artifact> artifacts = artifactService.getRecentlyUpdated(limit, principal.getName());
            List<ArtifactResponse> responses = artifacts.stream()
                    .map(ArtifactResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(enhanceListWithRatingAndComments(responses));
        } catch (Exception e) {
            logger.error("Error fetching recent artifacts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== ANALYTICS ENDPOINTS ====================

    @Operation(
        summary = "Get artifact analytics",
        description = "Retrieve statistics and analytics about artifacts"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully")
    })
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(Principal principal) {
        try {
            Map<String, Object> analytics = artifactService.getAnalytics(principal.getName());
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            logger.error("Error fetching artifact analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get all tags",
        description = "Retrieve all available tags for artifacts"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tags retrieved successfully")
    })
    @GetMapping("/tags")
    public ResponseEntity<List<Map<String, Object>>> getAllTags() {
        try {
            List<Map<String, Object>> tags = artifactService.getAllTags();
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            logger.error("Error fetching artifact tags", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get popular tags",
        description = "Retrieve the most popular tags for artifacts"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Popular tags retrieved successfully")
    })
    @GetMapping("/tags/popular")
    public ResponseEntity<List<Map<String, Object>>> getPopularTags(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> tags = artifactService.getPopularTags(limit);
            return ResponseEntity.ok(tags);
        } catch (Exception e) {
            logger.error("Error fetching popular artifact tags", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== RATING ENDPOINTS ====================

    @Operation(
        summary = "Rate an artifact",
        description = "Create or update your rating for an artifact (1-5 stars)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rating submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid rating value"),
        @ApiResponse(responseCode = "404", description = "Artifact not found")
    })
    @PostMapping("/{artifactId}/ratings")
    public ResponseEntity<RatingResponse> rateArtifact(
            @PathVariable String artifactId,
            @Valid @RequestBody RatingRequest request,
            Principal principal) {
        try {
            logger.info("Rating request for artifact {} by user {}", artifactId, principal.getName());
            RatingResponse response = ratingService.rateArtifact(artifactId, request.getRating(), principal.getName());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error rating artifact {}: {}", artifactId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Get rating statistics for an artifact",
        description = "Returns average rating, total ratings, and the current user's rating"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rating statistics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Artifact not found")
    })
    @GetMapping("/{artifactId}/ratings")
    public ResponseEntity<RatingResponse> getRatings(
            @PathVariable String artifactId,
            Principal principal) {
        try {
            String username = principal != null ? principal.getName() : null;
            RatingResponse response = ratingService.getRatingStats(artifactId, username);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error getting ratings for artifact {}: {}", artifactId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Remove your rating from an artifact",
        description = "Deletes the current user's rating for the specified artifact"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Rating removed successfully"),
        @ApiResponse(responseCode = "404", description = "Artifact or rating not found")
    })
    @DeleteMapping("/{artifactId}/ratings")
    public ResponseEntity<Void> deleteRating(
            @PathVariable String artifactId,
            Principal principal) {
        try {
            logger.info("Delete rating request for artifact {} by user {}", artifactId, principal.getName());
            ratingService.deleteRating(artifactId, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting rating for artifact {}: {}", artifactId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== COMMENT ENDPOINTS ====================

    @Operation(
        summary = "Add a comment to an artifact",
        description = "Create a new comment or reply to an existing comment"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Comment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid comment content"),
        @ApiResponse(responseCode = "404", description = "Artifact or parent comment not found")
    })
    @PostMapping("/{artifactId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable String artifactId,
            @Valid @RequestBody CommentRequest request,
            Principal principal) {
        try {
            logger.info("Create comment request for artifact {} by user {}", artifactId, principal.getName());
            CommentResponse response = commentService.createComment(
                    artifactId,
                    request.getContent(),
                    request.getParentCommentId(),
                    principal.getName()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Error creating comment on artifact {}: {}", artifactId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Get all comments for an artifact",
        description = "Returns threaded comments with nested replies"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comments retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Artifact not found")
    })
    @GetMapping("/{artifactId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable String artifactId) {
        try {
            List<CommentResponse> comments = commentService.getComments(artifactId);
            return ResponseEntity.ok(comments);
        } catch (RuntimeException e) {
            logger.error("Error getting comments for artifact {}: {}", artifactId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get comment count for an artifact",
        description = "Returns the total number of comments (including replies)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comment count retrieved successfully")
    })
    @GetMapping("/{artifactId}/comments/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable String artifactId) {
        Long count = commentService.getCommentCount(artifactId);
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
    @PutMapping("/{artifactId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable String artifactId,
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
    @DeleteMapping("/{artifactId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String artifactId,
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
