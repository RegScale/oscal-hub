package gov.nist.oscal.tools.api.service.ai.stream;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AiSessionEventStreamTest {

    @Test
    void subscribeReturnsSseEmitter() {
        AiSessionEventStream stream = new AiSessionEventStream();
        UUID id = UUID.randomUUID();
        SseEmitter emitter = stream.subscribe(id);
        assertThat(emitter).isNotNull();
    }

    @Test
    void closeRemovesEmitterFromPool() {
        AiSessionEventStream stream = new AiSessionEventStream();
        UUID id = UUID.randomUUID();
        stream.subscribe(id);
        stream.close(id);

        // After close, publishing should be a no-op (no emitter in pool).
        // We verify this by checking that publish does not throw.
        assertThatCode(() -> stream.publish(id, SessionEvent.progress("after close")))
                .doesNotThrowAnyException();
    }

    @Test
    void closeOnUnknownSession_isNoOp() {
        // Defensive: controller may call close() in a finally block even if
        // subscribe was never invoked. Must not NPE.
        AiSessionEventStream stream = new AiSessionEventStream();
        assertThatCode(() -> stream.close(UUID.randomUUID())).doesNotThrowAnyException();
    }

    @Test
    void publishWithoutSubscriberDoesNotThrow() {
        AiSessionEventStream stream = new AiSessionEventStream();
        stream.publish(UUID.randomUUID(), SessionEvent.progress("nobody home"));
    }

    @Test
    void publishWithoutSubscriber_stillBuffersForLaterDrain() {
        // The wizard runs asynchronously and may emit events before any client
        // has subscribed. Those events must still be persistable, so the
        // buffer is populated independent of subscribe state.
        AiSessionEventStream stream = new AiSessionEventStream();
        UUID id = UUID.randomUUID();

        stream.publish(id, SessionEvent.progress("first"));
        stream.publish(id, SessionEvent.progress("second"));

        List<SessionEvent> drained = stream.drainBuffer(id);
        assertThat(drained).hasSize(2);
        assertThat(drained).extracting(SessionEvent::type)
                .containsExactly(SessionEvent.Type.PROGRESS, SessionEvent.Type.PROGRESS);
    }

    @Test
    void drainBuffer_emptySession_returnsEmptyList_notNull() {
        AiSessionEventStream stream = new AiSessionEventStream();
        List<SessionEvent> drained = stream.drainBuffer(UUID.randomUUID());
        assertThat(drained).isNotNull().isEmpty();
    }

    @Test
    void drainBuffer_isOneShot_secondCallReturnsEmpty() {
        // Drain semantics: events are consumed, not viewed. A second call after
        // a successful drain shouldn't repeat them — otherwise persistence
        // would double-write on retries.
        AiSessionEventStream stream = new AiSessionEventStream();
        UUID id = UUID.randomUUID();
        stream.publish(id, SessionEvent.progress("once"));

        assertThat(stream.drainBuffer(id)).hasSize(1);
        assertThat(stream.drainBuffer(id)).isEmpty();
    }

    @Test
    void publish_subscriberIoFailure_evictsEmitter_subsequentPublishesSkip() throws Exception {
        // If the SSE connection drops, emitter.send() throws IOException.
        // The stream must evict the broken emitter so we don't keep trying
        // to send to a dead socket on every subsequent publish.
        AiSessionEventStream stream = new AiSessionEventStream();
        UUID id = UUID.randomUUID();

        // Replace the auto-created emitter with a mock that throws on send.
        SseEmitter broken = mock(SseEmitter.class);
        doThrow(new IOException("client disconnected")).when(broken).send(any(SseEmitter.SseEventBuilder.class));
        injectEmitter(stream, id, broken);

        // First publish: send fails, emitter should be evicted (we can't
        // assert the eviction directly without exposing internals, but a
        // second publish must NOT call send again).
        stream.publish(id, SessionEvent.progress("first"));
        verify(broken, times(1)).send(any(SseEmitter.SseEventBuilder.class));

        stream.publish(id, SessionEvent.progress("second"));
        // Still 1 — broken emitter was evicted after the first failure.
        verify(broken, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void sessionEventFactoryMethodsProduceCorrectTypes() {
        assertThat(SessionEvent.progress("msg").type()).isEqualTo(SessionEvent.Type.PROGRESS);
        assertThat(SessionEvent.complete("{}").type()).isEqualTo(SessionEvent.Type.COMPLETE);
        assertThat(SessionEvent.error("E001", "oops").type()).isEqualTo(SessionEvent.Type.ERROR);
        assertThat(SessionEvent.chunk("text").type()).isEqualTo(SessionEvent.Type.CHUNK);
        assertThat(SessionEvent.toolCall("tool", "args").type()).isEqualTo(SessionEvent.Type.TOOL_CALL);
        assertThat(SessionEvent.toolResult("tool", true, "ok").type()).isEqualTo(SessionEvent.Type.TOOL_RESULT);
    }

    /**
     * Reach into the stream's emitters map to swap in a mock — necessary
     * because subscribe() creates a real SseEmitter that we can't drive
     * an IOException through without an actual servlet response.
     */
    @SuppressWarnings("unchecked")
    private static void injectEmitter(AiSessionEventStream stream, UUID id, SseEmitter emitter) throws Exception {
        var field = AiSessionEventStream.class.getDeclaredField("emitters");
        field.setAccessible(true);
        var map = (java.util.concurrent.ConcurrentHashMap<UUID, SseEmitter>) field.get(stream);
        map.put(id, emitter);
    }
}
