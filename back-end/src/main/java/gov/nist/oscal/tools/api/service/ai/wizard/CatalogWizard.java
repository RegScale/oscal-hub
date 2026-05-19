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
    private static final int TOKEN_BUDGET_IN = 5_000_000;
    private static final int TOKEN_BUDGET_OUT = 200_000;

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
            AnthropicResult outlineRes = client.send(ctx.apiKey(), b.build(),
                    msg -> stream.publish(ctx.sessionId(), SessionEvent.progress(msg)));
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

            // Pass 2 — per family. Wrap each family's Anthropic call in its own
            // try/catch: a single family rejection (the most common cause is
            // Anthropic's safety filter blocking specific source language with
            // an HTTP 400 "Output blocked by content filtering policy") should
            // NOT discard the work for the families that already succeeded.
            // We collect the skipped families and surface them in metadata
            // remarks + a progress event so the user knows the partial result
            // is incomplete and which sections to fill in manually.
            List<JsonNode> producedGroups = new ArrayList<>();
            List<SkippedFamily> skipped = new ArrayList<>();
            int chunkIndex = 0;
            int familyIndex = 0;
            for (List<CatalogChunkingStrategy.Family> chunk : chunking.chunk(families)) {
                for (CatalogChunkingStrategy.Family fam : chunk) {
                    familyIndex++;
                    stream.publish(ctx.sessionId(), SessionEvent.progress(
                            "Drafting " + fam.id().toUpperCase() + " family — " + fam.title()
                            + " (" + familyIndex + " of " + families.size() + ", "
                            + fam.controlIds().size() + " controls)"));
                    AnthropicResult famRes;
                    try {
                        famRes = client.send(ctx.apiKey(), AnthropicCall.builder()
                                .model(ctx.model())
                                .systemPrompt(system)
                                .userMessage(prompts.familyPrompt(fam.id(), fam.title(), fam.controlIds())
                                        + "\n\n---\n\n" + docText)
                                .maxTokens(8000)
                                .build(),
                                msg -> stream.publish(ctx.sessionId(), SessionEvent.progress(msg)));
                    } catch (IllegalArgumentException e) {
                        // Missing API key / auth — fatal, propagate.
                        throw e;
                    } catch (RuntimeException e) {
                        // Content-filter rejections (the most common reason for a
                        // mid-run failure on security-heavy families) get one
                        // automatic retry with a stricter style preface. Anything
                        // else — rate limits, transient model errors, malformed
                        // requests — is skipped without retry; retrying those
                        // wouldn't change the outcome and would just waste tokens.
                        if (isContentFilterError(e)) {
                            String reason = summarizeAnthropicError(e);
                            log.info("Catalog wizard sessionId={} family {} hit content filter — retrying with safer prompt",
                                    ctx.sessionId(), fam.id());
                            stream.publish(ctx.sessionId(), SessionEvent.progress(
                                    "Retrying " + fam.id().toUpperCase() + " with safer wording "
                                    + "(content filter blocked first attempt)"));
                            try {
                                famRes = client.send(ctx.apiKey(), AnthropicCall.builder()
                                        .model(ctx.model())
                                        .systemPrompt(system)
                                        .userMessage(prompts.familyPrompt(fam.id(), fam.title(), fam.controlIds(), true)
                                                + "\n\n---\n\n" + docText)
                                        .maxTokens(8000)
                                        .build(),
                                        msg -> stream.publish(ctx.sessionId(), SessionEvent.progress(msg)));
                            } catch (IllegalArgumentException retryAuth) {
                                throw retryAuth;
                            } catch (RuntimeException retryErr) {
                                String retryReason = summarizeAnthropicError(retryErr);
                                log.warn("Catalog wizard sessionId={} family {} failed safe retry: {}",
                                        ctx.sessionId(), fam.id(), retryReason);
                                stream.publish(ctx.sessionId(), SessionEvent.progress(
                                        "Skipping " + fam.id().toUpperCase() + " — " + reason
                                        + " (safe retry also " + retryReason + "). "
                                        + "Continuing with the remaining families."));
                                skipped.add(new SkippedFamily(fam.id(), fam.title(),
                                        reason + "; safe retry: " + retryReason));
                                continue;
                            }
                        } else {
                            String reason = summarizeAnthropicError(e);
                            log.warn("Catalog wizard sessionId={} family {} failed: {}",
                                    ctx.sessionId(), fam.id(), reason);
                            stream.publish(ctx.sessionId(), SessionEvent.progress(
                                    "Skipping " + fam.id().toUpperCase() + " — " + reason
                                    + ". Continuing with the remaining families."));
                            skipped.add(new SkippedFamily(fam.id(), fam.title(), reason));
                            continue;
                        }
                    }
                    tokensIn += famRes.tokensIn();
                    tokensOut += famRes.tokensOut();
                    producedGroups.add(MAPPER.readTree(extractJson(famRes.text())));
                    log.info("Catalog wizard sessionId={} family {} done tokensIn={} tokensOut={} cumIn={} cumOut={}",
                            ctx.sessionId(), fam.id(), famRes.tokensIn(), famRes.tokensOut(),
                            tokensIn, tokensOut);
                    if (tokensIn > TOKEN_BUDGET_IN || tokensOut > TOKEN_BUDGET_OUT) {
                        String msg = "Token budget exceeded after " + (chunkIndex + 1) + " families"
                                + " (in=" + tokensIn + ", out=" + tokensOut + ")";
                        log.warn("Catalog wizard sessionId={} {}", ctx.sessionId(), msg);
                        stream.publish(ctx.sessionId(), SessionEvent.error("token_budget", msg));
                        return WizardOutcome.failed("token_budget", msg);
                    }
                }
                chunkIndex++;
            }

            if (producedGroups.isEmpty()) {
                // All families failed — there's nothing to ship.
                String firstReason = skipped.isEmpty()
                        ? "no families produced output"
                        : skipped.getFirst().reason();
                String msg = "All " + families.size() + " families were rejected by the model. "
                        + "First reason: " + firstReason;
                stream.publish(ctx.sessionId(), SessionEvent.error("all_families_failed", msg));
                return WizardOutcome.failed("all_families_failed", msg);
            }

            // Pass 3 — merge
            String mergeMsg = "Assembling OSCAL Catalog metadata and merging "
                    + producedGroups.size() + " groups";
            if (!skipped.isEmpty()) {
                mergeMsg += " (skipped " + skipped.size() + ": "
                        + String.join(", ", skipped.stream().map(SkippedFamily::id).toList())
                        + ")";
            }
            stream.publish(ctx.sessionId(), SessionEvent.progress(mergeMsg));
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
            if (!skipped.isEmpty()) {
                // OSCAL `remarks` is markdown — leave a breadcrumb on the
                // catalog so reviewers can see at a glance which families need
                // manual completion and why the AI skipped them.
                StringBuilder remarks = new StringBuilder();
                remarks.append("AI wizard skipped ").append(skipped.size())
                        .append(" of ").append(families.size())
                        .append(" families. Complete these manually:\n\n");
                for (SkippedFamily s : skipped) {
                    remarks.append("- **").append(s.id().toUpperCase()).append("** ")
                            .append(s.title()).append(" — ").append(s.reason()).append("\n");
                }
                meta.put("remarks", remarks.toString());
            }
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

    /** Tracks per-family failures so the partial result can call them out. */
    private record SkippedFamily(String id, String title, String reason) {}

    /**
     * Returns true if the throwable chain looks like Anthropic's output safety
     * filter rejection (HTTP 400 with "content filtering" in the message). Used
     * to decide whether a per-family rejection is worth retrying with a stricter
     * style preface.
     */
    static boolean isContentFilterError(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String s = t.getMessage();
            if (s != null && s.toLowerCase().contains("content filtering")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Boils an Anthropic SDK error down to a short user-facing reason. The SDK
     * wraps the JSON payload in its own message; we surface the human-readable
     * bits (content-filter / rate-limit / overload / generic 4xx) so users
     * understand why a family was skipped without us leaking request IDs.
     */
    private static String summarizeAnthropicError(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String s = t.getMessage();
            if (s == null) continue;
            String lower = s.toLowerCase();
            if (lower.contains("content filtering")) {
                return "blocked by Anthropic's content-filtering policy";
            }
            if (lower.contains("rate_limit") || lower.contains("rate limit")) {
                return "Anthropic rate limit reached";
            }
            if (lower.contains("overloaded")) {
                return "Anthropic temporarily overloaded";
            }
            if (lower.contains("invalid_request_error")) {
                return "rejected by Anthropic as invalid_request_error";
            }
            if (lower.contains("authentication") || lower.contains("invalid_api_key")) {
                return "authentication error";
            }
        }
        String msg = e.getMessage();
        if (msg == null) return "model error";
        // Trim to keep the per-family progress event readable.
        return msg.length() > 200 ? msg.substring(0, 200) + "…" : msg;
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
