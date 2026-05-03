package gov.nist.oscal.tools.api.service.ai;

public record AnthropicResult(
        String text,
        int tokensIn,
        int tokensOut
) { }
