package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a FedRAMP POA&amp;M Excel template (.xlsx). Looks for sheets whose names
 * contain "Open POA&amp;M Items" and/or "Closed POA&amp;M Items" (case-insensitive).
 * Header row matched permissively against known columns.
 */
@Component
public class FedrampPoamExcelParser {

    public ParsedPoam parse(InputStream input) {
        try (Workbook wb = WorkbookFactory.create(input)) {
            Sheet open = findSheet(wb, "open poa&m items");
            Sheet closed = findSheet(wb, "closed poa&m items");
            if (open == null && closed == null) {
                throw new IllegalArgumentException(
                        "Workbook does not contain expected POA&M sheets " +
                        "(\"Open POA&M Items\" or \"Closed POA&M Items\").");
            }

            List<ParsedPoamItem> items = new ArrayList<>();
            if (open != null) {
                items.addAll(parseSheet(open, ConMonItemStatus.OPEN));
            }
            if (closed != null) {
                items.addAll(parseSheet(closed, ConMonItemStatus.CLOSED));
            }
            return new ParsedPoam(null, null, null, null, items);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel workbook: " + e.getMessage(), e);
        }
    }

    private Sheet findSheet(Workbook wb, String needle) {
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet s = wb.getSheetAt(i);
            if (s.getSheetName() != null && s.getSheetName().toLowerCase().contains(needle)) return s;
        }
        return null;
    }

    private List<ParsedPoamItem> parseSheet(Sheet sheet, ConMonItemStatus sheetStatus) {
        List<ParsedPoamItem> rows = new ArrayList<>();
        if (sheet.getLastRowNum() < 1) return rows;

        Row header = sheet.getRow(0);
        if (header == null) return rows;

        Map<String, Integer> col = new LinkedHashMap<>();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell c = header.getCell(i);
            if (c != null) {
                String v = stringValue(c);
                if (v != null) col.put(v.toLowerCase().trim(), i);
            }
        }

        Integer cId = matchColumn(col, "poa&m item id", "item id", "id");
        Integer cTitle = matchColumn(col, "weakness name", "title", "weakness");
        Integer cDesc = matchColumn(col, "weakness description", "description");
        Integer cSev = matchColumn(col, "severity");
        Integer cSched = matchColumn(col, "scheduled completion date", "scheduled");
        Integer cActual = matchColumn(col, "actual completion date", "actual");
        Integer cPoc = matchColumn(col, "point of contact", "poc");
        Integer cStatus = matchColumn(col, "status");

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String externalId = cellString(row, cId);
            if (externalId == null || externalId.isBlank()) continue;
            String title = cellString(row, cTitle);
            String description = cellString(row, cDesc);
            String severityRaw = cellString(row, cSev);
            String severity = normalizeSeverity(severityRaw);
            LocalDate sched = cellDate(row, cSched);
            LocalDate actual = cellDate(row, cActual);
            String poc = cellString(row, cPoc);
            String rawStatus = cellString(row, cStatus);

            Map<String, Object> extra = new HashMap<>();
            if (severityRaw != null && severity == null) extra.put("rawSeverity", severityRaw);

            rows.add(new ParsedPoamItem(
                    externalId,
                    title == null ? "(untitled)" : title,
                    description,
                    sheetStatus,
                    rawStatus,
                    severity,
                    null,
                    sched,
                    actual,
                    poc,
                    null,
                    extra));
        }
        return rows;
    }

    private static Integer matchColumn(Map<String, Integer> headers, String... candidates) {
        for (String c : candidates) {
            for (var e : headers.entrySet()) {
                if (e.getKey().contains(c)) return e.getValue();
            }
        }
        return null;
    }

    private static String stringValue(Cell c) {
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(c)
                    ? c.getDateCellValue().toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
                    : Double.toString(c.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(c.getBooleanCellValue());
            default -> null;
        };
    }

    private static String cellString(Row row, Integer idx) {
        if (idx == null) return null;
        return stringValue(row.getCell(idx));
    }

    private static LocalDate cellDate(Row row, Integer idx) {
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return c.getDateCellValue().toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate();
        }
        String s = stringValue(c);
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String normalizeSeverity(String raw) {
        if (raw == null) return null;
        String n = raw.trim().toLowerCase();
        return switch (n) {
            case "low" -> "LOW";
            case "moderate", "medium", "med" -> "MODERATE";
            case "high" -> "HIGH";
            case "critical" -> "CRITICAL";
            default -> null;
        };
    }
}
