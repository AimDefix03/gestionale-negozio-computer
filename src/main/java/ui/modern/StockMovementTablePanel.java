package ui.modern;

import model.StockMovement;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

class StockMovementTablePanel extends JPanel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Data", "Utente", "Prodotto", "Tipo", "Quantità", "Prima", "Dopo", "Causale"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    StockMovementTablePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Ui.styleTable(table);
        Ui.setColumnWidths(table, 140, 110, 190, 90, 80, 72, 72, 220);
        add(Ui.scrollPane(table), BorderLayout.CENTER);
    }

    void setMovements(List<StockMovement> movements) {
        model.setRowCount(0);
        for (StockMovement movement : movements) {
            model.addRow(new Object[]{
                    movement.timestamp().format(FORMATTER),
                    movement.actor(),
                    movement.productCode() + " - " + movement.productName(),
                    movement.type().getLabel(),
                    movement.quantity(),
                    movement.previousQuantity(),
                    movement.newQuantity(),
                    movement.reason()
            });
        }
    }
}
