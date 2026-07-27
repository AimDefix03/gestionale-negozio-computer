package it.giovannidefilippo.gestionale.reporting;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
class PdfReportWriter {
    private static final Color NAVY = new Color(15, 47, 87);
    private static final Color LIGHT_BLUE = new Color(232, 241, 252);
    private static final Color BORDER = new Color(195, 207, 222);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.ITALY);

    byte[] sales(SalesReportResponse report) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = open(output, "Report vendite", "Periodo " + report.from() + " - " + report.to() + " | " + report.statusLabel());
            document.add(summaryTable(new String[][]{
                    {"Numero ordini", String.valueOf(report.orderCount())},
                    {"Valore ordini", money(report.orderValue())},
                    {"Incassato", money(report.paidAmount())},
                    {"Rimborsato", money(report.refundedAmount())},
                    {"Netto incassato", money(report.netCollectedAmount())},
                    {"Residuo", money(report.outstandingAmount())},
                    {"Valore medio", money(report.averageOrderValue())}
            }));
            document.add(spacer());
            PdfPTable orders = table(new float[]{1.1f, 1.25f, 2f, 1.1f, 1.15f, 1.15f, 1.15f, 1.15f},
                    "Codice", "Data", "Cliente", "Stato", "Valore", "Incassato", "Rimborsato", "Residuo");
            report.orders().forEach(order -> {
                row(orders,
                        order.code(),
                        DATE_TIME.format(order.timestamp()),
                        order.customer(),
                        order.statusLabel(),
                        money(order.total()),
                        money(order.paidAmount()),
                        money(order.refundedAmount()),
                        money(order.outstandingAmount()));
            });
            document.add(orders);
            if (!report.topProducts().isEmpty()) {
                document.add(section("Prodotti principali"));
                PdfPTable products = table(new float[]{1.3f, 3f, 1f, 1.4f}, "Codice", "Nome", "Quantita", "Valore ordini");
                report.topProducts().forEach(product -> row(products, product.productCode(), product.productName(), String.valueOf(product.quantity()), money(product.orderValue())));
                document.add(products);
            }
            close(document, report.generatedAt());
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Impossibile generare il PDF del report vendite.", exception);
        }
    }

    byte[] inventory(InventoryReportResponse report) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = open(output, "Report magazzino", "Snapshot operativo del " + report.generatedAt().toLocalDate());
            document.add(summaryTable(new String[][]{
                    {"Prodotti", String.valueOf(report.productCount())},
                    {"Unita fisiche", String.valueOf(report.physicalUnits())},
                    {"Unita riservate", String.valueOf(report.reservedUnits())},
                    {"Unita disponibili", String.valueOf(report.availableUnits())},
                    {"Valore magazzino", money(report.inventoryValue())},
                    {"Scorte basse", String.valueOf(report.lowStockCount())},
                    {"Esauriti", String.valueOf(report.outOfStockCount())},
                    {"Disattivati", String.valueOf(report.discontinuedCount())}
            }));
            document.add(spacer());
            PdfPTable products = table(new float[]{1.05f, 1.8f, 1.05f, 1.15f, 1.35f, .7f, .7f, .7f, 1f, 1.1f, 1f},
                    "Codice", "Nome", "Categoria", "Brand", "Tipo", "Fisico", "Ris.", "Disp.", "Prezzo netto", "Valore stock", "Stato");
            report.products().forEach(product -> row(products,
                    product.code(),
                    product.name(),
                    product.categoryLabel(),
                    product.brand(),
                    product.productType(),
                    String.valueOf(product.quantity()),
                    String.valueOf(product.reservedQuantity()),
                    String.valueOf(product.availableQuantity()),
                    money(product.discountedPrice()),
                    money(product.stockValue()),
                    product.discontinued() ? "Disattivato" : product.stockStatusLabel()));
            document.add(products);
            close(document, report.generatedAt());
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Impossibile generare il PDF del report magazzino.", exception);
        }
    }

    private static Document open(ByteArrayOutputStream output, String title, String subtitle) throws DocumentException {
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter.getInstance(document, output);
        document.addTitle(title);
        document.addCreator("Gestionale Negozio Computer");
        document.open();
        Paragraph heading = new Paragraph(title, font(18, Font.BOLD, NAVY));
        heading.setSpacingAfter(4);
        document.add(heading);
        Paragraph subheading = new Paragraph(subtitle, font(9, Font.NORMAL, Color.DARK_GRAY));
        subheading.setSpacingAfter(14);
        document.add(subheading);
        return document;
    }

    private static void close(Document document, java.time.LocalDateTime generatedAt) throws DocumentException {
        Paragraph footer = new Paragraph("Report gestionale interno | Generato il " + DATE_TIME.format(generatedAt), font(8, Font.NORMAL, Color.GRAY));
        footer.setSpacingBefore(12);
        footer.setAlignment(Element.ALIGN_RIGHT);
        document.add(footer);
        document.close();
    }

    private static PdfPTable summaryTable(String[][] metrics) throws DocumentException {
        PdfPTable table = new PdfPTable(Math.min(metrics.length, 4));
        table.setWidthPercentage(100);
        for (String[] metric : metrics) {
            PdfPCell cell = new PdfPCell();
            cell.setPadding(8);
            cell.setBackgroundColor(LIGHT_BLUE);
            cell.setBorderColor(BORDER);
            Paragraph label = new Paragraph(metric[0], font(7, Font.BOLD, NAVY));
            Paragraph value = new Paragraph(metric[1], font(11, Font.BOLD, Color.BLACK));
            value.setSpacingBefore(3);
            cell.addElement(label);
            cell.addElement(value);
            table.addCell(cell);
        }
        int remainder = metrics.length % table.getNumberOfColumns();
        int missingCells = remainder == 0 ? 0 : table.getNumberOfColumns() - remainder;
        for (int index = 0; index < missingCells; index++) {
            PdfPCell empty = new PdfPCell(new Phrase(""));
            empty.setBorderColor(BORDER);
            table.addCell(empty);
        }
        return table;
    }

    private static PdfPTable table(float[] widths, String... headers) throws DocumentException {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        table.setHeaderRows(1);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font(7, Font.BOLD, Color.WHITE)));
            cell.setBackgroundColor(NAVY);
            cell.setBorderColor(NAVY);
            cell.setPadding(5);
            table.addCell(cell);
        }
        return table;
    }

    private static void row(PdfPTable table, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font(7, Font.NORMAL, Color.BLACK)));
            cell.setBorderColor(BORDER);
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    private static Paragraph section(String title) {
        Paragraph paragraph = new Paragraph(title, font(12, Font.BOLD, NAVY));
        paragraph.setSpacingBefore(14);
        paragraph.setSpacingAfter(6);
        return paragraph;
    }

    private static Paragraph spacer() {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(5);
        return paragraph;
    }

    private static Font font(int size, int style, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.WINANSI, false, size, style, color);
    }

    private static String money(BigDecimal value) {
        return MONEY.format(value);
    }
}
