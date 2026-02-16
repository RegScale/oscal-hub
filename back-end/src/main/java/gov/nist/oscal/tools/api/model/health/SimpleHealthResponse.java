package gov.nist.oscal.tools.api.model.health;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Simple health response for public health check endpoint.
 * Used by load balancers and basic monitoring.
 */
@Schema(description = "Simple health check response")
public class SimpleHealthResponse {

    @Schema(description = "Health status", example = "UP")
    private String status;

    @Schema(description = "Timestamp of health check", example = "2024-01-15T10:30:00Z")
    private String timestamp;

    @Schema(description = "Application version", example = "1.0.0")
    private String version;

    public SimpleHealthResponse() {
    }

    public SimpleHealthResponse(String status, String timestamp, String version) {
        this.status = status;
        this.timestamp = timestamp;
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
