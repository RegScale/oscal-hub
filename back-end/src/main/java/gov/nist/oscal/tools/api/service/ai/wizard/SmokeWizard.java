package gov.nist.oscal.tools.api.service.ai.wizard;

import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import org.springframework.stereotype.Component;

@Component
public class SmokeWizard implements Wizard {

    private final AnthropicClient client;
    private final AiSessionEventStream stream;
    private final KnowledgeLoader knowledge;

    public SmokeWizard(AnthropicClient client, AiSessionEventStream stream, KnowledgeLoader knowledge) {
        this.client = client;
        this.stream = stream;
        this.knowledge = knowledge;
    }

    @Override public WizardKind kind() { return WizardKind.SMOKE; }

    @Override
    public WizardOutcome run(WizardContext ctx) {
        stream.publish(ctx.sessionId(), SessionEvent.progress("Calling Claude…"));
        try {
            AnthropicCall call = AnthropicCall.builder()
                    .model(ctx.model())
                    .systemPrompt(knowledge.systemFor(WizardKind.SMOKE))
                    .userMessage(ctx.input() == null ? "Say hello back." : ctx.input())
                    .maxTokens(256)
                    .build();
            AnthropicResult result = client.send(ctx.apiKey(), call);
            stream.publish(ctx.sessionId(), SessionEvent.chunk(result.text()));
            stream.publish(ctx.sessionId(), SessionEvent.complete(
                    "{\"reply\":" + JsonStrings.quote(result.text()) + "}"));
            return WizardOutcome.ok(result.tokensIn(), result.tokensOut());
        } catch (IllegalArgumentException e) {
            stream.publish(ctx.sessionId(), SessionEvent.error("auth_failed", e.getMessage()));
            return WizardOutcome.failed("auth_failed", e.getMessage());
        } catch (Exception e) {
            stream.publish(ctx.sessionId(), SessionEvent.error("model_error", e.getMessage()));
            return WizardOutcome.failed("model_error", e.getMessage());
        }
    }
}
