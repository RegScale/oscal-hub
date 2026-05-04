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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

@Service("anthropicClientService")
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);

    public AnthropicResult send(String apiKey, AnthropicCall call) {
        return send(apiKey, call, null);
    }

    /**
     * @param onRetry optional callback invoked once before each retry attempt
     *                with a human-readable reason (e.g. for surfacing retry
     *                progress to the wizard UI). May be {@code null}.
     */
    public AnthropicResult send(String apiKey, AnthropicCall call, Consumer<String> onRetry) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Missing Anthropic API key");
        }

        // Pin SDK-level retries to 0 — we run our own retry loop below, and
        // the SDK's default of ~2 internal retries with up-to-60s backoff was
        // compounding on top of ours into 10+ minute stalls. Set an explicit
        // 2-minute per-request timeout so a hung connection surfaces quickly.
        com.anthropic.client.AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .maxRetries(0)
                .timeout(Duration.ofMinutes(2))
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

        Message message = sendWithRetry(client, params, onRetry);

        String text = message.content().stream()
                .filter(c -> c.text().isPresent())
                .map(c -> c.text().get().text())
                .reduce("", (a, b) -> a + b);

        long tokensIn = message.usage().inputTokens();
        long tokensOut = message.usage().outputTokens();
        log.info("Anthropic call complete: model={} in={} out={}", call.model(), tokensIn, tokensOut);
        return new AnthropicResult(text, (int) tokensIn, (int) tokensOut);
    }

    /**
     * Anthropic returns 529 ({@code overloaded_error}) when their inference
     * fleet is shedding load — purely transient. 429 ({@code rate_limit_error})
     * is also transient given enough wait. Both are safe to retry. Any other
     * 4xx is a real client error and should bubble up immediately.
     *
     * <p>Backoff: 2s, 6s, 18s (3 retries → 4 attempts total). Caps the
     * worst-case extra latency around 26s, which is well within the 30-min
     * SSE emitter timeout but long enough to clear most overload windows.
     */
    private Message sendWithRetry(com.anthropic.client.AnthropicClient client,
                                  MessageCreateParams params,
                                  Consumer<String> onRetry) {
        int maxAttempts = 4;
        long backoffMs = 2_000L;
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long started = System.currentTimeMillis();
            try {
                return client.messages().create(params);
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - started;
                if (!isRetriable(e) || attempt == maxAttempts) {
                    log.warn("Anthropic API call failed after {}ms on attempt {} of {}: {}",
                            elapsed, attempt, maxAttempts, e.getMessage());
                    throw e;
                }
                lastError = e;
                String kind = e.getMessage() != null && e.getMessage().toLowerCase().contains("overload")
                        ? "overload" : "rate-limit";
                log.warn("Anthropic API transient {} (attempt {} of {}, took {}ms): {} — retrying in {}ms",
                        kind, attempt, maxAttempts, elapsed, e.getMessage(), backoffMs);
                if (onRetry != null) {
                    onRetry.accept(String.format(
                            "Anthropic %s — retry %d of %d in %ds…",
                            kind, attempt, maxAttempts - 1, backoffMs / 1000));
                }
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while retrying Anthropic call", ie);
                }
                backoffMs *= 3;
            }
        }
        // Defensive — loop exit without return only if maxAttempts == 0.
        throw new RuntimeException("Anthropic call failed after " + maxAttempts + " attempts", lastError);
    }

    private boolean isRetriable(Throwable e) {
        // Walk the cause chain — the SDK wraps errors in its own types but the
        // status code and "overloaded" / "rate_limit" markers always appear in
        // toString() somewhere along the chain.
        for (Throwable t = e; t != null; t = t.getCause()) {
            String s = (t.getMessage() == null ? "" : t.getMessage()).toLowerCase();
            if (s.contains("overloaded") || s.contains("rate_limit") || s.contains("rate limit")) {
                return true;
            }
            if (s.contains("529") || s.contains("status: 529")) return true;
            if (s.contains("429") || s.contains("status: 429")) return true;
        }
        return false;
    }
}
