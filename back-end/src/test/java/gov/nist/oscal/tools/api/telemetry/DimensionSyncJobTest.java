package gov.nist.oscal.tools.api.telemetry;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DimensionSyncJobTest {

    private UserRepository userRepo;
    private OrganizationRepository orgRepo;
    private BigQuery bq;
    private DimensionSyncJob job;

    @BeforeEach
    void setUp() throws Exception {
        userRepo = mock(UserRepository.class);
        orgRepo = mock(OrganizationRepository.class);
        bq = mock(BigQuery.class);

        TableResult ok = mock(TableResult.class);
        when(bq.query(any(QueryJobConfiguration.class))).thenReturn(ok);

        job = new DimensionSyncJob(userRepo, orgRepo, bq, "analytics_test", "test-project");
    }

    // -------------------------------------------------------------------------
    // Empty-data smoke tests
    // -------------------------------------------------------------------------

    @Test
    void runWithEmptyDataDoesNotThrow() throws Exception {
        when(userRepo.findAll()).thenReturn(Collections.emptyList());
        when(orgRepo.findAll()).thenReturn(Collections.emptyList());

        job.run();

        verify(userRepo, atLeast(1)).findAll();
        verify(orgRepo, atLeast(1)).findAll();
    }

    @Test
    void emptyReposIssueOnlyTombstoneQueries() throws Exception {
        when(userRepo.findAll()).thenReturn(Collections.emptyList());
        when(orgRepo.findAll()).thenReturn(Collections.emptyList());

        job.run();

        // With empty repos, only the two tombstone UPDATE queries should be issued
        // (no per-row MERGE queries).
        ArgumentCaptor<QueryJobConfiguration> cap =
                ArgumentCaptor.forClass(QueryJobConfiguration.class);
        verify(bq, times(2)).query(cap.capture());

        long mergeCount = cap.getAllValues().stream()
                .filter(q -> q.getQuery().toUpperCase().contains("MERGE"))
                .count();
        assertEquals(0, mergeCount, "No MERGE queries should be issued when repos are empty");
    }

    // -------------------------------------------------------------------------
    // Tombstone tests
    // -------------------------------------------------------------------------

    @Test
    void tombstoneUpdateContainsSetActiveFalse() throws Exception {
        when(userRepo.findAll()).thenReturn(Collections.emptyList());
        when(orgRepo.findAll()).thenReturn(Collections.emptyList());

        job.run();

        ArgumentCaptor<QueryJobConfiguration> cap =
                ArgumentCaptor.forClass(QueryJobConfiguration.class);
        verify(bq, atLeastOnce()).query(cap.capture());

        boolean hasTombstone = cap.getAllValues().stream()
                .anyMatch(q -> q.getQuery().toLowerCase().contains("set active=false"));
        assertTrue(hasTombstone, "At least one tombstone UPDATE must set active=false");
    }

    // -------------------------------------------------------------------------
    // Single-user MERGE test
    // -------------------------------------------------------------------------

    @Test
    void singleUserProducesMergeAndTombstoneQueries() throws Exception {
        User u = buildUser(42L, "jdoe", "jdoe@example.com");
        when(userRepo.findAll()).thenReturn(List.of(u));
        when(orgRepo.findAll()).thenReturn(Collections.emptyList());

        job.run();

        // Expect: 1 MERGE (user) + 1 tombstone (users) + 1 tombstone (orgs) = 3 total
        // (syncOrgs loop over empty list issues 0 MERGEs)
        ArgumentCaptor<QueryJobConfiguration> cap =
                ArgumentCaptor.forClass(QueryJobConfiguration.class);
        verify(bq, times(3)).query(cap.capture());

        long mergeCount = cap.getAllValues().stream()
                .filter(q -> q.getQuery().toUpperCase().contains("MERGE"))
                .count();
        assertEquals(1, mergeCount, "Exactly 1 MERGE expected for a single user");

        // The MERGE query should target the users table
        String mergeQuery = cap.getAllValues().stream()
                .filter(q -> q.getQuery().toUpperCase().contains("MERGE"))
                .findFirst()
                .orElseThrow()
                .getQuery();
        assertTrue(mergeQuery.contains("analytics_test.users"),
                "MERGE query must reference the configured dataset and users table");
    }

    @Test
    void singleOrgProducesMergeQuery() throws Exception {
        Organization o = buildOrg(7L, "NIST", "National Institute of Standards and Technology");
        when(userRepo.findAll()).thenReturn(Collections.emptyList());
        when(orgRepo.findAll()).thenReturn(List.of(o));

        job.run();

        ArgumentCaptor<QueryJobConfiguration> cap =
                ArgumentCaptor.forClass(QueryJobConfiguration.class);
        // 0 user MERGEs + 1 org MERGE + 2 tombstone UPDATEs = 3
        verify(bq, times(3)).query(cap.capture());

        boolean hasOrgMerge = cap.getAllValues().stream()
                .anyMatch(q -> q.getQuery().toUpperCase().contains("MERGE")
                        && q.getQuery().contains(".orgs"));
        assertTrue(hasOrgMerge, "MERGE for orgs table must be issued");
    }

    // -------------------------------------------------------------------------
    // Project / dataset interpolation
    // -------------------------------------------------------------------------

    @Test
    void queriesUseConfiguredProjectAndDataset() throws Exception {
        when(userRepo.findAll()).thenReturn(Collections.emptyList());
        when(orgRepo.findAll()).thenReturn(Collections.emptyList());

        DimensionSyncJob customJob =
                new DimensionSyncJob(userRepo, orgRepo, bq, "my_dataset", "my-project");
        customJob.run();

        ArgumentCaptor<QueryJobConfiguration> cap =
                ArgumentCaptor.forClass(QueryJobConfiguration.class);
        verify(bq, atLeastOnce()).query(cap.capture());

        boolean allQueriesUseProject = cap.getAllValues().stream()
                .allMatch(q -> q.getQuery().contains("my-project") && q.getQuery().contains("my_dataset"));
        assertTrue(allQueriesUseProject, "All queries must embed the configured project/dataset");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User buildUser(Long id, String username, String email) {
        User u = new User(username, "hashed-pw", email);
        u.setId(id);
        u.setFirstName("John");
        u.setLastName("Doe");
        u.setEnabled(true);
        u.setGlobalRole(User.GlobalRole.USER);
        u.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));
        return u;
    }

    private Organization buildOrg(Long id, String name, String description) {
        Organization o = new Organization(name, description);
        o.setId(id);
        o.setActive(true);
        o.setCreatedAt(LocalDateTime.of(2024, 2, 1, 8, 0));
        return o;
    }
}
