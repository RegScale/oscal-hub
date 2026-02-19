package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Artifact;
import gov.nist.oscal.tools.api.entity.ArtifactRating;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.RatingResponse;
import gov.nist.oscal.tools.api.repository.ArtifactRatingRepository;
import gov.nist.oscal.tools.api.repository.ArtifactRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing artifact ratings.
 */
@Service
public class ArtifactRatingService {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactRatingService.class);

    @Autowired
    private ArtifactRatingRepository ratingRepository;

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Rate an artifact. Creates a new rating or updates existing one (upsert).
     *
     * @param artifactId The artifact UUID
     * @param rating     The rating value (1-5)
     * @param username   The username of the rater
     * @return The rating response with updated statistics
     */
    @Transactional
    public RatingResponse rateArtifact(String artifactId, Integer rating, String username) {
        logger.info("User {} rating artifact {} with {} stars", username, artifactId, rating);

        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        // Find the artifact
        Artifact artifact = artifactRepository.findByArtifactId(artifactId)
                .orElseThrow(() -> new RuntimeException("Artifact not found: " + artifactId));

        // Find the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Check for existing rating (upsert pattern)
        Optional<ArtifactRating> existingRating = ratingRepository.findByArtifactIdAndUser(artifactId, user);

        if (existingRating.isPresent()) {
            // Update existing rating
            ArtifactRating ratingEntity = existingRating.get();
            ratingEntity.setRating(rating);
            ratingRepository.save(ratingEntity);
            logger.info("Updated existing rating for user {} on artifact {}", username, artifactId);
        } else {
            // Create new rating
            ArtifactRating newRating = new ArtifactRating(artifact, user, rating);
            ratingRepository.save(newRating);
            logger.info("Created new rating for user {} on artifact {}", username, artifactId);
        }

        // Return updated statistics
        return getRatingStats(artifactId, username);
    }

    /**
     * Get rating statistics for an artifact.
     *
     * @param artifactId The artifact UUID
     * @param username   The current user's username (optional, for getting their rating)
     * @return Rating statistics including average, total, and user's rating
     */
    public RatingResponse getRatingStats(String artifactId, String username) {
        Double averageRating = ratingRepository.getAverageRatingByArtifactId(artifactId);
        Long totalRatings = ratingRepository.countByArtifactId(artifactId);

        Integer userRating = null;
        if (username != null) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                Optional<ArtifactRating> existingRating = ratingRepository.findByArtifactIdAndUser(artifactId, user);
                if (existingRating.isPresent()) {
                    userRating = existingRating.get().getRating();
                }
            }
        }

        // Handle null average (no ratings yet)
        if (averageRating == null) {
            averageRating = 0.0;
        }
        if (totalRatings == null) {
            totalRatings = 0L;
        }

        return RatingResponse.of(averageRating, totalRatings, userRating);
    }

    /**
     * Delete a user's rating for an artifact.
     *
     * @param artifactId The artifact UUID
     * @param username   The username of the rater
     */
    @Transactional
    public void deleteRating(String artifactId, String username) {
        logger.info("User {} removing rating from artifact {}", username, artifactId);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Optional<ArtifactRating> existingRating = ratingRepository.findByArtifactIdAndUser(artifactId, user);
        if (existingRating.isPresent()) {
            ratingRepository.delete(existingRating.get());
            logger.info("Deleted rating for user {} on artifact {}", username, artifactId);
        }
    }

    /**
     * Get rating statistics for multiple artifacts (for efficient card display).
     *
     * @param artifactIds List of artifact UUIDs
     * @return Map of artifactId to RatingResponse
     */
    public Map<String, RatingResponse> getBatchRatingStats(List<String> artifactIds) {
        Map<String, RatingResponse> result = new HashMap<>();

        if (artifactIds == null || artifactIds.isEmpty()) {
            return result;
        }

        // Initialize all artifacts with empty ratings
        for (String artifactId : artifactIds) {
            result.put(artifactId, RatingResponse.empty());
        }

        // Batch query for rating stats
        List<Object[]> stats = ratingRepository.getRatingStatsByArtifactIds(artifactIds);
        for (Object[] stat : stats) {
            String artifactId = (String) stat[0];
            Double avgRating = (Double) stat[1];
            Long totalRatings = (Long) stat[2];
            result.put(artifactId, new RatingResponse(avgRating, totalRatings));
        }

        return result;
    }
}
