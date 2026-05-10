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

            You are analyzing a configuration guide to draft an OSCAL
            Component-definition. The input may be in any of these formats —
            recognize whichever you receive and extract accordingly:

            * XCCDF XML (DISA STIGs, OpenSCAP datastreams) — look for
              <xccdf:Benchmark>, <xccdf:Group>, <xccdf:Rule>; pull
              <xccdf:title>, <xccdf:version>, group/rule IDs; map CCI
              references (CCI-NNNNNN inside <xccdf:ident>) to the
              corresponding NIST SP 800-53 rev5 control IDs.
            * SCAP package contents (XCCDF + OVAL) — same as XCCDF; ignore
              OVAL <oval-def:definition> bodies, those are check logic, not
              control mappings.
            * DISA JSON STIG — look for "stig.title", "stig.version",
              groups[].rules[].rule_id and CCIs.
            * CIS Benchmark (PDF, HTML, JSON) — recommendations are usually
              numbered like "1.1.2 Ensure ..."; CIS publishes a 800-53
              mapping in their docs, use it when present.
            * Ansible playbook (YAML) — task names and tags often encode the
              STIG/CIS rule, e.g. tags: ['high', 'V-230222', 'CCI-000196'].
            * STIG Viewer CSV — columns typically include "V-ID", "Severity",
              "Title", "CCIs", "STIG ID".
            * SRG (Security Requirements Guide) PDF/HTML — higher-level
              parent doc; if specific STIG isn't available, use SRG entries
              directly mapped via their CCIs.
            * Plain configuration guide (PDF, HTML, DOCX) — extract product
              name, settings, and the controls they address.

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
