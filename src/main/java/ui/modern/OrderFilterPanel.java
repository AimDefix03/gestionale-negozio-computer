package ui.modern;

import strategy.PagamentoStrategyFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;

class OrderFilterPanel extends JPanel {
    static final String ALL_PAYMENTS = "Tutti i pagamenti";
    static final String SORT_NEWEST = "Data recente";
    static final String SORT_OLDEST = "Data meno recente";
    static final String SORT_CUSTOMER = "Cliente";
    static final String SORT_TOTAL_ASC = "Totale crescente";
    static final String SORT_TOTAL_DESC = "Totale decrescente";

    private final JTextField searchField = Ui.textField();
    private final JComboBox<String> paymentBox = new JComboBox<>(paymentOptions());
    private final JComboBox<String> sortBox = new JComboBox<>(new String[]{
            SORT_NEWEST,
            SORT_OLDEST,
            SORT_CUSTOMER,
            SORT_TOTAL_ASC,
            SORT_TOTAL_DESC
    });
    private final Consumer<OrderCatalogFilters> onFiltersChanged;

    OrderFilterPanel(Consumer<OrderCatalogFilters> onFiltersChanged) {
        this.onFiltersChanged = onFiltersChanged;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        Ui.styleComboBox(paymentBox);
        Ui.styleComboBox(sortBox);
        add(buildFields(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
        installListeners();
    }

    private JPanel buildFields() {
        JPanel panel = Ui.panel(new GridLayout(1, 3, 10, 0));
        panel.setOpaque(false);
        addField(panel, "Cerca ordine", searchField);
        addField(panel, "Pagamento", paymentBox);
        addField(panel, "Ordina per", sortBox);
        return panel;
    }

    private JPanel buildActions() {
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton resetButton = Ui.secondaryButton("Reset filtri");
        resetButton.addActionListener(e -> resetFilters());
        actions.add(resetButton);
        return actions;
    }

    private void addField(JPanel panel, String label, JComponent field) {
        JPanel wrapper = Ui.panel(new BorderLayout(0, 5));
        wrapper.setOpaque(false);
        JLabel text = Ui.text(label);
        text.setForeground(Ui.TEXT);
        wrapper.add(text, BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);
        panel.add(wrapper);
    }

    private void installListeners() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                notifyFiltersChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                notifyFiltersChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                notifyFiltersChanged();
            }
        });
        paymentBox.addActionListener(event -> notifyFiltersChanged());
        sortBox.addActionListener(event -> notifyFiltersChanged());
    }

    private void resetFilters() {
        searchField.setText("");
        paymentBox.setSelectedItem(ALL_PAYMENTS);
        sortBox.setSelectedItem(SORT_NEWEST);
        notifyFiltersChanged();
    }

    private void notifyFiltersChanged() {
        onFiltersChanged.accept(currentFilters());
    }

    OrderCatalogFilters currentFilters() {
        return new OrderCatalogFilters(
                searchField.getText(),
                (String) paymentBox.getSelectedItem(),
                (String) sortBox.getSelectedItem()
        );
    }

    private static String[] paymentOptions() {
        String[] methods = PagamentoStrategyFactory.getMetodiPagamento();
        String[] options = new String[methods.length + 1];
        options[0] = ALL_PAYMENTS;
        System.arraycopy(methods, 0, options, 1, methods.length);
        return options;
    }
}
