package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.service.ai.wizard.SmokeWizard;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardContext;
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
    void persistsSessionAndDispatchesToAsyncRunner() {
        SmokeWizard smoke = mock(SmokeWizard.class);
        when(smoke.kind()).thenReturn(WizardKind.SMOKE);

        AiSessionRepository sessions = mock(AiSessionRepository.class);
        when(sessions.save(any(AiSession.class))).thenAnswer(inv -> inv.getArgument(0));

        AiSettingsServiceFacade settings = mock(AiSettingsServiceFacade.class);
        when(settings.requireApiKey(1L)).thenReturn("sk-ant-test-1234567890");
        when(settings.getDefaultModel(1L)).thenReturn("claude-opus-4-7");

        AsyncWizardRunner asyncRunner = mock(AsyncWizardRunner.class);

        WizardRouter router = new WizardRouter(List.of(smoke));
        AiOrchestrator orch = new AiOrchestrator(sessions, settings, router, asyncRunner);

        UUID id = orch.start(1L, 7L, WizardKind.SMOKE, AiSessionMode.STREAMING, "ping");

        // Verify session is saved with RUNNING status
        ArgumentCaptor<AiSession> captor = ArgumentCaptor.forClass(AiSession.class);
        verify(sessions).save(captor.capture());
        AiSession saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AiSessionStatus.RUNNING);
        assertThat(saved.getOrganizationId()).isEqualTo(1L);
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(id).isNotNull();

        // Verify async runner is called with the correct wizard and context
        ArgumentCaptor<WizardContext> ctxCaptor = ArgumentCaptor.forClass(WizardContext.class);
        verify(asyncRunner).run(eq(smoke), ctxCaptor.capture());
        WizardContext ctx = ctxCaptor.getValue();
        assertThat(ctx.sessionId()).isEqualTo(id);
        assertThat(ctx.organizationId()).isEqualTo(1L);
        assertThat(ctx.userId()).isEqualTo(7L);
        assertThat(ctx.input()).isEqualTo("ping");
    }
}
