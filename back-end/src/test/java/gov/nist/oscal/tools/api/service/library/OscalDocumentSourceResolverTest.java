package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.OscalDocument;
import gov.nist.oscal.tools.api.entity.OscalModelType;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.OscalDocumentRepository;
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

class OscalDocumentSourceResolverTest {

    @Mock OscalDocumentRepository repo;
    @Mock StorageService storage;
    OscalDocumentSourceResolver resolver;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        resolver = new OscalDocumentSourceResolver(repo, storage);
    }

    private OscalDocument doc(Long id, OscalModelType type, String username) {
        User creator = new User();
        creator.setUsername(username);

        OscalDocument d = new OscalDocument();
        d.setId(id);
        d.setOscalUuid("44444444-4444-4444-4444-44444444444" + (id % 10));
        d.setModelType(type);
        d.setTitle("Doc " + id);
        d.setStoragePath("build/" + username + "/doc-" + id + ".json");
        d.setFilename("doc-" + id + ".json");
        d.setCreatedBy(creator);
        return d;
    }

    @Test
    void supportedTypesCoversAllFourDocumentKinds() {
        assertThat(resolver.supportedTypes())
                .containsExactlyInAnyOrder(
                        SourceType.SSP,
                        SourceType.AP,
                        SourceType.AR,
                        SourceType.POAM);
    }

    @Test
    void resolveSspMapsToSspOscalType() {
        OscalDocument d = doc(1L, OscalModelType.SYSTEM_SECURITY_PLAN, "alice");
        when(repo.findById(1L)).thenReturn(Optional.of(d));
        when(storage.downloadComponent(d.getStoragePath())).thenReturn("{\"ssp\":1}");

        SourceContent sc = resolver.resolve(1L, "alice");

        assertThat(sc.oscalType()).isEqualTo("ssp");
        assertThat(sc.bytes()).isEqualTo("{\"ssp\":1}".getBytes(StandardCharsets.UTF_8));
        assertThat(sc.sourceId()).isEqualTo(UUID.fromString(d.getOscalUuid()));
        assertThat(sc.filename()).isEqualTo("doc-1.json");
        assertThat(sc.defaultTitle()).isEqualTo("Doc 1");
    }

    @Test
    void resolveApMapsToApOscalType() {
        OscalDocument d = doc(2L, OscalModelType.ASSESSMENT_PLAN, "alice");
        when(repo.findById(2L)).thenReturn(Optional.of(d));
        when(storage.downloadComponent(d.getStoragePath())).thenReturn("{}");

        SourceContent sc = resolver.resolve(2L, "alice");

        assertThat(sc.oscalType()).isEqualTo("ap");
    }

    @Test
    void resolveArMapsToArOscalType() {
        OscalDocument d = doc(3L, OscalModelType.ASSESSMENT_RESULTS, "alice");
        when(repo.findById(3L)).thenReturn(Optional.of(d));
        when(storage.downloadComponent(d.getStoragePath())).thenReturn("{}");

        SourceContent sc = resolver.resolve(3L, "alice");

        assertThat(sc.oscalType()).isEqualTo("ar");
    }

    @Test
    void resolvePoamMapsToPoamOscalType() {
        OscalDocument d = doc(4L, OscalModelType.PLAN_OF_ACTION_AND_MILESTONES, "alice");
        when(repo.findById(4L)).thenReturn(Optional.of(d));
        when(storage.downloadComponent(d.getStoragePath())).thenReturn("{}");

        SourceContent sc = resolver.resolve(4L, "alice");

        assertThat(sc.oscalType()).isEqualTo("poam");
    }

    @Test
    void resolveThrowsWhenRowMissing() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> resolver.resolve(99L, "alice"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveThrowsWhenCallerIsNotCreator() {
        OscalDocument d = doc(5L, OscalModelType.SYSTEM_SECURITY_PLAN, "alice");
        when(repo.findById(5L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> resolver.resolve(5L, "bob"))
                .isInstanceOf(SecurityException.class);
    }
}
