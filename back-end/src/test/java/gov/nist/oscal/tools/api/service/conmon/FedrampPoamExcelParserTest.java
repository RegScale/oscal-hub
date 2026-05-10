package gov.nist.oscal.tools.api.service.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FedrampPoamExcelParserTest {

    @Test
    void parsesRev5SingleSheet() throws Exception {
        byte[] xlsx = buildRev5Workbook(true);
        ParsedPoam parsed = new FedrampPoamExcelParser().parse(new ByteArrayInputStream(xlsx));
        assertThat(parsed.items()).hasSize(2);

        var open = parsed.items().stream().filter(i -> "POAM-0001".equals(i.externalId())).findFirst().orElseThrow();
        assertThat(open.status()).isEqualTo(ConMonItemStatus.OPEN);
        assertThat(open.title()).isEqualTo("Open weakness");
        assertThat(open.severity()).isEqualTo("HIGH");
        assertThat(open.pointOfContact()).isEqualTo("alice");

        var closed = parsed.items().stream().filter(i -> "POAM-0002".equals(i.externalId())).findFirst().orElseThrow();
        assertThat(closed.status()).isEqualTo(ConMonItemStatus.CLOSED);
        assertThat(closed.rawStatus()).isEqualTo("False Positive");
    }

    @Test
    void ignoresInfoSheetAndPicksDataSheet() throws Exception {
        byte[] xlsx = buildRev5Workbook(true);
        ParsedPoam parsed = new FedrampPoamExcelParser().parse(new ByteArrayInputStream(xlsx));
        // Should have parsed from the "POA&M" sheet, not the "POA&M Info" cover sheet.
        assertThat(parsed.items()).hasSize(2);
    }

    @Test
    void rejectsWorkbookWithoutPoamSheet() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            wb.createSheet("Cover Page");
            wb.createSheet("README");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            byte[] xlsx = out.toByteArray();

            assertThatThrownBy(() -> new FedrampPoamExcelParser().parse(new ByteArrayInputStream(xlsx)))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .hasMessageContaining("POA&M")
                    .hasMessageContaining("Cover Page")
                    .hasMessageContaining("README");
        }
    }

    @Test
    void rejectsSheetWithoutPoamIdColumn() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("POA&M");
            // Row 1: ignored section labels
            s.createRow(0).createCell(0).setCellValue("Identification");
            // Row 2: headers but no POA&M ID column
            Row r2 = s.createRow(1);
            r2.createCell(0).setCellValue("Some random column");
            r2.createCell(1).setCellValue("Another column");
            // Row 3: would-be data
            Row r3 = s.createRow(2);
            r3.createCell(0).setCellValue("value");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            byte[] xlsx = out.toByteArray();

            assertThatThrownBy(() -> new FedrampPoamExcelParser().parse(new ByteArrayInputStream(xlsx)))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .hasMessageContaining("POA&M ID");
        }
    }

    /** Builds a minimal FedRAMP Rev 5-shaped workbook with one open and one false-positive item. */
    private byte[] buildRev5Workbook(boolean includeInfoSheet) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            if (includeInfoSheet) {
                Sheet info = wb.createSheet("POA&M Info");
                info.createRow(0).createCell(0).setCellValue("Cover page content");
            }

            Sheet s = wb.createSheet("POA&M");

            // Row 1 (index 0): sparse section labels
            Row r1 = s.createRow(0);
            r1.createCell(0).setCellValue("Identification");
            r1.createCell(2).setCellValue("Weakness Details");

            // Row 2 (index 1): real headers (Rev 5 column set)
            Row r2 = s.createRow(1);
            String[] cols = {
                "POA&M ID", "Controls Affected", "Weakness Name", "Weakness Description",
                "Weakness Detector Source", "Weakness Source Identifier", "Asset Identifier",
                "Point of Contact", "Resources Required ($)", "Overall Remediation Plan",
                "Original Detection Date", "Scheduled Completion Date", "Planned Milestones",
                "Milestone Changes", "Status Date", "Vendor Dependency",
                "Last Vendor Check-in Date", "Vendor Dependent Product Name",
                "Original Risk Rating", "Adjusted Risk Rating", "Risk Adjustment",
                "False Positive", "Operational Requirement", "Deviation Rationale",
                "Supporting Documents", "Comments", "Auto-Approve"
            };
            for (int i = 0; i < cols.length; i++) r2.createCell(i).setCellValue(cols[i]);

            // Row 3 (index 2): an open item
            Row r3 = s.createRow(2);
            r3.createCell(0).setCellValue("POAM-0001");
            r3.createCell(2).setCellValue("Open weakness");
            r3.createCell(3).setCellValue("Description");
            r3.createCell(7).setCellValue("alice");
            r3.createCell(18).setCellValue("High");

            // Row 4 (index 3): a false-positive item (CLOSED)
            Row r4 = s.createRow(3);
            r4.createCell(0).setCellValue("POAM-0002");
            r4.createCell(2).setCellValue("False positive weakness");
            r4.createCell(3).setCellValue("Not actually a finding");
            r4.createCell(7).setCellValue("bob");
            r4.createCell(18).setCellValue("Moderate");
            r4.createCell(21).setCellValue("Yes");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}
