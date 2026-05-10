package gov.nist.oscal.tools.api.service.ai.wizard;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PoamPromptBuilder {

    public String outlinePrompt() {
        return """
            CRITICAL: Respond with a SINGLE raw JSON object only. No prose, no
            preamble, no ```json fences, no commentary.
            First character must be `{`, last character must be `}`.

            You are analyzing a source document to draft an OSCAL Plan of
            Action and Milestones (POA&M). The input may be:

            * A FedRAMP POA&M spreadsheet (.xlsx) — recognize columns like
              POAM ID, Weakness Name, Weakness Description, Source, Asset
              Identifier, Point of Contact, Resources Required, Overall
              Remediation Plan, Original Detection Date, Scheduled
              Completion Date, Planned Milestones, Status, Risk Rating.
            * A CSV export of any POA&M tracker — same columns or similar.
            * A penetration-test report (PDF, Word, HTML) — each finding
              becomes one POA&M item with its severity and remediation
              recommendation.
            * A vulnerability scan summary — each unique weakness becomes
              one item.
            * Plain narrative text describing planned remediations.

            Identify every distinct weakness or finding. For each, capture a
            short, stable identifier you can use as a label (e.g. the
            POAM ID column, the finding number, or a slug derived from the
            title). The platform will use these IDs to chunk the per-item
            detail extraction across multiple LLM calls.

            Schema:

            {
              "title": "<POA&M document title — usually 'POA&M for <system>' or the spreadsheet's title>",
              "version": "<document version, default '1.0' if unspecified>",
              "publisher": "<organization that owns this POA&M, or 'unspecified'>",
              "systemTitle": "<system the POA&M applies to, or empty>",
              "itemIds": ["P-001", "P-002", "..."]
            }

            For `itemIds`, prefer the source's native identifiers (FedRAMP
            POAM IDs like "V-001", finding numbers like "F-12", or simple
            sequential labels if none exist). Use lowercase-hyphenated form
            if you generate IDs yourself. If the source has zero
            identifiable findings, return an empty `itemIds` array — do not
            invent.
            """;
    }

    public String itemsPrompt(String systemTitle, List<String> itemIds) {
        return """
            CRITICAL: Respond with a SINGLE raw JSON array only. No prose. No
            preamble like "Here is the JSON" or "I'll draft...". No ```json
            fences. No commentary after the JSON. The first character of your
            reply MUST be `[` and the last MUST be `]`.

            Generate OSCAL poam-item entries for the system "%s".

            Output schema — a JSON array of objects, one per requested item ID:

            [
              {
                "uuid": "<v4 UUID>",
                "title": "<short weakness/finding title>",
                "description": "<full narrative: weakness, impact, remediation plan>",
                "props": [
                  { "name": "poam-id", "value": "<source identifier, e.g. P-001>" },
                  { "name": "severity", "value": "high | moderate | low" },
                  { "name": "status", "value": "open | ongoing | risk-accepted | closed" },
                  { "name": "scheduled-completion-date", "value": "<YYYY-MM-DD or empty>" },
                  { "name": "ai-confidence", "ns": "https://oscal-hub.io/ns", "value": "high | medium | low" }
                ]
              },
              ...
            ]

            Item IDs to produce: %s

            Always emit one entry per requested item ID, in the same order.
            Score each entry's `ai-confidence`:

            * "high"   — the source document has a complete row/finding for
                        this item with title, description, severity, and a
                        remediation plan or due date.
            * "medium" — the source has the weakness but is missing one or
                        more of: severity, due date, remediation plan.
            * "low"    — the source mentions the item only in passing and
                        you had to extrapolate; or no direct evidence
                        exists. Description should be: "Source document
                        does not provide details for this item. To be
                        completed."

            Severity values: use exactly one of "high", "moderate", "low"
            (lowercase). Status values: use exactly one of "open",
            "ongoing", "risk-accepted", "closed".

            Date format: YYYY-MM-DD. Leave the value empty (empty string) if
            unknown. Do not invent dates.

            For items where the source supplies a value for a prop, use it.
            For items where the source is silent on a particular prop, omit
            that prop from the array (don't emit empty values except for
            scheduled-completion-date which always appears as a placeholder).
            """.formatted(systemTitle, String.join(", ", itemIds));
    }
}
