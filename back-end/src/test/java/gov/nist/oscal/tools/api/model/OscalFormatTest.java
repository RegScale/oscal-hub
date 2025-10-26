package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OscalFormatTest {

    @Test
    void testEnumValues() {
        OscalFormat[] formats = OscalFormat.values();
        assertEquals(3, formats.length);
        assertEquals(OscalFormat.XML, formats[0]);
        assertEquals(OscalFormat.JSON, formats[1]);
        assertEquals(OscalFormat.YAML, formats[2]);
    }

    @Test
    void testFromStringUpperCase() {
        assertEquals(OscalFormat.XML, OscalFormat.fromString("XML"));
        assertEquals(OscalFormat.JSON, OscalFormat.fromString("JSON"));
        assertEquals(OscalFormat.YAML, OscalFormat.fromString("YAML"));
    }

    @Test
    void testFromStringLowerCase() {
        assertEquals(OscalFormat.XML, OscalFormat.fromString("xml"));
        assertEquals(OscalFormat.JSON, OscalFormat.fromString("json"));
        assertEquals(OscalFormat.YAML, OscalFormat.fromString("yaml"));
    }

    @Test
    void testFromStringMixedCase() {
        assertEquals(OscalFormat.XML, OscalFormat.fromString("Xml"));
        assertEquals(OscalFormat.JSON, OscalFormat.fromString("Json"));
        assertEquals(OscalFormat.YAML, OscalFormat.fromString("Yaml"));
        assertEquals(OscalFormat.XML, OscalFormat.fromString("XmL"));
        assertEquals(OscalFormat.JSON, OscalFormat.fromString("JsOn"));
        assertEquals(OscalFormat.YAML, OscalFormat.fromString("YaMl"));
    }

    @Test
    void testFromStringNull() {
        assertNull(OscalFormat.fromString(null));
    }

    @Test
    void testFromStringInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            OscalFormat.fromString("INVALID");
        });
    }

    @Test
    void testFromStringEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> {
            OscalFormat.fromString("");
        });
    }

    @Test
    void testValueOf() {
        assertEquals(OscalFormat.XML, OscalFormat.valueOf("XML"));
        assertEquals(OscalFormat.JSON, OscalFormat.valueOf("JSON"));
        assertEquals(OscalFormat.YAML, OscalFormat.valueOf("YAML"));
    }

    @Test
    void testEnumToString() {
        assertEquals("XML", OscalFormat.XML.toString());
        assertEquals("JSON", OscalFormat.JSON.toString());
        assertEquals("YAML", OscalFormat.YAML.toString());
    }

    @Test
    void testEnumName() {
        assertEquals("XML", OscalFormat.XML.name());
        assertEquals("JSON", OscalFormat.JSON.name());
        assertEquals("YAML", OscalFormat.YAML.name());
    }

    @Test
    void testEnumEquality() {
        OscalFormat xml1 = OscalFormat.XML;
        OscalFormat xml2 = OscalFormat.valueOf("XML");
        OscalFormat xml3 = OscalFormat.fromString("xml");

        assertEquals(xml1, xml2);
        assertEquals(xml2, xml3);
        assertEquals(xml1, xml3);
    }

    @Test
    void testEnumInequality() {
        assertNotEquals(OscalFormat.XML, OscalFormat.JSON);
        assertNotEquals(OscalFormat.JSON, OscalFormat.YAML);
        assertNotEquals(OscalFormat.YAML, OscalFormat.XML);
    }

    @Test
    void testFromStringWithWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> {
            OscalFormat.fromString("XML ");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            OscalFormat.fromString(" XML");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            OscalFormat.fromString(" XML ");
        });
    }

    @Test
    void testSwitchStatement() {
        for (OscalFormat format : OscalFormat.values()) {
            String result = switch (format) {
                case XML -> "xml";
                case JSON -> "json";
                case YAML -> "yaml";
            };
            assertNotNull(result);
        }
    }

    @Test
    void testAllFormatsAreDistinct() {
        OscalFormat[] formats = OscalFormat.values();
        for (int i = 0; i < formats.length; i++) {
            for (int j = i + 1; j < formats.length; j++) {
                assertNotEquals(formats[i], formats[j]);
            }
        }
    }

    @Test
    void testFromStringPreservesCase() {
        // fromString converts to uppercase before valueOf
        assertEquals(OscalFormat.XML, OscalFormat.fromString("xml"));
        assertEquals(OscalFormat.JSON, OscalFormat.fromString("json"));
        assertEquals(OscalFormat.YAML, OscalFormat.fromString("yaml"));
    }

    @Test
    void testAllVariations() {
        String[][] variations = {
            {"XML", "xml", "Xml", "XmL", "xMl", "xML"},
            {"JSON", "json", "Json", "JsOn", "jSon", "jSON"},
            {"YAML", "yaml", "Yaml", "YaMl", "yAml", "yAML"}
        };

        OscalFormat[] expected = {OscalFormat.XML, OscalFormat.JSON, OscalFormat.YAML};

        for (int i = 0; i < variations.length; i++) {
            for (String variation : variations[i]) {
                assertEquals(expected[i], OscalFormat.fromString(variation),
                    "Failed for variation: " + variation);
            }
        }
    }

    @Test
    void testOrdinal() {
        assertEquals(0, OscalFormat.XML.ordinal());
        assertEquals(1, OscalFormat.JSON.ordinal());
        assertEquals(2, OscalFormat.YAML.ordinal());
    }

    @Test
    void testCompareTo() {
        assertTrue(OscalFormat.XML.compareTo(OscalFormat.JSON) < 0);
        assertTrue(OscalFormat.JSON.compareTo(OscalFormat.YAML) < 0);
        assertTrue(OscalFormat.YAML.compareTo(OscalFormat.XML) > 0);
        assertEquals(0, OscalFormat.XML.compareTo(OscalFormat.XML));
    }
}
