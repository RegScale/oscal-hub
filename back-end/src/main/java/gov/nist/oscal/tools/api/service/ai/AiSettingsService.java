package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.OrgAiSettings;
import gov.nist.oscal.tools.api.model.ai.AiSettingsResponse;
import gov.nist.oscal.tools.api.repository.OrgAiSettingsRepository;
import gov.nist.oscal.tools.api.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AiSettingsService implements AiSettingsServiceFacade {

    private static final Logger log = LoggerFactory.getLogger(AiSettingsService.class);

    private final OrgAiSettingsRepository repo;
    private final EncryptionService encryption;

    public AiSettingsService(OrgAiSettingsRepository repo, EncryptionService encryption) {
        this.repo = repo;
        this.encryption = encryption;
    }

    @Transactional
    public AiSettingsResponse setApiKey(Long organizationId, String apiKey, String defaultModel) {
        OrgAiSettings s = repo.findByOrganizationId(organizationId).orElseGet(() -> {
            OrgAiSettings n = new OrgAiSettings();
            n.setOrganizationId(organizationId);
            return n;
        });
        s.setAnthropicKeyEncrypted(encryption.encrypt(apiKey));
        s.setAnthropicKeyFingerprint(fingerprint(apiKey));
        if (defaultModel != null && !defaultModel.isBlank()) {
            s.setDefaultModel(defaultModel);
        }
        s.setEnabled(true);
        repo.save(s);
        log.info("AI settings updated for org {}", organizationId);
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    public AiSettingsResponse getSettings(Long organizationId) {
        Optional<OrgAiSettings> s = repo.findByOrganizationId(organizationId);
        if (s.isEmpty()) {
            return new AiSettingsResponse(false, null, "claude-opus-4-7");
        }
        return toResponse(s.get());
    }

    @Transactional(readOnly = true)
    public boolean isEnabledFor(Long organizationId) {
        return repo.findByOrganizationId(organizationId)
                .map(s -> s.isEnabled() && s.getAnthropicKeyEncrypted() != null)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public String getDecryptedKey(Long organizationId) {
        OrgAiSettings s = repo.findByOrganizationId(organizationId)
                .filter(OrgAiSettings::isEnabled)
                .orElseThrow(() -> new IllegalStateException("AI not enabled for org " + organizationId));
        return encryption.decrypt(s.getAnthropicKeyEncrypted());
    }

    @Override
    public String requireApiKey(Long organizationId) {
        return getDecryptedKey(organizationId);
    }

    @Transactional(readOnly = true)
    public String getDefaultModel(Long organizationId) {
        return repo.findByOrganizationId(organizationId)
                .map(OrgAiSettings::getDefaultModel)
                .orElse("claude-opus-4-7");
    }

    @Transactional
    public void disable(Long organizationId) {
        repo.findByOrganizationId(organizationId).ifPresent(s -> {
            s.setEnabled(false);
            s.setAnthropicKeyEncrypted(null);
            s.setAnthropicKeyFingerprint(null);
            repo.save(s);
            log.info("AI settings disabled for org {}", organizationId);
        });
    }

    private AiSettingsResponse toResponse(OrgAiSettings s) {
        return new AiSettingsResponse(s.isEnabled() && s.getAnthropicKeyEncrypted() != null,
                s.getAnthropicKeyFingerprint(),
                s.getDefaultModel());
    }

    private String fingerprint(String apiKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(apiKey.getBytes());
            String hex = HexFormat.of().formatHex(digest).substring(0, 8);
            String last4 = apiKey.substring(Math.max(0, apiKey.length() - 4));
            return hex + "..." + last4;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
