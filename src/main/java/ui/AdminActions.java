package ui;

import factory.CategoriaProdotto;
import model.Prodotto;
import service.ProductService;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public final class AdminActions {
    private static final ProductService productService = new ProductService();

    private AdminActions() {
    }

    public static void addProduct() {
        JTextField codiceField = new JTextField();
        JTextField nomeField = new JTextField();
        JTextField descrizioneField = new JTextField();
        JTextField utilizzoField = new JTextField();
        JTextField quantitaField = new JTextField();
        JTextField costoField = new JTextField();
        JTextField scontoField = new JTextField();
        String[] categories = Arrays.stream(CategoriaProdotto.values())
                .map(CategoriaProdotto::getLabel)
                .toArray(String[]::new);
        JComboBox<String> categoryBox = new JComboBox<>(categories);

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

        int result = JOptionPane.showConfirmDialog(null, panel, "Aggiungi Prodotto", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String codice = codiceField.getText();
                String nome = nomeField.getText();
                String descrizione = descrizioneField.getText();
                String utilizzo = utilizzoField.getText();
                int quantita = Integer.parseInt(quantitaField.getText());
                double costo = Double.parseDouble(costoField.getText());
                double sconto = Double.parseDouble(scontoField.getText());
                CategoriaProdotto categoria = CategoriaProdotto.fromLabel((String) categoryBox.getSelectedItem());

                productService.creaEInserisciProdotto(codice, nome, descrizione, utilizzo, quantita, costo, sconto, categoria);
                JOptionPane.showMessageDialog(null, "Prodotto aggiunto con successo!", "Successo", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Errore: Inserisci valori numerici validi per quantità, costo e sconto.", "Errore", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void viewProducts() {
        List<Prodotto> productList = productService.getProdotti();
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

    public static List<Prodotto> getProductList() {
        return productService.getProdotti();
    }
}
