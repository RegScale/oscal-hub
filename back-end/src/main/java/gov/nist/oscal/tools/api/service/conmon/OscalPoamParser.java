package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonSourceFormat;
import gov.nist.secauto.metaschema.databind.io.Format;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.PlanOfActionAndMilestones;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses an OSCAL POAM document (JSON, XML, or YAML) into the parser's
 * intermediate ParsedPoam shape. Reflection is used for traversing the
 * Metaschema-bound model to keep this resilient to library version
 * differences in field-name casing or wrapper classes.
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
        String uuid = invokeStringGetter(poam, "getUuid");
        Object metadata = invokeGetter(poam, "getMetadata");
        String oscalVersion = metadata == null ? null : invokeStringGetter(metadata, "getOscalVersion");
        String title = metadata == null ? null : invokeStringGetter(metadata, "getTitle");
        Object lastModifiedRaw = metadata == null ? null : invokeGetter(metadata, "getLastModified");
        LocalDateTime lastModified = parseInstantToLocal(lastModifiedRaw);

        Object itemsRaw = invokeGetter(poam, "getPoamItems");
        List<?> rawItems = itemsRaw instanceof List<?> ? (List<?>) itemsRaw : List.of();

        List<ParsedPoamItem> items = new ArrayList<>(rawItems.size());
        for (Object raw : rawItems) {
            items.add(toParsedItem(raw));
        }
        return new ParsedPoam(uuid, oscalVersion, title, lastModified, items);
    }

    private static ParsedPoamItem toParsedItem(Object raw) {
        String externalId = invokeStringGetter(raw, "getUuid");
        String itemTitle = invokeStringGetter(raw, "getTitle");
        String description = invokeStringGetter(raw, "getDescription");

        // Look for FedRAMP-style status prop
        Object propsRaw = invokeGetter(raw, "getProps");
        List<?> props = propsRaw instanceof List<?> ? (List<?>) propsRaw : List.of();
        String statusKeyword = null;
        Map<String, Object> extra = new HashMap<>();
        for (Object p : props) {
            String name = invokeStringGetter(p, "getName");
            String value = invokeStringGetter(p, "getValue");
            if ("status".equalsIgnoreCase(name)) {
                statusKeyword = value;
            } else if (name != null) {
                extra.put("prop:" + name, value);
            }
        }

        // Linked-finding rollup (best-effort; many POAMs don't include this)
        List<String> findingStatuses = new ArrayList<>();
        Object related = invokeGetter(raw, "getRelatedFindings");
        if (related instanceof List<?> rl) {
            for (Object rf : rl) {
                String fStatus = invokeStringGetter(rf, "getStatus");
                if (fStatus != null) findingStatuses.add(fStatus);
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

    private static Object invokeGetter(Object target, String name) {
        if (target == null) return null;
        try {
            var m = target.getClass().getMethod(name);
            return m.invoke(target);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static String invokeStringGetter(Object target, String name) {
        Object v = invokeGetter(target, name);
        return v == null ? null : v.toString();
    }

    private static LocalDateTime parseInstantToLocal(Object instant) {
        if (instant == null) return null;
        try {
            if (instant instanceof java.time.Instant i) return LocalDateTime.ofInstant(i, ZoneOffset.UTC);
            if (instant instanceof java.time.OffsetDateTime o) return o.toLocalDateTime();
            if (instant instanceof java.time.ZonedDateTime z) return z.toLocalDateTime();
        } catch (Exception ignore) {}
        return null;
    }
}
