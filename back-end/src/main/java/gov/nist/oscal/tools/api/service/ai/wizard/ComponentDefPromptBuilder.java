package gov.nist.oscal.tools.api.service.ai.wizard;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ComponentDefPromptBuilder {

    public String outlinePrompt() {
        return """
            CRITICAL: Respond with a SINGLE raw JSON object only. No prose, no
            preamble, no ```json fences, no commentary. First character must be
            `{`, last character must be `}`.

            You are analyzing a configuration guide (STIG, CIS Benchmark, or vendor
            hardening guide) to draft an OSCAL Component-definition.
            Produce a JSON outline. Schema:

            {
              "productTitle": "<extracted product/service name, e.g. 'Red Hat Enterprise Linux 9 STIG' or 'CIS Ubuntu 20.04 LTS Benchmark'>",
              "productDescription": "<one-paragraph description of what the product is and what this guide configures>",
              "componentType": "<one of 'software' | 'service' | 'policy' | 'process' | 'physical' | 'system' | 'this-system'; pick the closest match>",
              "version": "<extracted version of the guide>",
              "publisher": "<DISA, CIS, vendor name, etc.>",
              "catalogSource": "<URI of the controls catalog the recommendations map to; default to 'https://raw.githubusercontent.com/usnistgov/oscal-content/main/nist.gov/SP800-53/rev5/json/NIST_SP-800-53_rev5_catalog.json' if unspecified>",
              "controlIds": ["ac-1", "ac-2", ...]
            }

            Use canonical lowercase-hyphenated control IDs (ac-1, not AC-1). If the
            source document uses a different ID convention (e.g. STIG STIG-IDs or
            CCI numbers), infer an OSCAL-compatible mapping to SP 800-53 rev5 IDs.
            """;
    }

    public String componentPrompt(String productTitle, List<String> controlIds) {
        return """
            CRITICAL: Respond with a SINGLE raw JSON array only. No prose. No
            preamble like "Here is the JSON" or "I'll draft...". No ```json fences.
            No commentary after the JSON. The first character of your reply MUST
            be `[` and the last MUST be `]`.

            Generate OSCAL implemented-requirements entries for the product "%s".

            Output schema — a JSON array of objects:

            [
              {
                "uuid": "<v4 UUID>",
                "control-id": "<control-id>",
                "description": "<how this product addresses the control, grounded in the source document>"
              },
              ...
            ]

            Control IDs to produce: %s

            For each implemented-requirement, ground the description in specific
            configuration settings, rules, or recommendations from the source document.
            Do not invent content not present in the source document.
            """.formatted(productTitle, String.join(", ", controlIds));
    }

    public String mergePrompt(String productTitle, String productDescription,
                              String componentType, String version,
                              String publisher, String catalogSource) {
        return """
            Step 3 — Wrap all generated implemented-requirements into a single
            OSCAL Component-definition.

            Output ONLY a JSON object:

            {
              "component-definition": {
                "uuid": "<generate a v4 UUID>",
                "metadata": {
                  "title": "%s",
                  "last-modified": "<ISO-8601 timestamp>",
                  "version": "%s",
                  "oscal-version": "1.1.2",
                  "parties": [ { "uuid": "<v4>", "type": "organization", "name": "%s" } ]
                },
                "components": [{
                  "uuid": "<v4>",
                  "type": "%s",
                  "title": "%s",
                  "description": "%s",
                  "control-implementations": [{
                    "uuid": "<v4>",
                    "source": "%s",
                    "description": "Control implementations derived from the source document.",
                    "implemented-requirements": [ <ALL implemented-requirements from the per-chunk passes, merged> ]
                  }]
                }]
              }
            }
            """.formatted(productTitle, version, publisher,
                          componentType, productTitle,
                          productDescription.replace("\"", "\\\""),
                          catalogSource);
    }
}
