package gov.nist.oscal.tools.api.health;

import gov.nist.oscal.tools.api.service.AzureBlobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Azure Blob Storage connectivity.
 * Checks if the application can connect to Azure Blob Storage or if it's using local storage.
 */
@Component
public class AzureBlobStorageHealthIndicator implements HealthIndicator {

    @Autowired(required = false)
    private AzureBlobService azureBlobService;

    @Override
    public Health health() {
        try {
            // If service is not available or not configured
            if (azureBlobService == null || !azureBlobService.isConfigured()) {
                return Health.up()
                        .withDetail("status", "not_configured")
                        .withDetail("storage", "local_fallback")
                        .withDetail("message", "Azure Blob Storage is not configured, using local storage")
                        .build();
            }

            // Storage is configured and ready
            return Health.up()
                    .withDetail("status", "configured")
                    .withDetail("storage", "azure_blob_storage")
                    .withDetail("message", "Azure Blob Storage is configured and ready")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "error")
                    .withDetail("error", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }
}
