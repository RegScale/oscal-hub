package gov.nist.oscal.tools.api.service.ai.wizard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.DocumentNormalizer;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.ProfileSourceLoader;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SspWizardTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Pulls the COMPLETE event's wrapped SSP doc — SessionEvent.complete wraps as {"document": ...}. */
    private JsonNode extractCompletedDocument(java.util.List<SessionEvent> events) throws Exception {
        SessionEvent complete = events.stream()
                .filter(e -> e.type() == SessionEvent.Type.COMPLETE)
                .findFirst().orElseThrow();
        return MAPPER.readTree(complete.dataJson()).path("document");
    }

    private boolean hasProgressContaining(java.util.List<SessionEvent> events, String needle) {
        return events.stream()
                .filter(e -> e.type() == SessionEvent.Type.PROGRESS)
                .anyMatch(e -> e.dataJson().contains(needle));
    }

    private static final String OUTLINE_JSON = """
        {
          "title": "Acme Trust Center SSP",
          "version": "1.0",
          "publisher": "Acme",
          "systemName": "Acme Trust Center",
          "systemDescription": "Customer trust portal.",
          "systemId": "acme-trust",
          "sensitivityLevel": "moderate",
          "informationTypes": [{
            "uuid": "00000000-0000-0000-0000-000000000010",
            "title": "Customer Data",
            "description": "PII.",
            "categorizations": []
          }],
          "components": [{
            "uuid": "00000000-0000-0000-0000-000000000020",
            "type": "this-system",
            "title": "Trust Center",
            "description": "Web app."
          }],
          "users": [{
            "uuid": "00000000-0000-0000-0000-000000000030",
            "title": "Admin",
            "role-ids": ["admin"]
          }],
          "authorizationBoundary": "Cloud Run + Cloud SQL.",
          "controlIds": ["ac-1", "ac-2"]
        }
        """;

    private static final String CHUNK_JSON = """
        [
          {"uuid":"00000000-0000-0000-0000-000000000100","control-id":"ac-1","description":"D1","props":[{"name":"ai-confidence","ns":"https://oscal-hub.io/ns","value":"high"}]},
          {"uuid":"00000000-0000-0000-0000-000000000101","control-id":"ac-2","description":"D2","props":[{"name":"ai-confidence","ns":"https://oscal-hub.io/ns","value":"medium"}]}
        ]
        """;

    @Test
    void skipProfileHappyPathProducesSchemaShapedSsp() throws Exception {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        ProfileSourceLoader profileLoader = mock(ProfileSourceLoader.class);
        ProfileControlIdExtractor extractor = mock(ProfileControlIdExtractor.class);
        UserRepository users = mock(UserRepository.class);
        when(knowledge.systemFor(WizardKind.SSP)).thenReturn("system");

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("System Security Plan")), any()))
                .thenReturn(new AnthropicResult(OUTLINE_JSON, 100, 50));
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("implemented-requirement entries")), any()))
                .thenReturn(new AnthropicResult(CHUNK_JSON, 50, 30));

        SspWizard wizard = new SspWizard(client, stream, knowledge, normalizer,
                new SspPromptBuilder(), new SspChunkingStrategy(),
                profileLoader, extractor, users);

        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source doc text", null);

        WizardOutcome outcome = wizard.run(ctx);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.tokensIn()).isEqualTo(150);
        assertThat(outcome.tokensOut()).isEqualTo(80);

        ArgumentCaptor<SessionEvent> ev = ArgumentCaptor.forClass(SessionEvent.class);
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), ev.capture());
        JsonNode doc = extractCompletedDocument(ev.getAllValues());
        JsonNode ssp = doc.path("system-security-plan");
        assertThat(ssp.path("metadata").path("title").asText()).isEqualTo("Acme Trust Center SSP");
        assertThat(ssp.path("import-profile").path("href").asText()).isEmpty();
        assertThat(ssp.path("system-characteristics").path("system-name").asText()).isEqualTo("Acme Trust Center");
        assertThat(ssp.path("system-characteristics").path("security-sensitivity-level").asText()).isEqualTo("moderate");
        assertThat(ssp.path("system-implementation").path("components").size()).isEqualTo(1);
        JsonNode reqs = ssp.path("control-implementation").path("implemented-requirements");
        assertThat(reqs.size()).isEqualTo(2);
        assertThat(reqs.get(0).path("control-id").asText()).isEqualTo("ac-1");
        assertThat(reqs.get(0).path("props").get(0).path("name").asText()).isEqualTo("ai-confidence");
    }
}
