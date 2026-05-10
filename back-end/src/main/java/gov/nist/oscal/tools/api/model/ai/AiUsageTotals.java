package gov.nist.oscal.tools.api.model.ai;

public record AiUsageTotals(
        int totalSessions,
        long totalTokensIn,
        long totalTokensOut,
        long totalCostUsdMicros,
        int sessionsThisMonth,
        long costThisMonthUsdMicros) {
}
