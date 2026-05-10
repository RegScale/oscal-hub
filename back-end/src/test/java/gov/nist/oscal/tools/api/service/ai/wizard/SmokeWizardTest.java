package gov.nist.oscal.tools.api.service.ai.wizard;

import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SmokeWizardTest {

    @Test
    void runsHelloWorldAndPublishesProgressAndComplete() {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader loader = mock(KnowledgeLoader.class);
        when(loader.systemFor(WizardKind.SMOKE)).thenReturn("system");
        when(client.send(eq("sk-ant-test-key-1234567890"), any(AnthropicCall.class)))
                .thenReturn(new AnthropicResult("hello back", 5, 3));

        SmokeWizard wizard = new SmokeWizard(client, stream, loader);
        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-test-key-1234567890", "claude-opus-4-7", "ping me");

        WizardOutcome outcome = wizard.run(ctx);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.tokensIn()).isEqualTo(5);
        assertThat(outcome.tokensOut()).isEqualTo(3);
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), any(SessionEvent.class));
    }
}
