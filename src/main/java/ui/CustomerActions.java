package ui;

import command.AggiungiAlCarrelloCommand;
import decorator.ServizioExtraFactory;
import model.Carrello;
import model.Prodotto;
import strategy.PagamentoStrategy;
import strategy.PagamentoStrategyFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class CustomerActions {
    private static final Carrello carrello = new Carrello();

    private CustomerActions() {
    }

    public static void viewProducts(List<Prodotto> productList) {
        if (productList.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nessun prodotto disponibile.", "Visualizza Prodotti", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn("Seleziona");
        tableModel.addColumn("Codice");
        tableModel.addColumn("Nome");
        tableModel.addColumn("Descrizione");
        tableModel.addColumn("Categoria");
        tableModel.addColumn("Prezzo (€)");
        tableModel.addColumn("Sconto (%)");
        tableModel.addColumn("Quantità");

        for (Prodotto product : productList) {
            tableModel.addRow(new Object[]{false, product.getCodice(), product.getNome(), product.getDescrizione(),
                    product.getCategoria(), product.getCostoScontato(), product.getSconto(), product.getQuantita()});
        }

        JTable table = new JTable(tableModel) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : super.getColumnClass(column);
            }
        };

        JScrollPane scrollPane = new JScrollPane(table);

        JButton addToCartButton = new JButton("Aggiungi al Carrello");
        addToCartButton.addActionListener(e -> {
            List<Prodotto> selectedProducts = new ArrayList<>();
            for (int i = 0; i < table.getRowCount(); i++) {
                Boolean isSelected = (Boolean) table.getValueAt(i, 0);
                if (isSelected) {
                    String selectedProductCode = (String) table.getValueAt(i, 1);
                    Prodotto selectedProduct = productList.stream()
                            .filter(p -> p.getCodice().equals(selectedProductCode))
                            .findFirst()
                            .orElse(null);

                    if (selectedProduct != null) {
                        selectedProducts.add(selectedProduct);
                    }
                }
            }

            if (!selectedProducts.isEmpty()) {
                for (Prodotto p : selectedProducts) {
                    AggiungiAlCarrelloCommand command = new AggiungiAlCarrelloCommand(carrello, p);
                    command.execute();
                }
                JOptionPane.showMessageDialog(null, "Prodotti aggiunti al carrello!", "Successo", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Nessun prodotto selezionato.", "Errore", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(addToCartButton, BorderLayout.SOUTH);

        JFrame frame = new JFrame("Lista Prodotti");
        frame.setSize(800, 400);
        frame.add(panel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void viewCart() {
        List<Prodotto> cart = carrello.getProdotti();
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Il carrello è vuoto.", "Carrello", JOptionPane.INFORMATION_MESSAGE);
        } else {
            StringBuilder cartContents = new StringBuilder("Carrello:\n");
            for (Prodotto item : cart) {
                cartContents.append("Codice: ").append(item.getCodice())
                        .append(", Nome: ").append(item.getNome())
                        .append(", Descrizione: ").append(item.getDescrizione())
                        .append(", Prezzo: ").append(item.getCostoScontato()).append("€ (Sconto: ").append(item.getSconto()).append("%)")
                        .append(", Quantità: 1")
                        .append("\n");
            }
            JOptionPane.showMessageDialog(null, cartContents.toString(), "Carrello", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static void purchase() {
        List<Prodotto> cart = carrello.getProdotti();
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Non ci sono prodotti nel carrello per acquistare.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Carrello carrelloDecorato = new Carrello();
        StringBuilder orderSummary = new StringBuilder("Riepilogo ordine:\n\n");

        for (Prodotto prodotto : cart) {
            String[] options = ServizioExtraFactory.getOpzioni();
            String scelta = (String) JOptionPane.showInputDialog(null,
                    "Vuoi aggiungere un'opzione extra a " + prodotto.getNome() + "?",
                    "Servizi Extra", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            Prodotto prodottoDecorato = ServizioExtraFactory.applica(scelta, prodotto);
            carrelloDecorato.aggiungiProdotto(prodottoDecorato);

            orderSummary.append("- ").append(prodottoDecorato.getNome()).append("\n")
                    .append("  Prezzo unitario: ").append(prodottoDecorato.getCostoScontato()).append("€\n\n");
        }

        double total = carrelloDecorato.calcolaTotale();
        orderSummary.append("Totale da pagare: ").append(total).append("€");

        JOptionPane.showMessageDialog(null, orderSummary.toString(), "Riepilogo Ordine", JOptionPane.INFORMATION_MESSAGE);

        String[] paymentOptions = PagamentoStrategyFactory.getMetodiPagamento();
        String paymentMethod = (String) JOptionPane.showInputDialog(null, "Seleziona il metodo di pagamento:",
                "Pagamento", JOptionPane.QUESTION_MESSAGE, null, paymentOptions, paymentOptions[0]);

        PagamentoStrategy strategy = PagamentoStrategyFactory.crea(paymentMethod);

        carrelloDecorato.acquista(strategy);
        carrello.svuotaCarrello();
        JOptionPane.showMessageDialog(null, "Acquisto completato con successo!", "Successo", JOptionPane.INFORMATION_MESSAGE);
    }
}
