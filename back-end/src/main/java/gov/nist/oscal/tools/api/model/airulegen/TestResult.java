package gov.nist.oscal.tools.api.model.airulegen;

public record TestResult(
    int index,
    String description,
    String expected,
    String actual,
    boolean passed,
    String violationMessage
) {}
