package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonSourceFormat;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class OscalPoamParserTest {

    @Test
    void parsesValidJsonFixture() throws Exception {
        OscalPoamParser parser = new OscalPoamParser();
        try (InputStream in = getClass().getResourceAsStream("/conmon/example_poam_valid.json")) {
            assertThat(in).as("fixture must be on classpath").isNotNull();
            ParsedPoam parsed = parser.parse(in, ConMonSourceFormat.OSCAL_JSON);

            assertThat(parsed.oscalUuid()).isEqualTo("51657392-cc1f-4e77-977c-91528f690be2");
            assertThat(parsed.oscalVersion()).isEqualTo("1.1.1");
            assertThat(parsed.items()).hasSize(1);

            ParsedPoamItem item = parsed.items().get(0);
            assertThat(item.externalId()).isEqualTo("c8d39ca5-7563-45d8-b90b-969f8c38bb48");
            assertThat(item.title()).contains("Example PO");
            // Fixture has no status prop and no findings → UNKNOWN
            assertThat(item.status()).isEqualTo(ConMonItemStatus.UNKNOWN);
        }
    }

    @Test
    void parsesRealWorldRev5JsonWith100Items() throws Exception {
        OscalPoamParser parser = new OscalPoamParser();
        try (InputStream in = getClass().getResourceAsStream("/conmon/oscal_poam_real_world.json")) {
            assertThat(in).as("real-world fixture must be on classpath").isNotNull();
            ParsedPoam parsed = parser.parse(in, ConMonSourceFormat.OSCAL_JSON);

            // The file has 100 poam-items per jq inspection.
            assertThat(parsed.items()).hasSize(100);

            // Spot-check the first item carries through external_id and title
            assertThat(parsed.items().get(0).externalId()).isNotBlank();
            assertThat(parsed.items().get(0).title()).contains("POAM-0001");
        }
    }

    @Test
    void emptyContent_throws() {
        OscalPoamParser parser = new OscalPoamParser();
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> parser.parse(new java.io.ByteArrayInputStream(new byte[0]), ConMonSourceFormat.OSCAL_JSON))
                .isInstanceOf(RuntimeException.class);
    }
}
