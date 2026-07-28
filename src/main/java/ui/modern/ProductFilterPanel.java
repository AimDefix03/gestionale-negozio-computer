package ui.modern;

import model.Prodotto;
import service.InventoryService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

class ProductFilterPanel extends JPanel {
    static final String ALL_CATEGORIES = "Tutte le categorie";
    static final String ALL_BRANDS = ProductTaxonomy.ALL_BRANDS;
    static final String ALL_TYPES = ProductTaxonomy.ALL_TYPES;
    static final String ALL_STOCK = "Tutte le scorte";
    static final String LOW_STOCK = "Scorte basse";
    static final String AVAILABLE = "Disponibili";
    static final String OUT_OF_STOCK = "Esauriti";
    static final String SORT_NAME = "Nome";
    static final String SORT_CODE = "Codice";
    static final String SORT_BRAND = "Brand";
    static final String SORT_TYPE = "Tipo prodotto";
    static final String SORT_PRICE = "Prezzo";
    static final String SORT_QUANTITY = "Quantità";

    private final JTextField searchField = Ui.textField();
    private final JComboBox<String> categoryBox = new JComboBox<>(new String[]{ALL_CATEGORIES, "Hardware", "Software"});
    private final JComboBox<String> brandBox = new JComboBox<>(ProductTaxonomy.brandFilterOptions(List.of()));
    private final JComboBox<String> productTypeBox = new JComboBox<>(ProductTaxonomy.productTypeFilterOptions(List.of()));
    private final JComboBox<String> stockBox = new JComboBox<>(new String[]{ALL_STOCK, LOW_STOCK, AVAILABLE, OUT_OF_STOCK});
    private final JComboBox<String> sortBox = new JComboBox<>(new String[]{SORT_NAME, SORT_CODE, SORT_BRAND, SORT_TYPE, SORT_PRICE, SORT_QUANTITY});
    private final Consumer<ProductCatalogFilters> onFiltersChanged;

    ProductFilterPanel(Consumer<ProductCatalogFilters> onFiltersChanged) {
        this.onFiltersChanged = onFiltersChanged;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        Ui.styleComboBox(categoryBox);
        Ui.styleComboBox(brandBox);
        Ui.styleComboBox(productTypeBox);
        Ui.styleComboBox(stockBox);
        Ui.styleComboBox(sortBox);
        add(buildFields(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
        installListeners();
    }

    private JPanel buildFields() {
        JPanel panel = Ui.panel(new GridLayout(2, 3, 10, 10));
        panel.setOpaque(false);
        addField(panel, "Cerca", searchField);
        addField(panel, "Categoria", categoryBox);
        addField(panel, "Brand", brandBox);
        addField(panel, "Tipo prodotto", productTypeBox);
        addField(panel, "Scorte", stockBox);
        addField(panel, "Ordina per", sortBox);
        return panel;
    }

    private JPanel buildActions() {
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JLabel thresholdLabel = Ui.text("Scorte basse: ≤ " + InventoryService.LOW_STOCK_THRESHOLD);
        JButton resetButton = Ui.secondaryButton("Reset filtri");
        resetButton.addActionListener(e -> resetFilters());
        actions.add(thresholdLabel);
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
            public void insertUpdate(DocumentEvent e) {
                notifyFiltersChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyFiltersChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyFiltersChanged();
            }
        });
        categoryBox.addActionListener(e -> notifyFiltersChanged());
        brandBox.addActionListener(e -> notifyFiltersChanged());
        productTypeBox.addActionListener(e -> notifyFiltersChanged());
        stockBox.addActionListener(e -> notifyFiltersChanged());
        sortBox.addActionListener(e -> notifyFiltersChanged());
    }

    private void resetFilters() {
        searchField.setText("");
        categoryBox.setSelectedItem(ALL_CATEGORIES);
        brandBox.setSelectedItem(ALL_BRANDS);
        productTypeBox.setSelectedItem(ALL_TYPES);
        stockBox.setSelectedItem(ALL_STOCK);
        sortBox.setSelectedItem(SORT_NAME);
        notifyFiltersChanged();
    }

    private void notifyFiltersChanged() {
        onFiltersChanged.accept(currentFilters());
    }

    ProductCatalogFilters currentFilters() {
        return new ProductCatalogFilters(
                searchField.getText(),
                (String) categoryBox.getSelectedItem(),
                (String) brandBox.getSelectedItem(),
                (String) productTypeBox.getSelectedItem(),
                (String) stockBox.getSelectedItem(),
                (String) sortBox.getSelectedItem()
        );
    }

    void updateFilterOptions(List<Prodotto> products) {
        String selectedBrand = (String) brandBox.getSelectedItem();
        String selectedType = (String) productTypeBox.getSelectedItem();

        replaceOptions(
                brandBox,
                ProductTaxonomy.brandFilterOptions(products.stream().map(Prodotto::getBrand).toList()),
                selectedBrand,
                ALL_BRANDS
        );
        replaceOptions(
                productTypeBox,
                ProductTaxonomy.productTypeFilterOptions(products.stream().map(Prodotto::getTipoProdotto).toList()),
                selectedType,
                ALL_TYPES
        );
    }

    private void replaceOptions(JComboBox<String> comboBox, String[] options, String selectedValue, String defaultValue) {
        comboBox.removeAllItems();
        for (String option : options) {
            comboBox.addItem(option);
        }
        comboBox.setSelectedItem(contains(options, selectedValue) ? selectedValue : defaultValue);
    }

    private boolean contains(String[] options, String value) {
        if (value == null) {
            return false;
        }
        for (String option : options) {
            if (option.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
