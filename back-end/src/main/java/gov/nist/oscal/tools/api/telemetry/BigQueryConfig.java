package gov.nist.oscal.tools.api.telemetry;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Provides a {@link BigQuery} client bean for use by {@link DimensionSyncJob}.
 * The client is configured via Application Default Credentials (ADC), which on
 * Cloud Run resolves to the service account attached to the revision.
 *
 * <p>Scoped to the {@code dimsync} profile because {@link BigQueryOptions#getDefaultInstance()}
 * requires a project ID resolved from ADC or {@code GOOGLE_CLOUD_PROJECT}, which is
 * unavailable in tests or in the main API runtime. The dimension-sync workload is the
 * only consumer of this bean (via {@link DimensionSyncJob}, which carries the same
 * profile gate).
 */
@Configuration
@Profile("dimsync")
public class BigQueryConfig {

    @Bean
    public BigQuery bigQuery() {
        return BigQueryOptions.getDefaultInstance().getService();
    }
}
