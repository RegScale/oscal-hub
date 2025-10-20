package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for creating or updating an authorization
 */
public class AuthorizationRequest {

    @NotBlank(message = "Authorization name is required")
    private String name;

    @NotBlank(message = "SSP item ID is required")
    private String sspItemId;

    @NotNull(message = "Template ID is required")
    private Long templateId;

    @NotNull(message = "Variable values are required")
    private Map<String, String> variableValues;

    private String dateAuthorized; // ISO date string

    @NotBlank(message = "Date expired is required")
    private String dateExpired; // ISO date string

    @NotBlank(message = "System owner is required")
    private String systemOwner;

    @NotBlank(message = "Security manager is required")
    private String securityManager;

    @NotBlank(message = "Authorizing official is required")
    private String authorizingOfficial;

    // Optional: User-edited template content (if null, uses original template content)
    private String editedContent;

    // Constructors
    public AuthorizationRequest() {
    }

    public AuthorizationRequest(String name, String sspItemId, Long templateId, Map<String, String> variableValues) {
        this.name = name;
        this.sspItemId = sspItemId;
        this.templateId = templateId;
        this.variableValues = variableValues;
    }

    // Getters and Setters
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

    public Map<String, String> getVariableValues() {
        return variableValues;
    }

    public void setVariableValues(Map<String, String> variableValues) {
        this.variableValues = variableValues;
    }

    public String getDateAuthorized() {
        return dateAuthorized;
    }

    public void setDateAuthorized(String dateAuthorized) {
        this.dateAuthorized = dateAuthorized;
    }

    public String getDateExpired() {
        return dateExpired;
    }

    public void setDateExpired(String dateExpired) {
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

    public String getEditedContent() {
        return editedContent;
    }

    public void setEditedContent(String editedContent) {
        this.editedContent = editedContent;
    }
}
