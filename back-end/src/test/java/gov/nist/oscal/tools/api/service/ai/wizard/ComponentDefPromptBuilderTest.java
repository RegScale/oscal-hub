package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentDefPromptBuilderTest {

    private final ComponentDefPromptBuilder builder = new ComponentDefPromptBuilder();

    @Test
    void outlinePromptRequestsCanonicalIdsAndProductInfo() {
        String prompt = builder.outlinePrompt();

        assertThat(prompt).contains("productTitle");
        assertThat(prompt).contains("productDescription");
        assertThat(prompt).contains("componentType");
        assertThat(prompt).contains("controlIds");
        // Canonical lowercase-hyphenated IDs
        assertThat(prompt).contains("ac-1");
        // No markdown fences directive
        assertThat(prompt).contains("```json");
        // Must start with { requirement
        assertThat(prompt).contains("First character must be");
    }

    @Test
    void componentPromptIncludesPassedControlIds() {
        List<String> ids = List.of("ac-1", "ac-2", "au-3");
        String prompt = builder.componentPrompt("Red Hat Enterprise Linux 9", ids);

        assertThat(prompt).contains("ac-1");
        assertThat(prompt).contains("ac-2");
        assertThat(prompt).contains("au-3");
        assertThat(prompt).contains("Red Hat Enterprise Linux 9");
        assertThat(prompt).contains("implemented-requirements");
        // Array output directive
        assertThat(prompt).contains("[");
        assertThat(prompt).contains("]");
    }

    @Test
    void mergePromptCarriesMetadata() {
        String prompt = builder.mergePrompt(
                "CIS Ubuntu 20.04 LTS Benchmark",
                "A hardening guide for Ubuntu 20.04.",
                "software",
                "v1.0.0",
                "CIS",
                "https://raw.githubusercontent.com/usnistgov/oscal-content/main/nist.gov/SP800-53/rev5/json/NIST_SP-800-53_rev5_catalog.json");

        assertThat(prompt).contains("CIS Ubuntu 20.04 LTS Benchmark");
        assertThat(prompt).contains("v1.0.0");
        assertThat(prompt).contains("CIS");
        assertThat(prompt).contains("software");
        assertThat(prompt).contains("component-definition");
        assertThat(prompt).contains("1.1.2");
        assertThat(prompt).contains("implemented-requirements");
    }
}
