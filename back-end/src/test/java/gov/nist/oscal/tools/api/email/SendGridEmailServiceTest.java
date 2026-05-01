package gov.nist.oscal.tools.api.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import gov.nist.oscal.tools.api.entity.Invitation;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SendGridEmailServiceTest {

    private SendGrid sendGrid;
    private EmailAuditLogger audit;
    private TemplateRenderer renderer;
    private SendGridEmailService service;

    @BeforeEach
    void setUp() throws IOException {
        sendGrid = mock(SendGrid.class);
        audit = mock(EmailAuditLogger.class);
        renderer = new TemplateRenderer();
        when(sendGrid.api(any())).thenReturn(okResponse());

        service = new SendGridEmailService(
            "fake-key", "noreply@oscalhub.local", "OSCAL Hub",
            "http://localhost:3000", renderer, audit);
        service.setClientForTesting(sendGrid);
    }

    private Response okResponse() {
        Response ok = new Response();
        ok.setStatusCode(202);
        ok.setHeaders(Collections.singletonMap("X-Message-Id", "msg-1"));
        return ok;
    }

    @Test
    void sendsWelcomeEmailAndRecordsSuccess() throws IOException {
        User u = new User();
        u.setUsername("travis");
        u.setEmail("t@example.com");

        service.sendWelcome(u);

        verify(sendGrid, times(1)).api(any());
        verify(audit, times(1)).recordSuccess(eq("welcome"), eq("t@example.com"), any());
    }

    @Test
    void swallowsAndLogsSendFailure() throws IOException {
        when(sendGrid.api(any())).thenThrow(new IOException("boom"));
        User u = new User();
        u.setUsername("travis");
        u.setEmail("t@example.com");

        assertDoesNotThrow(() -> service.sendWelcome(u));
        verify(audit, times(1)).recordFailure(eq("welcome"), eq("t@example.com"), any());
    }

    @Test
    void buildsInvitationLinkWithToken() throws IOException {
        Organization org = new Organization();
        org.setName("Acme");
        User inviter = new User();
        inviter.setUsername("admin");
        inviter.setEmail("admin@acme.com");
        Invitation inv = mock(Invitation.class);
        when(inv.getEmail()).thenReturn("teammate@example.com");
        when(inv.getToken()).thenReturn("tok-123");

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        when(sendGrid.api(captor.capture())).thenReturn(okResponse());

        service.sendInvitation(inv, inviter, org);

        String body = captor.getValue().getBody();
        assertTrue(body.contains("tok-123"), "body should contain token: " + body);
        assertTrue(body.contains("/accept-invite?token=tok-123"));
    }
}
