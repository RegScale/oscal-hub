package gov.nist.oscal.tools.api.service.ai.wizard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.DocumentNormalizer;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.NormalizedDoc;
import gov.nist.oscal.tools.api.service.ai.XccdfTrimmer;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ComponentDefWizard implements Wizard {

    private static final Logger log = LoggerFactory.getLogger(ComponentDefWizard.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TOKEN_BUDGET_IN = 250_000;
    private static final int TOKEN_BUDGET_OUT = 50_000;

    private final AnthropicClient client;
    private final AiSessionEventStream stream;
    private final KnowledgeLoader knowledge;
    private final DocumentNormalizer normalizer;
    private final ComponentDefPromptBuilder prompts;
    private final ComponentDefChunkingStrategy chunking;
    private final XccdfTrimmer xccdfTrimmer;

    public ComponentDefWizard(AnthropicClient client, AiSessionEventStream stream,
                              KnowledgeLoader knowledge, DocumentNormalizer normalizer,
                              ComponentDefPromptBuilder prompts,
                              ComponentDefChunkingStrategy chunking,
                              XccdfTrimmer xccdfTrimmer) {
        this.client = client;
        this.stream = stream;
        this.knowledge = knowledge;
        this.normalizer = normalizer;
        this.prompts = prompts;
        this.chunking = chunking;
        this.xccdfTrimmer = xccdfTrimmer;
    }

    @Override
    public WizardKind kind() { return WizardKind.COMPONENT_DEF; }

    @Override
    public WizardOutcome run(WizardContext ctx) {
        try {
            stream.publish(ctx.sessionId(), SessionEvent.progress("Analyzing configuration guide…"));

            String docText;
            byte[] pdfBytes = null;
            String filename = ctx.inputFilename() == null ? "input" : ctx.inputFilename();
            if (ctx.inputBytes() != null && filename.toLowerCase().endsWith(".pdf")) {
                pdfBytes = ctx.inputBytes();
                docText = "(PDF attached as document block)";
            } else if (ctx.inputBytes() != null) {
                NormalizedDoc d = normalizer.normalize(ctx.inputBytes(), filename);
                docText = d.plainText();
            } else {
                docText = ctx.input() == null ? "" : ctx.input();
            }

            // Catch the paste-XCCDF-as-text path. The file-upload path is
            // already trimmed inside DocumentNormalizer; for pasted text the
            // trimmer is a no-op on anything that isn't XCCDF.
            if (xccdfTrimmer.looksLikeXccdf(docText)) {
                docText = xccdfTrimmer.trim(docText);
            }

            String system = knowledge.systemFor(WizardKind.COMPONENT_DEF);
            int tokensIn = 0, tokensOut = 0;

            // Pass 1 — outline
            AnthropicCall.Builder b = AnthropicCall.builder()
                    .model(ctx.model())
                    .systemPrompt(system)
                    .userMessage(prompts.outlinePrompt() + "\n\n---\n\n" + docText)
                    .maxTokens(8000);
            if (pdfBytes != null) b.addPdf(pdfBytes);
            AnthropicResult outlineRes = client.send(ctx.apiKey(), b.build());
            tokensIn += outlineRes.tokensIn();
            tokensOut += outlineRes.tokensOut();

            JsonNode outline = MAPPER.readTree(extractJson(outlineRes.text()));
            String productTitle      = outline.path("productTitle").asText("Untitled Component");
            String productDescription = outline.path("productDescription").asText("");
            String componentType     = outline.path("componentType").asText("software");
            String version           = outline.path("version").asText("unspecified");
            String publisher         = outline.path("publisher").asText("unspecified");
            String catalogSource     = outline.path("catalogSource").asText(
                    "https://raw.githubusercontent.com/usnistgov/oscal-content/main/nist.gov/SP800-53/rev5/json/NIST_SP-800-53_rev5_catalog.json");

            List<String> controlIds = new ArrayList<>();
            for (JsonNode cid : outline.path("controlIds")) controlIds.add(cid.asText());

            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Outline complete — " + productTitle + ", " + controlIds.size()
                    + " controls (OSCAL Component-definition Layer 1.1.2)"));

            // Pass 2 — per chunk of control IDs
            List<JsonNode> producedRequirements = new ArrayList<>();
            List<List<String>> chunks = chunking.chunk(controlIds);
            int chunkIndex = 0;
            for (List<String> chunk : chunks) {
                chunkIndex++;
                stream.publish(ctx.sessionId(), SessionEvent.progress(
                        "Drafting implementation statements (" + chunkIndex + " of " + chunks.size() + ")…"));
                AnthropicResult chunkRes = client.send(ctx.apiKey(), AnthropicCall.builder()
                        .model(ctx.model())
                        .systemPrompt(system)
                        .userMessage(prompts.componentPrompt(productTitle, chunk)
                                + "\n\n---\n\n" + docText)
                        .maxTokens(8000)
                        .build());
                tokensIn += chunkRes.tokensIn();
                tokensOut += chunkRes.tokensOut();

                // Parse the returned array and accumulate individual requirement objects
                JsonNode arr = MAPPER.readTree(extractJsonArray(chunkRes.text()));
                if (arr.isArray()) {
                    for (JsonNode req : arr) producedRequirements.add(req);
                }

                if (tokensIn > TOKEN_BUDGET_IN || tokensOut > TOKEN_BUDGET_OUT) {
                    return WizardOutcome.failed("token_budget",
                            "Token budget exceeded after chunk " + chunkIndex);
                }
            }

            // Pass 3 — build merged OSCAL component-definition in Java (no LLM call)
            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Assembling OSCAL Component-definition and merging "
                    + producedRequirements.size() + " implemented-requirements"));

            Map<String, Object> meta = new HashMap<>();
            meta.put("title", productTitle);
            meta.put("last-modified", java.time.Instant.now().toString());
            meta.put("version", version);
            meta.put("oscal-version", "1.1.2");
            meta.put("parties", List.of(Map.of(
                    "uuid", UUID.randomUUID().toString(),
                    "type", "organization",
                    "name", publisher)));

            Map<String, Object> controlImpl = new HashMap<>();
            controlImpl.put("uuid", UUID.randomUUID().toString());
            controlImpl.put("source", catalogSource);
            controlImpl.put("description", "Control implementations derived from the source document.");
            controlImpl.put("implemented-requirements", producedRequirements);

            Map<String, Object> component = new HashMap<>();
            component.put("uuid", UUID.randomUUID().toString());
            component.put("type", componentType);
            component.put("title", productTitle);
            component.put("description", productDescription);
            component.put("control-implementations", List.of(controlImpl));

            Map<String, Object> root = new HashMap<>();
            root.put("uuid", UUID.randomUUID().toString());
            root.put("metadata", meta);
            root.put("components", List.of(component));

            Map<String, Object> doc = new HashMap<>();
            doc.put("component-definition", root);

            String resultJson = MAPPER.writeValueAsString(doc);
            stream.publish(ctx.sessionId(), SessionEvent.complete(resultJson));
            return WizardOutcome.ok(tokensIn, tokensOut);

        } catch (IllegalArgumentException e) {
            stream.publish(ctx.sessionId(), SessionEvent.error("auth_or_input", e.getMessage()));
            return WizardOutcome.failed("auth_or_input", e.getMessage());
        } catch (Exception e) {
            log.error("ComponentDef wizard failed", e);
            stream.publish(ctx.sessionId(), SessionEvent.error("model_error", e.getMessage()));
            return WizardOutcome.failed("model_error", e.getMessage());
        }
    }

    /**
     * Strips markdown code fences and extracts the outermost JSON object.
     */
    private static String extractJson(String text) {
        if (text == null) return "{}";
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        int firstBrace = t.indexOf('{');
        int lastBrace = t.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            t = t.substring(firstBrace, lastBrace + 1);
        }
        return t.trim();
    }

    /**
     * Strips markdown code fences and extracts the outermost JSON array.
     */
    private static String extractJsonArray(String text) {
        if (text == null) return "[]";
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        int firstBracket = t.indexOf('[');
        int lastBracket = t.lastIndexOf(']');
        if (firstBracket >= 0 && lastBracket > firstBracket) {
            t = t.substring(firstBracket, lastBracket + 1);
        }
        return t.trim();
    }
}
