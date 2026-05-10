package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.ComponentDefinition;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.ComponentDefinitionRepository;
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

class ComponentDefinitionSourceResolverTest {

    @Mock ComponentDefinitionRepository componentRepo;
    @Mock StorageService storage;
    ComponentDefinitionSourceResolver resolver;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        resolver = new ComponentDefinitionSourceResolver(componentRepo, storage);
    }

    @Test
    void resolveReturnsContentFromStorage() {
        UUID oscalUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");

        User creator = new User();
        creator.setUsername("alice");

        ComponentDefinition cd = new ComponentDefinition();
        cd.setId(42L);
        cd.setOscalUuid(oscalUuid.toString());
        cd.setTitle("My Component");
        cd.setStoragePath("build/alice/component.json");
        cd.setFilename("component.json");
        cd.setCreatedBy(creator);

        when(componentRepo.findById(42L)).thenReturn(Optional.of(cd));
        when(storage.downloadComponent("build/alice/component.json")).thenReturn("{\"x\":1}");

        SourceContent sc = resolver.resolve(42L, "alice");

        assertThat(sc.bytes()).isEqualTo("{\"x\":1}".getBytes(StandardCharsets.UTF_8));
        assertThat(sc.format()).isEqualTo("json");
        assertThat(sc.filename()).isEqualTo("component.json");
        assertThat(sc.oscalType()).isEqualTo("component-definition");
        assertThat(sc.sourceId()).isEqualTo(oscalUuid);
        assertThat(sc.defaultTitle()).isEqualTo("My Component");
    }

    @Test
    void resolveThrowsWhenRowMissing() {
        when(componentRepo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> resolver.resolve(99L, "alice"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveThrowsWhenCallerIsNotCreator() {
        User creator = new User();
        creator.setUsername("alice");

        ComponentDefinition cd = new ComponentDefinition();
        cd.setId(7L);
        cd.setOscalUuid("33333333-3333-3333-3333-333333333333");
        cd.setTitle("Mine");
        cd.setStoragePath("build/alice/x.json");
        cd.setFilename("x.json");
        cd.setCreatedBy(creator);

        when(componentRepo.findById(7L)).thenReturn(Optional.of(cd));

        assertThatThrownBy(() -> resolver.resolve(7L, "bob"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void supportedTypeIsComponentDefinition() {
        assertThat(resolver.supportedType()).isEqualTo(SourceType.COMPONENT_DEFINITION);
    }
}
