package ui.modern;

import decorator.ServizioExtraFactory;
import model.Carrello;
import model.Order;
import model.Prodotto;
import service.AuditService;
import service.InventoryService;
import service.OrderService;
import strategy.PagamentoStrategy;
import strategy.PagamentoStrategyFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

class CartPanel extends JPanel {
    private final Carrello carrello = new Carrello();
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final AuditService auditService;
    private final String username;
    private final String role;
    private final Runnable onCheckoutCompleted;
    private final DefaultListModel<String> cartModel = new DefaultListModel<>();
    private final JList<String> cartList = new JList<>(cartModel);
    private final JLabel totalLabel = Ui.text("Totale: 0.00 euro");
    private final JLabel statusLabel = Ui.text("Seleziona un prodotto dal catalogo.");

    CartPanel(
            OrderService orderService,
            InventoryService inventoryService,
            AuditService auditService,
            String username,
            String role,
            Runnable onCheckoutCompleted
    ) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
        this.username = username;
        this.role = role;
        this.onCheckoutCompleted = onCheckoutCompleted;
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        add(buildHeader(), BorderLayout.NORTH);
        cartList.setBackground(Ui.SURFACE);
        cartList.setForeground(Ui.TEXT);
        cartList.setSelectionBackground(Ui.SURFACE_LIGHT);
        cartList.setSelectionForeground(Ui.TEXT);
        cartList.setFont(Ui.font(Font.PLAIN, 13));
        add(Ui.scrollPane(cartList), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
    }

    void addProduct(Prodotto prodotto) {
        carrello.aggiungiProdotto(prodotto);
        refresh();
        statusLabel.setText("Prodotto aggiunto al carrello.");
    }

    private JPanel buildHeader() {
        JPanel header = Ui.panel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(Ui.title("Carrello", 20), BorderLayout.NORTH);
        header.add(statusLabel, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildActions() {
        JPanel panel = Ui.panel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel buttons = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton clearButton = Ui.secondaryButton("Svuota");
        JButton checkoutButton = Ui.primaryButton("Checkout");
        clearButton.addActionListener(e -> clearCart());
        checkoutButton.addActionListener(e -> checkout());
        buttons.add(clearButton);
        buttons.add(checkoutButton);

        totalLabel.setForeground(Ui.TEXT);
        totalLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        panel.add(totalLabel, BorderLayout.NORTH);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void refresh() {
        cartModel.clear();
        for (Prodotto prodotto : carrello.getProdotti()) {
            cartModel.addElement(prodotto.getNome() + " - " + String.format("%.2f euro", prodotto.getCostoScontato()));
        }
        totalLabel.setText("Totale: " + String.format("%.2f euro", carrello.calcolaTotale()));
    }

    private void clearCart() {
        carrello.svuotaCarrello();
        refresh();
        statusLabel.setText("Carrello svuotato.");
    }

    private void checkout() {
        List<Prodotto> prodotti = carrello.getProdotti();
        if (prodotti.isEmpty()) {
            statusLabel.setText("Aggiungi almeno un prodotto prima del checkout.");
            return;
        }

        Carrello carrelloDecorato = new Carrello();
        for (Prodotto prodotto : prodotti) {
            String scelta = (String) JOptionPane.showInputDialog(
                    this,
                    "Servizio extra per " + prodotto.getNome(),
                    "Servizi extra",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    ServizioExtraFactory.getOpzioni(),
                    ServizioExtraFactory.getOpzioni()[0]
            );
            if (scelta == null) {
                statusLabel.setText("Checkout annullato.");
                return;
            }
            carrelloDecorato.aggiungiProdotto(ServizioExtraFactory.applica(scelta, prodotto));
        }

        String metodo = (String) JOptionPane.showInputDialog(
                this,
                "Seleziona il metodo di pagamento",
                "Pagamento",
                JOptionPane.QUESTION_MESSAGE,
                null,
                PagamentoStrategyFactory.getMetodiPagamento(),
                PagamentoStrategyFactory.getMetodiPagamento()[0]
        );

        if (metodo == null) {
            statusLabel.setText("Pagamento annullato.");
            return;
        }

        try {
            Order order = orderService.createOrder(
                    username,
                    carrelloDecorato.getProdotti(),
                    metodo,
                    inventoryService,
                    username,
                    role
            );
            PagamentoStrategy strategy = PagamentoStrategyFactory.crea(metodo);
            strategy.paga(order.total());
            auditService.record(
                    username,
                    role,
                    "Nuovo ordine",
                    "Ordine " + order.code(),
                    "Totale: " + String.format("%.2f euro", order.total()) + " - Metodo: " + metodo
            );
            carrello.svuotaCarrello();
            refresh();
            onCheckoutCompleted.run();
            statusLabel.setText("Ordine " + order.code() + " completato. Totale: " + String.format("%.2f euro", order.total()));
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }
}
