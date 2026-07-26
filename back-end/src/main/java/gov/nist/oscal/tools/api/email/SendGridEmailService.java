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
import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.TicketComment;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import gov.nist.oscal.tools.api.entity.TicketType;
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
    private final String supportEmail;
    private final TemplateRenderer renderer;
    private final EmailAuditLogger audit;
    private SendGrid client;

    public SendGridEmailService(String apiKey, String fromEmail, String fromName,
                                 String baseUrl, String supportEmail,
                                 TemplateRenderer renderer, EmailAuditLogger audit) {
        this.client = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.baseUrl = baseUrl;
        this.supportEmail = supportEmail;
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

    @Override
    public void sendPasswordReset(User user, String tempPassword, User adminWhoReset) {
        Map<String, String> vars = new HashMap<>();
        vars.put("username", user.getUsername());
        vars.put("tempPassword", tempPassword);
        vars.put("adminName", adminWhoReset == null ? "an administrator" : adminWhoReset.getUsername());
        vars.put("loginUrl", baseUrl + "/login");
        send("password-reset", user.getEmail(),
             "Your OSCAL Hub password was reset", vars);
    }

    @Override
    public void sendPasswordResetLink(User user, String resetUrl, int ttlMinutes) {
        Map<String, String> vars = new HashMap<>();
        vars.put("username", user.getUsername());
        vars.put("resetUrl", resetUrl);
        vars.put("ttlMinutes", String.valueOf(ttlMinutes));
        send("password-reset-link", user.getEmail(),
             "Reset your OSCAL Hub password", vars);
    }

    @Override
    public void sendTicketCreatedToAdmin(Ticket t) {
        Map<String, String> vars = ticketVars(t);
        send("ticket-created-admin", supportEmail,
            "[OSCAL Hub] New " + humanType(t.getType()) + ": TKT-" + t.getId() + " — " + t.getTitle(),
            vars);
    }

    @Override
    public void sendTicketCreatedToReporter(Ticket t) {
        Map<String, String> vars = ticketVars(t);
        send("ticket-created-reporter", t.getReporter().getEmail(),
            "[OSCAL Hub] We received your " + humanType(t.getType()) + " — TKT-" + t.getId(),
            vars);
    }

    @Override
    public void sendTicketCommentAdded(Ticket t, TicketComment c, String recipientEmail) {
        Map<String, String> vars = ticketVars(t);
        vars.put("authorName", c.getAuthor().getUsername());
        vars.put("commentBody", nullSafe(c.getBody()));
        send("ticket-comment-added", recipientEmail,
            "[OSCAL Hub] New comment on TKT-" + t.getId(),
            vars);
    }

    @Override
    public void sendTicketStatusChanged(Ticket t, TicketStatus oldStatus, TicketStatus newStatus, String adminNote) {
        Map<String, String> vars = ticketVars(t);
        vars.put("oldStatus", oldStatus.name());
        vars.put("newStatus", newStatus.name());
        vars.put("adminNote", nullSafe(adminNote));
        send("ticket-status-changed", t.getReporter().getEmail(),
            "[OSCAL Hub] TKT-" + t.getId() + " is now " + newStatus.name(),
            vars);
    }

    @Override
    public void sendTicketReopened(Ticket t, TicketComment reopenComment) {
        Map<String, String> vars = ticketVars(t);
        vars.put("reopenBody", nullSafe(reopenComment.getBody()));
        send("ticket-reopened", supportEmail,
            "[OSCAL Hub] Reopened: TKT-" + t.getId() + " — " + t.getTitle(),
            vars);
    }

    private Map<String, String> ticketVars(Ticket t) {
        Map<String, String> vars = new HashMap<>();
        vars.put("ticketId", "TKT-" + t.getId());
        vars.put("title", t.getTitle());
        vars.put("type", humanType(t.getType()));
        vars.put("priority", t.getPriority().name());
        vars.put("status", t.getStatus().name());
        vars.put("description", nullSafe(t.getDescription()));
        vars.put("reporterName", t.getReporter().getUsername());
        vars.put("ticketUrl", baseUrl + "/tickets/" + t.getId());
        return vars;
    }

    private String humanType(TicketType type) {
        return type == TicketType.BUG ? "Bug Report" : "Feature Request";
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
