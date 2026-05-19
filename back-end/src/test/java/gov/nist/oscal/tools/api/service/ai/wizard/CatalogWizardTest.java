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
import static org.mockito.Mockito.*;

class CatalogWizardTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void runProducesMergedCatalogJsonAndPublishesComplete() {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.CATALOG)).thenReturn("system");

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("controls publication")), any()))
                .thenReturn(new AnthropicResult(
                        "{\"title\":\"Test Catalog\",\"version\":\"1.0\",\"publisher\":\"Acme\"," +
                        "\"families\":[{\"id\":\"ac\",\"title\":\"Access Control\",\"controlIds\":[\"ac-1\"]}]}",
                        100, 50));
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("Generate the")), any()))
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

    @Test
    void singleFamilyContentFilterFailureDoesNotTankTheRun() throws Exception {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.CATALOG)).thenReturn("system");

        // Outline returns 3 families.
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("controls publication")), any()))
                .thenReturn(new AnthropicResult(
                        "{\"title\":\"Cat\",\"version\":\"1.0\",\"publisher\":\"X\"," +
                        "\"families\":[" +
                        "{\"id\":\"a\",\"title\":\"A\",\"controlIds\":[\"a-1\"]}," +
                        "{\"id\":\"b\",\"title\":\"B\",\"controlIds\":[\"b-1\"]}," +
                        "{\"id\":\"c\",\"title\":\"C\",\"controlIds\":[\"c-1\"]}" +
                        "]}",
                        100, 50));

        // Family a — ok. Family b — content-filter block. Family c — ok.
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("\"a\"") && c.userMessage().contains("Generate the")), any()))
                .thenReturn(new AnthropicResult(
                        "{\"id\":\"a\",\"class\":\"family\",\"title\":\"A\",\"controls\":[{\"id\":\"a-1\",\"title\":\"T\"}]}",
                        50, 25));
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("\"b\"") && c.userMessage().contains("Generate the")), any()))
                .thenThrow(new RuntimeException(
                        "400: {type=error, error={type=invalid_request_error, message=Output blocked by content filtering policy}}"));
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("\"c\"") && c.userMessage().contains("Generate the")), any()))
                .thenReturn(new AnthropicResult(
                        "{\"id\":\"c\",\"class\":\"family\",\"title\":\"C\",\"controls\":[{\"id\":\"c-1\",\"title\":\"T\"}]}",
                        60, 30));

        CatalogWizard wizard = new CatalogWizard(client, stream, knowledge, normalizer,
                new CatalogPromptBuilder(), new CatalogChunkingStrategy());

        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source document text");

        WizardOutcome outcome = wizard.run(ctx);

        // Wizard succeeded with a partial catalog.
        assertThat(outcome.success()).isTrue();
        // Token totals reflect the outline call (100/50) plus only the
        // successful families a (50/25) and c (60/30) — family b's failed
        // call must NOT contribute.
        assertThat(outcome.tokensIn()).isEqualTo(210);
        assertThat(outcome.tokensOut()).isEqualTo(105);

        // The complete event contains the partial catalog with only a + c.
        ArgumentCaptor<SessionEvent> events = ArgumentCaptor.forClass(SessionEvent.class);
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), events.capture());
        SessionEvent completeEvent = events.getAllValues().stream()
                .filter(e -> e.type() == SessionEvent.Type.COMPLETE)
                .findFirst()
                .orElseThrow();
        JsonNode payload = MAPPER.readTree(completeEvent.dataJson());
        JsonNode groups = payload.path("document").path("catalog").path("groups");
        assertThat(groups.isArray()).isTrue();
        assertThat(groups.size()).isEqualTo(2);
        assertThat(groups.get(0).path("id").asText()).isEqualTo("a");
        assertThat(groups.get(1).path("id").asText()).isEqualTo("c");

        // Metadata remarks call out the skipped family by id.
        String remarks = payload.path("document").path("catalog").path("metadata").path("remarks").asText();
        assertThat(remarks).contains("skipped 1 of 3");
        assertThat(remarks).contains("**B**");
        assertThat(remarks).contains("content-filtering");

        // A progress event also mentioned the skip in human-readable form.
        boolean skipMentioned = events.getAllValues().stream()
                .filter(e -> e.type() == SessionEvent.Type.PROGRESS)
                .anyMatch(e -> e.dataJson().contains("Skipping B")
                        && e.dataJson().contains("content-filtering"));
        assertThat(skipMentioned).isTrue();
    }

    @Test
    void contentFilterRejectionTriggersSafeRetryAndRecovers() throws Exception {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.CATALOG)).thenReturn("system");

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("controls publication")), any()))
                .thenReturn(new AnthropicResult(
                        "{\"title\":\"X\",\"version\":\"1\",\"publisher\":\"Y\"," +
                        "\"families\":[{\"id\":\"os\",\"title\":\"Ops Sec\",\"controlIds\":[\"os-1\"]}]}",
                        100, 50));

        // First family attempt: blocked by safety filter. Safe retry: succeeds.
        when(client.send(any(),
                argThat(c -> c != null
                        && c.userMessage().contains("Generate the")
                        && !c.userMessage().contains("SAFETY RETRY")),
                any()))
                .thenThrow(new RuntimeException(
                        "400: {type=error, error={type=invalid_request_error, message=Output blocked by content filtering policy}}"));
        when(client.send(any(),
                argThat(c -> c != null && c.userMessage().contains("SAFETY RETRY")),
                any()))
                .thenReturn(new AnthropicResult(
                        "{\"id\":\"os\",\"class\":\"family\",\"title\":\"Ops Sec\",\"controls\":[{\"id\":\"os-1\",\"title\":\"Policy\"}]}",
                        80, 40));

        CatalogWizard wizard = new CatalogWizard(client, stream, knowledge, normalizer,
                new CatalogPromptBuilder(), new CatalogChunkingStrategy());
        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source");

        WizardOutcome outcome = wizard.run(ctx);

        assertThat(outcome.success()).isTrue();
        // outline (100/50) + safe-retry success (80/40); the first failed call is NOT counted
        assertThat(outcome.tokensIn()).isEqualTo(180);
        assertThat(outcome.tokensOut()).isEqualTo(90);

        // The complete event ships the recovered group, and metadata.remarks is
        // NOT present because nothing was ultimately skipped.
        ArgumentCaptor<SessionEvent> events = ArgumentCaptor.forClass(SessionEvent.class);
        verify(stream, atLeastOnce()).publish(eq(ctx.sessionId()), events.capture());
        SessionEvent completeEvent = events.getAllValues().stream()
                .filter(e -> e.type() == SessionEvent.Type.COMPLETE)
                .findFirst().orElseThrow();
        JsonNode payload = MAPPER.readTree(completeEvent.dataJson());
        assertThat(payload.path("document").path("catalog").path("groups").size()).isEqualTo(1);
        assertThat(payload.path("document").path("catalog").path("metadata").path("remarks").isMissingNode()).isTrue();

        // The user sees a "Retrying OS with safer wording" breadcrumb.
        boolean retryAnnounced = events.getAllValues().stream()
                .filter(e -> e.type() == SessionEvent.Type.PROGRESS)
                .anyMatch(e -> e.dataJson().contains("Retrying OS"));
        assertThat(retryAnnounced).isTrue();

        // Two family calls were made (initial + safe retry) and one outline call.
        verify(client, times(3)).send(any(), any(), any());
    }

    @Test
    void contentFilterBlockedTwiceFallsBackToSkip() {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.CATALOG)).thenReturn("system");

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("controls publication")), any()))
                .thenReturn(new AnthropicResult(
                        "{\"title\":\"X\",\"version\":\"1\",\"publisher\":\"Y\"," +
                        "\"families\":[" +
                        "{\"id\":\"os\",\"title\":\"Ops Sec\",\"controlIds\":[\"os-1\"]}," +
                        "{\"id\":\"ac\",\"title\":\"Access\",\"controlIds\":[\"ac-1\"]}" +
                        "]}",
                        100, 50));
        // os family: both initial and safe retry blocked. ac family: succeeds.
        when(client.send(any(),
                argThat(c -> c != null
                        && c.userMessage().contains("Generate the")
                        && c.userMessage().contains("\"os\"")),
                any()))
                .thenThrow(new RuntimeException(
                        "400: {type=error, error={type=invalid_request_error, message=Output blocked by content filtering policy}}"));
        when(client.send(any(),
                argThat(c -> c != null
                        && c.userMessage().contains("Generate the")
                        && c.userMessage().contains("\"ac\"")),
                any()))
                .thenReturn(new AnthropicResult(
                        "{\"id\":\"ac\",\"class\":\"family\",\"title\":\"Access\",\"controls\":[{\"id\":\"ac-1\",\"title\":\"T\"}]}",
                        50, 25));

        CatalogWizard wizard = new CatalogWizard(client, stream, knowledge, normalizer,
                new CatalogPromptBuilder(), new CatalogChunkingStrategy());
        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source");

        WizardOutcome outcome = wizard.run(ctx);

        assertThat(outcome.success()).isTrue();
        // os tried twice (both blocked, none counted); ac succeeded once.
        // outline (100/50) + ac (50/25) = 150/75
        assertThat(outcome.tokensIn()).isEqualTo(150);
        assertThat(outcome.tokensOut()).isEqualTo(75);
        // Outline + os initial + os safe retry + ac = 4 calls.
        verify(client, times(4)).send(any(), any(), any());
    }

    @Test
    void nonContentFilterRejectionSkipsWithoutRetry() {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.CATALOG)).thenReturn("system");

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("controls publication")), any()))
                .thenReturn(new AnthropicResult(
                        "{\"title\":\"X\",\"version\":\"1\",\"publisher\":\"Y\"," +
                        "\"families\":[" +
                        "{\"id\":\"a\",\"title\":\"A\",\"controlIds\":[\"a-1\"]}," +
                        "{\"id\":\"b\",\"title\":\"B\",\"controlIds\":[\"b-1\"]}" +
                        "]}",
                        50, 25));
        // a fails with a rate-limit (not content filter) — must NOT retry.
        when(client.send(any(),
                argThat(c -> c != null && c.userMessage().contains("Generate the") && c.userMessage().contains("\"a\"")),
                any()))
                .thenThrow(new RuntimeException("429: rate_limit_error"));
        when(client.send(any(),
                argThat(c -> c != null && c.userMessage().contains("Generate the") && c.userMessage().contains("\"b\"")),
                any()))
                .thenReturn(new AnthropicResult(
                        "{\"id\":\"b\",\"class\":\"family\",\"title\":\"B\",\"controls\":[{\"id\":\"b-1\",\"title\":\"T\"}]}",
                        40, 20));

        CatalogWizard wizard = new CatalogWizard(client, stream, knowledge, normalizer,
                new CatalogPromptBuilder(), new CatalogChunkingStrategy());
        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source");

        wizard.run(ctx);

        // Outline + a (rate-limited, no retry) + b = 3 calls. If we'd retried
        // on rate-limit we'd see 4.
        verify(client, times(3)).send(any(), any(), any());
    }

    @Test
    void isContentFilterErrorRecognizesAnthropicMessage() {
        assertThat(CatalogWizard.isContentFilterError(
                new RuntimeException("400: {type=error, error={type=invalid_request_error, message=Output blocked by content filtering policy}}")))
                .isTrue();
        assertThat(CatalogWizard.isContentFilterError(
                new RuntimeException("wrapper", new RuntimeException("content filtering policy"))))
                .isTrue();
        assertThat(CatalogWizard.isContentFilterError(
                new RuntimeException("429: rate_limit_error")))
                .isFalse();
        assertThat(CatalogWizard.isContentFilterError(new RuntimeException((String) null)))
                .isFalse();
    }

    @Test
    void allFamiliesFailingProducesFailedOutcome() {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.CATALOG)).thenReturn("system");

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("controls publication")), any()))
                .thenReturn(new AnthropicResult(
                        "{\"title\":\"X\",\"version\":\"1\",\"publisher\":\"Y\"," +
                        "\"families\":[" +
                        "{\"id\":\"a\",\"title\":\"A\",\"controlIds\":[\"a-1\"]}," +
                        "{\"id\":\"b\",\"title\":\"B\",\"controlIds\":[\"b-1\"]}" +
                        "]}",
                        50, 25));

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("Generate the")), any()))
                .thenThrow(new RuntimeException(
                        "400: {type=error, error={type=invalid_request_error, message=Output blocked by content filtering policy}}"));

        CatalogWizard wizard = new CatalogWizard(client, stream, knowledge, normalizer,
                new CatalogPromptBuilder(), new CatalogChunkingStrategy());
        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source document text");

        WizardOutcome outcome = wizard.run(ctx);
        assertThat(outcome.success()).isFalse();
        assertThat(outcome.errorCode()).isEqualTo("all_families_failed");
        assertThat(outcome.errorMessage()).contains("All 2 families were rejected");
    }

    @Test
    void authErrorOnAnyFamilyAbortsImmediately() {
        AnthropicClient client = mock(AnthropicClient.class);
        AiSessionEventStream stream = mock(AiSessionEventStream.class);
        KnowledgeLoader knowledge = mock(KnowledgeLoader.class);
        DocumentNormalizer normalizer = mock(DocumentNormalizer.class);
        when(knowledge.systemFor(WizardKind.CATALOG)).thenReturn("system");

        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("controls publication")), any()))
                .thenReturn(new AnthropicResult(
                        "{\"title\":\"X\",\"version\":\"1\",\"publisher\":\"Y\"," +
                        "\"families\":[" +
                        "{\"id\":\"a\",\"title\":\"A\",\"controlIds\":[\"a-1\"]}," +
                        "{\"id\":\"b\",\"title\":\"B\",\"controlIds\":[\"b-1\"]}" +
                        "]}",
                        50, 25));

        // First family throws IllegalArgumentException (missing API key, etc.) — must propagate.
        when(client.send(any(), argThat(c -> c != null && c.userMessage().contains("Generate the")), any()))
                .thenThrow(new IllegalArgumentException("Missing Anthropic API key"));

        CatalogWizard wizard = new CatalogWizard(client, stream, knowledge, normalizer,
                new CatalogPromptBuilder(), new CatalogChunkingStrategy());
        WizardContext ctx = WizardContext.text(UUID.randomUUID(), 1L, 7L,
                "sk-ant-xxx", "claude-opus-4-7", "Source document text");

        WizardOutcome outcome = wizard.run(ctx);
        assertThat(outcome.success()).isFalse();
        assertThat(outcome.errorCode()).isEqualTo("auth_or_input");
        // Only the first family was attempted before propagating — the second is not called.
        verify(client, times(2)).send(any(), any(), any()); // outline + first family
    }
}
