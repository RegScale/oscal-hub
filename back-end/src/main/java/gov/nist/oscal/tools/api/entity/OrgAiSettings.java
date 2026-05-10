package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "org_ai_settings")
public class OrgAiSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false, unique = true)
    private Long organizationId;

    @Column(name = "anthropic_key_encrypted", columnDefinition = "TEXT")
    private String anthropicKeyEncrypted;

    @Column(name = "anthropic_key_fingerprint", length = 32)
    private String anthropicKeyFingerprint;

    @Column(name = "default_model", nullable = false, length = 64)
    private String defaultModel = "claude-opus-4-7";

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public OrgAiSettings() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getAnthropicKeyEncrypted() { return anthropicKeyEncrypted; }
    public void setAnthropicKeyEncrypted(String anthropicKeyEncrypted) { this.anthropicKeyEncrypted = anthropicKeyEncrypted; }
    public String getAnthropicKeyFingerprint() { return anthropicKeyFingerprint; }
    public void setAnthropicKeyFingerprint(String anthropicKeyFingerprint) { this.anthropicKeyFingerprint = anthropicKeyFingerprint; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
