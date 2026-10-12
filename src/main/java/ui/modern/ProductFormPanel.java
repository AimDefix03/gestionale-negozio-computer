package ui.modern;

import factory.CategoriaProdotto;
import model.Prodotto;
import service.AuditService;
import service.ProductService;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

class ProductFormPanel extends JPanel {
    private final ProductService productService;
    private final Runnable onProductCreated;
    private final Prodotto productToEdit;
    private final AuditService auditService;
    private final String actorUsername;
    private final String actorRole;
    private final JTextField codeField = Ui.textField();
    private final JTextField nameField = Ui.textField();
    private final JTextField descriptionField = Ui.textField();
    private final JComboBox<String> brandBox = new JComboBox<>(ProductTaxonomy.BRAND_OPTIONS);
    private final JTextField customBrandField = Ui.textField();
    private final JComboBox<String> productTypeBox = new JComboBox<>(ProductTaxonomy.PRODUCT_TYPE_OPTIONS);
    private final JTextField customProductTypeField = Ui.textField();
    private final JTextField usageField = Ui.textField();
    private final JTextField quantityField = Ui.textField();
    private final JTextField priceField = Ui.textField();
    private final JTextField discountField = Ui.textField();
    private final JComboBox<String> categoryBox = new JComboBox<>(Arrays.stream(CategoriaProdotto.values())
            .map(CategoriaProdotto::getLabel)
            .toArray(String[]::new));
    private final JLabel statusLabel = Ui.text("Compila i campi per aggiungere un prodotto.");

    ProductFormPanel(ProductService productService, Runnable onProductCreated) {
        this(productService, onProductCreated, null, null, null, null);
    }

    ProductFormPanel(ProductService productService, Runnable onProductCreated, Prodotto productToEdit) {
        this(productService, onProductCreated, productToEdit, null, null, null);
    }

    ProductFormPanel(
            ProductService productService,
            Runnable onProductCreated,
            Prodotto productToEdit,
            AuditService auditService,
            String actorUsername,
            String actorRole
    ) {
        this.productService = productService;
        this.onProductCreated = onProductCreated;
        this.productToEdit = productToEdit;
        this.auditService = auditService;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        Ui.styleComboBox(categoryBox);
        Ui.styleComboBox(brandBox);
        Ui.styleComboBox(productTypeBox);
        installChoiceListeners();
        fillFieldsIfEditing();
        add(buildFields(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildFields() {
        JPanel panel = Ui.panel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel mainGroups = Ui.panel(new GridLayout(1, 2, 12, 0));
        mainGroups.setOpaque(false);

        JPanel identityFields = fieldStack();
        addField(identityFields, "Codice", codeField);
        addField(identityFields, "Nome", nameField);
        addField(identityFields, "Descrizione", descriptionField);
        mainGroups.add(fieldGroup("Identita prodotto", identityFields));

        JPanel classificationFields = fieldStack();
        addField(classificationFields, "Brand", choicePanel(brandBox, customBrandField));
        addField(classificationFields, "Tipo prodotto", choicePanel(productTypeBox, customProductTypeField));
        addField(classificationFields, "Categoria", categoryBox);
        addField(classificationFields, "Utilizzo opzionale", usageField);
        mainGroups.add(fieldGroup("Classificazione", classificationFields));

        JPanel commercialFields = Ui.panel(new GridLayout(1, 3, 10, 0));
        commercialFields.setOpaque(false);
        addField(commercialFields, "Quantita", quantityField);
        addField(commercialFields, "Costo", priceField);
        addField(commercialFields, "Sconto", discountField);

        panel.add(mainGroups, BorderLayout.CENTER);
        panel.add(fieldGroup("Prezzo e disponibilita", commercialFields), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel fieldGroup(String title, JPanel fields) {
        JPanel group = Ui.compactCard(new BorderLayout(0, 12));
        group.add(Ui.eyebrow(title.toUpperCase()), BorderLayout.NORTH);
        group.add(fields, BorderLayout.CENTER);
        return group;
    }

    private JPanel fieldStack() {
        JPanel panel = Ui.panel(new GridLayout(0, 1, 0, 10));
        panel.setOpaque(false);
        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = Ui.panel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        JButton saveButton = Ui.primaryButton(productToEdit == null ? "Aggiungi prodotto" : "Salva modifiche");
        saveButton.addActionListener(e -> saveProduct());
        footer.add(statusLabel, BorderLayout.CENTER);
        actions.add(saveButton);
        footer.add(actions, BorderLayout.SOUTH);
        return footer;
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

    private void saveProduct() {
        try {
            if (productToEdit == null) {
                Prodotto prodotto = productService.creaEInserisciProdotto(
                        codeField.getText(),
                        nameField.getText(),
                        descriptionField.getText(),
                        selectedBrand(),
                        selectedProductType(),
                        usageField.getText(),
                        Integer.parseInt(quantityField.getText()),
                        Double.parseDouble(priceField.getText()),
                        Double.parseDouble(discountField.getText()),
                        CategoriaProdotto.fromLabel((String) categoryBox.getSelectedItem())
                );
                recordAudit("Creazione prodotto", "Prodotto " + prodotto.getCodice(), prodotto.getNome());
                clearFields();
                statusLabel.setText("Prodotto aggiunto correttamente.");
            } else {
                Prodotto prodottoAggiornato = productService.aggiornaProdotto(
                        productToEdit.getCodice(),
                        codeField.getText(),
                        nameField.getText(),
                        descriptionField.getText(),
                        selectedBrand(),
                        selectedProductType(),
                        usageField.getText(),
                        Integer.parseInt(quantityField.getText()),
                        Double.parseDouble(priceField.getText()),
                        Double.parseDouble(discountField.getText()),
                        CategoriaProdotto.fromLabel((String) categoryBox.getSelectedItem())
                );
                recordAudit("Modifica prodotto", "Prodotto " + productToEdit.getCodice(), "Nuovo codice: " + prodottoAggiornato.getCodice());
                statusLabel.setText("Prodotto aggiornato correttamente.");
            }
            onProductCreated.run();
        } catch (NumberFormatException e) {
            statusLabel.setText("Quantita, costo e sconto devono essere numerici.");
        } catch (IllegalArgumentException e) {
            statusLabel.setText(e.getMessage());
        }
    }

    private void clearFields() {
        codeField.setText("");
        nameField.setText("");
        descriptionField.setText("");
        brandBox.setSelectedItem(ProductTaxonomy.UNDEFINED);
        customBrandField.setText("");
        productTypeBox.setSelectedItem(ProductTaxonomy.UNDEFINED);
        customProductTypeField.setText("");
        usageField.setText("");
        quantityField.setText("");
        priceField.setText("");
        discountField.setText("");
    }

    private void fillFieldsIfEditing() {
        if (productToEdit == null) {
            return;
        }

        codeField.setText(productToEdit.getCodice());
        nameField.setText(productToEdit.getNome());
        descriptionField.setText(productToEdit.getDescrizione());
        selectBrand(productToEdit.getBrand());
        selectProductType(productToEdit.getTipoProdotto());
        usageField.setText(productToEdit.getUtilizzo());
        quantityField.setText(String.valueOf(productToEdit.getQuantita()));
        priceField.setText(String.valueOf(productToEdit.getCosto()));
        discountField.setText(String.valueOf(productToEdit.getSconto()));
        categoryBox.setSelectedItem(productToEdit.getCategoria());
        statusLabel.setText("Modifica i campi e salva il prodotto selezionato.");
    }

    private void recordAudit(String action, String target, String details) {
        if (auditService != null) {
            auditService.record(actorUsername, actorRole, action, target, details);
        }
    }

    private JPanel choicePanel(JComboBox<String> comboBox, JTextField customField) {
        JPanel panel = Ui.panel(new GridLayout(0, 1, 0, 6));
        panel.setOpaque(false);
        panel.add(comboBox);
        panel.add(customField);
        customField.setVisible(ProductTaxonomy.OTHER.equals(comboBox.getSelectedItem()));
        return panel;
    }

    private void installChoiceListeners() {
        brandBox.addActionListener(event -> toggleCustomField(brandBox, customBrandField));
        productTypeBox.addActionListener(event -> toggleCustomField(productTypeBox, customProductTypeField));
    }

    private void toggleCustomField(JComboBox<String> comboBox, JTextField customField) {
        customField.setVisible(ProductTaxonomy.OTHER.equals(comboBox.getSelectedItem()));
        revalidate();
        repaint();
    }

    private String selectedBrand() {
        return selectedChoice(brandBox, customBrandField);
    }

    private String selectedProductType() {
        return selectedChoice(productTypeBox, customProductTypeField);
    }

    private String selectedChoice(JComboBox<String> comboBox, JTextField customField) {
        String selected = (String) comboBox.getSelectedItem();
        if (ProductTaxonomy.OTHER.equals(selected)) {
            return customField.getText();
        }
        return selected;
    }

    private void selectBrand(String brand) {
        if (ProductTaxonomy.containsBrand(brand)) {
            brandBox.setSelectedItem(brand);
            customBrandField.setText("");
        } else {
            brandBox.setSelectedItem(ProductTaxonomy.OTHER);
            customBrandField.setText(brand);
        }
        toggleCustomField(brandBox, customBrandField);
    }

    private void selectProductType(String productType) {
        if (ProductTaxonomy.containsProductType(productType)) {
            productTypeBox.setSelectedItem(productType);
            customProductTypeField.setText("");
        } else {
            productTypeBox.setSelectedItem(ProductTaxonomy.OTHER);
            customProductTypeField.setText(productType);
        }
        toggleCustomField(productTypeBox, customProductTypeField);
    }
}
