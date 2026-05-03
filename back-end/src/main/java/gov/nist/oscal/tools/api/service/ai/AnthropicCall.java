package gov.nist.oscal.tools.api.service.ai;

import java.util.ArrayList;
import java.util.List;

public record AnthropicCall(
        String model,
        String systemPrompt,
        String userMessage,
        int maxTokens,
        List<byte[]> pdfDocuments,
        List<String> textDocuments
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String model = "claude-opus-4-7";
        private String systemPrompt = "";
        private String userMessage = "";
        private int maxTokens = 4096;
        private final List<byte[]> pdfs = new ArrayList<>();
        private final List<String> texts = new ArrayList<>();

        public Builder model(String m) { this.model = m; return this; }
        public Builder systemPrompt(String s) { this.systemPrompt = s; return this; }
        public Builder userMessage(String u) { this.userMessage = u; return this; }
        public Builder maxTokens(int m) { this.maxTokens = m; return this; }
        public Builder addPdf(byte[] bytes) { pdfs.add(bytes); return this; }
        public Builder addText(String text) { texts.add(text); return this; }

        public AnthropicCall build() {
            return new AnthropicCall(model, systemPrompt, userMessage, maxTokens, pdfs, texts);
        }
    }
}
