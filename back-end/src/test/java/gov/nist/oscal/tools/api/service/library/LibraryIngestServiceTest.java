package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.Visibility;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.LibraryTagRepository;
import gov.nist.oscal.tools.api.repository.LibraryVersionRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.LibraryStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LibraryIngestServiceTest {

    @Mock LibraryItemRepository itemRepo;
    @Mock LibraryVersionRepository versionRepo;
    @Mock LibraryStorageService libraryStorage;
    @Mock UserRepository userRepo;
    @Mock OrganizationRepository organizationRepo;
    @Mock LibraryTagRepository libraryTagRepo;
    @Mock SourceContentResolver catalogResolver;

    LibraryIngestService service;

    User caller;
    UUID sourceUuid;
    SourceContent sourceContent;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(catalogResolver.supportedType()).thenReturn(SourceType.CATALOG);
        when(catalogResolver.supportedTypes()).thenReturn(Set.of(SourceType.CATALOG));

        service = new LibraryIngestService(
                itemRepo, versionRepo, libraryStorage,
                List.of(catalogResolver),
                userRepo, organizationRepo, libraryTagRepo);

        caller = new User();
        caller.setId(7L);
        caller.setUsername("alice");

        sourceUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        sourceContent = new SourceContent(
                "{\"x\":1}".getBytes(StandardCharsets.UTF_8),
                "json",
                "cat.json",
                "catalog",
                sourceUuid,
                "My Catalog");
    }

    @Test
    void firstSaveCreatesItemAndVersion1() {
        when(catalogResolver.resolve(42L, "alice")).thenReturn(sourceContent);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(caller));
        when(itemRepo.findByCreatedBy_IdAndSourceTypeAndSourceId(7L, SourceType.CATALOG, sourceUuid))
                .thenReturn(Optional.empty());
        when(itemRepo.save(any(LibraryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepo.save(any(LibraryVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(libraryStorage.buildBlobPath(anyString(), anyString(), anyString()))
                .thenReturn("itemId/versionId/cat.json");
        when(libraryStorage.saveLibraryFile(anyString(), anyString(), anyMap())).thenReturn(true);

        LibraryItem result = service.saveToLibrary(
                SourceType.CATALOG, 42L,
                null, "desc", null,
                Visibility.PRIVATE, null,
                "alice");

        // Item was saved twice — once after creation, once after pointing to current version.
        ArgumentCaptor<LibraryItem> itemCaptor = ArgumentCaptor.forClass(LibraryItem.class);
        verify(itemRepo, times(2)).save(itemCaptor.capture());
        LibraryItem persisted = itemCaptor.getAllValues().get(0);
        assertThat(persisted.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(persisted.getSourceType()).isEqualTo(SourceType.CATALOG);
        assertThat(persisted.getSourceId()).isEqualTo(sourceUuid);
        assertThat(persisted.getOscalType()).isEqualTo("catalog");
        assertThat(persisted.getTitle()).isEqualTo("My Catalog"); // fell back to defaultTitle
        assertThat(persisted.getDescription()).isEqualTo("desc");
        assertThat(persisted.getCreatedBy()).isSameAs(caller);
        assertThat(persisted.getPublishedAt()).isNull();

        // Version 1 saved.
        ArgumentCaptor<LibraryVersion> versionCaptor = ArgumentCaptor.forClass(LibraryVersion.class);
        verify(versionRepo).save(versionCaptor.capture());
        LibraryVersion v = versionCaptor.getValue();
        assertThat(v.getVersionNumber()).isEqualTo(1);
        assertThat(v.getFileName()).isEqualTo("cat.json");
        assertThat(v.getFormat()).isEqualTo("json");
        assertThat(v.getFileSize()).isEqualTo((long) sourceContent.bytes().length);
        assertThat(v.getUploadedBy()).isSameAs(caller);

        // Blob written.
        verify(libraryStorage).saveLibraryFile(eq("{\"x\":1}"), eq("itemId/versionId/cat.json"), anyMap());

        // Returned item points at the new current version.
        assertThat(result.getCurrentVersion()).isNotNull();
        assertThat(result.getCurrentVersion().getVersionNumber()).isEqualTo(1);
    }

    @Test
    void secondSaveAppendsNewVersionToExistingItem() {
        when(catalogResolver.resolve(42L, "alice")).thenReturn(sourceContent);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(caller));

        LibraryItem existing = new LibraryItem(
                "existing-item-uuid", "Existing Title", "Existing description", "catalog", caller);
        existing.setSourceType(SourceType.CATALOG);
        existing.setSourceId(sourceUuid);
        existing.setVisibility(Visibility.PRIVATE);
        LibraryVersion v1 = new LibraryVersion();
        v1.setVersionNumber(1);
        v1.setVersionId("existing-version-uuid");
        existing.setCurrentVersion(v1);

        when(itemRepo.findByCreatedBy_IdAndSourceTypeAndSourceId(7L, SourceType.CATALOG, sourceUuid))
                .thenReturn(Optional.of(existing));
        when(itemRepo.save(any(LibraryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepo.save(any(LibraryVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(libraryStorage.buildBlobPath(anyString(), anyString(), anyString()))
                .thenReturn("itemId/versionId/cat.json");
        when(libraryStorage.saveLibraryFile(anyString(), anyString(), anyMap())).thenReturn(true);

        LibraryItem result = service.saveToLibrary(
                SourceType.CATALOG, 42L,
                null, null, null,
                Visibility.PRIVATE, null,
                "alice");

        // Version 2 saved on append.
        ArgumentCaptor<LibraryVersion> versionCaptor = ArgumentCaptor.forClass(LibraryVersion.class);
        verify(versionRepo).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getVersionNumber()).isEqualTo(2);

        // The same item is returned (we did NOT create a brand-new LibraryItem).
        assertThat(result).isSameAs(existing);
        assertThat(result.getCurrentVersion()).isNotNull();
        assertThat(result.getCurrentVersion().getVersionNumber()).isEqualTo(2);

        // Item is saved twice during append — once after metadata fix-up, once after currentVersion update.
        verify(itemRepo, times(2)).save(any(LibraryItem.class));
    }

    @Test
    void organizationVisibilityRequiresOrgId() {
        when(catalogResolver.resolve(42L, "alice")).thenReturn(sourceContent);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(caller));
        when(itemRepo.findByCreatedBy_IdAndSourceTypeAndSourceId(7L, SourceType.CATALOG, sourceUuid))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveToLibrary(
                SourceType.CATALOG, 42L,
                "Title", null, null,
                Visibility.ORGANIZATION, null,
                "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("organizationId");

        // No save should have happened.
        verify(itemRepo, never()).save(any());
        verify(versionRepo, never()).save(any());
    }

    @Test
    void unknownSourceTypeThrows() {
        // Service was built with only a CATALOG resolver — asking for PROFILE has no resolver.
        assertThatThrownBy(() -> service.saveToLibrary(
                SourceType.PROFILE, 42L,
                "Title", null, null,
                Visibility.PRIVATE, null,
                "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no resolver");
    }

    @Test
    void unknownCallerThrows() {
        when(catalogResolver.resolve(42L, "ghost")).thenReturn(sourceContent);
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveToLibrary(
                SourceType.CATALOG, 42L,
                "Title", null, null,
                Visibility.PRIVATE, null,
                "ghost"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("caller not found");
    }
}
