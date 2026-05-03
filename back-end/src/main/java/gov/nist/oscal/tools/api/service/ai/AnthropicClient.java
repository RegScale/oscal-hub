package gov.nist.oscal.tools.api.service.ai;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlockParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service("anthropicClientService")
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);

    public AnthropicResult send(String apiKey, AnthropicCall call) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Missing Anthropic API key");
        }

        com.anthropic.client.AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();

        List<ContentBlockParam> blocks = new ArrayList<>();

        for (String text : call.textDocuments()) {
            blocks.add(ContentBlockParam.ofText(
                    TextBlockParam.builder().text(text).build()));
        }

        for (byte[] pdf : call.pdfDocuments()) {
            String base64 = Base64.getEncoder().encodeToString(pdf);
            blocks.add(ContentBlockParam.ofDocument(
                    DocumentBlockParam.builder()
                            .base64Source(base64)
                            .build()));
        }

        blocks.add(ContentBlockParam.ofText(
                TextBlockParam.builder().text(call.userMessage()).build()));

        MessageCreateParams params = MessageCreateParams.builder()
                .model(call.model())
                .maxTokens(call.maxTokens())
                .system(call.systemPrompt())
                .addMessage(MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .contentOfBlockParams(blocks)
                        .build())
                .build();

        Message message = client.messages().create(params);

        String text = message.content().stream()
                .filter(c -> c.text().isPresent())
                .map(c -> c.text().get().text())
                .reduce("", (a, b) -> a + b);

        long tokensIn = message.usage().inputTokens();
        long tokensOut = message.usage().outputTokens();
        log.info("Anthropic call complete: model={} in={} out={}", call.model(), tokensIn, tokensOut);
        return new AnthropicResult(text, (int) tokensIn, (int) tokensOut);
    }
}
