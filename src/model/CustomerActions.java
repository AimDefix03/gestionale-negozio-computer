package model;

import decorator.AssemblaggioPC;
import decorator.GaranziaEstesa;
import decorator.PacchettoManutenzione;
import strategy.PagamentoBancomat;
import strategy.PagamentoCartaCredito;
import strategy.PagamentoContanti;
import strategy.PagamentoStrategy;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe che gestisce le azioni disponibili per il cliente.
 * Permette di visualizzare i prodotti, aggiungerli al carrello e procedere con l'acquisto.
 */
public class CustomerActions {
    private static final Carrello carrello = new Carrello(); // Carrello associato al cliente

    /**
     * Visualizza la lista dei prodotti disponibili.
     * Permette al cliente di selezionare uno o più prodotti da aggiungere al carrello.
     *
     * @param productList Lista dei prodotti disponibili
     */
    public static void viewProducts(List<Prodotto> productList) {
        if (productList.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nessun prodotto disponibile.", "Visualizza Prodotti", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Modello della tabella per visualizzare i prodotti
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.addColumn("Seleziona");
        tableModel.addColumn("Codice");
        tableModel.addColumn("Nome");
        tableModel.addColumn("Descrizione");
        tableModel.addColumn("Categoria");
        tableModel.addColumn("Prezzo (€)");
        tableModel.addColumn("Sconto (%)");
        tableModel.addColumn("Quantità");

        // Aggiunta dei prodotti alla tabella
        for (Prodotto product : productList) {
            tableModel.addRow(new Object[]{false, product.getCodice(), product.getNome(), product.getDescrizione(),
                    product.getCategoria(), product.getCostoScontato(), product.getSconto(), product.getQuantita()});
        }

        JTable table = new JTable(tableModel) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : super.getColumnClass(column); // La prima colonna è un checkbox
            }
        };

        JScrollPane scrollPane = new JScrollPane(table);

        // Pulsante per aggiungere i prodotti selezionati al carrello
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
                    AggiungiAlCarrello command = new AggiungiAlCarrello(carrello, p);
                    command.esegui(); // Aggiunge il prodotto al carrello utilizzando il Command Pattern
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

    /**
     * Visualizza il contenuto del carrello.
     * Se il carrello è vuoto, viene mostrato un messaggio informativo.
     */
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

    /**
     * Procede con l'acquisto dei prodotti presenti nel carrello.
     * Permette al cliente di aggiungere servizi extra (Decorator Pattern) e di selezionare il metodo di pagamento (Strategy Pattern).
     */
    public static void purchase() {
        List<Prodotto> cart = carrello.getProdotti();
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Non ci sono prodotti nel carrello per acquistare.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Prodotto> prodottiFinali = new ArrayList<>();
        StringBuilder orderSummary = new StringBuilder("📦 **Riepilogo Ordine:**\n\n");

        double total = 0.0;

        for (Prodotto prodotto : cart) {
            // Opzioni per aggiungere decorazioni extra al prodotto
            String[] options = {"Nessuna", "Garanzia Estesa", "Assemblaggio PC", "Pacchetto Manutenzione"};
            String scelta = (String) JOptionPane.showInputDialog(null,
                    "Vuoi aggiungere un'opzione extra a " + prodotto.getNome() + "?",
                    "Servizi Extra", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            // Applicazione del Decorator Pattern per i servizi extra
            if ("Garanzia Estesa".equals(scelta)) {
                prodotto = new GaranziaEstesa(prodotto);
            } else if ("Assemblaggio PC".equals(scelta)) {
                prodotto = new AssemblaggioPC(prodotto);
            } else if ("Pacchetto Manutenzione".equals(scelta)) {
                prodotto = new PacchettoManutenzione(prodotto);
            }

            prodottiFinali.add(prodotto);
            total += prodotto.getCostoScontato();

            orderSummary.append("🔹 ").append(prodotto.getNome()).append("\n")
                    .append("   📌 Prezzo unitario: ").append(prodotto.getCostoScontato()).append("€\n\n");
        }

        orderSummary.append("💰 **Totale da pagare:** ").append(total).append("€");

        JOptionPane.showMessageDialog(null, orderSummary.toString(), "Riepilogo Ordine", JOptionPane.INFORMATION_MESSAGE);

        // Selezione del metodo di pagamento (Strategy Pattern)
        String[] paymentOptions = {"Contanti", "Carta di Credito", "Bancomat"};
        String paymentMethod = (String) JOptionPane.showInputDialog(null, "Seleziona il metodo di pagamento:",
                "Pagamento", JOptionPane.QUESTION_MESSAGE, null, paymentOptions, paymentOptions[0]);

        PagamentoStrategy strategy = switch (paymentMethod) {
            case "Carta di Credito" -> new PagamentoCartaCredito();
            case "Bancomat" -> new PagamentoBancomat();
            default -> new PagamentoContanti();
        };

        strategy.paga(total); // Esecuzione del pagamento utilizzando la strategia selezionata
        carrello.svuotaCarrello(); // Svuotamento del carrello dopo l'acquisto
        JOptionPane.showMessageDialog(null, "Acquisto completato con successo!", "Successo", JOptionPane.INFORMATION_MESSAGE);
    }
}
