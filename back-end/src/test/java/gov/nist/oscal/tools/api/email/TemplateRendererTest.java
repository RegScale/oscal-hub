package gov.nist.oscal.tools.api.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemplateRendererTest {

    private TemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new TemplateRenderer();
    }

    @Test
    void substitutesPlaceholders() {
        String out = renderer.render("Hello ${name}", Map.of("name", "Travis"));
        assertEquals("Hello Travis", out);
    }

    @Test
    void escapesHtmlInUserInput() {
        String out = renderer.render("Msg: ${msg}", Map.of("msg", "<script>x</script>"));
        assertTrue(out.contains("&lt;script&gt;"), "expected HTML to be escaped, got: " + out);
        assertEquals("Msg: &lt;script&gt;x&lt;/script&gt;", out);
    }

    @Test
    void escapesAmpersandsAndQuotes() {
        String out = renderer.render("${v}", Map.of("v", "Tom & \"Jerry\""));
        assertEquals("Tom &amp; &quot;Jerry&quot;", out);
    }

    @Test
    void leavesUnknownPlaceholdersUntouched() {
        String out = renderer.render("Hello ${name} from ${unknown}", Map.of("name", "Travis"));
        // Implementation choice: missing keys are left as-is rather than throwing.
        assertTrue(out.contains("Travis"));
        assertTrue(out.contains("${unknown}"));
    }

    @Test
    void supportsMultiplePlaceholders() {
        String out = renderer.render("${greeting}, ${name}!",
            Map.of("greeting", "Hi", "name", "Travis"));
        assertEquals("Hi, Travis!", out);
    }

    @Test
    void rejectsNullTemplate() {
        assertThrows(IllegalArgumentException.class,
            () -> renderer.render(null, Map.of()));
    }

    @Test
    void loadsTemplateFromClasspath() {
        String out = renderer.renderFromClasspath(
            "email-templates/test-fixture.html", Map.of("name", "Travis"));
        assertTrue(out.contains("Travis"));
    }

    @Test
    void renderTextDoesNotEscapeHtmlEntities() {
        String out = renderer.renderText("Hello ${name}", Map.of("name", "Tom & Jerry"));
        assertEquals("Hello Tom & Jerry", out);
    }

    @Test
    void renderTextStillTreatsDollarAndBackslashLiterally() {
        String out = renderer.renderText("${v}", Map.of("v", "$1 \\n"));
        assertEquals("$1 \\n", out);
    }

    @Test
    void renderTextRejectsNullTemplate() {
        assertThrows(IllegalArgumentException.class,
            () -> renderer.renderText(null, Map.of()));
    }
}
