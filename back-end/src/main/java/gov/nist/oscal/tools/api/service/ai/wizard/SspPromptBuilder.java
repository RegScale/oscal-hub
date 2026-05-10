package gov.nist.oscal.tools.api.service.ai.wizard;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SspPromptBuilder {

    public String outlinePrompt() {
        return """
            CRITICAL: Respond with a SINGLE raw JSON object only. No prose, no
            preamble, no ```json fences, no commentary.
            First character must be `{`, last character must be `}`.

            You are analyzing a source document to draft an OSCAL System
            Security Plan (SSP). The input may be any of:

            * An architecture or design document (PDF, Word, HTML)
            * A system description / discovery document
            * An existing draft SSP from another tool (Word, PDF)
            * Plain narrative text describing a system

            Extract everything you can identify about the system. Schema:

            {
              "title": "<SSP title — usually 'System Security Plan for <system>'>",
              "version": "<document version, default '1.0' if unspecified>",
              "publisher": "<organization that owns the system, or 'unspecified'>",
              "systemName": "<canonical system name>",
              "systemDescription": "<one paragraph: what the system does>",
              "systemId": "<external system identifier; generate a stable string if unspecified>",
              "sensitivityLevel": "low | moderate | high",
              "informationTypes": [{
                "uuid": "<v4 UUID>",
                "title": "<information type title>",
                "description": "<short description>",
                "categorizations": [{
                  "system": "https://doi.org/10.6028/NIST.SP.800-60v2r1",
                  "information-type-ids": ["<NIST 800-60 ID, e.g. C.2.4.1>"]
                }]
              }],
              "components": [{
                "uuid": "<v4 UUID>",
                "type": "software | service | policy | process | physical | system | this-system",
                "title": "<component name>",
                "description": "<what this component does and how it fits in the system>"
              }],
              "users": [{
                "uuid": "<v4 UUID>",
                "title": "<user role title, e.g. 'System Administrator'>",
                "role-ids": ["<role-id, e.g. 'admin'>"]
              }],
              "authorizationBoundary": "<paragraph describing the authorization boundary>",
              "controlIds": ["ac-1", "ac-2", "..."]
            }

            For `sensitivityLevel`, infer from FIPS-199 / 800-60 context if
            present, otherwise default to "moderate".

            For `controlIds`, list every NIST SP 800-53 control ID the source
            document mentions or addresses. Use canonical lowercase-hyphenated
            IDs (ac-1, not AC-1). This list is a fallback; if the user has
            picked a baseline profile separately, the platform will override
            it with the resolved control list. If the source document doesn't
            mention specific controls, return an empty array — do not invent.

            If a field is genuinely unknown, return an empty string for
            scalar fields or an empty array for list fields. Do not invent.
            """;
    }

    public String controlsPrompt(String systemTitle, List<String> controlIds) {
        return """
            CRITICAL: Respond with a SINGLE raw JSON array only. No prose. No
            preamble like "Here is the JSON" or "I'll draft...". No ```json
            fences. No commentary after the JSON. The first character of your
            reply MUST be `[` and the last MUST be `]`.

            Generate OSCAL implemented-requirement entries for the system "%s".

            Output schema — a JSON array of objects:

            [
              {
                "uuid": "<v4 UUID>",
                "control-id": "<control-id>",
                "description": "<implementation narrative grounded in source>",
                "props": [
                  { "name": "ai-confidence", "ns": "https://oscal-hub.io/ns", "value": "high" }
                ]
              },
              ...
            ]

            Control IDs to produce: %s

            Always emit one entry per requested control. Score each entry's
            ai-confidence:

            * "high"   — the source document directly addresses this control
                        with implementation specifics.
            * "medium" — the source document addresses the topic generally but
                        does not fully describe the implementation.
            * "low"    — the source document has no direct evidence; you had
                        to extrapolate or no relevant content exists.

            For "low"-confidence entries, set the description to exactly:
            "Source document does not address this control. To be completed."

            For "high" and "medium" entries, ground the description in
            specific configuration settings, statements, or recommendations
            from the source document. Do not invent content not present in
            the source document.
            """.formatted(systemTitle, String.join(", ", controlIds));
    }
}
