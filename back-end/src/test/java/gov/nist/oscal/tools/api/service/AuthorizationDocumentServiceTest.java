package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationDocument;
import gov.nist.oscal.tools.api.entity.DocumentType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.UnsupportedDocumentTypeException;
import gov.nist.oscal.tools.api.repository.AuthorizationDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationDocumentServiceTest {

    @Mock AuthorizationDocumentRepository repository;
    @Mock FileStorageService fileStorageService;

    @InjectMocks
    AuthorizationDocumentService service;

    Authorization auth;
    User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");

        auth = new Authorization();
        auth.setId(42L);
    }

    @Test
    void upload_pdf_persistsMetadataAndStoresBytes() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "pen-test.pdf", "application/pdf", "PDF body".getBytes());
        when(repository.save(any(AuthorizationDocument.class)))
                .thenAnswer(inv -> {
                    AuthorizationDocument doc = inv.getArgument(0);
                    doc.setId(99L);
                    return doc;
                });
        when(fileStorageService.saveBinary(anyString(), any(byte[].class), anyString()))
                .thenAnswer(inv -> inv.getArgument(0));

        AuthorizationDocument saved = service.upload(
                auth, alice, file, DocumentType.PENETRATION_TEST,
                "Q3 pen test", "internal,external", "v1", null, null);

        assertThat(saved.getId()).isEqualTo(99L);
        assertThat(saved.getOriginalFilename()).isEqualTo("pen-test.pdf");
        assertThat(saved.getContentType()).isEqualTo("application/pdf");
        assertThat(saved.getDocumentType()).isEqualTo(DocumentType.PENETRATION_TEST);
        assertThat(saved.getStoragePath()).startsWith("authorizations/42/documents/");
        assertThat(saved.getStoragePath()).endsWith("-pen-test.pdf");
        verify(fileStorageService).saveBinary(anyString(), any(byte[].class), anyString());
    }

    @Test
    void upload_executableContentType_rejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.exe", "application/x-msdownload", "MZ".getBytes());

        assertThatThrownBy(() -> service.upload(
                auth, alice, file, DocumentType.OTHER, null, null, null, null, null))
                .isInstanceOf(UnsupportedDocumentTypeException.class);
    }

    @Test
    void upload_emptyFile_rejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.upload(
                auth, alice, file, DocumentType.OTHER, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void download_returnsBytesAndMetadata() {
        AuthorizationDocument doc = new AuthorizationDocument();
        doc.setStoragePath("authorizations/42/documents/abc-x.pdf");
        doc.setContentType("application/pdf");
        doc.setOriginalFilename("x.pdf");
        when(fileStorageService.loadBinary("authorizations/42/documents/abc-x.pdf"))
                .thenReturn("PDF body".getBytes());

        byte[] bytes = service.download(doc);

        assertThat(bytes).isEqualTo("PDF body".getBytes());
    }

    @Test
    void delete_removesRowAndBlob() {
        AuthorizationDocument doc = new AuthorizationDocument();
        doc.setId(99L);
        doc.setStoragePath("authorizations/42/documents/abc-x.pdf");

        service.delete(doc);

        verify(repository).delete(doc);
        verify(fileStorageService).deleteBinary("authorizations/42/documents/abc-x.pdf");
    }
}
