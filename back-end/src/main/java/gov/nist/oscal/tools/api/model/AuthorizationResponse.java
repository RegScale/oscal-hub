package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.Authorization;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Response DTO for authorization
 */
public class AuthorizationResponse {

    private Long id;
    private String name;
    private String sspItemId;
    private String sarItemId;
    private Long templateId;
    private String templateName;
    private Map<String, String> variableValues;
    private String completedContent;
    private String authorizedBy;
    private LocalDateTime authorizedAt;
    private LocalDateTime createdAt;
    private LocalDate dateAuthorized;
    private LocalDate dateExpired;
    private String systemOwner;
    private String securityManager;
    private String authorizingOfficial;
    private List<ConditionOfApprovalResponse> conditions;

    // Digital Signature fields
    private String digitalSignatureMethod;
    private String signerCertificate;
    private String electronicSignatureImage;
    private String signerCommonName;
    private String signerEmail;
    private String signerEdipi;
    private String certificateIssuer;
    private String certificateSerial;
    private LocalDateTime certificateNotBefore;
    private LocalDateTime certificateNotAfter;
    private LocalDateTime signatureTimestamp;
    private String documentHash;
    private Boolean certificateVerified;
    private LocalDateTime certificateVerificationDate;
    private String certificateVerificationNotes;

    // Constructors
    public AuthorizationResponse() {
    }

    public AuthorizationResponse(Authorization authorization) {
        this.id = authorization.getId();
        this.name = authorization.getName();
        this.sspItemId = authorization.getSspItemId();
        this.sarItemId = authorization.getSarItemId();
        this.templateId = authorization.getTemplate().getId();
        this.templateName = authorization.getTemplate().getName();
        this.variableValues = authorization.getVariableValues();
        this.completedContent = authorization.getCompletedContent();
        this.authorizedBy = authorization.getAuthorizedBy().getUsername();
        this.authorizedAt = authorization.getAuthorizedAt();
        this.createdAt = authorization.getCreatedAt();
        this.dateAuthorized = authorization.getDateAuthorized();
        this.dateExpired = authorization.getDateExpired();
        this.systemOwner = authorization.getSystemOwner();
        this.securityManager = authorization.getSecurityManager();
        this.authorizingOfficial = authorization.getAuthorizingOfficial();
        this.conditions = authorization.getConditions().stream()
                .map(ConditionOfApprovalResponse::new)
                .collect(Collectors.toList());

        // Digital Signature fields
        this.digitalSignatureMethod = authorization.getDigitalSignatureMethod();
        this.signerCertificate = authorization.getSignerCertificate();
        this.electronicSignatureImage = authorization.getElectronicSignatureImage();
        this.signerCommonName = authorization.getSignerCommonName();
        this.signerEmail = authorization.getSignerEmail();
        this.signerEdipi = authorization.getSignerEdipi();
        this.certificateIssuer = authorization.getCertificateIssuer();
        this.certificateSerial = authorization.getCertificateSerial();
        this.certificateNotBefore = authorization.getCertificateNotBefore();
        this.certificateNotAfter = authorization.getCertificateNotAfter();
        this.signatureTimestamp = authorization.getSignatureTimestamp();
        this.documentHash = authorization.getDocumentHash();
        this.certificateVerified = authorization.getCertificateVerified();
        this.certificateVerificationDate = authorization.getCertificateVerificationDate();
        this.certificateVerificationNotes = authorization.getCertificateVerificationNotes();
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

    public List<ConditionOfApprovalResponse> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionOfApprovalResponse> conditions) {
        this.conditions = conditions;
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

    public String getElectronicSignatureImage() {
        return electronicSignatureImage;
    }

    public void setElectronicSignatureImage(String electronicSignatureImage) {
        this.electronicSignatureImage = electronicSignatureImage;
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
