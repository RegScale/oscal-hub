package gov.nist.oscal.tools.api.integration;

import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.service.ai.AiOrchestrator;
import gov.nist.oscal.tools.api.service.ai.AiSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class AiCatalogWizardIntegrationTest {

    @Autowired private AiOrchestrator orchestrator;
    @Autowired private AiSessionRepository sessions;
    // Mock the concrete class (not the interface) so AiSettingsController's
    // injection of AiSettingsService still resolves to the correct bean type.
    @MockitoBean private AiSettingsService settings;

    @Test
    void catalogWizardProducesValidOscalFromSampleText() throws Exception {
        String key = System.getenv("ANTHROPIC_API_KEY");
        when(settings.requireApiKey(1L)).thenReturn(key);
        when(settings.getDefaultModel(1L)).thenReturn("claude-haiku-4-5-20251001");

        String sampleText = Files.readString(new ClassPathResource("ai-fixtures/sample-catalog.txt").getFile().toPath());

        UUID id = orchestrator.start(1L, 1L, WizardKind.CATALOG, AiSessionMode.STREAMING, sampleText, null, null);

        await().atMost(180, SECONDS).until(() ->
                sessions.findById(id).map(s -> s.getStatus() == AiSessionStatus.COMPLETED).orElse(false));

        AiSession s = sessions.findById(id).orElseThrow();
        assertThat(s.getTokensIn()).isPositive();
        assertThat(s.getTokensOut()).isPositive();
    }
}
