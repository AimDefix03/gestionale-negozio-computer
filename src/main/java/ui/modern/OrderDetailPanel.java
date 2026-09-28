package ui.modern;

import model.Order;
import model.OrderItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;

class OrderDetailPanel extends JPanel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    OrderDetailPanel(Order order) {
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setBackground(Ui.BACKGROUND);
        add(buildHeader(order), BorderLayout.NORTH);
        add(buildItemsTable(order), BorderLayout.CENTER);
        add(buildTotal(order), BorderLayout.SOUTH);
    }

    private JPanel buildHeader(Order order) {
        JPanel header = Ui.card(new GridLayout(0, 2, 18, 8));
        header.add(label("Ordine"));
        header.add(value(order.code()));
        header.add(label("Data"));
        header.add(value(order.timestamp().format(FORMATTER)));
        header.add(label("Cliente"));
        header.add(value(order.customer()));
        header.add(label("Pagamento"));
        header.add(value(order.paymentMethod()));
        return header;
    }

    private JScrollPane buildItemsTable(Order order) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Codice", "Prodotto", "Quantita", "Prezzo unitario", "Subtotale"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (OrderItem item : order.items()) {
            model.addRow(new Object[]{
                    item.productCode(),
                    item.productName(),
                    item.quantity(),
                    String.format("%.2f euro", item.unitPrice()),
                    String.format("%.2f euro", item.lineTotal())
            });
        }

        JTable table = new JTable(model);
        Ui.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return Ui.scrollPane(table);
    }

    private JPanel buildTotal(Order order) {
        JPanel panel = Ui.card(new BorderLayout());
        JLabel total = Ui.title("Totale ordine: " + String.format("%.2f euro", order.total()), 18);
        panel.add(total, BorderLayout.EAST);
        return panel;
    }

    private JLabel label(String text) {
        return Ui.eyebrow(text.toUpperCase());
    }

    private JLabel value(String text) {
        JLabel label = Ui.text(text);
        label.setForeground(Ui.TEXT);
        return label;
    }
}
