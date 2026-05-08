package gov.nist.oscal.tools.api.service.ai.wizard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.DocumentNormalizer;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PoamWizardTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode extractCompletedDocument(java.util.List<SessionEvent> events) throws Exception {
        SessionEvent complete = events.stream()
                .filter(e -> e.type() == SessionEvent.Type.COMPLETE)
                .findFirst().orElseThrow();
        return MAPPER.readTree(complete.dataJson()).path("document");
    }

    private static final String OUTLINE_JSON = """
        {
          "title": "Acme Trust Center POA&M",
          "version": "1.0",
          "publisher": "Acme",
          "systemTitle": "Acme Trust Center",
          "itemIds": ["P-001", "P-002"]
        }
        """;

    private static final String CHUNK_JSON = """
        [
          {"uuid":"daf bcedd-bad-uuid","title":"Stale TLS cert","description":"Cert expires in 30 days; rotate before deadline.",
           "props":[{"name":"poam-id","value":"P-001"},{"name":"severity","value":"moderate"},
                    {"name":"status","value":"open"},{"name":"scheduled-completion-date","value":"2026-06-01"},
                    {"name":"ai-confidence","ns":"https://oscal-hub.io/ns","value":"high"}]},
          {"uuid":"another-bad uuid","title":"Missing MFA","description":"Console MFA not enforced for admins.",
           "props":[{"name":"poam-id","value":"P-002"},{"name":"severity","value":"high"},
                    {"name":"status","value":"open"},
                    {"name":"ai-confidence","ns":"https://oscal-hub.io/ns","value":"medium"}]}
        ]
        """;

    @Test
    void happyPathProducesAssembledPoamWithFreshUuids() throws Exception {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.POAM)).thenReturn("system");

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("Action and Milestones")), any()))
                .thenReturn(new AnthropicResult(OUTLINE_JSON, 100, 50));
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("poam-item entries")), any()))
                .thenReturn(new AnthropicResult(CHUNK_JSON, 50, 30));

        PoamWizard wizard = new PoamWizard(client, stream, knowledge, normalizer,
                new PoamPromptBuilder(), new PoamChunkingStrategy());

        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source POA&M spreadsheet text");

        WizardOutcome outcome = wizard.run(ctx);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.tokensIn()).isEqualTo(150);
        assertThat(outcome.tokensOut()).isEqualTo(80);

        ArgumentCaptor<SessionEvent> ev = ArgumentCaptor.forClass(SessionEvent.class);
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), ev.capture());
        JsonNode doc = extractCompletedDocument(ev.getAllValues());

        JsonNode poam = doc.path("plan-of-action-and-milestones");
        assertThat(poam.path("metadata").path("title").asText()).isEqualTo("Acme Trust Center POA&M");

        JsonNode items = poam.path("poam-items");
        assertThat(items.size()).isEqualTo(2);

        JsonNode first = items.get(0);
        assertThat(first.path("title").asText()).isEqualTo("Stale TLS cert");
        // The model's bogus uuid was replaced by a fresh one — must be a valid UUID and NOT contain a space.
        String uuid1 = first.path("uuid").asText();
        assertThat(uuid1).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        // ai-confidence prop survives.
        boolean hasConfidence = false;
        for (JsonNode p : first.path("props")) {
            if ("ai-confidence".equals(p.path("name").asText())
                    && "https://oscal-hub.io/ns".equals(p.path("ns").asText())
                    && "high".equals(p.path("value").asText())) {
                hasConfidence = true;
            }
        }
        assertThat(hasConfidence).isTrue();
    }

    @Test
    void unparseableChunkIsSkippedNotFatal() throws Exception {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.POAM)).thenReturn("system");

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("Action and Milestones")), any()))
                .thenReturn(new AnthropicResult(OUTLINE_JSON, 100, 50));
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("poam-item entries")), any()))
                .thenReturn(new AnthropicResult("not json at all { broken", 50, 30));

        PoamWizard wizard = new PoamWizard(client, stream, knowledge, normalizer,
                new PoamPromptBuilder(), new PoamChunkingStrategy());

        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source");

        WizardOutcome outcome = wizard.run(ctx);
        assertThat(outcome.success()).isTrue();

        ArgumentCaptor<SessionEvent> ev = ArgumentCaptor.forClass(SessionEvent.class);
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), ev.capture());
        boolean sawWarning = ev.getAllValues().stream()
                .filter(e -> e.type() == SessionEvent.Type.PROGRESS)
                .anyMatch(e -> e.dataJson().contains("unparseable JSON"));
        assertThat(sawWarning).isTrue();

        JsonNode doc = extractCompletedDocument(ev.getAllValues());
        JsonNode items = doc.path("plan-of-action-and-milestones").path("poam-items");
        assertThat(items.size()).isEqualTo(0);
    }
}
