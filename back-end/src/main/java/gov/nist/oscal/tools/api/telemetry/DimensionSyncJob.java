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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * users (BQ table: users):
 *   id                       → user_id       (STRING, REQUIRED)
 *   username                 → username       (STRING)
 *   email                    → email          (STRING)
 *   first_name               → first_name     (STRING, nullable)
 *   last_name                → last_name      (STRING, nullable)
 *   organizationMemberships  → org_id_primary (STRING, primary org or null)
 *   global_role              → roles_global   (STRING, REPEATED — single-element array)
 *   enabled                  → active         (BOOL)
 *   created_at               → created_at     (TIMESTAMP)
 *   last_login               → last_login     (TIMESTAMP, nullable)
 *   (synthetic)              → synced_at      (TIMESTAMP, REQUIRED)
 *
 * orgs (BQ table: orgs):
 *   id          → org_id      (STRING, REQUIRED)
 *   name        → name        (STRING)
 *   description → description (STRING, nullable)
 *   active      → active      (BOOL)
 *   (synthetic) → member_count (INT64, always 0 — count not tracked in dimsync)
 *   created_at  → created_at  (TIMESTAMP)
 *   (synthetic) → synced_at   (TIMESTAMP, REQUIRED)
 * </pre>
 */
@Service
@Profile("dimsync")
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
     *
     * <p>{@code @Transactional(readOnly = true)} keeps the JPA session open for the
     * full sync so that lazy collections (e.g. {@code User.organizationMemberships})
     * can be safely walked while building the dimension rows.
     */
    @Transactional(readOnly = true)
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

        for (User u : all) {
            // Derive primary org ID from the first membership (if any).
            String primaryOrgId = u.getOrganizationMemberships() != null
                    && !u.getOrganizationMemberships().isEmpty()
                    ? String.valueOf(u.getOrganizationMemberships().iterator().next()
                            .getOrganization().getId())
                    : null;

            // roles_global is a REPEATED STRING column in BQ — wrap the single role in an array.
            String[] rolesGlobal = u.getGlobalRole() != null
                    ? new String[]{u.getGlobalRole().name()}
                    : new String[]{};

            String mergeQuery = String.format(
                    "MERGE `%s.%s.users` T\n" +
                    "USING (SELECT @user_id AS user_id) S\n" +
                    "ON T.user_id = S.user_id\n" +
                    "WHEN MATCHED THEN\n" +
                    "  UPDATE SET\n" +
                    "    username       = @username,\n" +
                    "    email          = @email,\n" +
                    "    first_name     = @first_name,\n" +
                    "    last_name      = @last_name,\n" +
                    "    org_id_primary = @org_id_primary,\n" +
                    "    roles_global   = @roles_global,\n" +
                    "    active         = @active,\n" +
                    "    last_login     = @last_login,\n" +
                    "    synced_at      = @synced_at\n" +
                    "WHEN NOT MATCHED THEN\n" +
                    "  INSERT (user_id, username, email, first_name, last_name,\n" +
                    "          org_id_primary, roles_global, active, created_at, last_login, synced_at)\n" +
                    "  VALUES (@user_id, @username, @email, @first_name, @last_name,\n" +
                    "          @org_id_primary, @roles_global, @active, @created_at, @last_login, @synced_at)",
                    project, dataset);

            QueryJobConfiguration q = QueryJobConfiguration.newBuilder(mergeQuery)
                    .addNamedParameter("user_id", QueryParameterValue.string(String.valueOf(u.getId())))
                    .addNamedParameter("username", QueryParameterValue.string(u.getUsername()))
                    .addNamedParameter("email", QueryParameterValue.string(u.getEmail()))
                    .addNamedParameter("first_name", QueryParameterValue.string(nullSafe(u.getFirstName())))
                    .addNamedParameter("last_name", QueryParameterValue.string(nullSafe(u.getLastName())))
                    .addNamedParameter("org_id_primary", QueryParameterValue.string(primaryOrgId))
                    .addNamedParameter("roles_global", QueryParameterValue.array(rolesGlobal, String.class))
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
        List<String> currentIds = all.stream()
                .map(u -> String.valueOf(u.getId()))
                .collect(Collectors.toList());

        // Build an ARRAY literal of current user IDs for the NOT IN UNNEST(...) clause.
        QueryParameterValue idsParam = QueryParameterValue.array(
                currentIds.toArray(new String[0]), String.class);

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
            // BQ table is `orgs` (not `organizations`); columns: org_id, name, description,
            // active, member_count (INT64), created_at, synced_at.  There is no updated_at column.
            // member_count is not readily available here; write 0 as a placeholder.
            String mergeQuery = String.format(
                    "MERGE `%s.%s.orgs` T\n" +
                    "USING (SELECT @org_id AS org_id) S\n" +
                    "ON T.org_id = S.org_id\n" +
                    "WHEN MATCHED THEN\n" +
                    "  UPDATE SET\n" +
                    "    name         = @name,\n" +
                    "    description  = @description,\n" +
                    "    active       = @active,\n" +
                    "    member_count = @member_count,\n" +
                    "    synced_at    = @synced_at\n" +
                    "WHEN NOT MATCHED THEN\n" +
                    "  INSERT (org_id, name, description, active, member_count, created_at, synced_at)\n" +
                    "  VALUES (@org_id, @name, @description, @active, @member_count, @created_at, @synced_at)",
                    project, dataset);

            QueryJobConfiguration q = QueryJobConfiguration.newBuilder(mergeQuery)
                    .addNamedParameter("org_id", QueryParameterValue.string(String.valueOf(o.getId())))
                    .addNamedParameter("name", QueryParameterValue.string(o.getName()))
                    .addNamedParameter("description", QueryParameterValue.string(nullSafe(o.getDescription())))
                    .addNamedParameter("active", QueryParameterValue.bool(Boolean.TRUE.equals(o.getActive())))
                    .addNamedParameter("member_count", QueryParameterValue.int64(0L))
                    .addNamedParameter("created_at", QueryParameterValue.timestamp(toMicros(
                            o.getCreatedAt() != null ? o.getCreatedAt().toInstant(java.time.ZoneOffset.UTC) : now)))
                    .addNamedParameter("synced_at", QueryParameterValue.timestamp(toMicros(now)))
                    .build();

            bq.query(q);
        }

        log.debug("syncOrgs: merged {} rows", all.size());
        return all.size();
    }

    int tombstoneOrgs(Instant now) throws Exception {
        List<Organization> all = orgRepo.findAll();
        List<String> currentIds = all.stream()
                .map(o -> String.valueOf(o.getId()))
                .collect(Collectors.toList());

        QueryParameterValue idsParam = QueryParameterValue.array(
                currentIds.toArray(new String[0]), String.class);

        String updateQuery = String.format(
                "UPDATE `%s.%s.orgs`\n" +
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
