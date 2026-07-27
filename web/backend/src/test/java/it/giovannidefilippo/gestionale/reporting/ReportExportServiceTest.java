package it.giovannidefilippo.gestionale.reporting;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportExportServiceTest {
    private ReportExportService service;

    @BeforeEach
    void setUp() {
        service = new ReportExportService(new SpreadsheetReportWriter(), new PdfReportWriter());
    }

    @Test
    void csvUsesUtf8BomEscapesDelimitersAndNeutralizesSpreadsheetFormulas() {
        ReportFile file = service.sales(salesReport("=HYPERLINK(\"https://invalid\")", "Cliente; Test"), ReportFormat.CSV);

        assertThat(file.content()).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        String csv = new String(file.content(), StandardCharsets.UTF_8);
        assertThat(csv).contains("'=HYPERLINK");
        assertThat(csv).contains("\"Cliente; Test\"");
        assertThat(file.contentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(file.filename()).endsWith(".csv");
    }

    @Test
    void xlsxIsARealWorkbookWithTypedCurrencyAndNeutralizedText() throws Exception {
        ReportFile file = service.sales(salesReport("ORD-1", "=SUM(1,1)"), ReportFormat.XLSX);

        assertThat(file.content()).startsWith((byte) 'P', (byte) 'K');
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.content()))) {
            assertThat(workbook.getSheet("Vendite")).isNotNull();
            assertThat(workbook.getSheet("Vendite").getRow(8).getCell(2).getStringCellValue()).isEqualTo("'=SUM(1,1)");
            assertThat(workbook.getSheet("Vendite").getRow(8).getCell(4).getNumericCellValue()).isEqualTo(120.0);
        }
    }

    @Test
    void pdfExportsHaveAValidPdfSignatureForBothReports() {
        ReportFile sales = service.sales(salesReport("ORD-1", "Cliente"), ReportFormat.PDF);
        ReportFile inventory = service.inventory(inventoryReport(), ReportFormat.PDF);

        assertThat(new String(sales.content(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(new String(inventory.content(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(sales.contentType()).isEqualTo("application/pdf");
        assertThat(inventory.filename()).endsWith(".pdf");
    }

    private static SalesReportResponse salesReport(String code, String customer) {
        LocalDateTime generatedAt = LocalDateTime.of(2026, 7, 14, 10, 30);
        SalesReportResponse.SalesOrderRow order = new SalesReportResponse.SalesOrderRow(
                code,
                generatedAt.minusDays(1),
                customer,
                "FULFILLED",
                "Evaso",
                money("120.00"),
                money("100.00"),
                money("10.00"),
                money("90.00"),
                money("20.00")
        );
        return new SalesReportResponse(
                generatedAt,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 7, 14),
                "FULFILLED",
                "Evaso",
                1,
                money("120.00"),
                money("100.00"),
                money("10.00"),
                money("90.00"),
                money("20.00"),
                money("120.00"),
                List.of(order),
                List.of(new SalesReportResponse.TopProductRow("GPU-1", "Scheda grafica", 1, money("120.00")))
        );
    }

    private static InventoryReportResponse inventoryReport() {
        InventoryReportResponse.InventoryProductRow product = new InventoryReportResponse.InventoryProductRow(
                "GPU-1",
                "Scheda grafica",
                "HARDWARE",
                "Hardware",
                "Brand",
                "Scheda grafica",
                4,
                1,
                3,
                money("120.00"),
                money("10.00"),
                money("108.00"),
                money("432.00"),
                false,
                "LOW",
                "Scorta bassa"
        );
        return new InventoryReportResponse(
                LocalDateTime.of(2026, 7, 14, 10, 30),
                1,
                4,
                1,
                3,
                money("432.00"),
                1,
                0,
                0,
                List.of(product)
        );
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
