package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.Catalog;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.CatalogRepository;
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

class CatalogSourceResolverTest {

    @Mock CatalogRepository catalogRepo;
    @Mock StorageService storage;
    CatalogSourceResolver resolver;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        resolver = new CatalogSourceResolver(catalogRepo, storage);
    }

    @Test
    void resolveReturnsContentFromStorage() {
        UUID oscalUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

        User creator = new User();
        creator.setUsername("alice");

        Catalog c = new Catalog();
        c.setId(42L);
        c.setOscalUuid(oscalUuid.toString());
        c.setTitle("My Catalog");
        c.setStoragePath("build/alice/cat.json");
        c.setFilename("cat.json");
        c.setCreatedBy(creator);

        when(catalogRepo.findById(42L)).thenReturn(Optional.of(c));
        when(storage.downloadComponent("build/alice/cat.json")).thenReturn("{\"x\":1}");

        SourceContent sc = resolver.resolve(42L, "alice");

        assertThat(sc.bytes()).isEqualTo("{\"x\":1}".getBytes(StandardCharsets.UTF_8));
        assertThat(sc.format()).isEqualTo("json");
        assertThat(sc.filename()).isEqualTo("cat.json");
        assertThat(sc.oscalType()).isEqualTo("catalog");
        assertThat(sc.sourceId()).isEqualTo(oscalUuid);
        assertThat(sc.defaultTitle()).isEqualTo("My Catalog");
    }

    @Test
    void resolveThrowsWhenRowMissing() {
        when(catalogRepo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> resolver.resolve(99L, "alice"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveThrowsWhenCallerIsNotCreator() {
        User creator = new User();
        creator.setUsername("alice");

        Catalog c = new Catalog();
        c.setId(7L);
        c.setOscalUuid("11111111-1111-1111-1111-111111111111");
        c.setTitle("Mine");
        c.setStoragePath("build/alice/x.json");
        c.setFilename("x.json");
        c.setCreatedBy(creator);

        when(catalogRepo.findById(7L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> resolver.resolve(7L, "bob"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void supportedTypeIsCatalog() {
        assertThat(resolver.supportedType()).isEqualTo(SourceType.CATALOG);
    }
}
