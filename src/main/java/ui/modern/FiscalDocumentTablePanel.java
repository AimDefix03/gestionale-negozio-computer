package ui.modern;

import model.FiscalDocument;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

class FiscalDocumentTablePanel extends JPanel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private List<FiscalDocument> documents = List.of();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Numero", "Tipo", "Stato", "Data", "Ordine", "Cliente", "Imponibile", "IVA", "Totale"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    FiscalDocumentTablePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Ui.styleTable(table);
        Ui.setColumnWidths(table, 90, 140, 90, 140, 95, 130, 95, 95, 95);
        add(Ui.scrollPane(table), BorderLayout.CENTER);
    }

    void setDocuments(List<FiscalDocument> documents) {
        this.documents = List.copyOf(documents);
        model.setRowCount(0);
        for (FiscalDocument document : documents) {
            model.addRow(new Object[]{
                    document.code(),
                    document.type().getLabel(),
                    document.status().getLabel(),
                    document.createdAt().format(FORMATTER),
                    document.relatedOrderCode(),
                    document.customer(),
                    formatCurrency(document.taxableAmount()),
                    formatCurrency(document.vatAmount()),
                    formatCurrency(document.totalAmount())
            });
        }
        table.clearSelection();
    }

    FiscalDocument getSelectedDocument() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= documents.size()) {
            return null;
        }
        return documents.get(table.convertRowIndexToModel(selectedRow));
    }

    private String formatCurrency(double value) {
        return String.format(java.util.Locale.ITALY, "%.2f euro", value);
    }
}
