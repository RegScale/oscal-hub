package gov.nist.oscal.tools.api.service.ai.wizard;

import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.DocumentNormalizer;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CatalogWizardTest {

    @Test
    void runProducesMergedCatalogJsonAndPublishesComplete() {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.CATALOG)).thenReturn("system");

        // Outline response
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("Step 1"))))
                .thenReturn(new AnthropicResult(
                        "{\"title\":\"Test Catalog\",\"version\":\"1.0\",\"publisher\":\"Acme\"," +
                        "\"families\":[{\"id\":\"ac\",\"title\":\"Access Control\",\"controlIds\":[\"ac-1\"]}]}",
                        100, 50));
        // Family response
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("Step 2"))))
                .thenReturn(new AnthropicResult(
                        "{\"id\":\"ac\",\"class\":\"family\",\"title\":\"Access Control\"," +
                        "\"controls\":[{\"id\":\"ac-1\",\"title\":\"Policy\"}]}",
                        100, 50));

        CatalogWizard wizard = new CatalogWizard(client, stream, knowledge, normalizer,
                new CatalogPromptBuilder(), new CatalogChunkingStrategy());

        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source document text");

        WizardOutcome outcome = wizard.run(ctx);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.tokensIn()).isEqualTo(200);
        assertThat(outcome.tokensOut()).isEqualTo(100);
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), any());
    }
}
