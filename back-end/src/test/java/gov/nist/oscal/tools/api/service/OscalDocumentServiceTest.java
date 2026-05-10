package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.OscalDocument;
import gov.nist.oscal.tools.api.entity.OscalModelType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OscalDocumentRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OscalDocumentServiceTest {

    @Mock
    private OscalDocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private OscalDocumentService service;

    private User user;
    private OscalDocument doc;
    private final String uuid = "770e8400-e29b-41d4-a716-446655440000";
    private final String storagePath = "build/testuser/system-security-plan-" + uuid + ".json";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        doc = new OscalDocument(uuid, OscalModelType.SYSTEM_SECURITY_PLAN, "Test SSP", storagePath, user);
        doc.setId(1L);
        doc.setVersion("1.0.0");
        doc.setOscalVersion("1.1.3");
        doc.setFilename("system-security-plan-" + uuid + ".json");
        doc.setFileSize(1024L);
        doc.setLastUpdatedBy(user);
    }

    @Test
    void create_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(documentRepository.findByOscalUuid(uuid)).thenReturn(Optional.empty());
        when(storageService.buildPath("testuser", doc.getFilename())).thenReturn(storagePath);
        when(storageService.getFileSize(storagePath)).thenReturn(1024L);
        when(documentRepository.save(any(OscalDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        OscalDocument result = service.create(
                OscalModelType.SYSTEM_SECURITY_PLAN, "Test SSP", null, "1.0.0", "1.1.3",
                doc.getFilename(), "{\"system-security-plan\":{}}", uuid,
                "{\"controls\":3}", false, "testuser");

        assertNotNull(result);
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, result.getModelType());
        assertEquals("Test SSP", result.getTitle());
        verify(storageService).uploadComponent(eq("testuser"), eq(doc.getFilename()), anyString(), any());
    }

    @Test
    void create_generatesUuidWhenMissing() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(documentRepository.findByOscalUuid(anyString())).thenReturn(Optional.empty());
        when(storageService.buildPath(anyString(), anyString())).thenReturn(storagePath);
        when(storageService.getFileSize(anyString())).thenReturn(1024L);
        when(documentRepository.save(any(OscalDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        OscalDocument result = service.create(
                OscalModelType.ASSESSMENT_PLAN, "Test AP", null, null, "1.1.3",
                "ap.json", "{}", null, null, null, "testuser");

        assertNotNull(result.getOscalUuid());
        assertFalse(result.isDraft());
    }

    @Test
    void create_draftFlag_setsDraft() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(documentRepository.findByOscalUuid(anyString())).thenReturn(Optional.empty());
        when(storageService.buildPath(anyString(), anyString())).thenReturn(storagePath);
        when(storageService.getFileSize(anyString())).thenReturn(1024L);
        when(documentRepository.save(any(OscalDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        OscalDocument result = service.create(
                OscalModelType.PLAN_OF_ACTION_AND_MILESTONES, "POAM", null, null, "1.1.3",
                "poam.json", "{}", null, null, true, "testuser");
        assertTrue(result.isDraft());
    }

    @Test
    void create_duplicateUuid_throws() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(documentRepository.findByOscalUuid(uuid)).thenReturn(Optional.of(doc));

        RuntimeException e = assertThrows(RuntimeException.class, () ->
                service.create(OscalModelType.SYSTEM_SECURITY_PLAN, "x", null, null, "1.1.3",
                        "f.json", "{}", uuid, null, false, "testuser"));
        assertTrue(e.getMessage().contains("already exists"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void create_userNotFound_throws() {
        when(userRepository.findByUsername("nope")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                service.create(OscalModelType.SYSTEM_SECURITY_PLAN, "x", null, null, "1.1.3",
                        "f.json", "{}", uuid, null, false, "nope"));
    }

    @Test
    void update_byCreator_success() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(storageService.getFileSize(anyString())).thenReturn(2048L);
        when(documentRepository.save(any(OscalDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        OscalDocument result = service.update(
                1L, "New title", "newdesc", "2.0.0",
                "{\"system-security-plan\":{}}", "{\"x\":1}", false, "testuser");

        assertEquals("New title", result.getTitle());
        verify(storageService).uploadComponent(eq("testuser"), eq(doc.getFilename()), anyString(), any());
    }

    @Test
    void update_notCreator_throws() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        User other = new User();
        other.setUsername("other");
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

        RuntimeException e = assertThrows(RuntimeException.class, () ->
                service.update(1L, "x", null, null, null, null, null, "other"));
        assertTrue(e.getMessage().contains("Only the creator"));
    }

    @Test
    void update_partialUpdate_skipsNullFields() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(documentRepository.save(any(OscalDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, null, null, null, null, null, null, "testuser");

        verify(storageService, never()).uploadComponent(anyString(), anyString(), anyString(), any());
        verify(documentRepository).save(any(OscalDocument.class));
    }

    @Test
    void update_promoteDraftToFinal() {
        doc.setDraft(true);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(documentRepository.save(any(OscalDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        OscalDocument result = service.update(1L, null, null, null, null, null, false, "testuser");
        assertFalse(result.isDraft());
    }

    @Test
    void getContent_downloadsFromStorage() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(storageService.downloadComponent(storagePath)).thenReturn("{\"x\":1}");

        assertEquals("{\"x\":1}", service.getContent(1L));
    }

    @Test
    void listByUserAndType_passesType() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(documentRepository.findByUserAndType(user, OscalModelType.SYSTEM_SECURITY_PLAN))
                .thenReturn(List.of(doc));

        List<OscalDocument> result = service.listByUserAndType("testuser", OscalModelType.SYSTEM_SECURITY_PLAN);
        assertEquals(1, result.size());
    }

    @Test
    void search_emptyTerm_listsByType() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(documentRepository.findByUserAndType(user, OscalModelType.ASSESSMENT_RESULTS))
                .thenReturn(List.of(doc));

        List<OscalDocument> result = service.search("testuser", OscalModelType.ASSESSMENT_RESULTS, "  ");
        assertEquals(1, result.size());
        verify(documentRepository, never()).searchByUserAndType(any(), any(), anyString());
    }

    @Test
    void search_withTerm_callsSearch() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(documentRepository.searchByUserAndType(user, OscalModelType.SYSTEM_SECURITY_PLAN, "prod"))
                .thenReturn(List.of(doc));

        List<OscalDocument> result = service.search("testuser", OscalModelType.SYSTEM_SECURITY_PLAN, "prod");
        assertEquals(1, result.size());
    }

    @Test
    void delete_byCreator_succeeds() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        service.delete(1L, "testuser");

        verify(storageService).deleteComponent(storagePath);
        verify(documentRepository).delete(doc);
    }

    @Test
    void delete_notCreator_throws() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

        assertThrows(RuntimeException.class, () -> service.delete(1L, "other"));
        verify(documentRepository, never()).delete(any());
    }

    @Test
    void modelType_slugRoundTrip() {
        for (OscalModelType t : OscalModelType.values()) {
            assertEquals(t, OscalModelType.fromSlug(t.slug()));
        }
    }

    @Test
    void modelType_fromSlug_acceptsCommonAliases() {
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, OscalModelType.fromSlug("ssp"));
        assertEquals(OscalModelType.ASSESSMENT_PLAN, OscalModelType.fromSlug("ap"));
        assertEquals(OscalModelType.ASSESSMENT_RESULTS, OscalModelType.fromSlug("ar"));
        assertEquals(OscalModelType.PLAN_OF_ACTION_AND_MILESTONES, OscalModelType.fromSlug("poam"));
    }

    @Test
    void modelType_fromSlug_unknown_throws() {
        assertThrows(IllegalArgumentException.class, () -> OscalModelType.fromSlug("unknown"));
    }
}
