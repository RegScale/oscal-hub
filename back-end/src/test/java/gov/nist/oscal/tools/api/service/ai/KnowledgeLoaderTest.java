package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.WizardKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeLoaderTest {

    @Test
    void loadsCatalogSystemPromptIncludesBasicsAndCatalog() {
        Path root = Paths.get("src/test/resources/claude-plugins");
        KnowledgeLoader loader = new KnowledgeLoader(root);
        String prompt = loader.systemFor(WizardKind.CATALOG);

        assertThat(prompt).contains("OSCAL Layer Overview"); // from oscal-basics
        assertThat(prompt).contains("Catalog skill");        // from oscal-catalog
        assertThat(prompt).contains("Metaschema Constraints"); // from metaschema-basics
    }

    @Test
    void loadsProfileSystemPromptStillLoadsAllForUntightenedKinds() {
        Path root = Paths.get("src/test/resources/claude-plugins");
        KnowledgeLoader loader = new KnowledgeLoader(root);
        String prompt = loader.systemFor(WizardKind.PROFILE);
        // PROFILE wizard plan hasn't landed yet — still load-all
        assertThat(prompt).contains("OSCAL Layer Overview");
        assertThat(prompt).contains("Catalog skill");
    }

    @Test
    void smokeWizardSystemPromptIsTerse() {
        Path root = Paths.get("src/test/resources/claude-plugins");
        KnowledgeLoader loader = new KnowledgeLoader(root);
        String prompt = loader.systemFor(WizardKind.SMOKE);

        assertThat(prompt).contains("smoke");
        assertThat(prompt.length()).isLessThan(2000);
    }

    @Test
    void loadsComponentDefSystemPromptIncludesBasicsAndComponentDef() {
        Path root = Paths.get("src/test/resources/claude-plugins");
        KnowledgeLoader loader = new KnowledgeLoader(root);
        String prompt = loader.systemFor(WizardKind.COMPONENT_DEF);

        assertThat(prompt).contains("OSCAL Layer Overview");         // from oscal-basics
        assertThat(prompt).contains("Component-definition skill");   // from oscal-component-definition
        assertThat(prompt).contains("Metaschema Constraints");       // from metaschema-basics
        // Should NOT contain catalog-specific content in the targeted branch
        assertThat(prompt).doesNotContain("Catalog skill");
    }
}
