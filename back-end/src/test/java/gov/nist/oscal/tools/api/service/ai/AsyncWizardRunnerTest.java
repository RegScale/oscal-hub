package gov.nist.oscal.tools.api.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.wizard.Wizard;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardContext;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AsyncWizardRunnerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ModelPricing pricing = new ModelPricing();

    private WizardContext context(UUID id) {
        return WizardContext.text(id, 1L, 7L, "sk-ant-key", "claude-opus-4-7", "ping");
    }

    private AsyncWizardRunner runner(AiSessionRepository sessions, AiSessionEventStream stream) {
        return new AsyncWizardRunner(sessions, stream, pricing, mapper);
    }

    @Test
    void successPathSetsCompletedStatus() {
        UUID id = UUID.randomUUID();
        AiSession session = new AiSession();
        session.setId(id);

        AiSessionRepository sessions = mock(AiSessionRepository.class);
        when(sessions.findById(id)).thenReturn(Optional.of(session));
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        when(stream.drainBuffer(any())).thenReturn(List.of());

        Wizard wizard = mock(Wizard.class);
        when(wizard.run(any())).thenReturn(WizardOutcome.ok(10, 5));

        runner(sessions, stream).run(wizard, context(id));

        ArgumentCaptor<AiSession> captor = ArgumentCaptor.forClass(AiSession.class);
        verify(sessions, times(2)).save(captor.capture());
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

        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        when(stream.drainBuffer(any())).thenReturn(List.of());

        Wizard wizard = mock(Wizard.class);
        when(wizard.run(any())).thenReturn(WizardOutcome.failed("api_error", "upstream failure"));

        runner(sessions, stream).run(wizard, context(id));

        ArgumentCaptor<AiSession> captor = ArgumentCaptor.forClass(AiSession.class);
        verify(sessions, times(2)).save(captor.capture());
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

        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        when(stream.drainBuffer(any())).thenReturn(List.of());

        Wizard wizard = mock(Wizard.class);
        when(wizard.run(any())).thenThrow(new RuntimeException("network timeout"));

        runner(sessions, stream).run(wizard, context(id));

        ArgumentCaptor<AiSession> captor = ArgumentCaptor.forClass(AiSession.class);
        verify(sessions, times(2)).save(captor.capture());
        AiSession saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AiSessionStatus.FAILED);
        assertThat(saved.getErrorCode()).isEqualTo("orchestrator_error");
        assertThat(saved.getErrorMessage()).isEqualTo("network timeout");
        assertThat(saved.getEndedAt()).isNotNull();
    }

    @Test
    void successRunComputesNonZeroCost() {
        UUID id = UUID.randomUUID();
        AiSession session = new AiSession();
        session.setId(id);
        session.setModel("claude-opus-4-7");

        AiSessionRepository sessions = mock(AiSessionRepository.class);
        when(sessions.findById(id)).thenReturn(Optional.of(session));
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        when(stream.drainBuffer(any())).thenReturn(List.of());

        Wizard wizard = mock(Wizard.class);
        // 1000 tokens in + 500 tokens out → cost > 0
        when(wizard.run(any())).thenReturn(WizardOutcome.ok(1000, 500));

        runner(sessions, stream).run(wizard, context(id));

        ArgumentCaptor<AiSession> captor = ArgumentCaptor.forClass(AiSession.class);
        verify(sessions, times(2)).save(captor.capture());
        AiSession saved = captor.getValue();
        assertThat(saved.getCostUsdMicros()).isPositive();
    }
}
