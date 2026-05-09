package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.wizard.Wizard;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardContext;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WizardRouterTest {

    @Test
    void routesEachKindToTheRegisteredWizard() {
        Wizard catalog = stub(WizardKind.CATALOG);
        Wizard ssp = stub(WizardKind.SSP);
        Wizard poam = stub(WizardKind.POAM);

        WizardRouter router = new WizardRouter(List.of(catalog, ssp, poam));

        assertThat(router.get(WizardKind.CATALOG)).isSameAs(catalog);
        assertThat(router.get(WizardKind.SSP)).isSameAs(ssp);
        assertThat(router.get(WizardKind.POAM)).isSameAs(poam);
    }

    @Test
    void unregisteredKind_throwsIllegalArgument_withKindInMessage() {
        WizardRouter router = new WizardRouter(List.of(stub(WizardKind.SMOKE)));

        assertThatThrownBy(() -> router.get(WizardKind.SSP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SSP");
    }

    @Test
    void emptyWizardList_anyLookupThrows() {
        WizardRouter router = new WizardRouter(List.of());
        assertThatThrownBy(() -> router.get(WizardKind.CATALOG))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateKinds_throwAtConstruction() {
        // Two wizards claiming the same kind is a wiring bug; Map.collect should fail-fast
        // rather than silently keep one and discard the other.
        Wizard a = stub(WizardKind.SSP);
        Wizard b = stub(WizardKind.SSP);

        assertThatThrownBy(() -> new WizardRouter(List.of(a, b)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Wizard stub(WizardKind k) {
        return new Wizard() {
            @Override public WizardKind kind() { return k; }
            @Override public WizardOutcome run(WizardContext ctx) { return null; }
        };
    }
}
