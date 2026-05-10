package gov.nist.oscal.tools.api.model.airulegen;

import java.util.List;

public record RuleProposal(
    String name,
    String description,
    String severity,
    String fieldPath,
    String constraintXml,
    List<TestCase> testCases
) {}
