package gov.nist.oscal.tools.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time updates.
 *
 * This enables STOMP over WebSocket for:
 * - Batch operation progress updates
 * - Async validation status updates
 * - Real-time notifications
 *
 * Clients connect to /ws endpoint and subscribe to topics:
 * - /topic/batch/{operationId} - Batch operation progress
 * - /topic/async/{operationId} - Async operation status
 * - /user/queue/notifications - User-specific notifications
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker for topics and queues
        // In production, consider using RabbitMQ or ActiveMQ for scalability
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages FROM clients TO server
        config.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint that clients connect to
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:3001",
                    "https://*.run.app" // Cloud Run domains
                )
                .withSockJS(); // Fallback for browsers without WebSocket support
    }
}
