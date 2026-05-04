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
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CatalogWizard implements Wizard {

    private static final Logger log = LoggerFactory.getLogger(CatalogWizard.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TOKEN_BUDGET_IN = 250_000;
    private static final int TOKEN_BUDGET_OUT = 50_000;

    private final AnthropicClient client;
    private final AiSessionEventStream stream;
    private final KnowledgeLoader knowledge;
    private final DocumentNormalizer normalizer;
    private final CatalogPromptBuilder prompts;
    private final CatalogChunkingStrategy chunking;

    public CatalogWizard(AnthropicClient client, AiSessionEventStream stream,
                         KnowledgeLoader knowledge, DocumentNormalizer normalizer,
                         CatalogPromptBuilder prompts, CatalogChunkingStrategy chunking) {
        this.client = client;
        this.stream = stream;
        this.knowledge = knowledge;
        this.normalizer = normalizer;
        this.prompts = prompts;
        this.chunking = chunking;
    }

    @Override
    public WizardKind kind() { return WizardKind.CATALOG; }

    @Override
    public WizardOutcome run(WizardContext ctx) {
        try {
            stream.publish(ctx.sessionId(), SessionEvent.progress("Reading source document via Apache Tika"));

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

            String system = knowledge.systemFor(WizardKind.CATALOG);
            int tokensIn = 0, tokensOut = 0;

            // Pass 1 — outline
            stream.publish(ctx.sessionId(), SessionEvent.progress("Identifying control families and groupings"));
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
            String title = outline.path("title").asText("Untitled Catalog");
            String version = outline.path("version").asText("unspecified");
            String publisher = outline.path("publisher").asText("unspecified");
            List<CatalogChunkingStrategy.Family> families = new ArrayList<>();
            for (JsonNode f : outline.path("families")) {
                List<String> ids = new ArrayList<>();
                for (JsonNode cid : f.path("controlIds")) ids.add(cid.asText());
                families.add(new CatalogChunkingStrategy.Family(
                        f.path("id").asText(), f.path("title").asText(), ids));
            }
            int totalControls = families.stream().mapToInt(f -> f.controlIds().size()).sum();
            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Outline complete — " + families.size() + " families, " + totalControls
                    + " controls (OSCAL Catalog Layer 1.1.2)"));

            // Pass 2 — per family
            List<JsonNode> producedGroups = new ArrayList<>();
            int chunkIndex = 0;
            int familyIndex = 0;
            for (List<CatalogChunkingStrategy.Family> chunk : chunking.chunk(families)) {
                for (CatalogChunkingStrategy.Family fam : chunk) {
                    familyIndex++;
                    stream.publish(ctx.sessionId(), SessionEvent.progress(
                            "Drafting " + fam.id().toUpperCase() + " family — " + fam.title()
                            + " (" + familyIndex + " of " + families.size() + ", "
                            + fam.controlIds().size() + " controls)"));
                    AnthropicResult famRes = client.send(ctx.apiKey(), AnthropicCall.builder()
                            .model(ctx.model())
                            .systemPrompt(system)
                            .userMessage(prompts.familyPrompt(fam.id(), fam.title(), fam.controlIds())
                                    + "\n\n---\n\n" + docText)
                            .maxTokens(8000)
                            .build());
                    tokensIn += famRes.tokensIn();
                    tokensOut += famRes.tokensOut();
                    producedGroups.add(MAPPER.readTree(extractJson(famRes.text())));
                    if (tokensIn > TOKEN_BUDGET_IN || tokensOut > TOKEN_BUDGET_OUT) {
                        return WizardOutcome.failed("token_budget",
                                "Token budget exceeded after " + (chunkIndex + 1) + " families");
                    }
                }
                chunkIndex++;
            }

            // Pass 3 — merge
            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Assembling OSCAL Catalog metadata and merging " + producedGroups.size() + " groups"));
            Map<String, Object> catalog = new HashMap<>();
            Map<String, Object> meta = new HashMap<>();
            meta.put("title", title);
            meta.put("last-modified", java.time.Instant.now().toString());
            meta.put("version", version);
            meta.put("oscal-version", "1.1.2");
            meta.put("parties", List.of(Map.of(
                    "uuid", java.util.UUID.randomUUID().toString(),
                    "type", "organization",
                    "name", publisher)));
            Map<String, Object> root = new HashMap<>();
            root.put("uuid", java.util.UUID.randomUUID().toString());
            root.put("metadata", meta);
            root.put("groups", producedGroups);
            catalog.put("catalog", root);
            String catalogJson = MAPPER.writeValueAsString(catalog);

            stream.publish(ctx.sessionId(), SessionEvent.complete(catalogJson));
            return WizardOutcome.ok(tokensIn, tokensOut);

        } catch (IllegalArgumentException e) {
            stream.publish(ctx.sessionId(), SessionEvent.error("auth_or_input", e.getMessage()));
            return WizardOutcome.failed("auth_or_input", e.getMessage());
        } catch (Exception e) {
            log.error("Catalog wizard failed", e);
            stream.publish(ctx.sessionId(), SessionEvent.error("model_error", e.getMessage()));
            return WizardOutcome.failed("model_error", e.getMessage());
        }
    }

    /**
     * Strips markdown code fences (```json ... ``` or ``` ... ```) that Claude sometimes
     * wraps around JSON output even when instructed otherwise.
     */
    private static String extractJson(String text) {
        if (text == null) return "{}";
        String t = text.trim();
        // Strip ```json ... ``` or ``` ... ``` fences if present
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        // Strip prose preludes/postludes by carving out the first { ... last }.
        // Claude often replies with text like "I'll draft the AC family. {...}" — find
        // the first '{' and the matching last '}' and use just that substring.
        int firstBrace = t.indexOf('{');
        int lastBrace = t.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            t = t.substring(firstBrace, lastBrace + 1);
        }
        return t.trim();
    }
}
