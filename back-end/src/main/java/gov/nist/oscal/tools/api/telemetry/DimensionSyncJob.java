package gov.nist.oscal.tools.api.telemetry;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hourly job that mirrors the {@code users} and {@code organizations} tables from
 * Postgres into BigQuery dimension tables used by the analytics dataset.
 *
 * <p><b>Strategy:</b> for each entity, a per-row MERGE statement is issued so that
 * existing BQ rows are updated in place (rather than deleting and re-inserting the
 * whole table, which would break in-flight queries).  After syncing the live rows a
 * tombstone UPDATE marks any BQ rows whose IDs are no longer in Postgres as inactive.
 *
 * <p><b>Scale note:</b> at thousands of rows per hour, per-row MERGE queries are
 * acceptable.  If the table grows beyond ~50k rows, switch to the BigQuery Storage
 * Write API (streaming inserts to a staging table + MERGE from there).
 *
 * <p>Entity field mapping (Postgres → BigQuery):
 * <pre>
 * users:
 *   id            → user_id  (INT64)
 *   username      → username (STRING)
 *   email         → email    (STRING)
 *   first_name    → first_name (STRING, nullable)
 *   last_name     → last_name  (STRING, nullable)
 *   global_role   → global_role (STRING)
 *   enabled       → active   (BOOL)
 *   created_at    → created_at (TIMESTAMP)
 *   last_login    → last_login (TIMESTAMP, nullable)
 *   (synthetic)   → synced_at  (TIMESTAMP)
 *
 * organizations:
 *   id          → org_id      (INT64)
 *   name        → name        (STRING)
 *   description → description (STRING, nullable)
 *   active      → active      (BOOL)
 *   created_at  → created_at  (TIMESTAMP)
 *   updated_at  → updated_at  (TIMESTAMP, nullable)
 *   (synthetic) → synced_at   (TIMESTAMP)
 * </pre>
 */
@Service
public class DimensionSyncJob {

    private static final Logger log = LoggerFactory.getLogger(DimensionSyncJob.class);

    private final UserRepository userRepo;
    private final OrganizationRepository orgRepo;
    private final BigQuery bq;
    private final String dataset;
    private final String project;

    public DimensionSyncJob(UserRepository userRepo,
                            OrganizationRepository orgRepo,
                            BigQuery bq,
                            @Value("${ANALYTICS_DATASET_ID:analytics_prod}") String dataset,
                            @Value("${GCP_PROJECT_ID:oscal-hub}") String project) {
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
        this.bq = bq;
        this.dataset = dataset;
        this.project = project;
    }

    /**
     * Entry point called by {@link DimensionSyncRunner}.
     */
    public void run() throws Exception {
        Instant now = Instant.now();
        int users = syncUsers(now);
        int orgs = syncOrgs(now);
        int userTombstones = tombstoneUsers(now);
        int orgTombstones = tombstoneOrgs(now);
        log.info("dimsync done: users={} orgs={} userTombstones={} orgTombstones={}",
                users, orgs, userTombstones, orgTombstones);
    }

    // -------------------------------------------------------------------------
    // Users
    // -------------------------------------------------------------------------

    int syncUsers(Instant now) throws Exception {
        List<User> all = userRepo.findAll();
        String nowStr = now.toString();

        for (User u : all) {
            String mergeQuery = String.format(
                    "MERGE `%s.%s.users` T\n" +
                    "USING (SELECT @user_id AS user_id) S\n" +
                    "ON T.user_id = S.user_id\n" +
                    "WHEN MATCHED THEN\n" +
                    "  UPDATE SET\n" +
                    "    username    = @username,\n" +
                    "    email       = @email,\n" +
                    "    first_name  = @first_name,\n" +
                    "    last_name   = @last_name,\n" +
                    "    global_role = @global_role,\n" +
                    "    active      = @active,\n" +
                    "    last_login  = @last_login,\n" +
                    "    synced_at   = @synced_at\n" +
                    "WHEN NOT MATCHED THEN\n" +
                    "  INSERT (user_id, username, email, first_name, last_name,\n" +
                    "          global_role, active, created_at, last_login, synced_at)\n" +
                    "  VALUES (@user_id, @username, @email, @first_name, @last_name,\n" +
                    "          @global_role, @active, @created_at, @last_login, @synced_at)",
                    project, dataset);

            QueryJobConfiguration q = QueryJobConfiguration.newBuilder(mergeQuery)
                    .addNamedParameter("user_id", QueryParameterValue.int64(u.getId()))
                    .addNamedParameter("username", QueryParameterValue.string(u.getUsername()))
                    .addNamedParameter("email", QueryParameterValue.string(u.getEmail()))
                    .addNamedParameter("first_name", QueryParameterValue.string(nullSafe(u.getFirstName())))
                    .addNamedParameter("last_name", QueryParameterValue.string(nullSafe(u.getLastName())))
                    .addNamedParameter("global_role", QueryParameterValue.string(
                            u.getGlobalRole() != null ? u.getGlobalRole().name() : "USER"))
                    .addNamedParameter("active", QueryParameterValue.bool(Boolean.TRUE.equals(u.getEnabled())))
                    .addNamedParameter("created_at", QueryParameterValue.timestamp(toMicros(
                            u.getCreatedAt() != null ? u.getCreatedAt().toInstant(java.time.ZoneOffset.UTC) : now)))
                    .addNamedParameter("last_login", QueryParameterValue.timestamp(
                            u.getLastLogin() != null
                                    ? toMicros(u.getLastLogin().toInstant(java.time.ZoneOffset.UTC))
                                    : null))
                    .addNamedParameter("synced_at", QueryParameterValue.timestamp(toMicros(now)))
                    .build();

            bq.query(q);
        }

        log.debug("syncUsers: merged {} rows", all.size());
        return all.size();
    }

    int tombstoneUsers(Instant now) throws Exception {
        List<User> all = userRepo.findAll();
        List<Long> currentIds = all.stream().map(User::getId).collect(Collectors.toList());

        // Build an ARRAY literal of current user IDs for the NOT IN UNNEST(...) clause.
        QueryParameterValue idsParam = QueryParameterValue.array(
                currentIds.toArray(new Long[0]), Long.class);

        String updateQuery = String.format(
                "UPDATE `%s.%s.users`\n" +
                "SET active=false, synced_at=@synced_at\n" +
                "WHERE active=true AND user_id NOT IN UNNEST(@current_ids)",
                project, dataset);

        QueryJobConfiguration q = QueryJobConfiguration.newBuilder(updateQuery)
                .addNamedParameter("current_ids", idsParam)
                .addNamedParameter("synced_at", QueryParameterValue.timestamp(toMicros(now)))
                .build();

        bq.query(q);
        log.debug("tombstoneUsers: issued tombstone update for ids not in set of {}", currentIds.size());
        // BigQuery UPDATE does not return affected row count via this API path; return 0 as sentinel.
        return 0;
    }

    // -------------------------------------------------------------------------
    // Organizations
    // -------------------------------------------------------------------------

    int syncOrgs(Instant now) throws Exception {
        List<Organization> all = orgRepo.findAll();

        for (Organization o : all) {
            String mergeQuery = String.format(
                    "MERGE `%s.%s.organizations` T\n" +
                    "USING (SELECT @org_id AS org_id) S\n" +
                    "ON T.org_id = S.org_id\n" +
                    "WHEN MATCHED THEN\n" +
                    "  UPDATE SET\n" +
                    "    name        = @name,\n" +
                    "    description = @description,\n" +
                    "    active      = @active,\n" +
                    "    updated_at  = @updated_at,\n" +
                    "    synced_at   = @synced_at\n" +
                    "WHEN NOT MATCHED THEN\n" +
                    "  INSERT (org_id, name, description, active, created_at, updated_at, synced_at)\n" +
                    "  VALUES (@org_id, @name, @description, @active, @created_at, @updated_at, @synced_at)",
                    project, dataset);

            QueryJobConfiguration q = QueryJobConfiguration.newBuilder(mergeQuery)
                    .addNamedParameter("org_id", QueryParameterValue.int64(o.getId()))
                    .addNamedParameter("name", QueryParameterValue.string(o.getName()))
                    .addNamedParameter("description", QueryParameterValue.string(nullSafe(o.getDescription())))
                    .addNamedParameter("active", QueryParameterValue.bool(Boolean.TRUE.equals(o.getActive())))
                    .addNamedParameter("created_at", QueryParameterValue.timestamp(toMicros(
                            o.getCreatedAt() != null ? o.getCreatedAt().toInstant(java.time.ZoneOffset.UTC) : now)))
                    .addNamedParameter("updated_at", QueryParameterValue.timestamp(
                            o.getUpdatedAt() != null
                                    ? toMicros(o.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC))
                                    : null))
                    .addNamedParameter("synced_at", QueryParameterValue.timestamp(toMicros(now)))
                    .build();

            bq.query(q);
        }

        log.debug("syncOrgs: merged {} rows", all.size());
        return all.size();
    }

    int tombstoneOrgs(Instant now) throws Exception {
        List<Organization> all = orgRepo.findAll();
        List<Long> currentIds = all.stream().map(Organization::getId).collect(Collectors.toList());

        QueryParameterValue idsParam = QueryParameterValue.array(
                currentIds.toArray(new Long[0]), Long.class);

        String updateQuery = String.format(
                "UPDATE `%s.%s.organizations`\n" +
                "SET active=false, synced_at=@synced_at\n" +
                "WHERE active=true AND org_id NOT IN UNNEST(@current_ids)",
                project, dataset);

        QueryJobConfiguration q = QueryJobConfiguration.newBuilder(updateQuery)
                .addNamedParameter("current_ids", idsParam)
                .addNamedParameter("synced_at", QueryParameterValue.timestamp(toMicros(now)))
                .build();

        bq.query(q);
        log.debug("tombstoneOrgs: issued tombstone update for ids not in set of {}", currentIds.size());
        return 0;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Convert {@link Instant} to microseconds since epoch (BigQuery TIMESTAMP unit). */
    private static long toMicros(Instant instant) {
        return instant.getEpochSecond() * 1_000_000L + instant.getNano() / 1_000L;
    }

    /** Return {@code value}, or an empty string if null (BigQuery STRING params disallow null via this path). */
    private static String nullSafe(String value) {
        return value != null ? value : "";
    }
}
