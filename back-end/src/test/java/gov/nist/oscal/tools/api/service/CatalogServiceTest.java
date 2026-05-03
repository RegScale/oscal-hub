package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Catalog;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.CatalogRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogServiceTest {

    @Mock
    private CatalogRepository catalogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private CatalogService catalogService;

    private User mockUser;
    private Catalog mockCatalog;
    private final String testUuid = "550e8400-e29b-41d4-a716-446655440000";
    private final String storagePath = "build/testuser/catalog-" + testUuid + ".json";

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        mockCatalog = new Catalog(testUuid, "Test Catalog", storagePath, mockUser);
        mockCatalog.setId(1L);
        mockCatalog.setDescription("desc");
        mockCatalog.setVersion("1.0.0");
        mockCatalog.setOscalVersion("1.1.3");
        mockCatalog.setFilename("catalog-" + testUuid + ".json");
        mockCatalog.setFileSize(2048L);
        mockCatalog.setGroupCount(2);
        mockCatalog.setControlCount(15);
        mockCatalog.setParamCount(3);
        mockCatalog.setLastUpdatedBy(mockUser);
    }

    @Test
    void create_success_persistsAndUploads() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(catalogRepository.findByOscalUuid(testUuid)).thenReturn(Optional.empty());
        when(storageService.buildPath("testuser", mockCatalog.getFilename())).thenReturn(storagePath);
        when(storageService.getFileSize(storagePath)).thenReturn(2048L);
        when(catalogRepository.save(any(Catalog.class))).thenReturn(mockCatalog);

        Catalog result = catalogService.createCatalog(
                "Test Catalog", "desc", "1.0.0", "1.1.3",
                mockCatalog.getFilename(), "{\"catalog\":{}}", testUuid,
                2, 15, 3, false, "testuser");

        assertNotNull(result);
        assertEquals("Test Catalog", result.getTitle());
        verify(storageService).uploadComponent(eq("testuser"), eq(mockCatalog.getFilename()), anyString(), any());
        verify(catalogRepository).save(any(Catalog.class));
    }

    @Test
    void create_generatesUuidWhenMissing() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(catalogRepository.findByOscalUuid(anyString())).thenReturn(Optional.empty());
        when(storageService.buildPath(anyString(), anyString())).thenReturn(storagePath);
        when(storageService.getFileSize(anyString())).thenReturn(2048L);
        when(catalogRepository.save(any(Catalog.class))).thenReturn(mockCatalog);

        Catalog result = catalogService.createCatalog(
                "Test", null, null, "1.1.3",
                "file.json", "{}", null,
                0, 0, 0, false, "testuser");

        assertNotNull(result);
        verify(catalogRepository).save(any(Catalog.class));
    }

    @Test
    void create_userNotFound_throws() {
        when(userRepository.findByUsername("nope")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                catalogService.createCatalog("t", null, null, "1.1.3",
                        "f.json", "{}", testUuid, 0, 0, 0, false, "nope"));
        verify(catalogRepository, never()).save(any());
    }

    @Test
    void create_duplicateUuid_throwsAndDoesNotSave() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(catalogRepository.findByOscalUuid(testUuid)).thenReturn(Optional.of(mockCatalog));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                catalogService.createCatalog("t", null, null, "1.1.3",
                        "f.json", "{}", testUuid, 0, 0, 0, false, "testuser"));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(catalogRepository, never()).save(any());
    }

    @Test
    void update_success_replacesContent() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(mockCatalog));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(storageService.getFileSize(anyString())).thenReturn(4096L);
        when(catalogRepository.save(any(Catalog.class))).thenReturn(mockCatalog);

        Catalog result = catalogService.updateCatalog(
                1L, "New Title", "newdesc", "2.0.0",
                "{\"catalog\":{\"updated\":true}}", 3, 20, 4, null, "testuser");

        assertNotNull(result);
        verify(storageService).uploadComponent(eq("testuser"), eq(mockCatalog.getFilename()), anyString(), any());
        verify(catalogRepository).save(any(Catalog.class));
    }

    @Test
    void update_notCreator_throwsForbidden() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(mockCatalog));
        User other = new User();
        other.setUsername("other");
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                catalogService.updateCatalog(1L, "New", null, null, null, null, null, null, null, "other"));
        assertTrue(ex.getMessage().contains("Only the creator"));
    }

    @Test
    void update_partialUpdate_skipsNullFields() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(mockCatalog));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(catalogRepository.save(any(Catalog.class))).thenReturn(mockCatalog);

        catalogService.updateCatalog(1L, null, null, null, null, null, null, null, null, "testuser");

        verify(storageService, never()).uploadComponent(anyString(), anyString(), anyString(), any());
        verify(catalogRepository).save(any(Catalog.class));
    }

    @Test
    void update_promoteDraftToFinal() {
        mockCatalog.setDraft(true);
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(mockCatalog));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(catalogRepository.save(any(Catalog.class))).thenAnswer(inv -> inv.getArgument(0));

        Catalog result = catalogService.updateCatalog(
                1L, null, null, null, null, null, null, null, false, "testuser");

        assertFalse(result.isDraft());
    }

    @Test
    void getCatalog_returnsEntity() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(mockCatalog));
        assertSame(mockCatalog, catalogService.getCatalog(1L));
    }

    @Test
    void getCatalog_missing_throws() {
        when(catalogRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> catalogService.getCatalog(99L));
    }

    @Test
    void getCatalogContent_downloadsFromStorage() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(mockCatalog));
        when(storageService.downloadComponent(storagePath)).thenReturn("{\"catalog\":{}}");

        String content = catalogService.getCatalogContent(1L);

        assertEquals("{\"catalog\":{}}", content);
        verify(storageService).downloadComponent(storagePath);
    }

    @Test
    void getUserCatalogs_returnsList() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(catalogRepository.findByCreatedByOrderByCreatedAtDesc(mockUser))
                .thenReturn(List.of(mockCatalog));

        List<Catalog> result = catalogService.getUserCatalogs("testuser");
        assertEquals(1, result.size());
    }

    @Test
    void searchCatalogs_emptyTerm_returnsAll() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(catalogRepository.findByCreatedByOrderByCreatedAtDesc(mockUser))
                .thenReturn(List.of(mockCatalog));

        List<Catalog> result = catalogService.searchCatalogs("testuser", "  ");
        assertEquals(1, result.size());
        verify(catalogRepository, never()).findByCreatedByAndSearch(any(), anyString());
    }

    @Test
    void searchCatalogs_withTerm_callsSearch() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(catalogRepository.findByCreatedByAndSearch(mockUser, "AC"))
                .thenReturn(List.of(mockCatalog));

        List<Catalog> result = catalogService.searchCatalogs("testuser", "AC");
        assertEquals(1, result.size());
    }

    @Test
    void deleteCatalog_byCreator_succeeds() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(mockCatalog));

        catalogService.deleteCatalog(1L, "testuser");

        verify(storageService).deleteComponent(storagePath);
        verify(catalogRepository).delete(mockCatalog);
    }

    @Test
    void deleteCatalog_notCreator_throws() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(mockCatalog));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                catalogService.deleteCatalog(1L, "other"));
        assertTrue(ex.getMessage().contains("Only the creator"));
        verify(catalogRepository, never()).delete(any());
    }

    @Test
    void getStatistics_aggregatesCounts() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(catalogRepository.findByCreatedBy(mockUser)).thenReturn(List.of(mockCatalog));

        Map<String, Object> stats = catalogService.getStatistics("testuser");

        assertEquals(1, stats.get("totalCatalogs"));
        assertEquals(15, stats.get("totalControls"));
        assertEquals(2, stats.get("totalGroups"));
        assertEquals(2048L, stats.get("totalStorageBytes"));
    }

    @Test
    void getStatistics_handlesEmpty() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(catalogRepository.findByCreatedBy(mockUser)).thenReturn(Collections.emptyList());

        Map<String, Object> stats = catalogService.getStatistics("testuser");
        assertEquals(0, stats.get("totalCatalogs"));
        assertEquals(0, stats.get("totalControls"));
    }

    @Test
    void getCatalogByUuid_returnsEntity() {
        when(catalogRepository.findByOscalUuid(testUuid)).thenReturn(Optional.of(mockCatalog));
        Catalog result = catalogService.getCatalogByUuid(testUuid);
        assertSame(mockCatalog, result);
    }

    @Test
    void getCatalogByUuid_missing_throws() {
        when(catalogRepository.findByOscalUuid("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> catalogService.getCatalogByUuid("missing"));
    }
}
