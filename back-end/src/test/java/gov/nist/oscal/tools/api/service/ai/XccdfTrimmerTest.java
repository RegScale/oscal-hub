package gov.nist.oscal.tools.api.service.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XccdfTrimmerTest {

    private final XccdfTrimmer trimmer = new XccdfTrimmer();

    @Test
    void nonXccdfReturnedUnchanged() {
        String json = "{\"foo\":\"bar\"}";
        assertThat(trimmer.trim(json)).isEqualTo(json);
    }

    @Test
    void looksLikeXccdfDetectsBenchmark() {
        assertThat(trimmer.looksLikeXccdf(
                "<xccdf:Benchmark xmlns:xccdf=\"http://checklists.nist.gov/xccdf/1.2\"/>"))
                .isTrue();
    }

    @Test
    void stripsCheckAndFixBlocksKeepsTitleDescriptionIdent() {
        String xccdf = """
                <Benchmark xmlns="http://checklists.nist.gov/xccdf/1.2">
                  <Rule id="V-230222" severity="medium">
                    <title>Set passwords to 15 characters</title>
                    <description>Long passwords slow brute force.</description>
                    <ident system="http://cyber.mil/cci">CCI-000196</ident>
                    <fixtext>Edit /etc/security/pwquality.conf</fixtext>
                    <fix>echo minlen=15 >> /etc/security/pwquality.conf</fix>
                    <check system="http://oval.mitre.org/XMLSchema/oval-definitions-5">
                      <check-content-ref href="oval.xml" name="oval:rule:def:1"/>
                      <check-content>HUGE_OVAL_BLOB_HERE</check-content>
                    </check>
                  </Rule>
                </Benchmark>
                """;
        String trimmed = trimmer.trim(xccdf);
        assertThat(trimmed).contains("V-230222");
        assertThat(trimmed).contains("Set passwords to 15 characters");
        assertThat(trimmed).contains("Long passwords slow brute force");
        assertThat(trimmed).contains("CCI-000196");
        assertThat(trimmed).doesNotContain("HUGE_OVAL_BLOB_HERE");
        assertThat(trimmed).doesNotContain("oval.xml");
        assertThat(trimmed).doesNotContain("/etc/security/pwquality.conf");
        assertThat(trimmed.length()).isLessThan(xccdf.length());
    }

    @Test
    void stripsXccdfProfileButKeepsRules() {
        String xccdf = """
                <Benchmark xmlns="http://checklists.nist.gov/xccdf/1.2">
                  <Profile id="cat-1">
                    <select idref="V-1" selected="true"/>
                    <select idref="V-2" selected="true"/>
                  </Profile>
                  <Rule id="V-1"><title>Rule one</title></Rule>
                </Benchmark>
                """;
        String trimmed = trimmer.trim(xccdf);
        assertThat(trimmed).contains("Rule one");
        assertThat(trimmed).doesNotContain("idref=\"V-1\" selected");
    }

    @Test
    void malformedXmlFallsBackToVerbatim() {
        String broken = "<xccdf:Benchmark><Rule>oops no close";
        // Should not throw; should return original.
        assertThat(trimmer.trim(broken)).isEqualTo(broken);
    }
}
