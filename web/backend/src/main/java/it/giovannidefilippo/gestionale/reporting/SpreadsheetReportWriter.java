package it.giovannidefilippo.gestionale.reporting;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
class SpreadsheetReportWriter {
    byte[] sales(SalesReportResponse report) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = styles(workbook);
            Sheet sheet = workbook.createSheet("Vendite");
            int rowIndex = title(sheet, styles, "Report vendite", "Periodo " + report.from() + " - " + report.to() + " | " + report.statusLabel());
            rowIndex = summary(sheet, styles, rowIndex, List.of(
                    new Metric("Numero ordini", report.orderCount()),
                    new Metric("Valore ordini", report.orderValue()),
                    new Metric("Incassato", report.paidAmount()),
                    new Metric("Rimborsato", report.refundedAmount()),
                    new Metric("Netto incassato", report.netCollectedAmount()),
                    new Metric("Residuo", report.outstandingAmount()),
                    new Metric("Valore medio", report.averageOrderValue())
            ));
            rowIndex += 2;
            Row header = sheet.createRow(rowIndex++);
            writeHeader(header, styles, "Codice", "Data", "Cliente", "Stato", "Valore ordine", "Incassato", "Rimborsato", "Netto", "Residuo");
            int tableStart = rowIndex - 1;
            for (SalesReportResponse.SalesOrderRow order : report.orders()) {
                Row row = sheet.createRow(rowIndex++);
                text(row, 0, order.code(), styles.text());
                dateTime(row, 1, order.timestamp(), styles.dateTime());
                text(row, 2, order.customer(), styles.text());
                text(row, 3, order.statusLabel(), styles.text());
                number(row, 4, order.total(), styles.currency());
                number(row, 5, order.paidAmount(), styles.currency());
                number(row, 6, order.refundedAmount(), styles.currency());
                number(row, 7, order.netCollectedAmount(), styles.currency());
                number(row, 8, order.outstandingAmount(), styles.currency());
            }
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(tableStart, Math.max(tableStart, rowIndex - 1), 0, 8));
            sheet.createFreezePane(0, tableStart + 1);
            rowIndex += 2;
            Row topHeader = sheet.createRow(rowIndex++);
            writeHeader(topHeader, styles, "Prodotto", "Nome", "Quantita", "Valore negli ordini");
            for (SalesReportResponse.TopProductRow product : report.topProducts()) {
                Row row = sheet.createRow(rowIndex++);
                text(row, 0, product.productCode(), styles.text());
                text(row, 1, product.productName(), styles.text());
                integer(row, 2, product.quantity(), styles.integer());
                number(row, 3, product.orderValue(), styles.currency());
            }
            finish(sheet, 9);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Impossibile generare il file Excel del report vendite.", exception);
        }
    }

    byte[] inventory(InventoryReportResponse report) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = styles(workbook);
            Sheet sheet = workbook.createSheet("Magazzino");
            int rowIndex = title(sheet, styles, "Report magazzino", "Snapshot del " + report.generatedAt().toLocalDate());
            rowIndex = summary(sheet, styles, rowIndex, List.of(
                    new Metric("Prodotti", report.productCount()),
                    new Metric("Unita fisiche", report.physicalUnits()),
                    new Metric("Riservate", report.reservedUnits()),
                    new Metric("Disponibili", report.availableUnits()),
                    new Metric("Valore magazzino", report.inventoryValue()),
                    new Metric("Scorte basse", report.lowStockCount()),
                    new Metric("Esauriti", report.outOfStockCount()),
                    new Metric("Disattivati", report.discontinuedCount())
            ));
            rowIndex += 2;
            Row header = sheet.createRow(rowIndex++);
            writeHeader(header, styles, "Codice", "Nome", "Categoria", "Brand", "Tipo", "Fisico", "Riservato", "Disponibile", "Prezzo", "Sconto %", "Prezzo netto", "Valore stock", "Stato stock", "Stato prodotto");
            int tableStart = rowIndex - 1;
            for (InventoryReportResponse.InventoryProductRow product : report.products()) {
                Row row = sheet.createRow(rowIndex++);
                text(row, 0, product.code(), styles.text());
                text(row, 1, product.name(), styles.text());
                text(row, 2, product.categoryLabel(), styles.text());
                text(row, 3, product.brand(), styles.text());
                text(row, 4, product.productType(), styles.text());
                integer(row, 5, product.quantity(), styles.integer());
                integer(row, 6, product.reservedQuantity(), styles.integer());
                integer(row, 7, product.availableQuantity(), styles.integer());
                number(row, 8, product.price(), styles.currency());
                number(row, 9, product.discount(), styles.percentage());
                number(row, 10, product.discountedPrice(), styles.currency());
                number(row, 11, product.stockValue(), styles.currency());
                text(row, 12, product.stockStatusLabel(), styles.text());
                text(row, 13, product.discontinued() ? "Disattivato" : "Attivo", styles.text());
            }
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(tableStart, Math.max(tableStart, rowIndex - 1), 0, 13));
            sheet.createFreezePane(0, tableStart + 1);
            finish(sheet, 14);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Impossibile generare il file Excel del report magazzino.", exception);
        }
    }

    private static int title(Sheet sheet, Styles styles, String title, String subtitle) {
        Row titleRow = sheet.createRow(0);
        text(titleRow, 0, title, styles.title());
        Row subtitleRow = sheet.createRow(1);
        text(subtitleRow, 0, subtitle, styles.subtitle());
        return 3;
    }

    private static int summary(Sheet sheet, Styles styles, int rowIndex, List<Metric> metrics) {
        Row labels = sheet.createRow(rowIndex++);
        Row values = sheet.createRow(rowIndex++);
        for (int index = 0; index < metrics.size(); index++) {
            Metric metric = metrics.get(index);
            text(labels, index, metric.label(), styles.metricLabel());
            if (metric.value() instanceof BigDecimal decimal) {
                number(values, index, decimal, styles.currency());
            } else if (metric.value() instanceof Number number) {
                integer(values, index, number.longValue(), styles.integer());
            } else {
                text(values, index, String.valueOf(metric.value()), styles.text());
            }
        }
        return rowIndex;
    }

    private static void writeHeader(Row row, Styles styles, String... labels) {
        for (int index = 0; index < labels.length; index++) {
            text(row, index, labels[index], styles.header());
        }
    }

    private static void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(safeText(value));
        cell.setCellStyle(style);
    }

    private static void integer(Row row, int column, long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void number(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private static void dateTime(Row row, int column, LocalDateTime value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void finish(Sheet sheet, int columns) {
        for (int column = 0; column < columns; column++) {
            sheet.autoSizeColumn(column);
            sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 768, 45 * 256));
        }
    }

    private static String safeText(String value) {
        String text = value == null ? "" : value;
        String leftTrimmed = text.stripLeading();
        if (!leftTrimmed.isEmpty() && "=+-@".indexOf(leftTrimmed.charAt(0)) >= 0) {
            return "'" + text;
        }
        return text;
    }

    private static Styles styles(Workbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 18);
        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);
        CellStyle subtitle = workbook.createCellStyle();
        subtitle.setFont(boldFont);
        CellStyle metricLabel = workbook.createCellStyle();
        metricLabel.setFont(boldFont);
        metricLabel.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        metricLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        CellStyle text = workbook.createCellStyle();
        CellStyle integer = workbook.createCellStyle();
        integer.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        CellStyle currency = workbook.createCellStyle();
        currency.setDataFormat(workbook.createDataFormat().getFormat("€ #,##0.00"));
        CellStyle percentage = workbook.createCellStyle();
        percentage.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
        CellStyle dateTime = workbook.createCellStyle();
        dateTime.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
        return new Styles(title, subtitle, metricLabel, header, text, integer, currency, percentage, dateTime);
    }

    private record Metric(String label, Object value) {
    }

    private record Styles(
            CellStyle title,
            CellStyle subtitle,
            CellStyle metricLabel,
            CellStyle header,
            CellStyle text,
            CellStyle integer,
            CellStyle currency,
            CellStyle percentage,
            CellStyle dateTime
    ) {
    }
}
