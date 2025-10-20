package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a completed system authorization
 * Links an SSP from the library with a template and stores the completed authorization
 */
@Entity
@Table(name = "authorizations")
public class Authorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "ssp_item_id", nullable = false, length = 100)
    private String sspItemId; // References LibraryItem.itemId

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    private AuthorizationTemplate template;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "authorization_variables", joinColumns = @JoinColumn(name = "authorization_id"))
    @MapKeyColumn(name = "variable_name")
    @Column(name = "variable_value", columnDefinition = "TEXT")
    private Map<String, String> variableValues = new HashMap<>();

    @Column(name = "completed_content", nullable = false, columnDefinition = "TEXT")
    private String completedContent; // Final markdown with variables replaced

    @ManyToOne
    @JoinColumn(name = "authorized_by", nullable = false)
    private User authorizedBy;

    @Column(name = "authorized_at", nullable = false)
    private LocalDateTime authorizedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "date_expired")
    private LocalDate dateExpired;

    @Column(name = "system_owner", length = 255)
    private String systemOwner;

    @Column(name = "security_manager", length = 255)
    private String securityManager;

    @Column(name = "authorizing_official", length = 255)
    private String authorizingOfficial;

    // Constructors
    public Authorization() {
        this.createdAt = LocalDateTime.now();
        this.authorizedAt = LocalDateTime.now();
    }

    public Authorization(String name, String sspItemId, AuthorizationTemplate template, User authorizedBy) {
        this();
        this.name = name;
        this.sspItemId = sspItemId;
        this.template = template;
        this.authorizedBy = authorizedBy;
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

    public AuthorizationTemplate getTemplate() {
        return template;
    }

    public void setTemplate(AuthorizationTemplate template) {
        this.template = template;
    }

    public Map<String, String> getVariableValues() {
        return variableValues;
    }

    public void setVariableValues(Map<String, String> variableValues) {
        this.variableValues = variableValues;
    }

    public void addVariableValue(String name, String value) {
        this.variableValues.put(name, value);
    }

    public String getCompletedContent() {
        return completedContent;
    }

    public void setCompletedContent(String completedContent) {
        this.completedContent = completedContent;
    }

    public User getAuthorizedBy() {
        return authorizedBy;
    }

    public void setAuthorizedBy(User authorizedBy) {
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
