package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.service.ai.wizard.SmokeWizard;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardContext;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class AiOrchestratorTest {

    @Test
    void persistsSessionAndRunsWizard() {
        SmokeWizard smoke = mock(SmokeWizard.class);
        when(smoke.kind()).thenReturn(WizardKind.SMOKE);
        when(smoke.run(any())).thenReturn(WizardOutcome.ok(7, 4));

        AiSessionRepository sessions = mock(AiSessionRepository.class);
        when(sessions.save(any(AiSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sessions.findById(any(UUID.class))).thenAnswer(inv -> {
            AiSession s = new AiSession();
            s.setId(inv.getArgument(0));
            return Optional.of(s);
        });

        AiSettingsServiceFacade settings = mock(AiSettingsServiceFacade.class);
        when(settings.requireApiKey(1L)).thenReturn("sk-ant-test-1234567890");
        when(settings.getDefaultModel(1L)).thenReturn("claude-opus-4-7");

        WizardRouter router = new WizardRouter(List.of(smoke));
        AiOrchestrator orch = new AiOrchestrator(sessions, settings, router);

        UUID id = orch.start(1L, 7L, WizardKind.SMOKE, AiSessionMode.STREAMING, "ping");

        ArgumentCaptor<AiSession> captor = ArgumentCaptor.forClass(AiSession.class);
        verify(sessions, atLeast(2)).save(captor.capture());
        AiSession last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(last.getStatus()).isEqualTo(AiSessionStatus.COMPLETED);
        assertThat(last.getTokensIn()).isEqualTo(7);
        assertThat(last.getTokensOut()).isEqualTo(4);
        assertThat(id).isNotNull();
    }
}
