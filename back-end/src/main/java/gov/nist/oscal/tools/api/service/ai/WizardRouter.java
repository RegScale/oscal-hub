package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.wizard.Wizard;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WizardRouter {
    private final Map<WizardKind, Wizard> byKind;

    public WizardRouter(List<Wizard> wizards) {
        this.byKind = wizards.stream().collect(Collectors.toMap(Wizard::kind, w -> w));
    }

    public Wizard get(WizardKind kind) {
        Wizard w = byKind.get(kind);
        if (w == null) throw new IllegalArgumentException("No wizard registered for " + kind);
        return w;
    }
}
