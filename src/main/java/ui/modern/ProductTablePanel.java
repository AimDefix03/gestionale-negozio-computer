package ui.modern;

import model.Prodotto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class ProductTablePanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Codice", "Nome", "Brand", "Tipo", "Categoria", "Utilizzo", "Prezzo", "Sconto", "Quantita"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private List<Prodotto> products = new ArrayList<>();
    private int lowStockThreshold = -1;

    ProductTablePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Ui.styleTable(table);
        Ui.setColumnWidths(table, 82, 170, 110, 140, 95, 120, 90, 74, 82);
        table.setDefaultRenderer(Object.class, new StockAwareRenderer());
        add(Ui.scrollPane(table), BorderLayout.CENTER);
    }

    void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
        table.repaint();
    }

    void setProducts(List<Prodotto> products) {
        this.products = new ArrayList<>(products);
        model.setRowCount(0);
        for (Prodotto prodotto : products) {
            model.addRow(new Object[]{
                    prodotto.getCodice(),
                    prodotto.getNome(),
                    display(prodotto.getBrand()),
                    display(prodotto.getTipoProdotto()),
                    prodotto.getCategoria(),
                    display(prodotto.getUtilizzo()),
                    formatPrice(prodotto.getCostoScontato()),
                    prodotto.getSconto() + "%",
                    prodotto.getQuantita()
            });
        }
        table.clearSelection();
    }

    Prodotto getSelectedProduct() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        return products.get(modelRow);
    }

    List<Prodotto> getSelectedProducts() {
        int[] selectedRows = table.getSelectedRows();
        List<Prodotto> selectedProducts = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            selectedProducts.add(products.get(table.convertRowIndexToModel(selectedRow)));
        }
        return selectedProducts;
    }

    private String formatPrice(double value) {
        return String.format("%.2f euro", value);
    }

    private String display(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private class StockAwareRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            if (isSelected) {
                return component;
            }

            int modelRow = table.convertRowIndexToModel(row);
            Prodotto prodotto = products.get(modelRow);
            if (lowStockThreshold >= 0 && prodotto.getQuantita() == 0) {
                component.setBackground(new Color(255, 241, 242));
                component.setForeground(new Color(185, 28, 28));
            } else if (lowStockThreshold >= 0 && prodotto.getQuantita() <= lowStockThreshold) {
                component.setBackground(new Color(255, 251, 235));
                component.setForeground(new Color(180, 83, 9));
            } else {
                component.setBackground(row % 2 == 0 ? Ui.SURFACE : Ui.SURFACE_SOFT);
                component.setForeground(Ui.TEXT);
            }
            return component;
        }
    }
}
