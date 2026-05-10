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

    @Test
    void digestEmitsCompactRuleLines() {
        String xccdf = """
                <Benchmark xmlns="http://checklists.nist.gov/xccdf/1.2">
                  <title>Red Hat Enterprise Linux 9 STIG</title>
                  <version>1.3</version>
                  <Rule id="V-230222" severity="medium">
                    <title>Set passwords to 15 characters</title>
                    <description>Long passwords slow brute force.</description>
                    <ident system="http://cyber.mil/cci">CCI-000196</ident>
                    <ident system="http://cyber.mil/cci">CCI-000205</ident>
                    <check>noisy oval payload</check>
                  </Rule>
                  <Rule id="V-230223" severity="high">
                    <title>Disable telnet</title>
                    <description>Cleartext credentials.</description>
                    <ident system="http://cyber.mil/cci">CCI-001942</ident>
                  </Rule>
                </Benchmark>
                """;
        String digest = trimmer.digest(xccdf);

        assertThat(digest).contains("Document: Red Hat Enterprise Linux 9 STIG");
        assertThat(digest).contains("Version: 1.3");
        assertThat(digest).contains("RULE V-230222 (severity=medium) [CCI-000196, CCI-000205]");
        assertThat(digest).contains("Title: Set passwords to 15 characters");
        assertThat(digest).contains("Description: Long passwords slow brute force.");
        assertThat(digest).contains("RULE V-230223 (severity=high) [CCI-001942]");
        assertThat(digest).contains("Title: Disable telnet");
        assertThat(digest).doesNotContain("oval payload");
        // Rough size sanity — digest should be markedly smaller than the source.
        assertThat(digest.length()).isLessThan(xccdf.length());
    }

    @Test
    void digestNonXccdfReturnedUnchanged() {
        String json = "{\"hello\":\"world\"}";
        assertThat(trimmer.digest(json)).isEqualTo(json);
    }

    @Test
    void digestExtractsOnlyVulnDiscussionFromStigDescriptions() {
        // DISA STIGs wrap the actual rationale inside <VulnDiscussion> with a
        // bunch of mostly-empty sibling sections. The digest should keep the
        // VulnDiscussion content and drop the noise.
        String xccdf = """
                <Benchmark xmlns="http://checklists.nist.gov/xccdf/1.2">
                  <Rule id="V-230222" severity="medium">
                    <title>Set passwords to 15 characters</title>
                    <description>&lt;VulnDiscussion&gt;Long passwords slow brute force attacks against captured hashes.&lt;/VulnDiscussion&gt;&lt;FalsePositives&gt;&lt;/FalsePositives&gt;&lt;FalseNegatives&gt;&lt;/FalseNegatives&gt;&lt;Documentable&gt;false&lt;/Documentable&gt;&lt;Mitigations&gt;&lt;/Mitigations&gt;&lt;SeverityOverrideGuidance&gt;&lt;/SeverityOverrideGuidance&gt;&lt;PotentialImpacts&gt;&lt;/PotentialImpacts&gt;&lt;ThirdPartyTools&gt;&lt;/ThirdPartyTools&gt;&lt;MitigationControl&gt;&lt;/MitigationControl&gt;&lt;Responsibility&gt;&lt;/Responsibility&gt;&lt;IAControls&gt;&lt;/IAControls&gt;</description>
                    <ident system="http://cyber.mil/cci">CCI-000196</ident>
                  </Rule>
                </Benchmark>
                """;
        String digest = trimmer.digest(xccdf);
        assertThat(digest).contains("Long passwords slow brute force");
        // Boilerplate section labels should NOT make it into the digest.
        assertThat(digest).doesNotContain("FalsePositives");
        assertThat(digest).doesNotContain("Mitigations");
        assertThat(digest).doesNotContain("SeverityOverrideGuidance");
        assertThat(digest).doesNotContain("IAControls");
    }
}
