package gov.nist.oscal.tools.api.service.ai.rulegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.airulegen.RuleGenTurnResponse;
import gov.nist.oscal.tools.api.model.airulegen.RuleProposal;
import gov.nist.oscal.tools.api.model.airulegen.TestCase;
import gov.nist.oscal.tools.api.model.airulegen.TestResult;
import gov.nist.oscal.tools.api.service.ai.AiSettingsService;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicToolUseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the rule-gen wizard conversation: builds the per-turn Claude
 * call from session transcript + tool definitions, branches on which tool
 * Claude invoked, and auto-iterates up to {@link #MAX_REVISIONS} revisions
 * when the synthetic test matrix has any failing rows.
 */
@Service
public class AiRuleGenService {

    private static final Logger log = LoggerFactory.getLogger(AiRuleGenService.class);
    private static final int MAX_REVISIONS = 3;
    private static final int MAX_TOKENS = 4096;

    private final AnthropicClient anthropic;
    private final AiSettingsService aiSettings;
    private final AiRuleGenSessionStore store;
    private final RuleGenPrompts prompts;
    private final RuleGenTestRunner testRunner;
    private final ObjectMapper om = new ObjectMapper();

    public AiRuleGenService(AnthropicClient anthropic,
                            AiSettingsService aiSettings,
                            AiRuleGenSessionStore store,
                            RuleGenPrompts prompts,
                            RuleGenTestRunner testRunner) {
        this.anthropic = anthropic;
        this.aiSettings = aiSettings;
        this.store = store;
        this.prompts = prompts;
        this.testRunner = testRunner;
    }

    public UUID start(long organizationId, long userId, String modelType) {
        String model = aiSettings.getDefaultModel(organizationId);
        return store.create(organizationId, userId, modelType, model);
    }

    public RuleGenTurnResponse turn(UUID sessionId, String userMessage) {
        AiRuleGenSession session = store.get(sessionId);
        store.appendUser(sessionId, userMessage);

        AnthropicToolUseResult res = callClaude(session, userMessage);
        session.addTokens(res.tokensIn(), res.tokensOut());

        return switch (res.toolName()) {
            case "ask_clarifying_question" -> handleClarify(session, res);
            case "generate_rule", "revise_rule" -> handleProposal(session, res, 1);
            default -> throw new IllegalStateException("Unexpected tool: " + res.toolName());
        };
    }

    public RuleGenTurnResponse rerunTests(UUID sessionId, String editedConstraintXml) {
        AiRuleGenSession session = store.get(sessionId);
        RuleProposal current = session.currentProposal();
        if (current == null) {
            throw new IllegalStateException("No current proposal to edit");
        }
        RuleProposal edited = new RuleProposal(
            current.name(), current.description(), current.severity(),
            current.fieldPath(), editedConstraintXml, current.testCases());
        List<TestResult> results = testRunner.run(
            "rule-" + sessionId, session.modelType(), editedConstraintXml, current.testCases());
        session.setCurrentProposal(edited);
        boolean clean = results.stream().allMatch(TestResult::passed);
        return new RuleGenTurnResponse(
            clean ? "proposal" : "exhausted",
            null, edited, results, edited,
            clean ? null : "Edited constraint produced failing tests.",
            1, session.tokensIn(), session.tokensOut());
    }

    public AiRuleGenSession session(UUID id) { return store.get(id); }
    public void close(UUID id) { store.close(id); }

    // ---- internals ----

    private AnthropicToolUseResult callClaude(AiRuleGenSession session, String userMessage) {
        String apiKey = aiSettings.requireApiKey(session.organizationId());
        AnthropicCall call = buildCall(session, userMessage);
        log.info("rule-gen session={} sending turn ({} chars input)",
                session.id(), userMessage.length());
        AnthropicToolUseResult res = anthropic.sendWithTools(apiKey, call, msg -> log.info(
            "rule-gen session={} retry: {}", session.id(), msg));
        log.info("rule-gen session={} got tool={} ({} in / {} out tokens)",
                session.id(), res.toolName(), res.tokensIn(), res.tokensOut());
        return res;
    }

    private AnthropicCall buildCall(AiRuleGenSession session, String userMessage) {
        // History as a single recap blob keeps the prompt-cache breakpoint at
        // the system prompt level and avoids fiddling with multi-turn assistant
        // messages in this synchronous flow.
        StringBuilder ctx = new StringBuilder();
        for (var entry : session.transcript()) {
            ctx.append(entry.role().toUpperCase()).append(": ").append(entry.text()).append("\n\n");
        }
        ctx.append("USER: ").append(userMessage);
        return AnthropicCall.builder()
                .model(session.anthropicModel())
                .maxTokens(MAX_TOKENS)
                .systemPrompt(prompts.systemPromptFor(session.modelType()))
                .userMessage(ctx.toString())
                .tools(prompts.toolDefinitions())
                .toolChoice("any")
                .build();
    }

    private RuleGenTurnResponse handleClarify(AiRuleGenSession session, AnthropicToolUseResult res) {
        String q = res.input().path("question").asText("");
        store.appendAssistant(session.id(), q);
        return new RuleGenTurnResponse("clarify", q, null, null, null, null, 1,
                session.tokensIn(), session.tokensOut());
    }

    private RuleGenTurnResponse handleProposal(AiRuleGenSession session,
                                               AnthropicToolUseResult res,
                                               int iteration) {
        RuleProposal proposal = parseProposal(res.input());
        store.appendAssistant(session.id(),
            "(proposed rule \"" + proposal.name() + "\" with " + proposal.testCases().size() + " test cases)");
        session.setCurrentProposal(proposal);

        List<TestResult> results = testRunner.run(
            "rule-" + session.id(), session.modelType(),
            proposal.constraintXml(), proposal.testCases());

        boolean clean = results.stream().allMatch(TestResult::passed);
        if (clean) {
            return new RuleGenTurnResponse("proposal", null, proposal, results, proposal,
                    null, iteration, session.tokensIn(), session.tokensOut());
        }

        if (iteration >= 1 + MAX_REVISIONS) {
            return new RuleGenTurnResponse(
                "exhausted", null, null, results, proposal,
                buildExhaustedMessage(results),
                iteration, session.tokensIn(), session.tokensOut());
        }

        // Auto-revise — feed failures back to Claude.
        String reviseMsg = "Your last proposal had failing tests:\n"
            + formatFailures(results)
            + "\nFix the constraint and regenerate test cases. Call revise_rule.";
        AnthropicToolUseResult revised = anthropic.sendWithTools(
            aiSettings.requireApiKey(session.organizationId()),
            buildCall(session, reviseMsg),
            m -> log.info("rule-gen session={} retry: {}", session.id(), m));
        session.addTokens(revised.tokensIn(), revised.tokensOut());

        return switch (revised.toolName()) {
            case "generate_rule", "revise_rule" -> handleProposal(session, revised, iteration + 1);
            case "ask_clarifying_question" -> handleClarify(session, revised);
            default -> throw new IllegalStateException("Unexpected tool: " + revised.toolName());
        };
    }

    private RuleProposal parseProposal(JsonNode node) {
        try {
            String constraintXml = node.path("constraintXml").asText();
            List<TestCase> cases = new ArrayList<>();
            JsonNode tcs = node.path("testCases");
            if (tcs.isArray()) {
                for (Iterator<JsonNode> it = tcs.elements(); it.hasNext();) {
                    JsonNode tc = it.next();
                    cases.add(new TestCase(
                        tc.path("description").asText(),
                        tc.path("fragmentJson").asText(),
                        tc.path("expected").asText()));
                }
            }
            return new RuleProposal(
                node.path("name").asText(),
                node.path("description").asText(),
                node.path("severity").asText("error"),
                node.path("fieldPath").asText(""),
                constraintXml,
                cases);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse rule proposal: " + e.getMessage(), e);
        }
    }

    private String formatFailures(List<TestResult> results) {
        StringBuilder sb = new StringBuilder();
        for (TestResult r : results) {
            if (!r.passed()) {
                sb.append("  - case \"").append(r.description()).append("\": ")
                  .append("expected ").append(r.expected())
                  .append(", got ").append(r.actual());
                if (r.violationMessage() != null) {
                    sb.append(" (").append(r.violationMessage()).append(")");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String buildExhaustedMessage(List<TestResult> results) {
        long failed = results.stream().filter(r -> !r.passed()).count();
        return "I couldn't reach a working rule after the maximum number of revisions. "
             + failed + " test case(s) still don't match expectations. "
             + "Please clarify your description, edit the constraint manually, or abandon.";
    }
}
