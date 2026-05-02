package gov.nist.oscal.tools.api.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Spring Boot {@link CommandLineRunner} that executes {@link DimensionSyncJob} and
 * exits the process.  Active only when the {@code dimsync} profile is set, which is
 * the case when the Cloud Run Job overrides {@code SPRING_PROFILES_ACTIVE=dimsync}.
 *
 * <p>Exit codes:
 * <ul>
 *   <li>0 — sync completed successfully</li>
 *   <li>1 — sync threw an exception (Cloud Run Job will mark the task as failed)</li>
 * </ul>
 */
@Component
@Profile("dimsync")
public class DimensionSyncRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DimensionSyncRunner.class);

    private final DimensionSyncJob job;

    public DimensionSyncRunner(DimensionSyncJob job) {
        this.job = job;
    }

    @Override
    public void run(String... args) {
        try {
            job.run();
            System.exit(0);
        } catch (Exception e) {
            log.error("dimsync failed", e);
            System.exit(1);
        }
    }
}
