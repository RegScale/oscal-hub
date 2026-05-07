package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for authorization template
 */
public class AuthorizationTemplateResponse {

    private Long id;
    private Long organizationId;
    private String name;
    private String content;
    private String createdBy;
    private LocalDateTime createdAt;
    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedAt;
    private Set<String> variables; // Extracted variables from content

    // Constructors
    public AuthorizationTemplateResponse() {
    }

    public AuthorizationTemplateResponse(AuthorizationTemplate template, Set<String> variables) {
        this.id = template.getId();
        this.organizationId = template.getOrganization().getId();
        this.name = template.getName();
        this.content = template.getContent();

        // Handle lazy-loaded createdBy
        try {
            this.createdBy = template.getCreatedBy().getUsername();
        } catch (org.hibernate.LazyInitializationException e) {
            this.createdBy = null;
        }

        this.createdAt = template.getCreatedAt();

        // Handle lazy-loaded lastUpdatedBy
        try {
            this.lastUpdatedBy = template.getLastUpdatedBy() != null ?
                    template.getLastUpdatedBy().getUsername() : this.createdBy;
        } catch (org.hibernate.LazyInitializationException e) {
            this.lastUpdatedBy = this.createdBy;
        }

        this.lastUpdatedAt = template.getLastUpdatedAt();
        this.variables = variables;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Set<String> getVariables() {
        return variables;
    }

    public void setVariables(Set<String> variables) {
        this.variables = variables;
    }
}
