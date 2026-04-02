package model;

import factory.HardwareFactory;
import factory.SoftwareFactory;
import factory.ProdottoFactory;
import utils.FileManager;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe che gestisce le azioni riservate all'amministratore, come l'aggiunta e la visualizzazione dei prodotti.
 * Gestisce la persistenza dei dati dei prodotti utilizzando il {@link FileManager}.
 */
public class AdminActions {
    private static final String FILE_PRODOTTI = "prodotti.dat"; // Nome del file per la persistenza dei prodotti
    private static List<Prodotto> productList = new ArrayList<>(); // Lista dei prodotti gestiti dall'amministratore

    // Blocco statico per caricare i prodotti dal file all'avvio dell'applicazione
    static {
        caricaProdotti();
    }

    /**
     * Salva la lista dei prodotti su file per garantire la persistenza dei dati.
     */
    private static void salvaProdotti() {
        FileManager.salvaSuFile(FILE_PRODOTTI, productList);
    }

    /**
     * Carica i prodotti dal file. Se il file esiste e contiene dati validi,
     * la lista dei prodotti viene popolata con questi dati.
     */
    private static void caricaProdotti() {
        List<Prodotto> prodottiSalvati = FileManager.caricaDaFile(FILE_PRODOTTI);
        if (prodottiSalvati != null) productList = prodottiSalvati;
    }

    /**
     * Apre una finestra di dialogo per permettere all'amministratore di aggiungere un nuovo prodotto.
     * I dati inseriti vengono validati e, se corretti, il prodotto viene aggiunto alla lista e salvato su file.
     */
    public static void addProduct() {
        // Creazione dei campi di input per i dati del prodotto
        JTextField codiceField = new JTextField();
        JTextField nomeField = new JTextField();
        JTextField descrizioneField = new JTextField();
        JTextField utilizzoField = new JTextField();
        JTextField quantitaField = new JTextField();
        JTextField costoField = new JTextField();
        JTextField scontoField = new JTextField();
        String[] categories = {"Hardware", "Software"};
        JComboBox<String> categoryBox = new JComboBox<>(categories);

        // Creazione del pannello per l'inserimento dei dati
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Codice Prodotto:"));
        panel.add(codiceField);
        panel.add(new JLabel("Nome:"));
        panel.add(nomeField);
        panel.add(new JLabel("Descrizione:"));
        panel.add(descrizioneField);
        panel.add(new JLabel("Utilizzo:"));
        panel.add(utilizzoField);
        panel.add(new JLabel("Quantità:"));
        panel.add(quantitaField);
        panel.add(new JLabel("Costo:"));
        panel.add(costoField);
        panel.add(new JLabel("Sconto (%):"));
        panel.add(scontoField);
        panel.add(new JLabel("Categoria:"));
        panel.add(categoryBox);

        // Finestra di dialogo per la conferma dell'inserimento del prodotto
        int result = JOptionPane.showConfirmDialog(null, panel, "Aggiungi Prodotto", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                // Lettura dei dati inseriti dall'utente
                String codice = codiceField.getText();
                String nome = nomeField.getText();
                String descrizione = descrizioneField.getText();
                String utilizzo = utilizzoField.getText();
                int quantita = Integer.parseInt(quantitaField.getText());
                double costo = Double.parseDouble(costoField.getText());
                double sconto = Double.parseDouble(scontoField.getText());
                String categoria = (String) categoryBox.getSelectedItem();

                // Creazione della factory giusta in base alla categoria
                ProdottoFactory factory;
                if (categoria.equalsIgnoreCase("Hardware")) {
                    factory = new HardwareFactory();
                } else {
                    factory = new SoftwareFactory();
                }

                // Creazione del prodotto
                Prodotto nuovoProdotto = factory.creaProdotto(codice, nome, descrizione, utilizzo, quantita, costo, sconto);

                if (nuovoProdotto != null) {
                    productList.add(nuovoProdotto);
                    salvaProdotti(); // Salvataggio su file
                    JOptionPane.showMessageDialog(null, "Prodotto aggiunto con successo!", "Successo", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Errore nella creazione del prodotto.", "Errore", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Errore: Inserisci valori numerici validi per quantità, costo e sconto.", "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Visualizza la lista dei prodotti disponibili.
     * Se la lista è vuota, viene mostrato un messaggio informativo.
     */
    public static void viewProducts() {
        if (productList.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nessun prodotto disponibile.", "Visualizza Prodotti", JOptionPane.INFORMATION_MESSAGE);
        } else {
            StringBuilder products = new StringBuilder("Lista Prodotti:\n");
            for (Prodotto product : productList) {
                products.append("Codice: ").append(product.getCodice())
                        .append(", Nome: ").append(product.getNome())
                        .append(", Descrizione: ").append(product.getDescrizione())
                        .append(", Prezzo: ").append(product.getCostoScontato()).append("€ (Sconto: ")
                        .append(product.getSconto()).append("%), Quantità: ").append(product.getQuantita())
                        .append("\n");
            }
            JOptionPane.showMessageDialog(null, products.toString(), "Visualizza Prodotti", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Restituisce la lista corrente dei prodotti.
     *
     * @return Lista dei prodotti
     */
    public static List<Prodotto> getProductList() {
        return productList;
    }
}
