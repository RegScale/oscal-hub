package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class FedrampPoamExcelParserTest {

    @Test
    void parsesOpenAndClosedSheets() throws Exception {
        byte[] xlsx = buildWorkbook(true, true);
        ParsedPoam parsed = new FedrampPoamExcelParser().parse(new ByteArrayInputStream(xlsx));

        assertThat(parsed.items()).hasSize(2);

        var open = parsed.items().stream().filter(i -> "P-1".equals(i.externalId())).findFirst().orElseThrow();
        assertThat(open.status()).isEqualTo(ConMonItemStatus.OPEN);
        assertThat(open.title()).isEqualTo("Open weakness");
        assertThat(open.severity()).isEqualTo("HIGH");

        var closed = parsed.items().stream().filter(i -> "P-2".equals(i.externalId())).findFirst().orElseThrow();
        assertThat(closed.status()).isEqualTo(ConMonItemStatus.CLOSED);
    }

    @Test
    void parsesOnlyOpenSheet() throws Exception {
        byte[] xlsx = buildWorkbook(true, false);
        ParsedPoam parsed = new FedrampPoamExcelParser().parse(new ByteArrayInputStream(xlsx));
        assertThat(parsed.items()).hasSize(1);
        assertThat(parsed.items().get(0).status()).isEqualTo(ConMonItemStatus.OPEN);
    }

    @Test
    void rejectsWorkbookWithoutPoamSheets() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            wb.createSheet("Cover Page");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            byte[] xlsx = out.toByteArray();

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> new FedrampPoamExcelParser().parse(new ByteArrayInputStream(xlsx)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("POA&M");
        }
    }

    private byte[] buildWorkbook(boolean openSheet, boolean closedSheet) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            if (openSheet) {
                Sheet s = wb.createSheet("Open POA&M Items");
                writeHeader(s);
                Row r = s.createRow(1);
                r.createCell(0).setCellValue("P-1");
                r.createCell(1).setCellValue("Open weakness");
                r.createCell(2).setCellValue("Description here");
                r.createCell(3).setCellValue("High");
                r.createCell(4).setCellValue("");      // scheduled
                r.createCell(5).setCellValue("");      // actual
                r.createCell(6).setCellValue("alice"); // POC
                r.createCell(7).setCellValue("Ongoing"); // raw status
            }
            if (closedSheet) {
                Sheet s = wb.createSheet("Closed POA&M Items");
                writeHeader(s);
                Row r = s.createRow(1);
                r.createCell(0).setCellValue("P-2");
                r.createCell(1).setCellValue("Closed weakness");
                r.createCell(2).setCellValue("Description here");
                r.createCell(3).setCellValue("Moderate");
                r.createCell(4).setCellValue("");
                r.createCell(5).setCellValue("");
                r.createCell(6).setCellValue("alice");
                r.createCell(7).setCellValue("Completed");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void writeHeader(Sheet s) {
        Row h = s.createRow(0);
        String[] cols = {
            "POA&M Item ID", "Weakness Name", "Weakness Description", "Severity",
            "Scheduled Completion Date", "Actual Completion Date", "Point of Contact", "Status"
        };
        for (int i = 0; i < cols.length; i++) h.createCell(i).setCellValue(cols[i]);
    }
}
