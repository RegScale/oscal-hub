package gov.nist.oscal.tools.api.telemetry;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a {@link BigQuery} client bean for use by {@link DimensionSyncJob}.
 * The client is configured via Application Default Credentials (ADC), which on
 * Cloud Run resolves to the service account attached to the revision.
 */
@Configuration
public class BigQueryConfig {

    @Bean
    public BigQuery bigQuery() {
        return BigQueryOptions.getDefaultInstance().getService();
    }
}
