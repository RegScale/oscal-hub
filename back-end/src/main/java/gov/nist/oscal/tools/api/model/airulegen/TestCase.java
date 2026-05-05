package gov.nist.oscal.tools.api.model.airulegen;

public record TestCase(
    String description,
    String fragmentJson,
    String expected
) {}
