package gov.nist.oscal.tools.api.model;

/**
 * Response DTO for rating statistics.
 */
public class RatingResponse {

    private Double averageRating;
    private Long totalRatings;
    private Integer userRating; // Current user's rating, null if not rated

    // Constructors
    public RatingResponse() {
    }

    public RatingResponse(Double averageRating, Long totalRatings) {
        this.averageRating = averageRating;
        this.totalRatings = totalRatings;
    }

    public RatingResponse(Double averageRating, Long totalRatings, Integer userRating) {
        this.averageRating = averageRating;
        this.totalRatings = totalRatings;
        this.userRating = userRating;
    }

    // Static factory method
    public static RatingResponse of(Double averageRating, Long totalRatings, Integer userRating) {
        return new RatingResponse(averageRating, totalRatings, userRating);
    }

    public static RatingResponse empty() {
        return new RatingResponse(0.0, 0L, null);
    }

    // Getters and Setters
    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Long getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Long totalRatings) {
        this.totalRatings = totalRatings;
    }

    public Integer getUserRating() {
        return userRating;
    }

    public void setUserRating(Integer userRating) {
        this.userRating = userRating;
    }
}
