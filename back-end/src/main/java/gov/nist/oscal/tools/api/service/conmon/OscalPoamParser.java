package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonSourceFormat;
import gov.nist.secauto.metaschema.core.datatype.markup.MarkupLine;
import gov.nist.secauto.metaschema.core.datatype.markup.MarkupMultiline;
import gov.nist.secauto.metaschema.databind.io.Format;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.Metadata;
import gov.nist.secauto.oscal.lib.model.PlanOfActionAndMilestones;
import gov.nist.secauto.oscal.lib.model.PoamItem;
import gov.nist.secauto.oscal.lib.model.Property;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses an OSCAL POAM document (JSON, XML, or YAML) into the parser's
 * intermediate ParsedPoam shape.
 *
 * <p>Typed calls are used throughout (no reflection) so that MarkupLine and
 * MarkupMultiline fields are rendered via {@code toMarkdown()} — which returns
 * the actual text — rather than {@code toString()}, which returns the internal
 * AST debug representation and NOT the text content.
 */
@Component
public class OscalPoamParser {

    public ParsedPoam parse(InputStream input, ConMonSourceFormat sourceFormat) {
        Format mFormat = switch (sourceFormat) {
            case OSCAL_JSON -> Format.JSON;
            case OSCAL_XML -> Format.XML;
            case OSCAL_YAML -> Format.YAML;
            default -> throw new IllegalArgumentException("Not an OSCAL format: " + sourceFormat);
        };

        Path tmp;
        try {
            tmp = Files.createTempFile("conmon-poam-", "." + mFormat.name().toLowerCase());
            Files.copy(input, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to buffer upload to temp file", e);
        }

        try {
            PlanOfActionAndMilestones poam = OscalBindingContext.instance()
                    .newDeserializer(mFormat, PlanOfActionAndMilestones.class)
                    .deserialize(tmp);

            return toParsedPoam(poam);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OSCAL POAM: " + e.getMessage(), e);
        } finally {
            try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
        }
    }

    private static ParsedPoam toParsedPoam(PlanOfActionAndMilestones poam) {
        String uuid = poam.getUuid() == null ? null : poam.getUuid().toString();

        Metadata metadata = poam.getMetadata();
        String oscalVersion = metadata == null ? null : metadata.getOscalVersion();
        String title = metadata == null ? null : markupLineToString(metadata.getTitle());
        LocalDateTime lastModified = metadata == null ? null
                : parseZonedToLocal(metadata.getLastModified());

        List<PoamItem> rawItems = poam.getPoamItems();
        List<ParsedPoamItem> items = new ArrayList<>(rawItems == null ? 0 : rawItems.size());
        if (rawItems != null) {
            for (PoamItem item : rawItems) {
                items.add(toParsedItem(item));
            }
        }
        return new ParsedPoam(uuid, oscalVersion, title, lastModified, items);
    }

    private static ParsedPoamItem toParsedItem(PoamItem item) {
        String externalId = item.getUuid() == null ? null : item.getUuid().toString();
        String itemTitle = markupLineToString(item.getTitle());
        String description = markupMultilineToString(item.getDescription());

        // Look for FedRAMP-style status prop (any namespace)
        List<Property> props = item.getProps();
        String statusKeyword = null;
        Map<String, Object> extra = new HashMap<>();
        if (props != null) {
            for (Property p : props) {
                String name = p.getName();
                String value = p.getValue();
                if ("status".equalsIgnoreCase(name)) {
                    statusKeyword = value;
                } else if (name != null) {
                    extra.put("prop:" + name, value);
                }
            }
        }

        // Linked-finding rollup (best-effort; many POAMs don't include this)
        List<String> findingStatuses = new ArrayList<>();
        List<PoamItem.RelatedFinding> relatedFindings = item.getRelatedFindings();
        if (relatedFindings != null) {
            for (PoamItem.RelatedFinding rf : relatedFindings) {
                // RelatedFinding only carries a finding-uuid reference; no inline status
            }
        }

        var derived = PoamStatusDeriver.derive(statusKeyword, findingStatuses);

        return new ParsedPoamItem(
                externalId,
                itemTitle == null ? "(untitled)" : itemTitle,
                description,
                derived.status(),
                derived.rawStatus(),
                null,   // severity not standard on poam-item
                null,
                null,
                null,
                null,
                null,
                extra
        );
    }

    // -------------------------------------------------------------------------
    // Markup helpers — use toMarkdown() to get actual text, not toString() which
    // returns the internal AST debug representation.
    // -------------------------------------------------------------------------

    private static String markupLineToString(MarkupLine markup) {
        if (markup == null) return null;
        return markup.toMarkdown();
    }

    private static String markupMultilineToString(MarkupMultiline markup) {
        if (markup == null) return null;
        return markup.toMarkdown();
    }

    private static LocalDateTime parseZonedToLocal(java.time.ZonedDateTime zdt) {
        if (zdt == null) return null;
        return zdt.toLocalDateTime();
    }
}
