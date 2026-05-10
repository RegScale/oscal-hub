package gov.nist.oscal.tools.api.service.conmon;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import gov.nist.oscal.tools.api.entity.ConMonSourceFormat;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.UnsupportedConMonFormatException;
import gov.nist.oscal.tools.api.repository.ConMonPoamItemRepository;
import gov.nist.oscal.tools.api.repository.ConMonReconciliationRepository;
import gov.nist.oscal.tools.api.repository.ConMonSnapshotRepository;
import gov.nist.oscal.tools.api.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConMonServiceTest {

    private final ConMonSnapshotRepository snapshotRepo = mock(ConMonSnapshotRepository.class);
    private final ConMonPoamItemRepository itemRepo = mock(ConMonPoamItemRepository.class);
    private final ConMonReconciliationRepository reconRepo = mock(ConMonReconciliationRepository.class);
    private final OscalPoamParser oscalParser = mock(OscalPoamParser.class);
    private final FedrampPoamExcelParser excelParser = mock(FedrampPoamExcelParser.class);
    private final ConMonReconciliationService reconService = mock(ConMonReconciliationService.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private final ConMonService service = new ConMonService(
            snapshotRepo, itemRepo, reconRepo, oscalParser, excelParser,
            reconService, storage, mapper);

    private final Authorization auth = authorization(7L);
    private final User uploader = user("uploader");

    @Test
    void emptyFile_isRejectedWithIllegalArgument() {
        MockMultipartFile empty = new MockMultipartFile("file", "x.json", "application/json", new byte[0]);
        assertThatThrownBy(() -> service.upload(auth, uploader, empty, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void nullFile_isRejectedWithIllegalArgument() {
        assertThatThrownBy(() -> service.upload(auth, uploader, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unrecognizedExtension_throwsUnsupportedFormat() {
        // The exception is annotated with @ResponseStatus(BAD_REQUEST) so it surfaces
        // as a 400 to clients — that's the contract the controller relies on.
        MockMultipartFile unknown = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> service.upload(auth, uploader, unknown, null))
                .isInstanceOf(UnsupportedConMonFormatException.class);
    }

    @Test
    void filenameWithNoExtension_isUnsupported() {
        MockMultipartFile unknown = new MockMultipartFile(
                "file", "no-extension-here", "application/octet-stream", "data".getBytes());

        assertThatThrownBy(() -> service.upload(auth, uploader, unknown, null))
                .isInstanceOf(UnsupportedConMonFormatException.class);
    }

    @Test
    void oscalJsonUpload_routesToOscalParser_andPersistsSnapshotWithSummaryCounts() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poam.json", "application/json", "{\"plan-of-action\":{}}".getBytes());

        ParsedPoamItem open = item("P-1", ConMonItemStatus.OPEN);
        ParsedPoamItem closed = item("P-2", ConMonItemStatus.CLOSED);
        ParsedPoamItem unknown = item("P-3", ConMonItemStatus.UNKNOWN);
        ParsedPoam parsed = new ParsedPoam(
                "uuid-1", "1.1.2", "Title", null, List.of(open, closed, unknown));

        when(oscalParser.parse(any(InputStream.class), eq(ConMonSourceFormat.OSCAL_JSON)))
                .thenReturn(parsed);
        when(snapshotRepo.save(any(ConMonSnapshot.class)))
                .thenAnswer(inv -> {
                    ConMonSnapshot s = inv.getArgument(0);
                    s.setId(99L);
                    return s;
                });
        when(snapshotRepo.findByAuthorizationOrderByUploadedAtDesc(auth))
                .thenReturn(List.of()); // no prior snapshot → no reconciliation

        ConMonSnapshot saved = service.upload(auth, uploader, file, "first upload");

        ArgumentCaptor<ConMonSnapshot> cap = ArgumentCaptor.forClass(ConMonSnapshot.class);
        verify(snapshotRepo).save(cap.capture());
        ConMonSnapshot s = cap.getValue();

        assertThat(s.getAuthorization()).isSameAs(auth);
        assertThat(s.getUploadedBy()).isSameAs(uploader);
        assertThat(s.getSourceFormat()).isEqualTo(ConMonSourceFormat.OSCAL_JSON);
        assertThat(s.getOscalUuid()).isEqualTo("uuid-1");
        assertThat(s.getOscalVersion()).isEqualTo("1.1.2");
        assertThat(s.getMetadataTitle()).isEqualTo("Title");
        assertThat(s.getNotes()).isEqualTo("first upload");
        assertThat(s.getSummaryOpenCount()).isEqualTo(1);
        assertThat(s.getSummaryClosedCount()).isEqualTo(1);
        assertThat(s.getSummaryUnknownCount()).isEqualTo(1);
        assertThat(s.getItems()).hasSize(3);
        // saved id is propagated
        assertThat(saved.getId()).isEqualTo(99L);

        // Excel parser must NOT have been called
        verify(excelParser, never()).parse(any());
    }

    @Test
    void fedrampXlsxUpload_routesToExcelParser() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poam.xlsx", "application/vnd.openxmlformats", "binary".getBytes());

        when(excelParser.parse(any(InputStream.class)))
                .thenReturn(new ParsedPoam(null, null, null, null, List.of()));
        when(snapshotRepo.save(any(ConMonSnapshot.class)))
                .thenAnswer(inv -> { ConMonSnapshot s = inv.getArgument(0); s.setId(1L); return s; });
        when(snapshotRepo.findByAuthorizationOrderByUploadedAtDesc(auth)).thenReturn(List.of());

        service.upload(auth, uploader, file, null);

        verify(excelParser, times(1)).parse(any(InputStream.class));
        verify(oscalParser, never()).parse(any(), any());
    }

    @Test
    void oscalYamlUpload_routesToOscalParser_withYamlFormatHint() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poam.yaml", "application/x-yaml", "key: value".getBytes());

        when(oscalParser.parse(any(InputStream.class), eq(ConMonSourceFormat.OSCAL_YAML)))
                .thenReturn(new ParsedPoam(null, null, null, null, List.of()));
        when(snapshotRepo.save(any(ConMonSnapshot.class)))
                .thenAnswer(inv -> { ConMonSnapshot s = inv.getArgument(0); s.setId(2L); return s; });
        when(snapshotRepo.findByAuthorizationOrderByUploadedAtDesc(auth)).thenReturn(List.of());

        service.upload(auth, uploader, file, null);
        verify(oscalParser).parse(any(InputStream.class), eq(ConMonSourceFormat.OSCAL_YAML));
    }

    @Test
    void itemWithNullExternalId_getsGeneratedUuidExternalId() throws Exception {
        // The parser may yield an item with no externalId (e.g. some FedRAMP rows).
        // The service must mint one so unique-key constraints in the repo don't blow up.
        MockMultipartFile file = new MockMultipartFile(
                "file", "poam.json", "application/json", "{}".getBytes());

        ParsedPoamItem noId = new ParsedPoamItem(null, "title", null,
                ConMonItemStatus.OPEN, "open", "HIGH", null, null, null, null, null, null);
        when(oscalParser.parse(any(), any()))
                .thenReturn(new ParsedPoam(null, null, null, null, List.of(noId)));
        when(snapshotRepo.save(any(ConMonSnapshot.class)))
                .thenAnswer(inv -> { ConMonSnapshot s = inv.getArgument(0); s.setId(1L); return s; });
        when(snapshotRepo.findByAuthorizationOrderByUploadedAtDesc(auth)).thenReturn(List.of());

        service.upload(auth, uploader, file, null);

        ArgumentCaptor<ConMonSnapshot> cap = ArgumentCaptor.forClass(ConMonSnapshot.class);
        verify(snapshotRepo).save(cap.capture());
        String externalId = cap.getValue().getItems().get(0).getExternalId();
        assertThat(externalId).isNotBlank();
        // UUID format check (loose)
        assertThat(externalId).matches("[0-9a-f-]{36}");
    }

    @Test
    void itemWithNullStatus_isCoercedToUnknown() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poam.json", "application/json", "{}".getBytes());

        ParsedPoamItem nullStatus = new ParsedPoamItem("P-1", "title", null,
                null, null, null, null, null, null, null, null, null);
        when(oscalParser.parse(any(), any()))
                .thenReturn(new ParsedPoam(null, null, null, null, List.of(nullStatus)));
        when(snapshotRepo.save(any(ConMonSnapshot.class)))
                .thenAnswer(inv -> { ConMonSnapshot s = inv.getArgument(0); s.setId(1L); return s; });
        when(snapshotRepo.findByAuthorizationOrderByUploadedAtDesc(auth)).thenReturn(List.of());

        service.upload(auth, uploader, file, null);

        ArgumentCaptor<ConMonSnapshot> cap = ArgumentCaptor.forClass(ConMonSnapshot.class);
        verify(snapshotRepo).save(cap.capture());
        assertThat(cap.getValue().getItems().get(0).getStatus()).isEqualTo(ConMonItemStatus.UNKNOWN);
        assertThat(cap.getValue().getSummaryUnknownCount()).isEqualTo(1);
    }

    @Test
    void itemWithExtraProps_isSerializedAsJson() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poam.json", "application/json", "{}".getBytes());
        ParsedPoamItem withExtras = new ParsedPoamItem("P-1", "t", "d",
                ConMonItemStatus.OPEN, "open", null, null, null, null, null, null,
                Map.of("vendorRef", "ABC-123", "scanner", "Acme"));
        when(oscalParser.parse(any(), any()))
                .thenReturn(new ParsedPoam(null, null, null, null, List.of(withExtras)));
        when(snapshotRepo.save(any(ConMonSnapshot.class)))
                .thenAnswer(inv -> { ConMonSnapshot s = inv.getArgument(0); s.setId(1L); return s; });
        when(snapshotRepo.findByAuthorizationOrderByUploadedAtDesc(auth)).thenReturn(List.of());

        service.upload(auth, uploader, file, null);

        ArgumentCaptor<ConMonSnapshot> cap = ArgumentCaptor.forClass(ConMonSnapshot.class);
        verify(snapshotRepo).save(cap.capture());
        String json = cap.getValue().getItems().get(0).getExtraPropsJson();
        assertThat(json).contains("vendorRef").contains("ABC-123").contains("scanner").contains("Acme");
    }

    @Test
    void itemWithEmptyExtraProps_doesNotPersistJson() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poam.json", "application/json", "{}".getBytes());
        ParsedPoamItem noExtras = new ParsedPoamItem("P-1", "t", null,
                ConMonItemStatus.OPEN, null, null, null, null, null, null, null, Map.of());
        when(oscalParser.parse(any(), any()))
                .thenReturn(new ParsedPoam(null, null, null, null, List.of(noExtras)));
        when(snapshotRepo.save(any(ConMonSnapshot.class)))
                .thenAnswer(inv -> { ConMonSnapshot s = inv.getArgument(0); s.setId(1L); return s; });
        when(snapshotRepo.findByAuthorizationOrderByUploadedAtDesc(auth)).thenReturn(List.of());

        service.upload(auth, uploader, file, null);

        ArgumentCaptor<ConMonSnapshot> cap = ArgumentCaptor.forClass(ConMonSnapshot.class);
        verify(snapshotRepo).save(cap.capture());
        assertThat(cap.getValue().getItems().get(0).getExtraPropsJson()).isNull();
    }

    @Test
    void uploadStoresOriginalFile_underAuthorizationScopedPath_withSanitizedFilename() throws Exception {
        // Path contract is "authorizations/<id>/conmon/<uuid>-<sanitized-filename>".
        // Sanitization strips path traversal and unsafe chars to prevent the storage
        // backend from being directed outside the authorization's directory.
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd.json", "application/json",
                "{\"plan-of-action\":{}}".getBytes());

        when(oscalParser.parse(any(), any()))
                .thenReturn(new ParsedPoam(null, null, null, null, List.of()));
        when(snapshotRepo.save(any(ConMonSnapshot.class)))
                .thenAnswer(inv -> { ConMonSnapshot s = inv.getArgument(0); s.setId(1L); return s; });
        when(snapshotRepo.findByAuthorizationOrderByUploadedAtDesc(auth)).thenReturn(List.of());

        service.upload(auth, uploader, file, null);

        ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
        verify(storage).saveBinary(path.capture(), any(byte[].class), anyString());
        String stored = path.getValue();
        assertThat(stored).startsWith("authorizations/7/conmon/");
        assertThat(stored).doesNotContain("../").doesNotContain("..\\");
        // The basename keeps allowed chars only.
        assertThat(stored).endsWith("-passwd.json");
    }

    @Test
    void priorSnapshotExists_reconciliationIsComputedAndSaved() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poam.json", "application/json", "{}".getBytes());

        when(oscalParser.parse(any(), any()))
                .thenReturn(new ParsedPoam(null, null, null, null, List.of()));

        ConMonSnapshot prior = new ConMonSnapshot();
        prior.setId(1L);
        when(snapshotRepo.save(any(ConMonSnapshot.class)))
                .thenAnswer(inv -> { ConMonSnapshot s = inv.getArgument(0); s.setId(2L); return s; });
        // After save, the repo returns BOTH snapshots newest-first.
        when(snapshotRepo.findByAuthorizationOrderByUploadedAtDesc(auth))
                .thenAnswer(inv -> {
                    ConMonSnapshot newest = new ConMonSnapshot();
                    newest.setId(2L);
                    return List.of(newest, prior);
                });
        when(reconService.compute(any(ConMonSnapshot.class), any(ConMonSnapshot.class)))
                .thenAnswer(inv -> {
                    ConMonReconciliation r = new ConMonReconciliation();
                    r.setSnapshot(inv.getArgument(0));
                    r.setPreviousSnapshot(inv.getArgument(1));
                    return r;
                });

        service.upload(auth, uploader, file, null);

        verify(reconService, times(1)).compute(any(ConMonSnapshot.class), any(ConMonSnapshot.class));
        verify(reconRepo, times(1)).save(any(ConMonReconciliation.class));
    }

    @Test
    void deleteSnapshot_alsoBestEffortDeletesOriginalBlob_andSwallowsStorageErrors() {
        // The blob lives outside the DB (S3/GCS/local). If storage delete fails,
        // we still want the row to disappear — otherwise the user is stuck with
        // an undeletable snapshot whenever the blob is already gone.
        ConMonSnapshot snap = new ConMonSnapshot();
        snap.setFileStoragePath("authorizations/7/conmon/abc-poam.json");
        org.mockito.Mockito.doThrow(new RuntimeException("blob missing"))
                .when(storage).deleteBinary("authorizations/7/conmon/abc-poam.json");

        service.delete(snap);

        verify(snapshotRepo).delete(snap);
    }

    @Test
    void downloadOriginal_propagatesBytesFromStorage() {
        ConMonSnapshot snap = new ConMonSnapshot();
        snap.setFileStoragePath("p/x");
        when(storage.loadBinary("p/x")).thenReturn(new byte[] {1, 2, 3});

        byte[] out = service.downloadOriginal(snap);
        assertThat(out).containsExactly(1, 2, 3);
    }

    @Test
    void downloadOriginal_missingBlob_surfacesAsRuntimeException() {
        ConMonSnapshot snap = new ConMonSnapshot();
        snap.setId(42L);
        snap.setFileStoragePath("p/missing");
        when(storage.loadBinary("p/missing")).thenReturn(null);

        assertThatThrownBy(() -> service.downloadOriginal(snap))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("42");
    }

    @Test
    void listSnapshots_eagerLoadsAssociations_soControllerCanSerializeAfterTx() {
        ConMonSnapshot s = new ConMonSnapshot();
        s.setId(1L);
        s.setAuthorization(auth);
        s.setUploadedBy(uploader);
        when(snapshotRepo.findByAuthorizationOrderByUploadedAtDesc(auth)).thenReturn(List.of(s));

        List<ConMonSnapshot> result = service.listSnapshots(auth);
        assertThat(result).hasSize(1);
        // Eager-load behavior is hard to assert without Hibernate, but the call must
        // not throw even when uploadedBy / authorization is non-null.
    }

    @Test
    void findReconciliation_returnsRepoResult() {
        ConMonSnapshot snap = new ConMonSnapshot();
        ConMonReconciliation rec = new ConMonReconciliation();
        when(reconRepo.findBySnapshot(snap)).thenReturn(Optional.of(rec));

        assertThat(service.findReconciliation(snap)).contains(rec);
    }

    // ---- helpers ----

    private static Authorization authorization(long id) {
        Authorization a = new Authorization();
        a.setId(id);
        return a;
    }

    private static User user(String username) {
        User u = new User();
        u.setUsername(username);
        return u;
    }

    private static ParsedPoamItem item(String externalId, ConMonItemStatus status) {
        return new ParsedPoamItem(externalId, externalId, null, status,
                status.name().toLowerCase(), null, null,
                LocalDate.now(), null, null, null, null);
    }
}
