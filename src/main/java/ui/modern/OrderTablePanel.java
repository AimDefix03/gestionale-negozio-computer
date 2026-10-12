package ui.modern;

import model.Order;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

class OrderTablePanel extends JPanel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private List<Order> orders = List.of();
    private Runnable selectionListener = () -> {
    };
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Codice", "Data", "Cliente", "Pagamento", "Articoli", "Totale"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    OrderTablePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Ui.styleTable(table);
        Ui.setColumnWidths(table, 90, 140, 120, 120, 120, 90);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selectionListener.run();
            }
        });
        add(Ui.scrollPane(table), BorderLayout.CENTER);
    }

    void setOrders(List<Order> orders) {
        this.orders = List.copyOf(orders);
        model.setRowCount(0);
        for (Order order : orders) {
            model.addRow(new Object[]{
                    order.code(),
                    order.timestamp().format(FORMATTER),
                    order.customer(),
                    order.paymentMethod(),
                    itemSummary(order),
                    String.format("%.2f euro", order.total())
            });
        }
    }

    Order getSelectedOrder() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= orders.size()) {
            return null;
        }
        return orders.get(table.convertRowIndexToModel(selectedRow));
    }

    void setSelectionListener(Runnable selectionListener) {
        this.selectionListener = selectionListener == null ? () -> {
        } : selectionListener;
    }

    private String itemSummary(Order order) {
        int totalQuantity = order.items().stream()
                .mapToInt(item -> item.quantity())
                .sum();
        return totalQuantity + " prodotto/i";
    }
}
