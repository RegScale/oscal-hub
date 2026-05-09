package gov.nist.oscal.tools.api.service.ai.rulegen;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleGenPromptsTest {

    private final RuleGenPrompts prompts = new RuleGenPrompts();

    @Test
    void systemPromptForCanonicalModel_loadsSummary_andEmbedsContract() {
        // 'catalog' loads catalog.txt directly.
        String prompt = prompts.systemPromptFor("catalog");
        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("OSCAL Hub")
                .contains("ask_clarifying_question")
                .contains("generate_rule")
                .contains("revise_rule")
                .contains("METASCHEMA-CONSTRAINTS");
    }

    @Test
    void systemPromptForShortAlias_ssp_resolvesToFullSummaryFile() {
        // 'ssp' must canonicalize to system-security-plan.txt — wizard sends
        // short codes from the frontend rather than full filenames.
        String prompt = prompts.systemPromptFor("ssp");
        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("OSCAL model summary");
    }

    @Test
    void systemPromptForShortAlias_poam_resolvesToPlanOfActionFile() {
        String prompt = prompts.systemPromptFor("poam");
        assertThat(prompt).isNotBlank();
    }

    @Test
    void systemPromptForShortAlias_ap_resolvesToAssessmentPlanFile() {
        String prompt = prompts.systemPromptFor("ap");
        assertThat(prompt).isNotBlank();
    }

    @Test
    void systemPromptForShortAlias_ar_resolvesToAssessmentResultsFile() {
        String prompt = prompts.systemPromptFor("ar");
        assertThat(prompt).isNotBlank();
    }

    @Test
    void unknownModelType_throwsIllegalArgumentWithModelName() {
        // The controller surfaces this as 400 — message must include the bad
        // input so the user gets actionable feedback.
        assertThatThrownBy(() -> prompts.systemPromptFor("totally-bogus"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totally-bogus");
    }

    @Test
    void toolDefinitions_advertise_threeStructuredOutputTools_inExpectedOrder() {
        // Order matters for the LLM contract — generate_rule before revise_rule
        // because the model picks the first matching tool when both are valid.
        List<Map<String, Object>> tools = prompts.toolDefinitions();
        assertThat(tools).hasSize(3);
        assertThat(tools.get(0).get("name")).isEqualTo("ask_clarifying_question");
        assertThat(tools.get(1).get("name")).isEqualTo("generate_rule");
        assertThat(tools.get(2).get("name")).isEqualTo("revise_rule");
    }

    @Test
    @SuppressWarnings("unchecked")
    void clarifyingQuestion_schema_requiresQuestionField() {
        Map<String, Object> q = prompts.toolDefinitions().get(0);
        Map<String, Object> schema = (Map<String, Object>) q.get("input_schema");
        List<String> required = (List<String>) schema.get("required");
        assertThat(required).containsExactly("question");
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateRule_schema_constrainsTestCases_to_4_through_6_items() {
        // Hard contract: too few test cases means insufficient coverage; too
        // many slows down the test runner and inflates token cost.
        Map<String, Object> g = prompts.toolDefinitions().get(1);
        Map<String, Object> schema = (Map<String, Object>) g.get("input_schema");
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        Map<String, Object> testCases = (Map<String, Object>) properties.get("testCases");
        assertThat(testCases.get("minItems")).isEqualTo(4);
        assertThat(testCases.get("maxItems")).isEqualTo(6);
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateRule_schema_severityIsConstrainedEnum() {
        Map<String, Object> g = prompts.toolDefinitions().get(1);
        Map<String, Object> schema = (Map<String, Object>) g.get("input_schema");
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        Map<String, Object> severity = (Map<String, Object>) properties.get("severity");
        List<String> allowed = (List<String>) severity.get("enum");
        assertThat(allowed).containsExactlyInAnyOrder("error", "warning", "info");
    }

    @Test
    @SuppressWarnings("unchecked")
    void reviseRule_hasSameSchemaShape_asGenerateRule() {
        // revise_rule reuses generate_rule's schema so the LLM doesn't need to
        // remember a second shape. Verify they're not accidentally drifting.
        Map<String, Object> generate = prompts.toolDefinitions().get(1);
        Map<String, Object> revise = prompts.toolDefinitions().get(2);
        assertThat(revise.get("input_schema")).isEqualTo(generate.get("input_schema"));
    }
}
