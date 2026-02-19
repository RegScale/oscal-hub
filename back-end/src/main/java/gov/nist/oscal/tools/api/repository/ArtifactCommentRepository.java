package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Artifact;
import gov.nist.oscal.tools.api.entity.ArtifactComment;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ArtifactComment entity.
 * Provides methods for managing threaded comments on artifacts.
 */
@Repository
public interface ArtifactCommentRepository extends JpaRepository<ArtifactComment, Long> {

    /**
     * Find a comment by its UUID.
     * Uses JOIN FETCH to eagerly load user to avoid LazyInitializationException.
     */
    @Query("SELECT c FROM ArtifactComment c JOIN FETCH c.user WHERE c.commentId = :commentId")
    Optional<ArtifactComment> findByCommentId(@Param("commentId") String commentId);

    /**
     * Get top-level comments (no parent) for an artifact, excluding deleted.
     * Ordered by creation date descending (newest first).
     * Uses JOIN FETCH to eagerly load user to avoid LazyInitializationException.
     */
    @Query("SELECT c FROM ArtifactComment c JOIN FETCH c.user WHERE c.artifact.artifactId = :artifactId " +
           "AND c.parentComment IS NULL AND c.deleted = false ORDER BY c.createdAt DESC")
    List<ArtifactComment> findTopLevelCommentsByArtifactId(@Param("artifactId") String artifactId);

    /**
     * Get all comments for an artifact (including replies), excluding deleted.
     * Ordered by creation date ascending for proper thread ordering.
     * Uses JOIN FETCH to eagerly load user to avoid LazyInitializationException.
     */
    @Query("SELECT c FROM ArtifactComment c JOIN FETCH c.user WHERE c.artifact.artifactId = :artifactId " +
           "AND c.deleted = false ORDER BY c.createdAt ASC")
    List<ArtifactComment> findAllByArtifactId(@Param("artifactId") String artifactId);

    /**
     * Count total non-deleted comments for an artifact (including replies).
     */
    @Query("SELECT COUNT(c) FROM ArtifactComment c WHERE c.artifact.artifactId = :artifactId " +
           "AND c.deleted = false")
    Long countByArtifactId(@Param("artifactId") String artifactId);

    /**
     * Batch query to get comment counts for multiple artifacts.
     * Returns [artifactId, commentCount] for each artifact.
     */
    @Query("SELECT c.artifact.artifactId, COUNT(c) FROM ArtifactComment c " +
           "WHERE c.artifact.artifactId IN :artifactIds AND c.deleted = false GROUP BY c.artifact.artifactId")
    List<Object[]> countByArtifactIds(@Param("artifactIds") List<String> artifactIds);

    /**
     * Find replies to a specific comment, excluding deleted.
     * Uses JOIN FETCH to eagerly load user to avoid LazyInitializationException.
     */
    @Query("SELECT c FROM ArtifactComment c JOIN FETCH c.user WHERE c.parentComment = :parentComment " +
           "AND c.deleted = false ORDER BY c.createdAt ASC")
    List<ArtifactComment> findRepliesByParentComment(@Param("parentComment") ArtifactComment parentComment);

    /**
     * Find comments by a specific user for an artifact.
     * Uses JOIN FETCH to eagerly load user to avoid LazyInitializationException.
     */
    @Query("SELECT c FROM ArtifactComment c JOIN FETCH c.user WHERE c.artifact.artifactId = :artifactId " +
           "AND c.user = :user AND c.deleted = false ORDER BY c.createdAt DESC")
    List<ArtifactComment> findByArtifactIdAndUser(@Param("artifactId") String artifactId, @Param("user") User user);

    /**
     * Check if a comment exists and belongs to a user.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ArtifactComment c " +
           "WHERE c.commentId = :commentId AND c.user = :user AND c.deleted = false")
    boolean existsByCommentIdAndUser(@Param("commentId") String commentId, @Param("user") User user);

    /**
     * Count replies to a specific comment.
     */
    @Query("SELECT COUNT(c) FROM ArtifactComment c WHERE c.parentComment.commentId = :parentCommentId " +
           "AND c.deleted = false")
    Long countRepliesByParentCommentId(@Param("parentCommentId") String parentCommentId);
}
