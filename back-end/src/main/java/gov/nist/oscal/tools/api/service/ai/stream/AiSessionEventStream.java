package gov.nist.oscal.tools.api.service.ai.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiSessionEventStream {

    private static final Logger log = LoggerFactory.getLogger(AiSessionEventStream.class);
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000; // 30 min

    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<SessionEvent>> buffers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID sessionId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError(e -> emitters.remove(sessionId));
        emitters.put(sessionId, emitter);
        return emitter;
    }

    public void publish(UUID sessionId, SessionEvent event) {
        // Buffer for persistence before forwarding to subscribers
        buffers.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(event);

        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name(event.type().name().toLowerCase())
                    .data(event.dataJson()));
        } catch (IOException e) {
            log.warn("SSE send failed for session {}: {}", sessionId, e.toString());
            emitters.remove(sessionId);
        }
    }

    /**
     * Drain and return all buffered events for the session, removing them from the buffer.
     * Intended for persistence after a run completes — not for replay to subscribers.
     */
    public List<SessionEvent> drainBuffer(UUID sessionId) {
        List<SessionEvent> events = buffers.remove(sessionId);
        return events == null ? List.of() : List.copyOf(events);
    }

    public void close(UUID sessionId) {
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) emitter.complete();
    }
}
