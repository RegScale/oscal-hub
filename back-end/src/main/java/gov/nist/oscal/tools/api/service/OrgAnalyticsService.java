package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.repository.AuditEventRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserAccessRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrgAnalyticsService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final AuditEventRepository auditEventRepository;
    private final UserAccessRequestRepository accessRequestRepository;

    public OrgAnalyticsService(OrganizationRepository organizationRepository,
                                OrganizationMembershipRepository membershipRepository,
                                AuditEventRepository auditEventRepository,
                                UserAccessRequestRepository accessRequestRepository) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.auditEventRepository = auditEventRepository;
        this.accessRequestRepository = accessRequestRepository;
    }

    public Map<String, Object> getAnalyticsSummary(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        List<String> usernames = getOrgUsernames(org);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        int totalMembers = membershipRepository.countByOrganizationId(organizationId);
        long pendingRequests = accessRequestRepository.countPendingByOrganizationId(organizationId);

        long loginsThisMonth = 0;
        long operationsThisMonth = 0;
        if (!usernames.isEmpty()) {
            loginsThisMonth = auditEventRepository.countLoginsByUsernamesSince(usernames, thirtyDaysAgo);
            operationsThisMonth = auditEventRepository.countOperationsByUsernamesSince(usernames, thirtyDaysAgo);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalMembers", totalMembers);
        summary.put("pendingRequests", pendingRequests);
        summary.put("loginsThisMonth", loginsThisMonth);
        summary.put("operationsThisMonth", operationsThisMonth);
        return summary;
    }

    public Map<String, Object> getFullAnalytics(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        List<String> usernames = getOrgUsernames(org);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        Map<String, Object> analytics = new LinkedHashMap<>();

        if (usernames.isEmpty()) {
            analytics.put("activeUsersLast7Days", 0);
            analytics.put("totalLoginsLast30Days", 0);
            analytics.put("totalOperationsLast30Days", 0);
            analytics.put("failedLoginsLast30Days", 0);
            analytics.put("loginsPerDay", Collections.emptyList());
            analytics.put("operationsByType", Collections.emptyList());
            analytics.put("topUsers", Collections.emptyList());
            return analytics;
        }

        analytics.put("activeUsersLast7Days", auditEventRepository.countActiveUsersByUsernamesSince(usernames, sevenDaysAgo));
        analytics.put("totalLoginsLast30Days", auditEventRepository.countLoginsByUsernamesSince(usernames, thirtyDaysAgo));
        analytics.put("totalOperationsLast30Days", auditEventRepository.countOperationsByUsernamesSince(usernames, thirtyDaysAgo));
        analytics.put("failedLoginsLast30Days", auditEventRepository.countFailedLoginsByUsernamesSince(usernames, thirtyDaysAgo));

        // Logins per day
        List<Object[]> loginsPerDayRaw = auditEventRepository.countLoginsPerDayByUsernamesSince(usernames, thirtyDaysAgo);
        List<Map<String, Object>> loginsPerDay = new ArrayList<>();
        for (Object[] row : loginsPerDayRaw) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", row[0].toString());
            entry.put("count", ((Number) row[1]).longValue());
            loginsPerDay.add(entry);
        }
        analytics.put("loginsPerDay", loginsPerDay);

        // Operations by type
        List<Object[]> opsByTypeRaw = auditEventRepository.countOperationsByTypeByUsernamesSince(usernames, thirtyDaysAgo);
        List<Map<String, Object>> opsByType = new ArrayList<>();
        for (Object[] row : opsByTypeRaw) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", row[0].toString());
            entry.put("count", ((Number) row[1]).longValue());
            opsByType.add(entry);
        }
        analytics.put("operationsByType", opsByType);

        // Top users
        List<Object[]> topUsersRaw = auditEventRepository.countTopUsersByUsernamesSince(usernames, thirtyDaysAgo);
        List<Map<String, Object>> topUsers = new ArrayList<>();
        int limit = Math.min(topUsersRaw.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = topUsersRaw.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("username", row[0].toString());
            entry.put("operationCount", ((Number) row[1]).longValue());
            topUsers.add(entry);
        }
        analytics.put("topUsers", topUsers);

        return analytics;
    }

    private List<String> getOrgUsernames(Organization org) {
        List<OrganizationMembership> memberships = membershipRepository.findByOrganization(org);
        return memberships.stream()
                .map(m -> m.getUser().getUsername())
                .collect(Collectors.toList());
    }
}
