package gov.nist.oscal.tools.api.service.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnthropicClientTest {

    @Test
    void rejectsBlankApiKey() {
        AnthropicClient client = new AnthropicClient();
        AnthropicCall call = AnthropicCall.builder()
                .model("claude-opus-4-7")
                .systemPrompt("hi")
                .userMessage("ping")
                .build();

        assertThatThrownBy(() -> client.send("", call))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API key");
    }

    @Test
    void buildsCallParams() {
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
}
