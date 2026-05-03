package gov.nist.oscal.tools.api.service.ai.stream;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
    void publishWithoutSubscriberDoesNotThrow() {
        AiSessionEventStream stream = new AiSessionEventStream();
        stream.publish(UUID.randomUUID(), SessionEvent.progress("nobody home"));
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
}
