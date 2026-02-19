package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryItemRating;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.RatingResponse;
import gov.nist.oscal.tools.api.repository.LibraryItemRatingRepository;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
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
 * Service for managing library item ratings.
 */
@Service
public class LibraryRatingService {

    private static final Logger logger = LoggerFactory.getLogger(LibraryRatingService.class);

    @Autowired
    private LibraryItemRatingRepository ratingRepository;

    @Autowired
    private LibraryItemRepository libraryItemRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Rate a library item. Creates a new rating or updates existing one (upsert).
     *
     * @param itemId   The library item UUID
     * @param rating   The rating value (1-5)
     * @param username The username of the rater
     * @return The rating response with updated statistics
     */
    @Transactional
    public RatingResponse rateItem(String itemId, Integer rating, String username) {
        logger.info("User {} rating item {} with {} stars", username, itemId, rating);

        // Find the library item
        LibraryItem libraryItem = libraryItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Library item not found: " + itemId));

        // Find the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Check for existing rating (upsert pattern)
        Optional<LibraryItemRating> existingRating = ratingRepository.findByItemIdAndUser(itemId, user);

        if (existingRating.isPresent()) {
            // Update existing rating
            LibraryItemRating ratingEntity = existingRating.get();
            ratingEntity.setRating(rating);
            ratingRepository.save(ratingEntity);
            logger.info("Updated existing rating for user {} on item {}", username, itemId);
        } else {
            // Create new rating
            LibraryItemRating newRating = new LibraryItemRating(libraryItem, user, rating);
            ratingRepository.save(newRating);
            logger.info("Created new rating for user {} on item {}", username, itemId);
        }

        // Return updated statistics
        return getRatingStats(itemId, username);
    }

    /**
     * Get rating statistics for a library item.
     *
     * @param itemId   The library item UUID
     * @param username The current user's username (optional, for getting their rating)
     * @return Rating statistics including average, total, and user's rating
     */
    public RatingResponse getRatingStats(String itemId, String username) {
        Double averageRating = ratingRepository.getAverageRatingByItemId(itemId);
        Long totalRatings = ratingRepository.countByItemId(itemId);

        Integer userRating = null;
        if (username != null) {
            userRepository.findByUsername(username).ifPresent(user -> {
                // Check for user's rating
            });

            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                Optional<LibraryItemRating> existingRating = ratingRepository.findByItemIdAndUser(itemId, user);
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
     * Delete a user's rating for a library item.
     *
     * @param itemId   The library item UUID
     * @param username The username of the rater
     */
    @Transactional
    public void deleteRating(String itemId, String username) {
        logger.info("User {} removing rating from item {}", username, itemId);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Optional<LibraryItemRating> existingRating = ratingRepository.findByItemIdAndUser(itemId, user);
        if (existingRating.isPresent()) {
            ratingRepository.delete(existingRating.get());
            logger.info("Deleted rating for user {} on item {}", username, itemId);
        }
    }

    /**
     * Get rating statistics for multiple library items (for efficient card display).
     *
     * @param itemIds List of library item UUIDs
     * @return Map of itemId to RatingResponse
     */
    public Map<String, RatingResponse> getBatchRatingStats(List<String> itemIds) {
        Map<String, RatingResponse> result = new HashMap<>();

        if (itemIds == null || itemIds.isEmpty()) {
            return result;
        }

        // Initialize all items with empty ratings
        for (String itemId : itemIds) {
            result.put(itemId, RatingResponse.empty());
        }

        // Batch query for rating stats
        List<Object[]> stats = ratingRepository.getRatingStatsByItemIds(itemIds);
        for (Object[] stat : stats) {
            String itemId = (String) stat[0];
            Double avgRating = (Double) stat[1];
            Long totalRatings = (Long) stat[2];
            result.put(itemId, new RatingResponse(avgRating, totalRatings));
        }

        return result;
    }
}
