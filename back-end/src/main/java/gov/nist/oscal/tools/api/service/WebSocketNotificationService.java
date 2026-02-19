package gov.nist.oscal.tools.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending WebSocket notifications to connected clients.
 *
 * This service provides methods to push real-time updates for:
 * - Batch operation progress
 * - Async operation status changes
 * - User notifications
 *
 * Clients subscribe to specific topics to receive updates:
 * - /topic/batch/{operationId} - Batch progress
 * - /topic/async/{operationId} - Async operation status
 * - /user/{username}/queue/notifications - Personal notifications
 */
@Service
public class WebSocketNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Send batch operation progress update.
     *
     * @param operationId The batch operation ID
     * @param completed Number of completed items
     * @param total Total items in batch
     * @param status Current status (PROCESSING, COMPLETED, FAILED)
     */
    public void sendBatchProgress(String operationId, int completed, int total, String status) {
        Map<String, Object> message = new HashMap<>();
        message.put("operationId", operationId);
        message.put("completed", completed);
        message.put("total", total);
        message.put("status", status);
        message.put("percentage", total > 0 ? (completed * 100) / total : 0);
        message.put("timestamp", System.currentTimeMillis());

        String destination = "/topic/batch/" + operationId;
        messagingTemplate.convertAndSend(destination, message);
        logger.debug("Sent batch progress for {}: {}/{} ({})", operationId, completed, total, status);
    }

    /**
     * Send async operation status update.
     *
     * @param operationId The async operation ID
     * @param status Current status
     * @param result Optional result data (for COMPLETED status)
     * @param error Optional error message (for FAILED status)
     */
    public void sendAsyncStatus(String operationId, String status, Object result, String error) {
        Map<String, Object> message = new HashMap<>();
        message.put("operationId", operationId);
        message.put("status", status);
        message.put("timestamp", System.currentTimeMillis());

        if (result != null) {
            message.put("result", result);
        }
        if (error != null) {
            message.put("error", error);
        }

        String destination = "/topic/async/" + operationId;
        messagingTemplate.convertAndSend(destination, message);
        logger.debug("Sent async status for {}: {}", operationId, status);
    }

    /**
     * Send a notification to a specific user.
     *
     * @param username The target username
     * @param type Notification type (INFO, SUCCESS, WARNING, ERROR)
     * @param title Notification title
     * @param message Notification message
     */
    public void sendUserNotification(String username, String type, String title, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
        logger.debug("Sent notification to {}: {} - {}", username, type, title);
    }

    /**
     * Send a validation result notification.
     *
     * @param operationId The operation ID
     * @param valid Whether the document is valid
     * @param errorCount Number of errors (if invalid)
     */
    public void sendValidationResult(String operationId, boolean valid, int errorCount) {
        Map<String, Object> message = new HashMap<>();
        message.put("operationId", operationId);
        message.put("status", "COMPLETED");
        message.put("valid", valid);
        message.put("errorCount", errorCount);
        message.put("timestamp", System.currentTimeMillis());

        String destination = "/topic/async/" + operationId;
        messagingTemplate.convertAndSend(destination, message);
    }

    /**
     * Broadcast a system-wide notification.
     *
     * @param type Notification type
     * @param title Notification title
     * @param message Notification message
     */
    public void broadcastNotification(String type, String title, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/notifications", notification);
        logger.info("Broadcast notification: {} - {}", type, title);
    }
}
