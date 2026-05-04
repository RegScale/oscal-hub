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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ComponentDefWizardTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void runProducesMergedComponentDefJsonAndPublishesComplete() throws Exception {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.COMPONENT_DEF)).thenReturn("system");

        String outlineJson =
                "{\"productTitle\":\"Red Hat Enterprise Linux 9 STIG\"," +
                "\"productDescription\":\"RHEL 9 hardening guide.\"," +
                "\"componentType\":\"software\"," +
                "\"version\":\"V1R1\"," +
                "\"publisher\":\"DISA\"," +
                "\"catalogSource\":\"https://raw.githubusercontent.com/usnistgov/oscal-content/main/nist.gov/SP800-53/rev5/json/NIST_SP-800-53_rev5_catalog.json\"," +
                "\"controlIds\":[\"ac-1\",\"ac-2\"]}";

        String chunkJson =
                "[{\"uuid\":\"" + UUID.randomUUID() + "\",\"control-id\":\"ac-1\"," +
                "\"description\":\"RHEL 9 enforces AC-1 via PAM.\"}," +
                "{\"uuid\":\"" + UUID.randomUUID() + "\",\"control-id\":\"ac-2\"," +
                "\"description\":\"Account management configured via authselect.\"}]";

        // Outline response — matched on the outline prompt keyword
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("configuration guide"))))
                .thenReturn(new AnthropicResult(outlineJson, 100, 50));

        // Per-chunk response — matched on the componentPrompt keyword
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("implemented-requirements entries"))))
                .thenReturn(new AnthropicResult(chunkJson, 100, 50));

        ComponentDefWizard wizard = new ComponentDefWizard(
                client, stream, knowledge, normalizer,
                new ComponentDefPromptBuilder(), new ComponentDefChunkingStrategy(),
                new gov.nist.oscal.tools.api.service.ai.XccdfTrimmer());

        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source STIG document text");

        WizardOutcome outcome = wizard.run(ctx);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.tokensIn()).isEqualTo(200);
        assertThat(outcome.tokensOut()).isEqualTo(100);

        // Capture the complete event and assert structure
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), any());

        // Verify the complete event was published with well-formed JSON
        verify(stream).publish(eq(ctx.sessionId()), argThat(event ->
                event.type() == SessionEvent.Type.COMPLETE));
    }

    @Test
    void mergedJsonContainsExpectedStructure() throws Exception {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.COMPONENT_DEF)).thenReturn("system");

        String outlineJson =
                "{\"productTitle\":\"CIS Ubuntu 20.04\"," +
                "\"productDescription\":\"Ubuntu hardening benchmark.\"," +
                "\"componentType\":\"software\"," +
                "\"version\":\"2.0.0\"," +
                "\"publisher\":\"CIS\"," +
                "\"catalogSource\":\"https://example.com/catalog.json\"," +
                "\"controlIds\":[\"ac-1\",\"ia-2\",\"au-3\"]}";

        String chunkJson =
                "[{\"uuid\":\"" + UUID.randomUUID() + "\",\"control-id\":\"ac-1\",\"description\":\"desc1\"}," +
                "{\"uuid\":\"" + UUID.randomUUID() + "\",\"control-id\":\"ia-2\",\"description\":\"desc2\"}," +
                "{\"uuid\":\"" + UUID.randomUUID() + "\",\"control-id\":\"au-3\",\"description\":\"desc3\"}]";

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("configuration guide"))))
                .thenReturn(new AnthropicResult(outlineJson, 80, 40));
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("implemented-requirements entries"))))
                .thenReturn(new AnthropicResult(chunkJson, 120, 60));

        // Capture the complete event JSON
        final String[] capturedJson = {null};
        doAnswer(inv -> {
            SessionEvent evt = inv.getArgument(1);
            if (evt.type() == SessionEvent.Type.COMPLETE) {
                // dataJson is {"document":<oscalJson>}
                String dataJson = evt.dataJson();
                capturedJson[0] = dataJson.substring("{\"document\":".length(), dataJson.length());
            }
            return null;
        }).when(stream).publish(any(), any());

        ComponentDefWizard wizard = new ComponentDefWizard(
                client, stream, knowledge, normalizer,
                new ComponentDefPromptBuilder(), new ComponentDefChunkingStrategy(),
                new gov.nist.oscal.tools.api.service.ai.XccdfTrimmer());

        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source document");

        WizardOutcome outcome = wizard.run(ctx);
        assertThat(outcome.success()).isTrue();
        assertThat(capturedJson[0]).isNotNull();

        JsonNode doc = MAPPER.readTree(capturedJson[0]);
        JsonNode compDef = doc.path("component-definition");

        // Title in metadata
        assertThat(compDef.path("metadata").path("title").asText())
                .isEqualTo("CIS Ubuntu 20.04");

        // Component type and title
        JsonNode comp = compDef.path("components").get(0);
        assertThat(comp.path("type").asText()).isEqualTo("software");
        assertThat(comp.path("title").asText()).isEqualTo("CIS Ubuntu 20.04");

        // Control-implementations source
        JsonNode ctrlImpl = comp.path("control-implementations").get(0);
        assertThat(ctrlImpl.path("source").asText()).isEqualTo("https://example.com/catalog.json");

        // Implemented-requirements contain all 3 control IDs
        JsonNode reqs = ctrlImpl.path("implemented-requirements");
        assertThat(reqs.isArray()).isTrue();
        assertThat(reqs).hasSize(3);
        assertThat(reqs.get(0).path("control-id").asText()).isEqualTo("ac-1");
        assertThat(reqs.get(1).path("control-id").asText()).isEqualTo("ia-2");
        assertThat(reqs.get(2).path("control-id").asText()).isEqualTo("au-3");
    }
}
