package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.Authorization;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for authorization
 */
public class AuthorizationResponse {

    private Long id;
    private String name;
    private String sspItemId;
    private Long templateId;
    private String templateName;
    private Map<String, String> variableValues;
    private String completedContent;
    private String authorizedBy;
    private LocalDateTime authorizedAt;
    private LocalDateTime createdAt;
    private LocalDate dateExpired;
    private String systemOwner;
    private String securityManager;
    private String authorizingOfficial;

    // Constructors
    public AuthorizationResponse() {
    }

    public AuthorizationResponse(Authorization authorization) {
        this.id = authorization.getId();
        this.name = authorization.getName();
        this.sspItemId = authorization.getSspItemId();
        this.templateId = authorization.getTemplate().getId();
        this.templateName = authorization.getTemplate().getName();
        this.variableValues = authorization.getVariableValues();
        this.completedContent = authorization.getCompletedContent();
        this.authorizedBy = authorization.getAuthorizedBy().getUsername();
        this.authorizedAt = authorization.getAuthorizedAt();
        this.createdAt = authorization.getCreatedAt();
        this.dateExpired = authorization.getDateExpired();
        this.systemOwner = authorization.getSystemOwner();
        this.securityManager = authorization.getSecurityManager();
        this.authorizingOfficial = authorization.getAuthorizingOfficial();
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

    public String getSspItemId() {
        return sspItemId;
    }

    public void setSspItemId(String sspItemId) {
        this.sspItemId = sspItemId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Map<String, String> getVariableValues() {
        return variableValues;
    }

    public void setVariableValues(Map<String, String> variableValues) {
        this.variableValues = variableValues;
    }

    public String getCompletedContent() {
        return completedContent;
    }

    public void setCompletedContent(String completedContent) {
        this.completedContent = completedContent;
    }

    public String getAuthorizedBy() {
        return authorizedBy;
    }

    public void setAuthorizedBy(String authorizedBy) {
        this.authorizedBy = authorizedBy;
    }

    public LocalDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(LocalDateTime authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getDateExpired() {
        return dateExpired;
    }

    public void setDateExpired(LocalDate dateExpired) {
        this.dateExpired = dateExpired;
    }

    public String getSystemOwner() {
        return systemOwner;
    }

    public void setSystemOwner(String systemOwner) {
        this.systemOwner = systemOwner;
    }

    public String getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(String securityManager) {
        this.securityManager = securityManager;
    }

    public String getAuthorizingOfficial() {
        return authorizingOfficial;
    }

    public void setAuthorizingOfficial(String authorizingOfficial) {
        this.authorizingOfficial = authorizingOfficial;
    }
}
