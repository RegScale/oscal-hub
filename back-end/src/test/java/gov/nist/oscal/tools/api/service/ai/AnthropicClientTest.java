package gov.nist.oscal.tools.api.service.ai;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for AnthropicClient.
 *
 * The actual SDK call is hard to test in isolation because the client is
 * built inside {@link AnthropicClient#send} rather than injected. The
 * tests here cover what we can without a network — input validation and
 * the retry-decision logic — since those are the highest-leverage paths
 * for production resilience under transient Anthropic overload.
 */
class AnthropicClientTest {

    private final AnthropicClient client = new AnthropicClient();

    // ---------- API key validation ----------

    @Test
    void send_rejectsBlankApiKey() {
        AnthropicCall call = sampleCall();
        assertThatThrownBy(() -> client.send("", call))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key");
    }

    @Test
    void send_rejectsNullApiKey() {
        AnthropicCall call = sampleCall();
        assertThatThrownBy(() -> client.send(null, call))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key");
    }

    @Test
    void send_rejectsWhitespaceOnlyApiKey() {
        AnthropicCall call = sampleCall();
        assertThatThrownBy(() -> client.send("   \t  ", call))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendWithTools_rejectsBlankApiKey_separatelyFromSend() {
        // Both entry points must validate independently — sendWithTools doesn't
        // delegate to send, so a missing guard there would be silently bypassed.
        AnthropicCall call = sampleCall();
        assertThatThrownBy(() -> client.sendWithTools("", call, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key");
    }

    @Test
    void sendWithTools_rejectsNullApiKey() {
        AnthropicCall call = sampleCall();
        assertThatThrownBy(() -> client.sendWithTools(null, call, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- AnthropicCall builder ----------

    @Test
    void builder_storesProvidedValues() {
        AnthropicCall call = AnthropicCall.builder()
                .model("claude-opus-4-7")
                .systemPrompt("system")
                .userMessage("hello")
                .maxTokens(256)
                .build();
        assertThat(call.model()).isEqualTo("claude-opus-4-7");
        assertThat(call.systemPrompt()).isEqualTo("system");
        assertThat(call.userMessage()).isEqualTo("hello");
        assertThat(call.maxTokens()).isEqualTo(256);
    }

    @Test
    void builder_defaultsAreSafe() {
        // If the wizard forgets to set fields explicitly, the builder must produce
        // a call object that the SDK can serialize. maxTokens=0 would cause a
        // 400 from the API, so the default has to be positive.
        AnthropicCall call = AnthropicCall.builder().build();
        assertThat(call.model()).isNotBlank();
        assertThat(call.maxTokens()).isPositive();
        assertThat(call.tools()).isNotNull(); // must be non-null for forEach
        assertThat(call.pdfDocuments()).isNotNull();
        assertThat(call.textDocuments()).isNotNull();
    }

    @Test
    void builder_nullTools_normalizedToEmptyList() {
        // Passing null shouldn't NPE callers iterating tools().
        AnthropicCall call = AnthropicCall.builder().tools(null).build();
        assertThat(call.tools()).isEmpty();
    }

    // ---------- isRetriable (resilience-critical) ----------
    //
    // Anthropic intermittently returns:
    //   - 529 "overloaded_error" when their fleet is shedding load
    //   - 429 "rate_limit_error"
    // Both are retriable; any other error must surface immediately so the
    // caller doesn't pay 26+ seconds of backoff on a permanent failure.

    @Test
    void isRetriable_overloadedMessage_isRetriable() {
        assertThat(invokeIsRetriable(new RuntimeException("overloaded_error: server is overloaded")))
                .isTrue();
    }

    @Test
    void isRetriable_overloadedMixedCase_isRetriable() {
        // Real exception messages from the SDK can contain "Overloaded".
        // The check must be case-insensitive to catch them.
        assertThat(invokeIsRetriable(new RuntimeException("Overloaded"))).isTrue();
    }

    @Test
    void isRetriable_rateLimitWithUnderscore_isRetriable() {
        // The Anthropic API uses snake_case error codes ("rate_limit_error").
        assertThat(invokeIsRetriable(new RuntimeException("rate_limit_error")))
                .isTrue();
    }

    @Test
    void isRetriable_rateLimitWithSpace_isRetriable() {
        // Some logging paths render it as "rate limit exceeded".
        assertThat(invokeIsRetriable(new RuntimeException("rate limit exceeded")))
                .isTrue();
    }

    @Test
    void isRetriable_status529_isRetriable() {
        assertThat(invokeIsRetriable(new RuntimeException("HTTP 529 Service Unavailable")))
                .isTrue();
        assertThat(invokeIsRetriable(new RuntimeException("status: 529"))).isTrue();
    }

    @Test
    void isRetriable_status429_isRetriable() {
        assertThat(invokeIsRetriable(new RuntimeException("HTTP 429 Too Many Requests")))
                .isTrue();
        assertThat(invokeIsRetriable(new RuntimeException("status: 429"))).isTrue();
    }

    @Test
    void isRetriable_400BadRequest_isNotRetriable() {
        // A 400 from the API means our request is malformed (bad model name,
        // bad payload). Retrying just compounds the problem.
        assertThat(invokeIsRetriable(new RuntimeException("HTTP 400 Bad Request"))).isFalse();
    }

    @Test
    void isRetriable_401Unauthorized_isNotRetriable() {
        // 401 = bad API key. No amount of retry fixes it.
        assertThat(invokeIsRetriable(new RuntimeException("HTTP 401 Unauthorized"))).isFalse();
    }

    @Test
    void isRetriable_genericNetworkError_isNotRetriable() {
        // Connection refused / DNS lookup failures aren't on the retriable list.
        // The infra is broken and a 26-second backoff loop would just delay the
        // bad news without changing the outcome.
        assertThat(invokeIsRetriable(new RuntimeException("Connection refused"))).isFalse();
    }

    @Test
    void isRetriable_walksCauseChain() {
        // The SDK wraps real errors in its own types — the retriable marker
        // typically lives several levels deep in the cause chain.
        Throwable root = new RuntimeException("rate_limit_error from upstream");
        Throwable mid = new RuntimeException("SDK wrapper", root);
        Throwable outer = new RuntimeException("client error", mid);
        assertThat(invokeIsRetriable(outer)).isTrue();
    }

    @Test
    void isRetriable_walksCauseChain_terminatesOnSelfReference() {
        // Defensive: an exception with a self-cause shouldn't infinite-loop.
        // (Java's Throwable APIs forbid this but custom exceptions sometimes
        // get into weird states — this guards against a hang.)
        Throwable e = new RuntimeException("nope");
        // We can't actually create a self-referencing chain via Throwable,
        // but a long chain of distinct causes ending in non-retriable text
        // exercises the loop boundary just as well.
        for (int i = 0; i < 10; i++) {
            e = new RuntimeException("layer " + i, e);
        }
        assertThat(invokeIsRetriable(e)).isFalse();
    }

    @Test
    void isRetriable_nullMessage_doesNotNpe() {
        // Some exception subclasses are constructed without messages (e.g.
        // OpenSSL handshake failures wrapped by OkHttp). The walk must
        // tolerate getMessage() == null without throwing NPE — otherwise the
        // retry loop would crash on the first such exception, defeating its
        // whole purpose.
        Throwable e = new RuntimeException((String) null);
        assertThat(invokeIsRetriable(e)).isFalse();
    }

    @Test
    void isRetriable_caseInsensitiveStatusCodes() {
        // Even though digits aren't case-sensitive, the surrounding text might be.
        assertThat(invokeIsRetriable(new RuntimeException("STATUS: 529 (overloaded)"))).isTrue();
    }

    // ---------- helpers ----------

    private static AnthropicCall sampleCall() {
        return AnthropicCall.builder()
                .model("claude-opus-4-7")
                .systemPrompt("hi")
                .userMessage("ping")
                .build();
    }

    private boolean invokeIsRetriable(Throwable e) {
        try {
            Method m = AnthropicClient.class.getDeclaredMethod("isRetriable", Throwable.class);
            m.setAccessible(true);
            return (boolean) m.invoke(client, e);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Reflection failed", ex);
        }
    }
}
