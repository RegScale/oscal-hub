package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Artifact;
import gov.nist.oscal.tools.api.entity.ArtifactRating;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ArtifactRating entity.
 * Provides methods for managing user ratings on artifacts.
 */
@Repository
public interface ArtifactRatingRepository extends JpaRepository<ArtifactRating, Long> {

    /**
     * Find a user's rating for a specific artifact.
     */
    Optional<ArtifactRating> findByArtifactAndUser(Artifact artifact, User user);

    /**
     * Find a user's rating by artifact ID and user.
     */
    @Query("SELECT r FROM ArtifactRating r WHERE r.artifact.artifactId = :artifactId AND r.user = :user")
    Optional<ArtifactRating> findByArtifactIdAndUser(@Param("artifactId") String artifactId, @Param("user") User user);

    /**
     * Get all ratings for a specific artifact.
     */
    List<ArtifactRating> findByArtifact(Artifact artifact);

    /**
     * Calculate average rating for an artifact by its UUID.
     */
    @Query("SELECT AVG(r.rating) FROM ArtifactRating r WHERE r.artifact.artifactId = :artifactId")
    Double getAverageRatingByArtifactId(@Param("artifactId") String artifactId);

    /**
     * Count total ratings for an artifact by its UUID.
     */
    @Query("SELECT COUNT(r) FROM ArtifactRating r WHERE r.artifact.artifactId = :artifactId")
    Long countByArtifactId(@Param("artifactId") String artifactId);

    /**
     * Batch query to get rating statistics for multiple artifacts.
     * Returns [artifactId, averageRating, totalRatings] for each artifact.
     */
    @Query("SELECT r.artifact.artifactId, AVG(r.rating), COUNT(r) FROM ArtifactRating r " +
           "WHERE r.artifact.artifactId IN :artifactIds GROUP BY r.artifact.artifactId")
    List<Object[]> getRatingStatsByArtifactIds(@Param("artifactIds") List<String> artifactIds);

    /**
     * Check if a user has already rated an artifact.
     */
    boolean existsByArtifactAndUser(Artifact artifact, User user);

    /**
     * Delete a user's rating for an artifact.
     */
    @Modifying
    @Query("DELETE FROM ArtifactRating r WHERE r.artifact.artifactId = :artifactId AND r.user = :user")
    void deleteByArtifactIdAndUser(@Param("artifactId") String artifactId, @Param("user") User user);
}
