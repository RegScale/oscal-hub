package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.Profile;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.ProfileRepository;
import gov.nist.oscal.tools.api.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class ProfileSourceResolverTest {

    @Mock ProfileRepository profileRepo;
    @Mock StorageService storage;
    ProfileSourceResolver resolver;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        resolver = new ProfileSourceResolver(profileRepo, storage);
    }

    @Test
    void resolveReturnsContentFromStorage() {
        UUID oscalUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");

        User creator = new User();
        creator.setUsername("alice");

        Profile p = new Profile();
        p.setId(42L);
        p.setOscalUuid(oscalUuid.toString());
        p.setTitle("My Profile");
        p.setStoragePath("build/alice/profile.json");
        p.setFilename("profile.json");
        p.setCreatedBy(creator);

        when(profileRepo.findById(42L)).thenReturn(Optional.of(p));
        when(storage.downloadComponent("build/alice/profile.json")).thenReturn("{\"x\":1}");

        SourceContent sc = resolver.resolve(42L, "alice");

        assertThat(sc.bytes()).isEqualTo("{\"x\":1}".getBytes(StandardCharsets.UTF_8));
        assertThat(sc.format()).isEqualTo("json");
        assertThat(sc.filename()).isEqualTo("profile.json");
        assertThat(sc.oscalType()).isEqualTo("profile");
        assertThat(sc.sourceId()).isEqualTo(oscalUuid);
        assertThat(sc.defaultTitle()).isEqualTo("My Profile");
    }

    @Test
    void resolveThrowsWhenRowMissing() {
        when(profileRepo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> resolver.resolve(99L, "alice"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveThrowsWhenCallerIsNotCreator() {
        User creator = new User();
        creator.setUsername("alice");

        Profile p = new Profile();
        p.setId(7L);
        p.setOscalUuid("22222222-2222-2222-2222-222222222222");
        p.setTitle("Mine");
        p.setStoragePath("build/alice/x.json");
        p.setFilename("x.json");
        p.setCreatedBy(creator);

        when(profileRepo.findById(7L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> resolver.resolve(7L, "bob"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void supportedTypeIsProfile() {
        assertThat(resolver.supportedType()).isEqualTo(SourceType.PROFILE);
    }
}
