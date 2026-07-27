package it.giovannidefilippo.gestionale.reporting;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Service
class ReportExportService {
    private static final byte[] UTF_8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SpreadsheetReportWriter spreadsheetWriter;
    private final PdfReportWriter pdfWriter;

    ReportExportService(SpreadsheetReportWriter spreadsheetWriter, PdfReportWriter pdfWriter) {
        this.spreadsheetWriter = spreadsheetWriter;
        this.pdfWriter = pdfWriter;
    }

    ReportFile sales(SalesReportResponse report, ReportFormat format) {
        byte[] content = switch (format) {
            case CSV -> salesCsv(report);
            case XLSX -> spreadsheetWriter.sales(report);
            case PDF -> pdfWriter.sales(report);
        };
        String baseName = "report-vendite-" + report.from() + "-" + report.to();
        return new ReportFile(content, format.mediaType(), baseName + "." + format.extension());
    }

    ReportFile inventory(InventoryReportResponse report, ReportFormat format) {
        byte[] content = switch (format) {
            case CSV -> inventoryCsv(report);
            case XLSX -> spreadsheetWriter.inventory(report);
            case PDF -> pdfWriter.inventory(report);
        };
        String baseName = "report-magazzino-" + report.generatedAt().toLocalDate();
        return new ReportFile(content, format.mediaType(), baseName + "." + format.extension());
    }

    private static byte[] salesCsv(SalesReportResponse report) {
        CsvBuilder csv = new CsvBuilder();
        csv.row("Report vendite", "Dal", report.from(), "Al", report.to(), "Stato", report.statusLabel());
        csv.row("Generato il", DATE_TIME.format(report.generatedAt()));
        csv.row();
        csv.row("Numero ordini", "Valore ordini", "Incassato", "Rimborsato", "Netto incassato", "Residuo", "Valore medio ordine");
        csv.row(report.orderCount(), report.orderValue(), report.paidAmount(), report.refundedAmount(), report.netCollectedAmount(), report.outstandingAmount(), report.averageOrderValue());
        csv.row();
        csv.row("Codice", "Data", "Cliente", "Stato", "Valore ordine", "Incassato", "Rimborsato", "Netto", "Residuo");
        report.orders().forEach(order -> csv.row(
                order.code(),
                DATE_TIME.format(order.timestamp()),
                order.customer(),
                order.statusLabel(),
                order.total(),
                order.paidAmount(),
                order.refundedAmount(),
                order.netCollectedAmount(),
                order.outstandingAmount()
        ));
        csv.row();
        csv.row("Prodotti principali", "Nome", "Quantita", "Valore negli ordini");
        report.topProducts().forEach(product -> csv.row(product.productCode(), product.productName(), product.quantity(), product.orderValue()));
        return csv.bytes();
    }

    private static byte[] inventoryCsv(InventoryReportResponse report) {
        CsvBuilder csv = new CsvBuilder();
        csv.row("Report magazzino", "Generato il", DATE_TIME.format(report.generatedAt()));
        csv.row();
        csv.row("Prodotti", "Unita fisiche", "Unita riservate", "Unita disponibili", "Valore magazzino", "Scorte basse", "Esauriti", "Disattivati");
        csv.row(report.productCount(), report.physicalUnits(), report.reservedUnits(), report.availableUnits(), report.inventoryValue(), report.lowStockCount(), report.outOfStockCount(), report.discontinuedCount());
        csv.row();
        csv.row("Codice", "Nome", "Categoria", "Brand", "Tipo", "Fisico", "Riservato", "Disponibile", "Prezzo", "Sconto %", "Prezzo netto", "Valore stock", "Stato stock", "Stato prodotto");
        report.products().forEach(product -> csv.row(
                product.code(),
                product.name(),
                product.categoryLabel(),
                product.brand(),
                product.productType(),
                product.quantity(),
                product.reservedQuantity(),
                product.availableQuantity(),
                product.price(),
                product.discount(),
                product.discountedPrice(),
                product.stockValue(),
                product.stockStatusLabel(),
                product.discontinued() ? "Disattivato" : "Attivo"
        ));
        return csv.bytes();
    }

    private static String safeText(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        String leftTrimmed = text.stripLeading();
        if (!leftTrimmed.isEmpty() && "=+-@".indexOf(leftTrimmed.charAt(0)) >= 0) {
            return "'" + text;
        }
        return text;
    }

    private static final class CsvBuilder {
        private final StringBuilder content = new StringBuilder();

        private void row(Object... values) {
            content.append(Arrays.stream(values)
                    .map(ReportExportService::safeText)
                    .map(CsvBuilder::escape)
                    .reduce((left, right) -> left + ";" + right)
                    .orElse(""));
            content.append("\r\n");
        }

        private byte[] bytes() {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.writeBytes(UTF_8_BOM);
            output.writeBytes(content.toString().getBytes(StandardCharsets.UTF_8));
            return output.toByteArray();
        }

        private static String escape(String value) {
            if (value.indexOf(';') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
            }
            return value;
        }
    }
}
