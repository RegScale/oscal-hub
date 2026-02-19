package gov.nist.oscal.tools.api.model;

/**
 * DTO for organization summary with user counts and pending request counts
 * Used by Super Admin dashboard
 */
public class OrganizationSummaryResponse {

    private Long id;
    private String name;
    private Integer memberCount;
    private Long pendingRequestCount;

    // Constructors
    public OrganizationSummaryResponse() {
    }

    public OrganizationSummaryResponse(Long id, String name, Integer memberCount, Long pendingRequestCount) {
        this.id = id;
        this.name = name;
        this.memberCount = memberCount;
        this.pendingRequestCount = pendingRequestCount;
    }

    // Getters and Setters
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

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public Long getPendingRequestCount() {
        return pendingRequestCount;
    }

    public void setPendingRequestCount(Long pendingRequestCount) {
        this.pendingRequestCount = pendingRequestCount;
    }
}
