package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PoamStatusDeriverTest {

    @Test
    void closedKeyword_yieldsClosed() {
        var r = PoamStatusDeriver.derive("closed", List.of());
        assertThat(r.status()).isEqualTo(ConMonItemStatus.CLOSED);
        assertThat(r.rawStatus()).isEqualTo("closed");
    }

    @Test
    void completedKeyword_yieldsClosed() {
        assertThat(PoamStatusDeriver.derive("Completed", List.of()).status())
                .isEqualTo(ConMonItemStatus.CLOSED);
    }

    @Test
    void falsePositive_yieldsClosed() {
        assertThat(PoamStatusDeriver.derive("False-Positive", List.of()).status())
                .isEqualTo(ConMonItemStatus.CLOSED);
    }

    @Test
    void riskAccepted_yieldsOpen() {
        assertThat(PoamStatusDeriver.derive("Risk Accepted", List.of()).status())
                .isEqualTo(ConMonItemStatus.OPEN);
    }

    @Test
    void ongoing_yieldsOpen() {
        assertThat(PoamStatusDeriver.derive("ongoing", List.of()).status())
                .isEqualTo(ConMonItemStatus.OPEN);
    }

    @Test
    void unrecognized_yieldsUnknown() {
        var r = PoamStatusDeriver.derive("flibbertigibbet", List.of());
        assertThat(r.status()).isEqualTo(ConMonItemStatus.UNKNOWN);
        assertThat(r.rawStatus()).isEqualTo("flibbertigibbet");
    }

    @Test
    void nullStatus_emptyFindings_yieldsUnknown() {
        assertThat(PoamStatusDeriver.derive(null, List.of()).status())
                .isEqualTo(ConMonItemStatus.UNKNOWN);
    }

    @Test
    void nullStatus_allFindingsClosed_yieldsClosed() {
        var r = PoamStatusDeriver.derive(null, List.of("closed", "completed"));
        assertThat(r.status()).isEqualTo(ConMonItemStatus.CLOSED);
    }

    @Test
    void nullStatus_anyFindingOpen_yieldsOpen() {
        var r = PoamStatusDeriver.derive(null, List.of("closed", "ongoing"));
        assertThat(r.status()).isEqualTo(ConMonItemStatus.OPEN);
    }

    @Test
    void oscalRiskStatuses_yieldOpenForActiveStates() {
        assertThat(PoamStatusDeriver.derive(null, List.of("investigating")).status())
                .isEqualTo(ConMonItemStatus.OPEN);
        assertThat(PoamStatusDeriver.derive(null, List.of("remediating")).status())
                .isEqualTo(ConMonItemStatus.OPEN);
        assertThat(PoamStatusDeriver.derive(null, List.of("deviation-approved")).status())
                .isEqualTo(ConMonItemStatus.OPEN);
    }

    @Test
    void oscalRiskStatuses_yieldClosedForClosed() {
        assertThat(PoamStatusDeriver.derive(null, List.of("closed")).status())
                .isEqualTo(ConMonItemStatus.CLOSED);
    }
}
