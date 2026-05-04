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
        return """
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
