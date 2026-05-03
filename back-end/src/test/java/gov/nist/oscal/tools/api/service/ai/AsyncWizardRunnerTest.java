package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.service.ai.wizard.Wizard;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardContext;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AsyncWizardRunnerTest {

    private WizardContext context(UUID id) {
        return new WizardContext(id, 1L, 7L, "sk-ant-key", "claude-opus-4-7", "ping");
    }

    @Test
    void successPathSetsCompletedStatus() {
        UUID id = UUID.randomUUID();
        AiSession session = new AiSession();
        session.setId(id);

        AiSessionRepository sessions = mock(AiSessionRepository.class);
        when(sessions.findById(id)).thenReturn(Optional.of(session));
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Wizard wizard = mock(Wizard.class);
        when(wizard.run(any())).thenReturn(WizardOutcome.ok(10, 5));

        AsyncWizardRunner runner = new AsyncWizardRunner(sessions);
        runner.run(wizard, context(id));

        ArgumentCaptor<AiSession> captor = ArgumentCaptor.forClass(AiSession.class);
        verify(sessions).save(captor.capture());
        AiSession saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AiSessionStatus.COMPLETED);
        assertThat(saved.getTokensIn()).isEqualTo(10);
        assertThat(saved.getTokensOut()).isEqualTo(5);
        assertThat(saved.getEndedAt()).isNotNull();
        assertThat(saved.getErrorCode()).isNull();
    }

    @Test
    void failureOutcomeSetsFailedStatus() {
        UUID id = UUID.randomUUID();
        AiSession session = new AiSession();
        session.setId(id);

        AiSessionRepository sessions = mock(AiSessionRepository.class);
        when(sessions.findById(id)).thenReturn(Optional.of(session));
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Wizard wizard = mock(Wizard.class);
        when(wizard.run(any())).thenReturn(WizardOutcome.failed("api_error", "upstream failure"));

        AsyncWizardRunner runner = new AsyncWizardRunner(sessions);
        runner.run(wizard, context(id));

        ArgumentCaptor<AiSession> captor = ArgumentCaptor.forClass(AiSession.class);
        verify(sessions).save(captor.capture());
        AiSession saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AiSessionStatus.FAILED);
        assertThat(saved.getErrorCode()).isEqualTo("api_error");
        assertThat(saved.getErrorMessage()).isEqualTo("upstream failure");
        assertThat(saved.getEndedAt()).isNotNull();
    }

    @Test
    void exceptionFromWizardSetsFailedStatus() {
        UUID id = UUID.randomUUID();
        AiSession session = new AiSession();
        session.setId(id);

        AiSessionRepository sessions = mock(AiSessionRepository.class);
        when(sessions.findById(id)).thenReturn(Optional.of(session));
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Wizard wizard = mock(Wizard.class);
        when(wizard.run(any())).thenThrow(new RuntimeException("network timeout"));

        AsyncWizardRunner runner = new AsyncWizardRunner(sessions);
        runner.run(wizard, context(id));

        ArgumentCaptor<AiSession> captor = ArgumentCaptor.forClass(AiSession.class);
        verify(sessions).save(captor.capture());
        AiSession saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AiSessionStatus.FAILED);
        assertThat(saved.getErrorCode()).isEqualTo("orchestrator_error");
        assertThat(saved.getErrorMessage()).isEqualTo("network timeout");
        assertThat(saved.getEndedAt()).isNotNull();
    }
}
