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
import gov.nist.oscal.tools.api.service.ai.NormalizedDoc;
import gov.nist.oscal.tools.api.service.ai.ProfileSourceLoader;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class SspWizard implements Wizard {

    private static final Logger log = LoggerFactory.getLogger(SspWizard.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TOKEN_BUDGET_IN = 5_000_000;
    private static final int TOKEN_BUDGET_OUT = 200_000;

    private final AnthropicClient client;
    private final AiSessionEventStream stream;
    private final KnowledgeLoader knowledge;
    private final DocumentNormalizer normalizer;
    private final SspPromptBuilder prompts;
    private final SspChunkingStrategy chunking;
    private final ProfileSourceLoader profileLoader;
    private final ProfileControlIdExtractor extractor;
    private final UserRepository users;

    public SspWizard(AnthropicClient client, AiSessionEventStream stream,
                     KnowledgeLoader knowledge, DocumentNormalizer normalizer,
                     SspPromptBuilder prompts, SspChunkingStrategy chunking,
                     ProfileSourceLoader profileLoader,
                     ProfileControlIdExtractor extractor,
                     UserRepository users) {
        this.client = client;
        this.stream = stream;
        this.knowledge = knowledge;
        this.normalizer = normalizer;
        this.prompts = prompts;
        this.chunking = chunking;
        this.profileLoader = profileLoader;
        this.extractor = extractor;
        this.users = users;
    }

    @Override
    public WizardKind kind() { return WizardKind.SSP; }

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

            String system = knowledge.systemFor(WizardKind.SSP);
            int tokensIn = 0, tokensOut = 0;

            // Pass 1 — outline
            stream.publish(ctx.sessionId(), SessionEvent.progress("Extracting system characteristics…"));
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
            String title = outline.path("title").asText("System Security Plan");
            String version = outline.path("version").asText("1.0");
            String publisher = outline.path("publisher").asText("unspecified");
            String systemName = outline.path("systemName").asText("Untitled System");
            String systemDescription = outline.path("systemDescription").asText("");
            String systemId = outline.path("systemId").asText("system-" + UUID.randomUUID());
            String sensitivityLevel = outline.path("sensitivityLevel").asText("moderate");
            String authorizationBoundary = outline.path("authorizationBoundary").asText("");

            List<JsonNode> informationTypes = new ArrayList<>();
            for (JsonNode t : outline.path("informationTypes")) informationTypes.add(t);
            List<JsonNode> components = new ArrayList<>();
            for (JsonNode c : outline.path("components")) components.add(c);
            List<JsonNode> ussers = new ArrayList<>();
            for (JsonNode u : outline.path("users")) ussers.add(u);

            List<String> controlIds = new ArrayList<>();
            for (JsonNode cid : outline.path("controlIds")) controlIds.add(cid.asText());

            // Pass 1.5 — optional profile resolution (skip-profile branch leaves controlIds as-is)
            String profileHref = ctx.profileHref();
            if (profileHref != null && !profileHref.isBlank()) {
                controlIds = resolveProfileControlIds(ctx, profileHref, controlIds);
            }

            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Outline complete — " + systemName + ", " + controlIds.size() + " controls"));

            // Pass 2 — per-chunk narratives
            List<JsonNode> producedRequirements = new ArrayList<>();
            List<List<String>> chunks = chunking.chunk(controlIds);
            int chunkIndex = 0;
            for (List<String> chunk : chunks) {
                chunkIndex++;
                stream.publish(ctx.sessionId(), SessionEvent.progress(
                        "Drafting implementation narratives (" + chunkIndex + " of " + chunks.size() + ")…"));
                AnthropicResult chunkRes = client.send(ctx.apiKey(), AnthropicCall.builder()
                        .model(ctx.model())
                        .systemPrompt(system)
                        .userMessage(prompts.controlsPrompt(systemName, chunk) + "\n\n---\n\n" + docText)
                        .maxTokens(8000)
                        .build(),
                        msg -> stream.publish(ctx.sessionId(), SessionEvent.progress(msg)));
                tokensIn += chunkRes.tokensIn();
                tokensOut += chunkRes.tokensOut();
                JsonNode arr = MAPPER.readTree(extractJsonArray(chunkRes.text()));
                if (arr.isArray()) {
                    for (JsonNode req : arr) producedRequirements.add(req);
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
                    "Assembling OSCAL System Security Plan…"));

            Map<String, Object> meta = new HashMap<>();
            meta.put("title", title);
            meta.put("last-modified", java.time.Instant.now().toString());
            meta.put("version", version);
            meta.put("oscal-version", "1.1.2");
            meta.put("parties", List.of(Map.of(
                    "uuid", UUID.randomUUID().toString(),
                    "type", "organization",
                    "name", publisher)));

            Map<String, Object> sysChar = new HashMap<>();
            sysChar.put("system-name", systemName);
            sysChar.put("description", systemDescription);
            sysChar.put("system-ids", List.of(Map.of("id", systemId)));
            sysChar.put("security-sensitivity-level", sensitivityLevel);
            sysChar.put("system-information", Map.of("information-types", informationTypes));
            sysChar.put("security-impact-level", Map.of(
                    "security-objective-confidentiality", sensitivityLevel,
                    "security-objective-integrity", sensitivityLevel,
                    "security-objective-availability", sensitivityLevel));
            sysChar.put("status", Map.of("state", "operational"));
            sysChar.put("authorization-boundary", Map.of("description", authorizationBoundary));

            Map<String, Object> sysImpl = new HashMap<>();
            sysImpl.put("users", ussers);
            sysImpl.put("components", components);

            Map<String, Object> ctrlImpl = new HashMap<>();
            ctrlImpl.put("uuid", UUID.randomUUID().toString());
            ctrlImpl.put("description", "Control implementations drafted from source document.");
            ctrlImpl.put("implemented-requirements", producedRequirements);

            Map<String, Object> root = new HashMap<>();
            root.put("uuid", UUID.randomUUID().toString());
            root.put("metadata", meta);
            root.put("import-profile", Map.of("href", profileHref == null ? "" : profileHref));
            root.put("system-characteristics", sysChar);
            root.put("system-implementation", sysImpl);
            root.put("control-implementation", ctrlImpl);

            Map<String, Object> doc = new HashMap<>();
            doc.put("system-security-plan", root);

            String resultJson = MAPPER.writeValueAsString(doc);
            stream.publish(ctx.sessionId(), SessionEvent.complete(resultJson));
            return WizardOutcome.ok(tokensIn, tokensOut);

        } catch (IllegalArgumentException e) {
            stream.publish(ctx.sessionId(), SessionEvent.error("auth_or_input", e.getMessage()));
            return WizardOutcome.failed("auth_or_input", e.getMessage());
        } catch (Exception e) {
            log.error("SSP wizard failed", e);
            stream.publish(ctx.sessionId(), SessionEvent.error("model_error", e.getMessage()));
            return WizardOutcome.failed("model_error", e.getMessage());
        }
    }

    private List<String> resolveProfileControlIds(WizardContext ctx, String profileHref, List<String> outlineFallback) {
        stream.publish(ctx.sessionId(), SessionEvent.progress("Resolving profile…"));
        try {
            User caller = users.findById(ctx.userId()).orElseThrow(
                    () -> new IllegalStateException("User " + ctx.userId() + " not found"));
            String profileJson = profileLoader.load(profileHref, caller);
            Optional<List<String>> ids = extractor.extract(profileJson);
            if (ids.isPresent()) {
                stream.publish(ctx.sessionId(), SessionEvent.progress(
                        "Profile resolved — " + ids.get().size() + " controls"));
                return ids.get();
            }
            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Profile uses include-all (catalog resolution not yet supported) — falling back to controls inferred from source document."));
            return outlineFallback;
        } catch (Exception e) {
            log.warn("Profile resolution failed for {}: {}", profileHref, e.getMessage());
            stream.publish(ctx.sessionId(), SessionEvent.progress(
                    "Profile resolution failed: " + e.getMessage()
                    + " — falling back to controls inferred from source document."));
            return outlineFallback;
        }
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
