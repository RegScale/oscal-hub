package gov.nist.oscal.tools.api.service.ai.wizard;

import gov.nist.oscal.tools.api.entity.WizardKind;

public interface Wizard {
    WizardKind kind();
    WizardOutcome run(WizardContext ctx);
}
