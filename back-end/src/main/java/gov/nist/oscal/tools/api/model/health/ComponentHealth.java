package gov.nist.oscal.tools.api.model.health;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Health status for an individual component.
 */
@Schema(description = "Health status for a single component")
public class ComponentHealth {

    @Schema(description = "Component health status", example = "UP")
    private String status;

    @Schema(description = "Human-readable status message", example = "Database connection pool is healthy")
    private String message;

    @Schema(description = "Additional details about the component")
    private Map<String, Object> details;

    @Schema(description = "Response time in milliseconds for the health check", example = "15")
    private Long responseTimeMs;

    public ComponentHealth() {
    }

    public ComponentHealth(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public ComponentHealth(String status, String message, Map<String, Object> details, Long responseTimeMs) {
        this.status = status;
        this.message = message;
        this.details = details;
        this.responseTimeMs = responseTimeMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    // Builder pattern for convenience
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String status;
        private String message;
        private Map<String, Object> details;
        private Long responseTimeMs;

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public Builder responseTimeMs(Long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
            return this;
        }

        public ComponentHealth build() {
            return new ComponentHealth(status, message, details, responseTimeMs);
        }
    }
}
