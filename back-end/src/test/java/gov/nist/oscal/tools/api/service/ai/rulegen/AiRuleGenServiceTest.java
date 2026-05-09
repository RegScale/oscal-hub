package gov.nist.oscal.tools.api.service.ai.rulegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.airulegen.RuleGenTurnResponse;
import gov.nist.oscal.tools.api.model.airulegen.TestCase;
import gov.nist.oscal.tools.api.model.airulegen.TestResult;
import gov.nist.oscal.tools.api.service.ai.AiSettingsService;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicToolUseResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiRuleGenServiceTest {

    private static final ObjectMapper OM = new ObjectMapper();

    private final AnthropicClient anthropic = mock(AnthropicClient.class);
    private final AiSettingsService settings = mock(AiSettingsService.class);
    private final AiRuleGenSessionStore store = new AiRuleGenSessionStore();
    private final RuleGenPrompts prompts = new RuleGenPrompts();
    private final RuleGenTestRunner testRunner = mock(RuleGenTestRunner.class);

    private final AiRuleGenService service =
            new AiRuleGenService(anthropic, settings, store, prompts, testRunner);

    @Test
    void start_resolvesDefaultModelFromSettings_andSeedsSessionStore() {
        when(settings.getDefaultModel(42L)).thenReturn("claude-opus-4-7");

        UUID id = service.start(42L, 7L, "catalog");

        AiRuleGenSession s = store.get(id);
        assertThat(s.organizationId()).isEqualTo(42L);
        assertThat(s.userId()).isEqualTo(7L);
        assertThat(s.modelType()).isEqualTo("catalog");
        assertThat(s.anthropicModel()).isEqualTo("claude-opus-4-7");
    }

    @Test
    void close_invalidatesSession() {
        when(settings.getDefaultModel(any())).thenReturn("claude-opus-4-7");
        UUID id = service.start(1L, 1L, "catalog");
        service.close(id);
        assertThatThrownBy(() -> service.session(id))
                .isInstanceOf(RuleGenSessionExpiredException.class);
    }

    @Test
    void turn_clarify_returnsClarifyPhase_andRecordsBothMessages() throws Exception {
        UUID id = startSession();
        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
                .thenReturn(new AnthropicToolUseResult(
                        "ask_clarifying_question",
                        OM.readTree("{\"question\":\"Which control family?\"}"),
                        100, 25));

        RuleGenTurnResponse r = service.turn(id, "I want a rule about controls");

        assertThat(r.phase()).isEqualTo("clarify");
        assertThat(r.clarifyingQuestion()).isEqualTo("Which control family?");
        assertThat(r.proposal()).isNull();
        assertThat(r.iterations()).isEqualTo(1);
        assertThat(r.totalTokensIn()).isEqualTo(100);
        assertThat(r.totalTokensOut()).isEqualTo(25);

        // Both user and assistant turns recorded.
        var transcript = store.get(id).transcript();
        assertThat(transcript).hasSize(2);
        assertThat(transcript.get(0).role()).isEqualTo("user");
        assertThat(transcript.get(1).role()).isEqualTo("assistant");
    }

    @Test
    void turn_proposal_cleanTestMatrix_returnsProposalPhase_inOneIteration() throws Exception {
        UUID id = startSession();
        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
                .thenReturn(toolResult("generate_rule", proposalJson("rule-1", 4), 200, 80));
        when(testRunner.run(anyString(), anyString(), anyString(), any()))
                .thenReturn(allPassing(4));

        RuleGenTurnResponse r = service.turn(id, "rule for ac-1 must have title");

        assertThat(r.phase()).isEqualTo("proposal");
        assertThat(r.proposal().name()).isEqualTo("rule-1");
        assertThat(r.testResults()).hasSize(4);
        assertThat(r.iterations()).isEqualTo(1);
        assertThat(r.message()).isNull();

        // Test runner used the session's modelType ("catalog") and a deterministic ruleId
        verify(testRunner).run(eq("rule-" + id), eq("catalog"), anyString(), any());
        // No revision happened.
        verify(anthropic, times(1)).sendWithTools(anyString(), any(), any());
    }

    @Test
    void turn_dirtyMatrix_triggersRevision_butNoMoreThanMaxRevisions() throws Exception {
        UUID id = startSession();
        // Every Claude call returns the same proposal; the test runner always reports failures.
        // The service should attempt initial + 3 revisions = 4 total Claude calls.
        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
                .thenReturn(toolResult("generate_rule", proposalJson("rule-bad", 4), 100, 50));
        when(testRunner.run(anyString(), anyString(), anyString(), any()))
                .thenReturn(allFailing(4));

        RuleGenTurnResponse r = service.turn(id, "tricky rule");

        // Always returns a save-able proposal even when matrix stays dirty —
        // the test bench is informational, not gating.
        assertThat(r.phase()).isEqualTo("proposal");
        assertThat(r.proposal()).isNotNull();
        assertThat(r.message()).contains("revisions");
        assertThat(r.iterations()).isEqualTo(4);

        // 1 initial + 3 revisions = 4 calls
        verify(anthropic, times(4)).sendWithTools(anyString(), any(), any());
    }

    @Test
    void turn_dirtyMatrix_then_clean_stopsRevising_earlyOnSuccess() throws Exception {
        UUID id = startSession();
        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
                .thenReturn(toolResult("generate_rule", proposalJson("r", 4), 100, 50));
        // First call: fails. Second call: passes.
        when(testRunner.run(anyString(), anyString(), anyString(), any()))
                .thenReturn(allFailing(4))
                .thenReturn(allPassing(4));

        RuleGenTurnResponse r = service.turn(id, "rule");

        assertThat(r.phase()).isEqualTo("proposal");
        assertThat(r.message()).isNull();
        assertThat(r.iterations()).isEqualTo(2);
        verify(anthropic, times(2)).sendWithTools(anyString(), any(), any());
    }

    @Test
    void turn_constraintXmlInvalid_andNeverParses_returnsExhaustedPhase() throws Exception {
        UUID id = startSession();
        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
                .thenReturn(toolResult("generate_rule", proposalJson("bad-xml", 4), 100, 50));
        when(testRunner.run(anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Constraint XML is not parseable"));

        RuleGenTurnResponse r = service.turn(id, "rule");

        assertThat(r.phase()).isEqualTo("exhausted");
        assertThat(r.message()).contains("Constraint XML is not parseable");
        // 1 initial + 3 revision attempts before giving up
        assertThat(r.iterations()).isEqualTo(4);
        verify(anthropic, times(4)).sendWithTools(anyString(), any(), any());
    }

    @Test
    void turn_unexpectedToolName_throws() throws Exception {
        UUID id = startSession();
        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
                .thenReturn(toolResult("write_haiku", OM.readTree("{}"), 10, 10));

        assertThatThrownBy(() -> service.turn(id, "rule"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("write_haiku");
    }

    @Test
    void turn_buildsCallWithSessionsModel_andSystemPromptForModelType() throws Exception {
        UUID id = startSession();
        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
                .thenReturn(toolResult("ask_clarifying_question",
                        OM.readTree("{\"question\":\"q?\"}"), 1, 1));

        service.turn(id, "hi");

        ArgumentCaptor<AnthropicCall> cap = ArgumentCaptor.forClass(AnthropicCall.class);
        verify(anthropic).sendWithTools(eq("api-key-secret"), cap.capture(), any(Consumer.class));
        AnthropicCall call = cap.getValue();
        assertThat(call.model()).isEqualTo("claude-opus-4-7");
        assertThat(call.toolChoice()).isEqualTo("any");
        // System prompt must be the model-type-specific one
        assertThat(call.systemPrompt()).contains("METASCHEMA-CONSTRAINTS");
    }

    @Test
    void rerunTests_withNoCurrentProposal_throwsIllegalState() {
        UUID id = startSession();
        assertThatThrownBy(() -> service.rerunTests(id, "<assembly/>"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No current proposal");
    }

    @Test
    void rerunTests_withDirtyResults_returnsExhaustedPhase_butKeepsEditedProposal() throws Exception {
        UUID id = startSession();
        // First, populate session.currentProposal via a normal proposal turn.
        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
                .thenReturn(toolResult("generate_rule", proposalJson("r", 4), 100, 50));
        when(testRunner.run(anyString(), anyString(), anyString(), any()))
                .thenReturn(allPassing(4));
        service.turn(id, "rule");

        // Now rerun with edited XML; the new run fails.
        when(testRunner.run(anyString(), anyString(), eq("<assembly id=\"e\"/>"), any()))
                .thenReturn(allFailing(4));
        RuleGenTurnResponse r = service.rerunTests(id, "<assembly id=\"e\"/>");

        assertThat(r.phase()).isEqualTo("exhausted");
        assertThat(r.message()).contains("failing");
        // Edited proposal preserved with the new constraint XML so the user can iterate further.
        assertThat(r.proposal().constraintXml()).isEqualTo("<assembly id=\"e\"/>");
        // No new Anthropic call — rerun uses the stored proposal.
        verify(anthropic, atLeast(1)).sendWithTools(anyString(), any(), any());
    }

    @Test
    void rerunTests_withCleanResults_returnsProposalPhase() throws Exception {
        UUID id = startSession();
        when(anthropic.sendWithTools(anyString(), any(AnthropicCall.class), any()))
                .thenReturn(toolResult("generate_rule", proposalJson("r", 4), 100, 50));
        when(testRunner.run(anyString(), anyString(), anyString(), any()))
                .thenReturn(allPassing(4));
        service.turn(id, "rule");

        when(testRunner.run(anyString(), anyString(), eq("<assembly id=\"e\"/>"), any()))
                .thenReturn(allPassing(4));
        RuleGenTurnResponse r = service.rerunTests(id, "<assembly id=\"e\"/>");

        assertThat(r.phase()).isEqualTo("proposal");
        assertThat(r.message()).isNull();
        assertThat(r.proposal().constraintXml()).isEqualTo("<assembly id=\"e\"/>");
    }

    @Test
    void turn_withUnknownSession_throwsExpiredException() {
        // No session created, no anthropic mock needed: the service should never
        // reach the Claude client.
        assertThatThrownBy(() -> service.turn(UUID.randomUUID(), "msg"))
                .isInstanceOf(RuleGenSessionExpiredException.class);
        verify(anthropic, never()).sendWithTools(anyString(), any(), any());
    }

    // ---- helpers ----

    private UUID startSession() {
        when(settings.getDefaultModel(1L)).thenReturn("claude-opus-4-7");
        when(settings.requireApiKey(1L)).thenReturn("api-key-secret");
        return service.start(1L, 1L, "catalog");
    }

    private static AnthropicToolUseResult toolResult(String tool,
                                                     com.fasterxml.jackson.databind.JsonNode input,
                                                     int in, int out) {
        return new AnthropicToolUseResult(tool, input, in, out);
    }

    private static com.fasterxml.jackson.databind.JsonNode proposalJson(String name, int caseCount) throws Exception {
        StringBuilder cases = new StringBuilder("[");
        for (int i = 0; i < caseCount; i++) {
            if (i > 0) cases.append(",");
            cases.append("{\"description\":\"case-").append(i).append("\",")
                 .append("\"fragmentJson\":\"{}\",")
                 .append("\"expected\":\"pass\"}");
        }
        cases.append("]");
        String json = "{"
                + "\"name\":\"" + name + "\","
                + "\"description\":\"d\","
                + "\"severity\":\"error\","
                + "\"fieldPath\":\"\","
                + "\"constraintXml\":\"<assembly target=\\\"x\\\"/>\","
                + "\"testCases\":" + cases
                + "}";
        return OM.readTree(json);
    }

    private static List<TestResult> allPassing(int n) {
        List<TestResult> out = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new TestResult(i, "case-" + i, "pass", "pass", true, null));
        }
        return out;
    }

    private static List<TestResult> allFailing(int n) {
        List<TestResult> out = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new TestResult(i, "case-" + i, "pass", "fail", false, "wrong"));
        }
        return out;
    }
}
