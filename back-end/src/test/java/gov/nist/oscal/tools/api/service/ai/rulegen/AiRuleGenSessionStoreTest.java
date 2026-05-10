package gov.nist.oscal.tools.api.service.ai.rulegen;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRuleGenSessionStoreTest {

    @Test
    void create_returnsUniqueId_andSessionIsRetrievable() {
        AiRuleGenSessionStore store = new AiRuleGenSessionStore();
        UUID id = store.create(1L, 100L, "ssp", "claude-opus-4-7");

        AiRuleGenSession s = store.get(id);
        assertThat(s.id()).isEqualTo(id);
        assertThat(s.organizationId()).isEqualTo(1L);
        assertThat(s.userId()).isEqualTo(100L);
        assertThat(s.modelType()).isEqualTo("ssp");
        assertThat(s.anthropicModel()).isEqualTo("claude-opus-4-7");
        assertThat(s.transcript()).isEmpty();
        assertThat(s.tokensIn()).isZero();
        assertThat(s.tokensOut()).isZero();
        assertThat(s.currentProposal()).isNull();
    }

    @Test
    void unknownId_throws410Gone_compatibleException() {
        AiRuleGenSessionStore store = new AiRuleGenSessionStore();
        UUID bogus = UUID.randomUUID();

        // Frontend distinguishes 410 from generic 5xx and silently restarts the
        // session. The exception class is annotated with @ResponseStatus(GONE).
        assertThatThrownBy(() -> store.get(bogus))
                .isInstanceOf(RuleGenSessionExpiredException.class)
                .hasMessageContaining(bogus.toString());
    }

    @Test
    void appendUserAndAssistant_recordTranscriptInOrder() {
        AiRuleGenSessionStore store = new AiRuleGenSessionStore();
        UUID id = store.create(1L, 1L, "catalog", "claude-opus-4-7");

        store.appendUser(id, "first user message");
        store.appendAssistant(id, "first assistant response");
        store.appendUser(id, "second user message");

        var transcript = store.get(id).transcript();
        assertThat(transcript).hasSize(3);
        assertThat(transcript.get(0).role()).isEqualTo("user");
        assertThat(transcript.get(0).text()).isEqualTo("first user message");
        assertThat(transcript.get(1).role()).isEqualTo("assistant");
        assertThat(transcript.get(2).role()).isEqualTo("user");
        assertThat(transcript.get(2).text()).isEqualTo("second user message");
    }

    @Test
    void appendUser_onUnknownSession_alsoThrowsExpired_notSilentDrop() {
        // Append must validate session existence — if we silently dropped the
        // message, the next get() would return a transcript missing user input
        // and the LLM would respond to nothing.
        AiRuleGenSessionStore store = new AiRuleGenSessionStore();
        assertThatThrownBy(() -> store.appendUser(UUID.randomUUID(), "msg"))
                .isInstanceOf(RuleGenSessionExpiredException.class);
    }

    @Test
    void close_invalidatesSession_subsequentGetThrows() {
        AiRuleGenSessionStore store = new AiRuleGenSessionStore();
        UUID id = store.create(1L, 1L, "ssp", "claude-opus-4-7");
        store.close(id);

        assertThatThrownBy(() -> store.get(id))
                .isInstanceOf(RuleGenSessionExpiredException.class);
    }

    @Test
    void close_isIdempotent_doesNotThrowOnUnknownId() {
        // Closing already-closed or never-created sessions should be a no-op so
        // the controller can call close() defensively without try/catch.
        AiRuleGenSessionStore store = new AiRuleGenSessionStore();
        store.close(UUID.randomUUID()); // does not throw
    }

    @Test
    void session_addTokens_accumulates() {
        AiRuleGenSessionStore store = new AiRuleGenSessionStore();
        UUID id = store.create(1L, 1L, "ssp", "claude-opus-4-7");
        AiRuleGenSession s = store.get(id);
        s.addTokens(100, 50);
        s.addTokens(25, 10);
        assertThat(s.tokensIn()).isEqualTo(125);
        assertThat(s.tokensOut()).isEqualTo(60);
    }
}
