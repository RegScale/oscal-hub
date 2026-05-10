package gov.nist.oscal.tools.api.service.ai.rulegen;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Builds the per-OSCAL-model system prompt for the rule-gen wizard and
 * returns the structured-output tool definitions Claude must call.
 */
@Component
public class RuleGenPrompts {

    public String systemPromptFor(String modelType) {
        String summary = loadSummary(modelType);
        return ""
            + "You help OSCAL Hub users author Metaschema validation rules.\n"
            + "The user describes a rule in plain English. You convert it into a\n"
            + "well-formed Metaschema external constraint XML fragment plus 4-6\n"
            + "synthetic test cases (minimal valid OSCAL stubs labeled pass/fail).\n"
            + "\n"
            + "OUTPUT CONTRACT — every turn you must call exactly ONE tool:\n"
            + "  ask_clarifying_question  — the description is ambiguous; ask\n"
            + "                            ONE short question.\n"
            + "  generate_rule            — you have enough info; produce the full\n"
            + "                            proposal with test cases.\n"
            + "  revise_rule              — same shape as generate_rule, used when\n"
            + "                            iterating from a failing test matrix.\n"
            + "\n"
            + "Constraint XML rules:\n"
            + "  * Output the inner <assembly target=...> / <field target=...>\n"
            + "    fragment ONLY — the system wraps it in METASCHEMA-CONSTRAINTS.\n"
            + "  * Use <expect>, <allowed-values>, <matches>, <has-cardinality>,\n"
            + "    <is-unique>, <index>, <index-has-key> only.\n"
            + "  * Every constraint has an id and a level (ERROR / WARNING / INFORMATIONAL).\n"
            + "  * Include a clear <message> on every constraint, prefixed with\n"
            + "    [custom: <ruleId>] so users can identify the source.\n"
            + "  * Test fragments are minimal valid JSON OSCAL stubs (just enough\n"
            + "    of the model to be parseable). Avoid full real-world docs.\n"
            + "\n"
            + "OSCAL model summary:\n"
            + "===================\n"
            + summary;
    }

    public List<Map<String, Object>> toolDefinitions() {
        return List.of(
            Map.of(
                "name", "ask_clarifying_question",
                "description", "Ask ONE short clarifying question when the user's description is ambiguous. Keep questions focused on the specific missing info.",
                "input_schema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "question", Map.of("type", "string", "description", "The question to ask the user.")),
                    "required", List.of("question"))
            ),
            Map.of(
                "name", "generate_rule",
                "description", "Produce a full rule proposal with synthetic test cases. Use this when you have enough information.",
                "input_schema", proposalSchema()
            ),
            Map.of(
                "name", "revise_rule",
                "description", "Same shape as generate_rule. Use only when revising a previous proposal that had failing test cases.",
                "input_schema", proposalSchema()
            )
        );
    }

    private static Map<String, Object> proposalSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "name", Map.of("type", "string"),
                "description", Map.of("type", "string"),
                "severity", Map.of("type", "string", "enum", List.of("error", "warning", "info")),
                "fieldPath", Map.of("type", "string"),
                "constraintXml", Map.of("type", "string", "description", "Inner <assembly>/<field> fragment, NOT a full METASCHEMA-CONSTRAINTS document."),
                "testCases", Map.of(
                    "type", "array",
                    "minItems", 4,
                    "maxItems", 6,
                    "items", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "description", Map.of("type", "string"),
                            "fragmentJson", Map.of("type", "string", "description", "Minimal JSON OSCAL fragment"),
                            "expected", Map.of("type", "string", "enum", List.of("pass", "fail"))),
                        "required", List.of("description", "fragmentJson", "expected")))),
            "required", List.of("name", "description", "severity", "constraintXml", "testCases"));
    }

    private String loadSummary(String modelType) {
        String resource = "oscal-schema-summaries/" + canonicalize(modelType) + ".txt";
        try {
            return new String(new ClassPathResource(resource).getInputStream().readAllBytes(),
                              StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unknown OSCAL model type: " + modelType, e);
        }
    }

    private static String canonicalize(String modelType) {
        return switch (modelType) {
            case "ssp" -> "system-security-plan";
            case "ap" -> "assessment-plan";
            case "ar" -> "assessment-results";
            case "poam" -> "plan-of-action-and-milestones";
            default -> modelType;
        };
    }
}
