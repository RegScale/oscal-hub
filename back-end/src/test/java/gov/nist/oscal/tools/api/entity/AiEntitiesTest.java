package gov.nist.oscal.tools.api.entity;

import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.repository.OrgAiSettingsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiEntitiesTest {

    @Autowired private OrgAiSettingsRepository settingsRepo;
    @Autowired private AiSessionRepository sessionRepo;

    @Test
    void persistsOrgAiSettings() {
        OrgAiSettings s = new OrgAiSettings();
        s.setOrganizationId(42L);
        s.setAnthropicKeyEncrypted("ciphertext");
        s.setAnthropicKeyFingerprint("abcd...1234");
        s.setDefaultModel("claude-opus-4-7");
        s.setEnabled(true);
        OrgAiSettings saved = settingsRepo.save(s);

        Optional<OrgAiSettings> loaded = settingsRepo.findByOrganizationId(42L);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getId()).isEqualTo(saved.getId());
        assertThat(loaded.get().getDefaultModel()).isEqualTo("claude-opus-4-7");
    }

    @Test
    void persistsAiSession() {
        AiSession s = new AiSession();
        s.setId(UUID.randomUUID());
        s.setOrganizationId(42L);
        s.setUserId(7L);
        s.setWizardKind(WizardKind.SMOKE);
        s.setMode(AiSessionMode.STREAMING);
        s.setModel("claude-opus-4-7");
        s.setStatus(AiSessionStatus.RUNNING);
        s.setStartedAt(LocalDateTime.now());
        AiSession saved = sessionRepo.save(s);

        assertThat(sessionRepo.findById(saved.getId())).isPresent();
    }
}
