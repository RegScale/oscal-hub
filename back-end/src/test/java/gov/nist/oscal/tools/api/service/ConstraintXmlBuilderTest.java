package gov.nist.oscal.tools.api.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintXmlBuilderTest {

    private final ConstraintXmlBuilder builder = new ConstraintXmlBuilder();

    @Test
    void wrapsAssemblyFragmentForCatalog() {
        String fragment = "<assembly target=\"metadata\">"
            + "<expect id=\"r1\" level=\"ERROR\" test=\"title\">"
            + "<message>need title</message></expect></assembly>";

        String xml = builder.build("rule-r1", "catalog", fragment);

        assertThat(xml)
            .contains("<METASCHEMA-CONSTRAINTS")
            .contains("xmlns=\"http://csrc.nist.gov/ns/oscal/metaschema/1.0\"")
            .contains("metaschema-short-name=\"oscal-catalog\"")
            .contains("metaschema-namespace=\"http://csrc.nist.gov/ns/oscal/1.0\"")
            .contains("<expect id=\"r1\"");
    }

    @Test
    void passesThroughCompleteDocument() {
        String complete = "<METASCHEMA-CONSTRAINTS xmlns=\"http://csrc.nist.gov/ns/oscal/metaschema/1.0\">"
            + "<name>x</name><version>1</version></METASCHEMA-CONSTRAINTS>";
        assertThat(builder.build("any", "ssp", complete)).isEqualTo(complete);
    }

    @Test
    void rejectsUnknownModelType() {
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> builder.build("r", "not-a-real-model", "<assembly target=\"x\"/>"));
    }
}
