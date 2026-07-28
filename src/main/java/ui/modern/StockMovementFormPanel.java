package ui.modern;

import model.Prodotto;
import model.StockMovement;
import model.StockMovementType;
import service.AuditService;
import service.InventoryService;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

class StockMovementFormPanel extends JPanel {
    private final InventoryService inventoryService;
    private final AuditService auditService;
    private final Prodotto product;
    private final Runnable onMovementSaved;
    private final String actorUsername;
    private final String actorRole;
    private final JComboBox<String> typeBox = new JComboBox<>(Arrays.stream(StockMovementType.values())
            .map(StockMovementType::getLabel)
            .toArray(String[]::new));
    private final JTextField quantityField = Ui.textField();
    private final JTextField reasonField = Ui.textField();
    private final JLabel statusLabel = Ui.text("Registra un carico o scarico di magazzino.");

    StockMovementFormPanel(
            InventoryService inventoryService,
            AuditService auditService,
            Prodotto product,
            Runnable onMovementSaved,
            String actorUsername,
            String actorRole
    ) {
        this.inventoryService = inventoryService;
        this.auditService = auditService;
        this.product = product;
        this.onMovementSaved = onMovementSaved;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        Ui.styleComboBox(typeBox);
        add(buildSummary(), BorderLayout.NORTH);
        add(buildFields(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildSummary() {
        JPanel panel = Ui.compactCard(new GridLayout(0, 1, 0, 6));
        panel.add(Ui.eyebrow("PRODOTTO"));
        panel.add(Ui.title(product.getCodice() + " - " + product.getNome(), 20));
        panel.add(Ui.text("Quantità attuale: " + product.getQuantita()));
        return panel;
    }

    private JPanel buildFields() {
        JPanel panel = Ui.panel(new GridLayout(0, 1, 0, 10));
        panel.setOpaque(false);
        addField(panel, "Tipo movimento", typeBox);
        addField(panel, "Quantità", quantityField);
        addField(panel, "Causale", reasonField);
        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = Ui.panel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        JButton saveButton = Ui.primaryButton("Registra movimento");
        saveButton.addActionListener(e -> saveMovement());
        actions.add(saveButton);
        footer.add(statusLabel, BorderLayout.CENTER);
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

    private void saveMovement() {
        try {
            StockMovement movement = inventoryService.registerMovement(
                    product.getCodice(),
                    StockMovementType.fromLabel((String) typeBox.getSelectedItem()),
                    Integer.parseInt(quantityField.getText()),
                    reasonField.getText(),
                    actorUsername,
                    actorRole
            );
            auditService.record(
                    actorUsername,
                    actorRole,
                    "Movimento magazzino",
                    "Prodotto " + movement.productCode(),
                    movement.type().getLabel() + " di " + movement.quantity() + " unità. Nuova quantità: " + movement.newQuantity()
            );
            statusLabel.setText("Movimento registrato correttamente.");
            onMovementSaved.run();
        } catch (NumberFormatException e) {
            statusLabel.setText("La quantità deve essere un numero intero.");
        } catch (IllegalArgumentException e) {
            statusLabel.setText(e.getMessage());
        }
    }
}
