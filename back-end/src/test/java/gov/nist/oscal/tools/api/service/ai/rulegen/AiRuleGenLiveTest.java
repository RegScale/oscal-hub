package gov.nist.oscal.tools.api.service.ai.rulegen;

import gov.nist.oscal.tools.api.model.airulegen.RuleGenTurnResponse;
import gov.nist.oscal.tools.api.service.ai.AiSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * End-to-end test against a live Anthropic API. Skipped unless
 * {@code ANTHROPIC_API_KEY} is set on the environment.
 *
 * <p>The wizard's tool-use flow can land in either {@code clarify} or
 * {@code proposal} on the first turn depending on how the model
 * interprets the prompt — both are acceptable outcomes. This test
 * asserts that we don't crash, that we get a structured response,
 * and that any returned proposal has a non-empty constraint with
 * test results attached.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class AiRuleGenLiveTest {

    @Autowired private AiRuleGenService service;
    @MockitoBean private AiSettingsService settings;

    @Test
    void wizardReachesStructuredResponse() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        when(settings.requireApiKey(anyLong())).thenReturn(key);
        when(settings.getDefaultModel(anyLong())).thenReturn("claude-opus-4-7");

        UUID sid = service.start(1L, 1L, "catalog");
        RuleGenTurnResponse res = service.turn(sid,
            "Every catalog must have a non-empty metadata title.");

        assertThat(res.phase()).isIn("clarify", "proposal", "exhausted");
        if ("proposal".equals(res.phase())) {
            assertThat(res.proposal()).isNotNull();
            assertThat(res.proposal().constraintXml()).isNotBlank();
            assertThat(res.testResults()).isNotEmpty();
            assertThat(res.testResults()).allMatch(r -> r.passed());
        }
        if ("clarify".equals(res.phase())) {
            assertThat(res.clarifyingQuestion()).isNotBlank();
        }
        assertThat(res.totalTokensIn()).isPositive();
        assertThat(res.totalTokensOut()).isPositive();

        service.close(sid);
    }
}
