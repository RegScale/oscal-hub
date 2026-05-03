package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_sessions")
public class AiSession {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "wizard_kind", nullable = false, length = 32)
    private WizardKind wizardKind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AiSessionMode mode;

    @Column(nullable = false, length = 64)
    private String model;

    @Column(name = "input_summary", columnDefinition = "TEXT")
    private String inputSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiSessionStatus status;

    @Column(name = "tokens_in", nullable = false)
    private int tokensIn = 0;

    @Column(name = "tokens_out", nullable = false)
    private int tokensOut = 0;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public WizardKind getWizardKind() { return wizardKind; }
    public void setWizardKind(WizardKind wizardKind) { this.wizardKind = wizardKind; }
    public AiSessionMode getMode() { return mode; }
    public void setMode(AiSessionMode mode) { this.mode = mode; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getInputSummary() { return inputSummary; }
    public void setInputSummary(String inputSummary) { this.inputSummary = inputSummary; }
    public AiSessionStatus getStatus() { return status; }
    public void setStatus(AiSessionStatus status) { this.status = status; }
    public int getTokensIn() { return tokensIn; }
    public void setTokensIn(int tokensIn) { this.tokensIn = tokensIn; }
    public int getTokensOut() { return tokensOut; }
    public void setTokensOut(int tokensOut) { this.tokensOut = tokensOut; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
}
