package gov.nist.oscal.tools.api.email;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import gov.nist.oscal.tools.api.entity.Invitation;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendGridEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(SendGridEmailService.class);

    private final String fromEmail;
    private final String fromName;
    private final String baseUrl;
    private final TemplateRenderer renderer;
    private final EmailAuditLogger audit;
    private SendGrid client;

    public SendGridEmailService(String apiKey, String fromEmail, String fromName,
                                 String baseUrl, TemplateRenderer renderer, EmailAuditLogger audit) {
        this.client = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.baseUrl = baseUrl;
        this.renderer = renderer;
        this.audit = audit;
    }

    // For tests
    void setClientForTesting(SendGrid client) { this.client = client; }

    @Override
    public void sendWelcome(User user) {
        Map<String, String> vars = new HashMap<>();
        vars.put("username", user.getUsername());
        vars.put("loginUrl", baseUrl + "/login");
        send("welcome", user.getEmail(), "Welcome to OSCAL Hub", vars);
    }

    @Override
    public void sendAccessRequestAcknowledged(UserAccessRequest r) {
        Map<String, String> vars = new HashMap<>();
        vars.put("firstName", nullSafe(r.getFirstName()));
        vars.put("orgName", r.getOrganization() == null ? "" : r.getOrganization().getName());
        send("access-request-acknowledged", r.getEmail(),
             "Your access request was received", vars);
    }

    @Override
    public void sendAccessRequestPendingForAdmins(UserAccessRequest r, List<User> admins) {
        for (User admin : admins) {
            Map<String, String> vars = new HashMap<>();
            vars.put("requesterName", nullSafe(r.getFirstName()) + " " + nullSafe(r.getLastName()));
            vars.put("requesterEmail", r.getEmail());
            vars.put("orgName", r.getOrganization() == null ? "" : r.getOrganization().getName());
            vars.put("message", nullSafe(r.getMessage()));
            vars.put("requestsUrl", baseUrl + "/org-admin/requests");
            send("access-request-pending-admin", admin.getEmail(),
                 "New access request for your organization", vars);
        }
    }

    @Override
    public void sendAccessRequestApproved(UserAccessRequest r, User approver) {
        Map<String, String> vars = new HashMap<>();
        vars.put("orgName", r.getOrganization() == null ? "" : r.getOrganization().getName());
        vars.put("approverName", approver.getUsername());
        vars.put("loginUrl", baseUrl + "/login");
        send("access-request-approved", r.getEmail(),
             "Your access request was approved", vars);
    }

    @Override
    public void sendAccessRequestRejected(UserAccessRequest r, User rejector, String reason) {
        Map<String, String> vars = new HashMap<>();
        vars.put("orgName", r.getOrganization() == null ? "" : r.getOrganization().getName());
        vars.put("reason", nullSafe(reason));
        send("access-request-rejected", r.getEmail(),
             "Your access request was not approved", vars);
    }

    @Override
    public void sendInvitation(Invitation inv, User inviter, Organization org) {
        Map<String, String> vars = new HashMap<>();
        vars.put("orgName", org.getName());
        vars.put("inviterName", inviter.getUsername());
        vars.put("acceptUrl", baseUrl + "/accept-invite?token=" + inv.getToken());
        vars.put("expiresAt", String.valueOf(inv.getExpiresAt()));
        send("invitation", inv.getEmail(),
             inviter.getUsername() + " invited you to join " + org.getName(), vars);
    }

    private void send(String template, String to, String subject, Map<String, String> vars) {
        try {
            String html = renderer.renderFromClasspath("email-templates/" + template + ".html", vars);
            String text = renderer.renderTextFromClasspath("email-templates/" + template + ".txt", vars);

            Mail mail = new Mail();
            mail.setFrom(new Email(fromEmail, fromName));
            mail.setSubject(subject);
            Personalization p = new Personalization();
            p.addTo(new Email(to));
            mail.addPersonalization(p);
            mail.addContent(new Content("text/plain", text));
            mail.addContent(new Content("text/html", html));

            Request req = new Request();
            req.setMethod(Method.POST);
            req.setEndpoint("mail/send");
            req.setBody(mail.build());

            Response resp = client.api(req);
            int statusCode = resp.getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException("SendGrid returned status " + statusCode + ": " + resp.getBody());
            }
            String messageId = resp.getHeaders() == null ? null : resp.getHeaders().get("X-Message-Id");
            try {
                audit.recordSuccess(template, to, messageId);
            } catch (Exception auditEx) {
                logger.error("audit recordSuccess failed for template={} to={}", template, to, auditEx);
            }
        } catch (Exception e) {
            try {
                audit.recordFailure(template, to, e);
            } catch (Exception auditEx) {
                logger.error("audit recordFailure failed for template={} to={}", template, to, auditEx);
            }
        }
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}
