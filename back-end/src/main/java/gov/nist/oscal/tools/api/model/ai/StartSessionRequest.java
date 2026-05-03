package gov.nist.oscal.tools.api.model.ai;

import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.WizardKind;
import jakarta.validation.constraints.NotNull;

public class StartSessionRequest {
    @NotNull private Long organizationId;
    @NotNull private WizardKind wizardKind;
    @NotNull private AiSessionMode mode = AiSessionMode.STREAMING;
    private String input;

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public WizardKind getWizardKind() { return wizardKind; }
    public void setWizardKind(WizardKind wizardKind) { this.wizardKind = wizardKind; }
    public AiSessionMode getMode() { return mode; }
    public void setMode(AiSessionMode mode) { this.mode = mode; }
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
}
