package gov.nist.oscal.tools.api.service.ai.wizard;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.service.ai.AnthropicCall;
import gov.nist.oscal.tools.api.service.ai.AnthropicClient;
import gov.nist.oscal.tools.api.service.ai.AnthropicResult;
import gov.nist.oscal.tools.api.service.ai.DocumentNormalizer;
import gov.nist.oscal.tools.api.service.ai.KnowledgeLoader;
import gov.nist.oscal.tools.api.service.ai.NormalizedDoc;
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
public class PoamWizard implements Wizard {

    private static final Logger log = LoggerFactory.getLogger(PoamWizard.class);
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();
    private static final int TOKEN_BUDGET_IN = 5_000_000;
    private static final int TOKEN_BUDGET_OUT = 200_000;

    private final AnthropicClient client;
    private final AiSessionEventStream stream;
    private final KnowledgeLoader knowledge;
    private final DocumentNormalizer normalizer;
    private final PoamPromptBuilder prompts;
    private final PoamChunkingStrategy chunking;

    public PoamWizard(AnthropicClient client, AiSessionEventStream stream,
                      KnowledgeLoader knowledge, DocumentNormalizer normalizer,
                      PoamPromptBuilder prompts, PoamChunkingStrategy chunking) {
        this.client = client;
        this.stream = stream;
        this.knowledge = knowledge;
        this.normalizer = normalizer;
        this.prompts = prompts;
        this.chunking = chunking;
    }

    @Override
    public WizardKind kind() { return WizardKind.POAM; }

    @Override
    public WizardOutcome run(WizardContext ctx) {
        try {
            stream.publish(ctx.sessionId(), SessionEvent.progress("Reading source document…"));

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

            String system = knowledge.systemFor(WizardKind.POAM);
            int tokensIn = 0, tokensOut = 0;

            // Pass 1 — outline
            stream.publish(ctx.sessionId(), SessionEvent.progress("Identifying POA&M items…"));
            AnthropicCall.Builder b = AnthropicCall.builder()
                    .model(ctx.model())
                    .systemPrompt(system)
                    .userMessage(prompts.outlinePrompt() + "\n\n---\n\n" + docText)
                    .maxTokens(8000);
            if (pdfBytes != null) b.addPdf(pdfBytes);
            AnthropicResult outlineRes = client.send(ctx.apiKey(), b.build(),
                    msg -> stream.publish(ctx.sessionId(), SessionEvent.progress(msg)));
            tokensIn += outlineRes.tokensIn();
            tokensOut += outlineRes.tokensOut();

            JsonNode outline = MAPPER.readTree(extractJson(outlineRes.text()));
            String title = outline.path("title").asText("Plan of Action and Milestones");
            String version = outline.path("version").asText("1.0");
            String publisher = outline.path("publisher").asText("unspecified");
            String systemTitle = outline.path("systemTitle").asText("");

            List<String> itemIds = new ArrayList<>();
            for (JsonNode iid : outline.path("itemIds")) itemIds.add(iid.asText());

            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Outline complete — " + itemIds.size() + " POA&M items"));

            // Pass 2 — per-chunk item details
            List<JsonNode> producedItems = new ArrayList<>();
            List<List<String>> chunks = chunking.chunk(itemIds);
            int chunkIndex = 0;
            for (List<String> chunk : chunks) {
                chunkIndex++;
                stream.publish(ctx.sessionId(), SessionEvent.progress(
                        "Drafting POA&M items (" + chunkIndex + " of " + chunks.size() + ")…"));
                AnthropicResult chunkRes = client.send(ctx.apiKey(), AnthropicCall.builder()
                        .model(ctx.model())
                        .systemPrompt(system)
                        .userMessage(prompts.itemsPrompt(systemTitle, chunk) + "\n\n---\n\n" + docText)
                        .maxTokens(8000)
                        .build(),
                        msg -> stream.publish(ctx.sessionId(), SessionEvent.progress(msg)));
                tokensIn += chunkRes.tokensIn();
                tokensOut += chunkRes.tokensOut();
                try {
                    JsonNode arr = MAPPER.readTree(extractJsonArray(chunkRes.text()));
                    if (arr.isArray()) {
                        for (JsonNode item : arr) producedItems.add(withFreshUuid(item));
                    }
                } catch (Exception parseEx) {
                    log.warn("POAM wizard sessionId={} chunk {} of {} failed to parse: {}",
                            ctx.sessionId(), chunkIndex, chunks.size(), parseEx.getMessage());
                    stream.publish(ctx.sessionId(), SessionEvent.progress(
                            "Chunk " + chunkIndex + " of " + chunks.size()
                            + " produced unparseable JSON — skipping " + chunk.size()
                            + " items. You can refine those entries in the editor."));
                }
                if (tokensIn > TOKEN_BUDGET_IN || tokensOut > TOKEN_BUDGET_OUT) {
                    String msg = "Token budget exceeded after chunk " + chunkIndex
                            + " (in=" + tokensIn + ", out=" + tokensOut + ")";
                    stream.publish(ctx.sessionId(), SessionEvent.error("token_budget", msg));
                    return WizardOutcome.failed("token_budget", msg);
                }
            }

            // Pass 3 — assemble
            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Assembling OSCAL Plan of Action and Milestones…"));

            Map<String, Object> meta = new HashMap<>();
            meta.put("title", title);
            meta.put("last-modified", java.time.Instant.now().toString());
            meta.put("version", version);
            meta.put("oscal-version", "1.1.2");
            meta.put("parties", List.of(Map.of(
                    "uuid", UUID.randomUUID().toString(),
                    "type", "organization",
                    "name", publisher)));

            Map<String, Object> root = new HashMap<>();
            root.put("uuid", UUID.randomUUID().toString());
            root.put("metadata", meta);
            root.put("import-ssp", Map.of("href", ""));
            root.put("system-id", Map.of(
                    "identifier-type", "https://ietf.org/rfc/rfc4122",
                    "id", UUID.randomUUID().toString()));
            root.put("observations", List.of());
            root.put("risks", List.of());
            root.put("findings", List.of());
            root.put("poam-items", producedItems);

            Map<String, Object> doc = new HashMap<>();
            doc.put("plan-of-action-and-milestones", root);

            String resultJson = MAPPER.writeValueAsString(doc);
            stream.publish(ctx.sessionId(), SessionEvent.complete(resultJson));
            return WizardOutcome.ok(tokensIn, tokensOut);

        } catch (IllegalArgumentException e) {
            stream.publish(ctx.sessionId(), SessionEvent.error("auth_or_input", e.getMessage()));
            return WizardOutcome.failed("auth_or_input", e.getMessage());
        } catch (Exception e) {
            log.error("POAM wizard failed", e);
            stream.publish(ctx.sessionId(), SessionEvent.error("model_error", e.getMessage()));
            return WizardOutcome.failed("model_error", e.getMessage());
        }
    }

    /**
     * Returns a copy of {@code node} with the {@code uuid} field replaced by a
     * fresh v4 UUID. Same untrust-the-LLM policy as SspWizard.
     */
    private static JsonNode withFreshUuid(JsonNode node) {
        if (node == null || !node.isObject()) return node;
        com.fasterxml.jackson.databind.node.ObjectNode obj = node.deepCopy();
        obj.put("uuid", UUID.randomUUID().toString());
        return obj;
    }

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
