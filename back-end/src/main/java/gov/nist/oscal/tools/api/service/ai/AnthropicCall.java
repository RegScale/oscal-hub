package gov.nist.oscal.tools.api.service.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record AnthropicCall(
        String model,
        String systemPrompt,
        String userMessage,
        int maxTokens,
        List<byte[]> pdfDocuments,
        List<String> textDocuments,
        List<Map<String, Object>> tools,
        String toolChoice
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String model = "claude-opus-4-7";
        private String systemPrompt = "";
        private String userMessage = "";
        private int maxTokens = 4096;
        private final List<byte[]> pdfs = new ArrayList<>();
        private final List<String> texts = new ArrayList<>();
        private List<Map<String, Object>> tools = Collections.emptyList();
        private String toolChoice = null;

        public Builder model(String m) { this.model = m; return this; }
        public Builder systemPrompt(String s) { this.systemPrompt = s; return this; }
        public Builder userMessage(String u) { this.userMessage = u; return this; }
        public Builder maxTokens(int m) { this.maxTokens = m; return this; }
        public Builder addPdf(byte[] bytes) { pdfs.add(bytes); return this; }
        public Builder addText(String text) { texts.add(text); return this; }

        /**
         * Tool definitions to send with the request, each as a JSON-shaped
         * map with keys {@code name}, {@code description}, {@code input_schema}.
         * Pass to {@link AnthropicClient#sendWithTools}.
         */
        public Builder tools(List<Map<String, Object>> tools) {
            this.tools = tools == null ? Collections.emptyList() : tools;
            return this;
        }

        /** "auto", "any", "none", or a tool name. */
        public Builder toolChoice(String choice) {
            this.toolChoice = choice;
            return this;
        }

        public AnthropicCall build() {
            return new AnthropicCall(model, systemPrompt, userMessage, maxTokens, pdfs, texts, tools, toolChoice);
        }
    }
}
