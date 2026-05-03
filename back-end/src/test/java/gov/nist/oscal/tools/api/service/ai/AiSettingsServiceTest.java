package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.OrgAiSettings;
import gov.nist.oscal.tools.api.model.ai.AiSettingsResponse;
import gov.nist.oscal.tools.api.repository.OrgAiSettingsRepository;
import gov.nist.oscal.tools.api.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiSettingsServiceTest {

    private OrgAiSettingsRepository repo;
    private EncryptionService encryption;
    private AiSettingsService service;

    @BeforeEach
    void setUp() {
        repo = mock(OrgAiSettingsRepository.class);
        encryption = mock(EncryptionService.class);
        service = new AiSettingsService(repo, encryption);
    }

    @Test
    void setKeyCreatesNewSettingsWhenNoneExist() {
        when(repo.findByOrganizationId(1L)).thenReturn(Optional.empty());
        when(encryption.encrypt("sk-ant-1234567890abcdef")).thenReturn("encrypted");
        when(repo.save(any(OrgAiSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        AiSettingsResponse resp = service.setApiKey(1L, "sk-ant-1234567890abcdef", "claude-opus-4-7");

        ArgumentCaptor<OrgAiSettings> captor = ArgumentCaptor.forClass(OrgAiSettings.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getAnthropicKeyEncrypted()).isEqualTo("encrypted");
        assertThat(captor.getValue().isEnabled()).isTrue();
        assertThat(resp.fingerprint()).endsWith("cdef");
    }

    @Test
    void enabledReturnsFalseWhenNoSettings() {
        when(repo.findByOrganizationId(2L)).thenReturn(Optional.empty());
        assertThat(service.isEnabledFor(2L)).isFalse();
    }

    @Test
    void enabledReturnsTrueWhenSettingsHaveKey() {
        OrgAiSettings s = new OrgAiSettings();
        s.setOrganizationId(3L);
        s.setAnthropicKeyEncrypted("ciphertext");
        s.setEnabled(true);
        when(repo.findByOrganizationId(3L)).thenReturn(Optional.of(s));
        assertThat(service.isEnabledFor(3L)).isTrue();
    }

    @Test
    void getDecryptedKeyDecryptsViaEncryptionService() {
        OrgAiSettings s = new OrgAiSettings();
        s.setOrganizationId(4L);
        s.setAnthropicKeyEncrypted("ciphertext");
        s.setEnabled(true);
        when(repo.findByOrganizationId(4L)).thenReturn(Optional.of(s));
        when(encryption.decrypt("ciphertext")).thenReturn("sk-ant-real");

        assertThat(service.getDecryptedKey(4L)).isEqualTo("sk-ant-real");
    }

    @Test
    void disableClearsKey() {
        OrgAiSettings s = new OrgAiSettings();
        s.setOrganizationId(5L);
        s.setAnthropicKeyEncrypted("ciphertext");
        s.setEnabled(true);
        when(repo.findByOrganizationId(5L)).thenReturn(Optional.of(s));

        service.disable(5L);

        ArgumentCaptor<OrgAiSettings> captor = ArgumentCaptor.forClass(OrgAiSettings.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getAnthropicKeyEncrypted()).isNull();
        assertThat(captor.getValue().isEnabled()).isFalse();
    }
}
