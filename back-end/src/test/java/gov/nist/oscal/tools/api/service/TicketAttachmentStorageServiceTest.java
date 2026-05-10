package gov.nist.oscal.tools.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketAttachmentStorageServiceTest {

    @Test
    void rejectsOversizeFile() {
        TicketAttachmentStorageService svc = new TicketAttachmentStorageService();
        byte[] eleven_mb = new byte[11 * 1024 * 1024];
        MultipartFile f = new MockMultipartFile("f", "big.png", "image/png", eleven_mb);

        assertThatThrownBy(() -> svc.validate(f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10");
    }

    @Test
    void rejectsForbiddenExtension() {
        TicketAttachmentStorageService svc = new TicketAttachmentStorageService();
        MultipartFile f = new MockMultipartFile("f", "evil.exe", "application/octet-stream", new byte[10]);

        assertThatThrownBy(() -> svc.validate(f))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void acceptsValidPng() {
        TicketAttachmentStorageService svc = new TicketAttachmentStorageService();
        MultipartFile f = new MockMultipartFile("f", "ok.png", "image/png", new byte[100]);
        svc.validate(f); // no throw
    }
}
