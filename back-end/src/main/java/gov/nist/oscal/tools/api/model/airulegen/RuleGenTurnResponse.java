package gov.nist.oscal.tools.api.model.airulegen;

import java.util.List;

/**
 * Tagged-union response. Exactly one of clarifyingQuestion / proposal is set.
 *
 * <ul>
 *   <li>{@code phase = "clarify"} — clarifyingQuestion is non-null</li>
 *   <li>{@code phase = "proposal"} — proposal + testResults are non-null and matrix is clean</li>
 *   <li>{@code phase = "exhausted"} — message describes why we couldn't reach a working
 *       rule; lastProposal may be non-null (best attempt)</li>
 * </ul>
 */
public record RuleGenTurnResponse(
    String phase,
    String clarifyingQuestion,
    RuleProposal proposal,
    List<TestResult> testResults,
    RuleProposal lastProposal,
    String message,
    int iterations,
    int totalTokensIn,
    int totalTokensOut
) {}
