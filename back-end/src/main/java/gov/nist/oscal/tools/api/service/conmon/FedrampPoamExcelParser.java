package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a FedRAMP Rev 5 POA&amp;M Excel template (.xlsx).
 *
 * Expected shape:
 *   - One data sheet named "POA&amp;M" (or a close variant containing "poa&amp;m"/"poam"
 *     and not flagged as info/cover/README).
 *   - Headers are on row 2 (index 1). Row 1 contains sparse section group labels
 *     ("Identification", "Weakness Details", ...) which are ignored.
 *   - Data rows start on row 3 (index 2).
 *   - There is no explicit Status column. Items default to OPEN. Rows with
 *     "False Positive" set to a truthy value are marked CLOSED.
 */
@Component
public class FedrampPoamExcelParser {

    public ParsedPoam parse(InputStream input) {
        try (Workbook wb = WorkbookFactory.create(input)) {
            Sheet data = findDataSheet(wb);
            if (data == null) {
                List<String> actual = new ArrayList<>();
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    actual.add(wb.getSheetAt(i).getSheetName());
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Workbook does not contain a recognizable POA&M sheet. Found sheets: " + actual
                                + ". Expected a FedRAMP Rev 5 template with a sheet named \"POA&M\".");
            }
            return new ParsedPoam(null, null, null, null, parseSheet(data));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel workbook: " + e.getMessage(), e);
        }
    }

    /**
     * Find the POA&amp;M data sheet. Excludes cover/info/README sheets.
     * Match by sheet-name substring "poa&amp;m" or "poam".
     */
    private static Sheet findDataSheet(Workbook wb) {
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet s = wb.getSheetAt(i);
            String name = s.getSheetName() == null ? "" : s.getSheetName().toLowerCase();
            boolean nameMatch = name.contains("poa&m") || name.contains("poam");
            boolean isMeta = name.contains("info") || name.contains("cover") || name.contains("read");
            if (nameMatch && !isMeta) return s;
        }
        return null;
    }

    private List<ParsedPoamItem> parseSheet(Sheet sheet) {
        List<ParsedPoamItem> rows = new ArrayList<>();
        if (sheet.getLastRowNum() < 2) return rows; // Need header (row index 1) + at least one data row

        Row header = sheet.getRow(1);
        if (header == null) return rows;

        Map<String, Integer> col = new LinkedHashMap<>();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell c = header.getCell(i);
            if (c != null) {
                String v = stringValue(c);
                if (v != null) col.put(v.toLowerCase().trim(), i);
            }
        }

        Integer cId = matchColumn(col, "poa&m id", "poam id", "item id", "id");
        Integer cTitle = matchColumn(col, "weakness name", "title", "weakness");
        Integer cDesc = matchColumn(col, "weakness description", "description");
        Integer cDetector = matchColumn(col, "weakness detector source", "weakness source");
        Integer cSev = matchColumn(col, "original risk rating", "adjusted risk rating", "severity", "risk rating");
        Integer cSched = matchColumn(col, "scheduled completion date", "scheduled");
        Integer cPoc = matchColumn(col, "point of contact", "poc");
        Integer cFalsePositive = matchColumn(col, "false positive");

        if (cId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "POA&M sheet does not have a recognizable POA&M ID column on row 2. " +
                    "Expected one of: \"POA&M ID\", \"POAM ID\", \"Item ID\", or \"ID\". " +
                    "Found columns: " + col.keySet());
        }

        for (int r = 2; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String externalId = cellString(row, cId);
            if (externalId == null || externalId.isBlank()) continue;
            String title = cellString(row, cTitle);
            String description = cellString(row, cDesc);
            String severityRaw = cellString(row, cSev);
            String severity = normalizeSeverity(severityRaw);
            String detector = cellString(row, cDetector);
            LocalDate sched = cellDate(row, cSched);
            String poc = cellString(row, cPoc);
            String falsePositive = cellString(row, cFalsePositive);

            ConMonItemStatus status;
            String rawStatus;
            if (falsePositive != null && isTruthy(falsePositive)) {
                status = ConMonItemStatus.CLOSED;
                rawStatus = "False Positive";
            } else {
                status = ConMonItemStatus.OPEN;
                rawStatus = null;
            }

            Map<String, Object> extra = new HashMap<>();
            if (severityRaw != null && severity == null) extra.put("rawSeverity", severityRaw);

            rows.add(new ParsedPoamItem(
                    externalId,
                    title == null ? "(untitled)" : title,
                    description,
                    status,
                    rawStatus,
                    severity,
                    detector,
                    sched,
                    null,
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

    private static boolean isTruthy(String v) {
        if (v == null) return false;
        String n = v.trim().toLowerCase();
        return n.equals("yes") || n.equals("y") || n.equals("true") || n.equals("1") || n.equals("x");
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
