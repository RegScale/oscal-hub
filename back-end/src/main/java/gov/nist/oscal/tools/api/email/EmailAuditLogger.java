package gov.nist.oscal.tools.api.email;

import gov.nist.oscal.tools.api.model.AuditEventType;
import gov.nist.oscal.tools.api.service.AuditLogService;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EmailAuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(EmailAuditLogger.class);

    @Autowired
    private AuditLogService auditLogService;

    public void recordSuccess(String template, String recipientEmail, String messageId) {
        logger.info("email_send template={} recipient_hash={} message_id={} status=success",
            template, hash(recipientEmail), messageId);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("template", template);
        metadata.put("recipient", recipientEmail);
        if (messageId != null) {
            metadata.put("messageId", messageId);
        }
        auditLogService.logEvent(AuditEventType.EMAIL_SEND_SUCCESS, "system", null,
            "SUCCESS", "EMAIL", template, metadata);
    }

    public void recordFailure(String template, String recipientEmail, Throwable t) {
        logger.warn("email_send template={} recipient_hash={} status=failure error={}",
            template, hash(recipientEmail), t.toString());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("template", template);
        metadata.put("recipient", recipientEmail);
        metadata.put("error", t.toString());
        auditLogService.logEvent(AuditEventType.EMAIL_SEND_FAILURE, "system", null,
            "FAILURE", "EMAIL", template, metadata);
    }

    private static String hash(String s) {
        if (s == null) return "";
        return Integer.toHexString(s.toLowerCase().hashCode());
    }
}
