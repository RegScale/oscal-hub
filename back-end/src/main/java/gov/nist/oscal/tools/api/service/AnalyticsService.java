package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.AuditEvent;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OperationHistory;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.AnalyticsResponse;
import gov.nist.oscal.tools.api.model.AnalyticsResponse.*;
import gov.nist.oscal.tools.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service
 * Aggregates data from various sources for the super admin analytics dashboard.
 */
@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditEventRepository auditEventRepository;
    private final HistoryRepository historyRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserAccessRequestRepository accessRequestRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public AnalyticsService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            AuditEventRepository auditEventRepository,
            HistoryRepository historyRepository,
            OrganizationMembershipRepository membershipRepository,
            UserAccessRequestRepository accessRequestRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.auditEventRepository = auditEventRepository;
        this.historyRepository = historyRepository;
        this.membershipRepository = membershipRepository;
        this.accessRequestRepository = accessRequestRepository;
    }

    /**
     * Get comprehensive analytics data for the dashboard
     */
    public AnalyticsResponse getAnalytics() {
        AnalyticsResponse response = new AnalyticsResponse();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last7Days = now.minusDays(7);
        LocalDateTime last30Days = now.minusDays(30);
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        // Summary statistics
        response.setTotalUsers(userRepository.count());
        response.setTotalOrganizations(organizationRepository.count());
        response.setTotalOperationsThisMonth(getOperationsCount(startOfMonth));
        response.setActiveUsersLast7Days(getActiveUsersCount(last7Days));
        response.setNewUsersThisMonth(getNewUsersCount(startOfMonth));
        response.setPendingAccessRequests((long) accessRequestRepository.findAllPending().size());

        // Activity over time (last 30 days)
        response.setActivityOverTime(getActivityOverTime(last30Days));

        // Operations by type
        response.setOperationsByType(getOperationsByType(last30Days));

        // Content by OSCAL type
        response.setContentByType(getContentByType(last30Days));

        // Most active users (top 10)
        response.setMostActiveUsers(getMostActiveUsers(last30Days, 10));

        // Most active organizations (top 10)
        response.setMostActiveOrganizations(getMostActiveOrganizations(last30Days, 10));

        // Newest organizations (last 10)
        response.setNewestOrganizations(getNewestOrganizations(10));

        // Recent activity feed (last 20)
        response.setRecentActivity(getRecentActivity(20));

        // User growth trend
        response.setUserGrowthTrend(getUserGrowthTrend(last30Days));

        // Format preferences
        response.setFormatPreferences(getFormatPreferences(last30Days));

        return response;
    }

    /**
     * Get summary statistics only (lighter weight for dashboard header)
     */
    public Map<String, Long> getSummaryStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last7Days = now.minusDays(7);
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalOrganizations", organizationRepository.count());
        stats.put("totalOperationsThisMonth", getOperationsCount(startOfMonth));
        stats.put("activeUsersLast7Days", getActiveUsersCount(last7Days));
        stats.put("pendingAccessRequests", (long) accessRequestRepository.findAllPending().size());
        return stats;
    }

    private long getOperationsCount(LocalDateTime since) {
        try {
            return auditEventRepository.countEventsSince(since);
        } catch (Exception e) {
            return historyRepository.count();
        }
    }

    private long getActiveUsersCount(LocalDateTime since) {
        try {
            return auditEventRepository.countUniqueUsersSince(since);
        } catch (Exception e) {
            return 0;
        }
    }

    private long getNewUsersCount(LocalDateTime since) {
        try {
            return userRepository.countByCreatedAtAfter(since);
        } catch (Exception e) {
            return 0;
        }
    }

    private List<DailyActivity> getActivityOverTime(LocalDateTime since) {
        List<DailyActivity> result = new ArrayList<>();
        try {
            List<Object[]> data = auditEventRepository.countByDateSince(since);
            for (Object[] row : data) {
                String date = row[0] != null ? row[0].toString() : "";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0;
                result.add(new DailyActivity(date, count));
            }
        } catch (Exception e) {
            // Return empty list on error
        }
        return result;
    }

    private List<CategoryCount> getOperationsByType(LocalDateTime since) {
        List<CategoryCount> result = new ArrayList<>();
        try {
            List<Object[]> data = auditEventRepository.countByCategorySince(since);
            for (Object[] row : data) {
                String category = row[0] != null ? row[0].toString() : "Unknown";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0;
                // Map to user-friendly names
                String displayName = mapCategoryToDisplayName(category);
                result.add(new CategoryCount(displayName, count));
            }
        } catch (Exception e) {
            // Fallback to operation history
            try {
                result.add(new CategoryCount("Validate", historyRepository.countByOperationType("VALIDATE")));
                result.add(new CategoryCount("Convert", historyRepository.countByOperationType("CONVERT")));
                result.add(new CategoryCount("Resolve", historyRepository.countByOperationType("RESOLVE")));
            } catch (Exception ex) {
                // Return empty
            }
        }
        return result;
    }

    private List<CategoryCount> getContentByType(LocalDateTime since) {
        List<CategoryCount> result = new ArrayList<>();
        try {
            // Get from operation history by model type
            List<Object[]> data = historyRepository.countByModelType();
            for (Object[] row : data) {
                String modelType = row[0] != null ? row[0].toString() : "Unknown";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0;
                result.add(new CategoryCount(modelType, count));
            }
        } catch (Exception e) {
            // Return placeholder data
            result.add(new CategoryCount("SSP", 0));
            result.add(new CategoryCount("Catalog", 0));
            result.add(new CategoryCount("Profile", 0));
        }
        return result;
    }

    private List<UserActivity> getMostActiveUsers(LocalDateTime since, int limit) {
        List<UserActivity> result = new ArrayList<>();
        try {
            List<Object[]> data = auditEventRepository.countByUsernameSince(since);
            int count = 0;
            for (Object[] row : data) {
                if (count >= limit) break;
                String username = row[0] != null ? row[0].toString() : "Unknown";
                long operations = row[1] != null ? ((Number) row[1]).longValue() : 0;

                // Get organization name for user
                String orgName = getOrganizationForUser(username);
                String lastActive = getLastActiveForUser(username);

                result.add(new UserActivity(username, orgName, operations, lastActive));
                count++;
            }
        } catch (Exception e) {
            // Return empty list
        }
        return result;
    }

    private List<OrganizationActivity> getMostActiveOrganizations(LocalDateTime since, int limit) {
        List<OrganizationActivity> result = new ArrayList<>();
        try {
            List<Organization> orgs = organizationRepository.findAll();
            for (Organization org : orgs) {
                int memberCount = membershipRepository.countByOrganizationId(org.getId());
                // For now, use member count as activity proxy
                // Could enhance with actual operation counts per org
                result.add(new OrganizationActivity(
                    org.getId(),
                    org.getName(),
                    memberCount,
                    memberCount * 10L, // Placeholder for actual operations
                    org.getCreatedAt() != null ? org.getCreatedAt().format(DATETIME_FORMATTER) : ""
                ));
            }
            // Sort by member count and limit
            result.sort((a, b) -> Long.compare(b.getTotalOperations(), a.getTotalOperations()));
            if (result.size() > limit) {
                result = result.subList(0, limit);
            }
        } catch (Exception e) {
            // Return empty list
        }
        return result;
    }

    private List<NewOrganization> getNewestOrganizations(int limit) {
        List<NewOrganization> result = new ArrayList<>();
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<Organization> orgs = organizationRepository.findAllByOrderByCreatedAtDesc(pageable);
            for (Organization org : orgs) {
                int memberCount = membershipRepository.countByOrganizationId(org.getId());
                result.add(new NewOrganization(
                    org.getId(),
                    org.getName(),
                    org.getCreatedAt() != null ? org.getCreatedAt().format(DATE_FORMATTER) : "",
                    memberCount,
                    "System"
                ));
            }
        } catch (Exception e) {
            // Return empty list
        }
        return result;
    }

    private List<RecentActivity> getRecentActivity(int limit) {
        List<RecentActivity> result = new ArrayList<>();
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<AuditEvent> events = auditEventRepository.findAll(pageable).getContent();

            // Sort by timestamp desc
            events.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

            for (AuditEvent event : events) {
                result.add(new RecentActivity(
                    event.getTimestamp() != null ? event.getTimestamp().format(DATETIME_FORMATTER) : "",
                    event.getUsername() != null ? event.getUsername() : "Anonymous",
                    event.getEventType() != null ? event.getEventType().name() : "",
                    extractDocumentType(event),
                    event.getOutcome() != null ? event.getOutcome() : "",
                    event.getResource() != null ? event.getResource() : ""
                ));
                if (result.size() >= limit) break;
            }
        } catch (Exception e) {
            // Return empty list
        }
        return result;
    }

    private List<DailyCount> getUserGrowthTrend(LocalDateTime since) {
        List<DailyCount> result = new ArrayList<>();
        try {
            // Get user registrations by date
            List<Object[]> data = userRepository.countByCreatedAtGroupByDate(since);
            for (Object[] row : data) {
                String date = row[0] != null ? row[0].toString() : "";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0;
                result.add(new DailyCount(date, count));
            }
        } catch (Exception e) {
            // Return empty list
        }
        return result;
    }

    private List<CategoryCount> getFormatPreferences(LocalDateTime since) {
        List<CategoryCount> result = new ArrayList<>();
        try {
            List<Object[]> data = historyRepository.countByFormat();
            for (Object[] row : data) {
                String format = row[0] != null ? row[0].toString() : "Unknown";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0;
                result.add(new CategoryCount(format.toUpperCase(), count));
            }
        } catch (Exception e) {
            // Return placeholder
            result.add(new CategoryCount("JSON", 0));
            result.add(new CategoryCount("XML", 0));
            result.add(new CategoryCount("YAML", 0));
        }
        return result;
    }

    // Helper methods
    private String mapCategoryToDisplayName(String category) {
        if (category == null) return "Other";
        switch (category) {
            case "Authentication": return "Login/Auth";
            case "Authorization": return "Access Control";
            case "Data Access": return "Data Operations";
            case "Configuration": return "Settings";
            case "Security": return "Security";
            case "System": return "System";
            default: return category;
        }
    }

    private String getOrganizationForUser(String username) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                // Get first organization membership
                return membershipRepository.findFirstOrganizationNameByUserId(user.getId());
            }
        } catch (Exception e) {
            // Ignore
        }
        return "N/A";
    }

    private String getLastActiveForUser(String username) {
        try {
            Pageable pageable = PageRequest.of(0, 1);
            List<AuditEvent> events = auditEventRepository
                .findByUsernameOrderByTimestampDesc(username, pageable).getContent();
            if (!events.isEmpty()) {
                return events.get(0).getTimestamp().format(DATETIME_FORMATTER);
            }
        } catch (Exception e) {
            // Ignore
        }
        return "N/A";
    }

    private String extractDocumentType(AuditEvent event) {
        if (event.getMetadata() != null) {
            // Try to extract from metadata
            if (event.getMetadata().contains("ssp")) return "SSP";
            if (event.getMetadata().contains("catalog")) return "Catalog";
            if (event.getMetadata().contains("profile")) return "Profile";
        }
        if (event.getResource() != null) {
            String resource = event.getResource().toLowerCase();
            if (resource.contains("ssp")) return "SSP";
            if (resource.contains("catalog")) return "Catalog";
            if (resource.contains("profile")) return "Profile";
            if (resource.contains("component")) return "Component";
        }
        return "N/A";
    }
}
