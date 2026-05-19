package gov.nist.oscal.tools.api.service.ai.wizard;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogPromptBuilder {

    public String outlinePrompt() {
        return """
            CRITICAL: Respond with a SINGLE raw JSON object only. No prose, no
            preamble, no ```json fences, no commentary. First character must be
            `{`, last character must be `}`.

            You are analyzing a controls publication to draft an OSCAL Catalog.
            Produce a JSON outline. Schema:

            {
              "title": "<extracted document title>",
              "version": "<extracted version or 'unspecified'>",
              "publisher": "<extracted org or 'unspecified'>",
              "families": [
                { "id": "<short upper-case family id, e.g. AC>",
                  "title": "<family title>",
                  "controlIds": ["ac-1", "ac-2", ...] }
              ]
            }

            Use canonical lowercase-hyphenated control IDs (ac-1, not AC-1). If the
            source document uses a different ID convention, infer an OSCAL-compatible
            mapping. If you cannot identify discrete control families, return one
            family named "all" containing every control.
            """;
    }

    public String familyPrompt(String familyId, String familyTitle, List<String> controlIds) {
        return familyPrompt(familyId, familyTitle, controlIds, false);
    }

    /**
     * Builds the per-family generation prompt.
     *
     * <p>The base prompt now includes a "Style guidance" clause that asks the
     * model to stay at the policy / process / governance abstraction level
     * appropriate for compliance catalogs — this both produces better OSCAL
     * output and avoids tripping Anthropic's output safety filter on
     * security-heavy families (operations security, incident response,
     * penetration testing, vulnerability management) where the source document
     * tends to mention specific attack techniques.
     *
     * <p>When {@code safeMode} is true, an additional "SAFETY RETRY" preface
     * is prepended that tightens the rules further. Used by the wizard after a
     * content-filter rejection to give the family one more chance before
     * skipping it.
     */
    public String familyPrompt(String familyId, String familyTitle, List<String> controlIds, boolean safeMode) {
        String safetyPreface = safeMode
                ? """
                  SAFETY RETRY: The previous attempt for this family was rejected by
                  Anthropic's output safety filter. Rewrite using ONLY high-level
                  compliance language. Strictly avoid:
                  - specific attack techniques, vectors, or named adversary TTPs
                  - exploit details, payloads, indicators of compromise, or signatures
                  - step-by-step offensive procedures or "how to" descriptions
                  Reference threats generically (e.g. "applicable cyber risks",
                  "threats to the system", "external adversaries"). Express each
                  control as an obligation: "must implement", "must monitor", "must
                  demonstrate", "must establish", "must maintain". Treat the output
                  as a compliance requirement document, not a threat-intelligence
                  summary.

                  """
                : "";
        return safetyPreface + """
            CRITICAL: Respond with a SINGLE raw JSON object only. No prose. No
            preamble like "Here is the JSON" or "I'll draft...". No ```json fences.
            No commentary after the JSON. The first character of your reply MUST
            be `{` and the last MUST be `}`.

            Generate the %s family ("%s") for the OSCAL Catalog.

            Output schema (https://pages.nist.gov/OSCAL/concepts/layer/control/catalog/):

            {
              "id": "%s",
              "class": "family",
              "title": "%s",
              "controls": [ ...one OSCAL control object per controlId... ]
            }

            Control IDs to produce: %s

            Style guidance: Write each control's narrative in defensive, policy-oriented
            compliance language. Describe what the organisation MUST do, MUST monitor
            for, and MUST be capable of. Refer to threats generically ("applicable
            cyber risks") rather than naming specific attack techniques, exploits,
            payloads, indicators, signatures, or adversary TTPs. Stay at the policy,
            process, governance, and accountability abstraction — a control statement
            is a requirement, not a threat-intelligence summary.

            For each control include: id, title, params (if any), parts (statement /
            guidance / objective / assessment), and props where the source supplies them.
            Do not invent content not present in the source document. Quote source
            statement text literally where possible.
            """.formatted(familyId, familyTitle, familyId, familyTitle, String.join(", ", controlIds));
    }

    public String mergePrompt(String title, String version, String publisher) {
        return """
            Step 3 — Wrap all generated families into a single OSCAL Catalog.

            Output ONLY a JSON object:

            {
              "catalog": {
                "uuid": "<generate a v4 UUID>",
                "metadata": {
                  "title": "%s",
                  "last-modified": "<ISO-8601 timestamp>",
                  "version": "%s",
                  "oscal-version": "1.1.2",
                  "parties": [ { "uuid": "<v4>", "type": "organization", "name": "%s" } ]
                },
                "groups": [ <all groups, in source-document order> ]
              }
            }

            Then call validate_oscal one final time. Return the JSON when valid; if
            still invalid after 3 attempts, return with a "validationWarnings" array.
            """.formatted(title, version, publisher);
    }
}
