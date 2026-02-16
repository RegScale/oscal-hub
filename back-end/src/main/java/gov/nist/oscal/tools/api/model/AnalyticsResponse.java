package gov.nist.oscal.tools.api.model;

import java.util.List;
import java.util.Map;

/**
 * Analytics Response DTO
 * Contains aggregated analytics data for the super admin dashboard.
 */
public class AnalyticsResponse {

    // Summary statistics
    private long totalUsers;
    private long totalOrganizations;
    private long totalOperationsThisMonth;
    private long activeUsersLast7Days;
    private long newUsersThisMonth;
    private long pendingAccessRequests;

    // Activity over time (last 30 days)
    private List<DailyActivity> activityOverTime;

    // Operations by type
    private List<CategoryCount> operationsByType;

    // Content by OSCAL type
    private List<CategoryCount> contentByType;

    // Most active users
    private List<UserActivity> mostActiveUsers;

    // Most active organizations
    private List<OrganizationActivity> mostActiveOrganizations;

    // Newest organizations
    private List<NewOrganization> newestOrganizations;

    // Recent activity feed
    private List<RecentActivity> recentActivity;

    // User growth trend (last 30 days)
    private List<DailyCount> userGrowthTrend;

    // Format preferences (JSON vs XML vs YAML)
    private List<CategoryCount> formatPreferences;

    // Getters and Setters
    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalOrganizations() {
        return totalOrganizations;
    }

    public void setTotalOrganizations(long totalOrganizations) {
        this.totalOrganizations = totalOrganizations;
    }

    public long getTotalOperationsThisMonth() {
        return totalOperationsThisMonth;
    }

    public void setTotalOperationsThisMonth(long totalOperationsThisMonth) {
        this.totalOperationsThisMonth = totalOperationsThisMonth;
    }

    public long getActiveUsersLast7Days() {
        return activeUsersLast7Days;
    }

    public void setActiveUsersLast7Days(long activeUsersLast7Days) {
        this.activeUsersLast7Days = activeUsersLast7Days;
    }

    public long getNewUsersThisMonth() {
        return newUsersThisMonth;
    }

    public void setNewUsersThisMonth(long newUsersThisMonth) {
        this.newUsersThisMonth = newUsersThisMonth;
    }

    public long getPendingAccessRequests() {
        return pendingAccessRequests;
    }

    public void setPendingAccessRequests(long pendingAccessRequests) {
        this.pendingAccessRequests = pendingAccessRequests;
    }

    public List<DailyActivity> getActivityOverTime() {
        return activityOverTime;
    }

    public void setActivityOverTime(List<DailyActivity> activityOverTime) {
        this.activityOverTime = activityOverTime;
    }

    public List<CategoryCount> getOperationsByType() {
        return operationsByType;
    }

    public void setOperationsByType(List<CategoryCount> operationsByType) {
        this.operationsByType = operationsByType;
    }

    public List<CategoryCount> getContentByType() {
        return contentByType;
    }

    public void setContentByType(List<CategoryCount> contentByType) {
        this.contentByType = contentByType;
    }

    public List<UserActivity> getMostActiveUsers() {
        return mostActiveUsers;
    }

    public void setMostActiveUsers(List<UserActivity> mostActiveUsers) {
        this.mostActiveUsers = mostActiveUsers;
    }

    public List<OrganizationActivity> getMostActiveOrganizations() {
        return mostActiveOrganizations;
    }

    public void setMostActiveOrganizations(List<OrganizationActivity> mostActiveOrganizations) {
        this.mostActiveOrganizations = mostActiveOrganizations;
    }

    public List<NewOrganization> getNewestOrganizations() {
        return newestOrganizations;
    }

    public void setNewestOrganizations(List<NewOrganization> newestOrganizations) {
        this.newestOrganizations = newestOrganizations;
    }

    public List<RecentActivity> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<RecentActivity> recentActivity) {
        this.recentActivity = recentActivity;
    }

    public List<DailyCount> getUserGrowthTrend() {
        return userGrowthTrend;
    }

    public void setUserGrowthTrend(List<DailyCount> userGrowthTrend) {
        this.userGrowthTrend = userGrowthTrend;
    }

    public List<CategoryCount> getFormatPreferences() {
        return formatPreferences;
    }

    public void setFormatPreferences(List<CategoryCount> formatPreferences) {
        this.formatPreferences = formatPreferences;
    }

    // Inner classes for structured data

    public static class DailyActivity {
        private String date;
        private long count;

        public DailyActivity() {}

        public DailyActivity(String date, long count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class DailyCount {
        private String date;
        private long count;

        public DailyCount() {}

        public DailyCount(String date, long count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class CategoryCount {
        private String name;
        private long count;

        public CategoryCount() {}

        public CategoryCount(String name, long count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class UserActivity {
        private String username;
        private String organizationName;
        private long operationsCount;
        private String lastActive;

        public UserActivity() {}

        public UserActivity(String username, String organizationName, long operationsCount, String lastActive) {
            this.username = username;
            this.organizationName = organizationName;
            this.operationsCount = operationsCount;
            this.lastActive = lastActive;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getOrganizationName() {
            return organizationName;
        }

        public void setOrganizationName(String organizationName) {
            this.organizationName = organizationName;
        }

        public long getOperationsCount() {
            return operationsCount;
        }

        public void setOperationsCount(long operationsCount) {
            this.operationsCount = operationsCount;
        }

        public String getLastActive() {
            return lastActive;
        }

        public void setLastActive(String lastActive) {
            this.lastActive = lastActive;
        }
    }

    public static class OrganizationActivity {
        private Long id;
        private String name;
        private int memberCount;
        private long totalOperations;
        private String lastActive;

        public OrganizationActivity() {}

        public OrganizationActivity(Long id, String name, int memberCount, long totalOperations, String lastActive) {
            this.id = id;
            this.name = name;
            this.memberCount = memberCount;
            this.totalOperations = totalOperations;
            this.lastActive = lastActive;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getMemberCount() {
            return memberCount;
        }

        public void setMemberCount(int memberCount) {
            this.memberCount = memberCount;
        }

        public long getTotalOperations() {
            return totalOperations;
        }

        public void setTotalOperations(long totalOperations) {
            this.totalOperations = totalOperations;
        }

        public String getLastActive() {
            return lastActive;
        }

        public void setLastActive(String lastActive) {
            this.lastActive = lastActive;
        }
    }

    public static class NewOrganization {
        private Long id;
        private String name;
        private String createdAt;
        private int memberCount;
        private String createdBy;

        public NewOrganization() {}

        public NewOrganization(Long id, String name, String createdAt, int memberCount, String createdBy) {
            this.id = id;
            this.name = name;
            this.createdAt = createdAt;
            this.memberCount = memberCount;
            this.createdBy = createdBy;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public int getMemberCount() {
            return memberCount;
        }

        public void setMemberCount(int memberCount) {
            this.memberCount = memberCount;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }
    }

    public static class RecentActivity {
        private String timestamp;
        private String username;
        private String operationType;
        private String documentType;
        private String status;
        private String resource;

        public RecentActivity() {}

        public RecentActivity(String timestamp, String username, String operationType,
                             String documentType, String status, String resource) {
            this.timestamp = timestamp;
            this.username = username;
            this.operationType = operationType;
            this.documentType = documentType;
            this.status = status;
            this.resource = resource;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public String getDocumentType() {
            return documentType;
        }

        public void setDocumentType(String documentType) {
            this.documentType = documentType;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }
    }
}
