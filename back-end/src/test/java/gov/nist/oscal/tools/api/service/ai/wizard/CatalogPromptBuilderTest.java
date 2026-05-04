package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogPromptBuilderTest {

    private final CatalogPromptBuilder builder = new CatalogPromptBuilder();

    @Test
    void outlinePromptRequestsCanonicalControlIds() {
        String p = builder.outlinePrompt();
        assertThat(p).contains("canonical lowercase-hyphenated");
        assertThat(p).contains("\"families\":");
    }

    @Test
    void familyPromptIncludesControlIds() {
        String p = builder.familyPrompt("ac", "Access Control", List.of("ac-1", "ac-2"));
        assertThat(p).contains("ac-1, ac-2");
        assertThat(p).contains("Access Control");
        // Tightened prompt forbids prose preludes — assert the JSON-only directive.
        assertThat(p).contains("SINGLE raw JSON object");
    }

    @Test
    void mergePromptCarriesMetadata() {
        String p = builder.mergePrompt("My Catalog", "1.0", "Acme");
        assertThat(p).contains("My Catalog");
        assertThat(p).contains("Acme");
        assertThat(p).contains("oscal-version");
    }
}
