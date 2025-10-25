package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @Column(name = "sar_item_id", length = 100)
    private String sarItemId; // References LibraryItem.itemId for SAR (Security Assessment Results)

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

    @Column(name = "date_authorized")
    private LocalDate dateAuthorized;

    @Column(name = "date_expired")
    private LocalDate dateExpired;

    @Column(name = "system_owner", length = 255)
    private String systemOwner;

    @Column(name = "security_manager", length = 255)
    private String securityManager;

    @Column(name = "authorizing_official", length = 255)
    private String authorizingOfficial;

    @OneToMany(mappedBy = "authorization", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ConditionOfApproval> conditions = new ArrayList<>();

    // Digital Signature Fields
    @Column(name = "digital_signature_method", length = 50)
    private String digitalSignatureMethod; // "TLS_CLIENT_CERT"

    @Column(name = "signer_certificate", columnDefinition = "TEXT")
    private String signerCertificate; // Base64-encoded X.509 cert

    @Column(name = "signer_common_name", length = 255)
    private String signerCommonName; // CN from certificate

    @Column(name = "signer_email", length = 255)
    private String signerEmail; // Email from certificate

    @Column(name = "signer_edipi", length = 10)
    private String signerEdipi; // EDIPI from certificate (last 10 digits of CN)

    @Column(name = "certificate_issuer", length = 500)
    private String certificateIssuer; // Certificate issuer DN

    @Column(name = "certificate_serial", length = 100)
    private String certificateSerial; // Certificate serial number

    @Column(name = "certificate_not_before")
    private LocalDateTime certificateNotBefore;

    @Column(name = "certificate_not_after")
    private LocalDateTime certificateNotAfter;

    @Column(name = "signature_timestamp")
    private LocalDateTime signatureTimestamp;

    @Column(name = "document_hash", length = 64)
    private String documentHash; // SHA-256 hash of signed content

    @Column(name = "certificate_verified")
    private Boolean certificateVerified; // Certificate validation result

    @Column(name = "certificate_verification_date")
    private LocalDateTime certificateVerificationDate;

    @Column(name = "certificate_verification_notes", columnDefinition = "TEXT")
    private String certificateVerificationNotes; // Any validation warnings

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

    public String getSarItemId() {
        return sarItemId;
    }

    public void setSarItemId(String sarItemId) {
        this.sarItemId = sarItemId;
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

    public LocalDate getDateAuthorized() {
        return dateAuthorized;
    }

    public void setDateAuthorized(LocalDate dateAuthorized) {
        this.dateAuthorized = dateAuthorized;
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

    public List<ConditionOfApproval> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionOfApproval> conditions) {
        this.conditions = conditions;
    }

    public void addCondition(ConditionOfApproval condition) {
        conditions.add(condition);
        condition.setAuthorization(this);
    }

    public void removeCondition(ConditionOfApproval condition) {
        conditions.remove(condition);
        condition.setAuthorization(null);
    }

    // Digital Signature Getters and Setters
    public String getDigitalSignatureMethod() {
        return digitalSignatureMethod;
    }

    public void setDigitalSignatureMethod(String digitalSignatureMethod) {
        this.digitalSignatureMethod = digitalSignatureMethod;
    }

    public String getSignerCertificate() {
        return signerCertificate;
    }

    public void setSignerCertificate(String signerCertificate) {
        this.signerCertificate = signerCertificate;
    }

    public String getSignerCommonName() {
        return signerCommonName;
    }

    public void setSignerCommonName(String signerCommonName) {
        this.signerCommonName = signerCommonName;
    }

    public String getSignerEmail() {
        return signerEmail;
    }

    public void setSignerEmail(String signerEmail) {
        this.signerEmail = signerEmail;
    }

    public String getSignerEdipi() {
        return signerEdipi;
    }

    public void setSignerEdipi(String signerEdipi) {
        this.signerEdipi = signerEdipi;
    }

    public String getCertificateIssuer() {
        return certificateIssuer;
    }

    public void setCertificateIssuer(String certificateIssuer) {
        this.certificateIssuer = certificateIssuer;
    }

    public String getCertificateSerial() {
        return certificateSerial;
    }

    public void setCertificateSerial(String certificateSerial) {
        this.certificateSerial = certificateSerial;
    }

    public LocalDateTime getCertificateNotBefore() {
        return certificateNotBefore;
    }

    public void setCertificateNotBefore(LocalDateTime certificateNotBefore) {
        this.certificateNotBefore = certificateNotBefore;
    }

    public LocalDateTime getCertificateNotAfter() {
        return certificateNotAfter;
    }

    public void setCertificateNotAfter(LocalDateTime certificateNotAfter) {
        this.certificateNotAfter = certificateNotAfter;
    }

    public LocalDateTime getSignatureTimestamp() {
        return signatureTimestamp;
    }

    public void setSignatureTimestamp(LocalDateTime signatureTimestamp) {
        this.signatureTimestamp = signatureTimestamp;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public void setDocumentHash(String documentHash) {
        this.documentHash = documentHash;
    }

    public Boolean getCertificateVerified() {
        return certificateVerified;
    }

    public void setCertificateVerified(Boolean certificateVerified) {
        this.certificateVerified = certificateVerified;
    }

    public LocalDateTime getCertificateVerificationDate() {
        return certificateVerificationDate;
    }

    public void setCertificateVerificationDate(LocalDateTime certificateVerificationDate) {
        this.certificateVerificationDate = certificateVerificationDate;
    }

    public String getCertificateVerificationNotes() {
        return certificateVerificationNotes;
    }

    public void setCertificateVerificationNotes(String certificateVerificationNotes) {
        this.certificateVerificationNotes = certificateVerificationNotes;
    }
}
