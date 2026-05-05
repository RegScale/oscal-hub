package gov.nist.oscal.tools.api.service.ai;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Structured result from an Anthropic tool-use call. {@link #toolName} is the
 * tool the model chose to invoke; {@link #input} is the JSON-shaped argument
 * object the model produced. Token counts are reported separately so callers
 * can aggregate per-session totals.
 */
public record AnthropicToolUseResult(
    String toolName,
    JsonNode input,
    int tokensIn,
    int tokensOut
) {}
