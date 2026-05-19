package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.WizardKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeLoaderTest {

    /**
     * The test fixtures live under {@code back-end/src/test/resources/claude-plugins},
     * which Maven puts on the test classpath at {@code /claude-plugins}.
     */
    private static final String TEST_ROOT = "classpath:/claude-plugins";

    @Test
    void loadsCatalogSystemPromptIncludesBasicsAndCatalog() {
        KnowledgeLoader loader = new KnowledgeLoader(TEST_ROOT);
        String prompt = loader.systemFor(WizardKind.CATALOG);

        assertThat(prompt).contains("OSCAL Layer Overview"); // from oscal-basics
        assertThat(prompt).contains("Catalog skill");        // from oscal-catalog
        assertThat(prompt).contains("Metaschema Constraints"); // from metaschema-basics
    }

    @Test
    void loadsProfileSystemPromptStillLoadsAllForUntightenedKinds() {
        KnowledgeLoader loader = new KnowledgeLoader(TEST_ROOT);
        String prompt = loader.systemFor(WizardKind.PROFILE);
        // PROFILE wizard plan hasn't landed yet — still load-all
        assertThat(prompt).contains("OSCAL Layer Overview");
        assertThat(prompt).contains("Catalog skill");
    }

    @Test
    void smokeWizardSystemPromptIsTerse() {
        KnowledgeLoader loader = new KnowledgeLoader(TEST_ROOT);
        String prompt = loader.systemFor(WizardKind.SMOKE);

        assertThat(prompt).contains("smoke");
        assertThat(prompt.length()).isLessThan(2000);
    }

    @Test
    void loadsComponentDefSystemPromptIncludesBasicsAndComponentDef() {
        KnowledgeLoader loader = new KnowledgeLoader(TEST_ROOT);
        String prompt = loader.systemFor(WizardKind.COMPONENT_DEF);

        assertThat(prompt).contains("OSCAL Layer Overview");         // from oscal-basics
        assertThat(prompt).contains("Component-definition skill");   // from oscal-component-definition
        assertThat(prompt).contains("Metaschema Constraints");       // from metaschema-basics
        // Should NOT contain catalog-specific content in the targeted branch
        assertThat(prompt).doesNotContain("Catalog skill");
    }
}
