package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.WizardKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeLoaderTest {

    @Test
    void loadsCatalogSystemPromptFromFixture() {
        Path root = Paths.get("src/test/resources/claude-plugins");
        KnowledgeLoader loader = new KnowledgeLoader(root);
        String prompt = loader.systemFor(WizardKind.CATALOG);

        assertThat(prompt).contains("OSCAL Layer Overview");
        assertThat(prompt).contains("Metaschema Constraints");
    }

    @Test
    void smokeWizardSystemPromptIsTerse() {
        Path root = Paths.get("src/test/resources/claude-plugins");
        KnowledgeLoader loader = new KnowledgeLoader(root);
        String prompt = loader.systemFor(WizardKind.SMOKE);

        assertThat(prompt).contains("smoke");
        assertThat(prompt.length()).isLessThan(2000);
    }
}
