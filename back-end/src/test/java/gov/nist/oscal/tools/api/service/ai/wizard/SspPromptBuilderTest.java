package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SspPromptBuilderTest {

    private final SspPromptBuilder builder = new SspPromptBuilder();

    @Test
    void outlinePromptDemandsSingleRawJsonObject() {
        String prompt = builder.outlinePrompt();
        assertThat(prompt).contains("Respond with a SINGLE raw JSON object");
        assertThat(prompt).contains("First character must be `{`");
        assertThat(prompt).contains("\"systemName\"");
        assertThat(prompt).contains("\"sensitivityLevel\"");
        assertThat(prompt).contains("\"informationTypes\"");
        assertThat(prompt).contains("\"components\"");
        assertThat(prompt).contains("\"users\"");
        assertThat(prompt).contains("\"authorizationBoundary\"");
        assertThat(prompt).contains("\"controlIds\"");
    }

    @Test
    void controlsPromptInterpolatesTitleAndIds() {
        String prompt = builder.controlsPrompt("Acme Trust Center", List.of("ac-1", "ac-2", "ac-3"));
        assertThat(prompt).contains("SINGLE raw JSON array");
        assertThat(prompt).contains("Acme Trust Center");
        assertThat(prompt).contains("ac-1, ac-2, ac-3");
        assertThat(prompt).contains("ai-confidence");
        assertThat(prompt).contains("https://oscal-hub.io/ns");
        assertThat(prompt).contains("\"high\"");
        assertThat(prompt).contains("\"medium\"");
        assertThat(prompt).contains("\"low\"");
        // Confidence rubric must be present
        assertThat(prompt).contains("directly addresses");
        assertThat(prompt).contains("topic generally");
        assertThat(prompt).contains("no direct evidence");
        // Stub instruction for low-confidence entries
        assertThat(prompt).contains("Source document does not address this control. To be completed.");
    }

    @Test
    void controlsPromptAlwaysEmitsOnePerControl() {
        String prompt = builder.controlsPrompt("X", List.of("ac-1"));
        assertThat(prompt).contains("Always emit one entry per requested control");
    }
}
