package service;

import factory.CategoriaProdotto;
import factory.ProdottoFactory;
import factory.ProdottoFactoryProvider;
import model.Prodotto;
import utils.FileManager;

import java.util.ArrayList;
import java.util.List;

public class ProductService {
    private static final String FILE_PRODOTTI = "prodotti.dat";
    private final String storageFile;
    private List<Prodotto> prodotti = new ArrayList<>();

    public ProductService() {
        this(FILE_PRODOTTI);
    }

    public ProductService(String storageFile) {
        if (isBlank(storageFile)) {
            throw new IllegalArgumentException("Il file di persistenza dei prodotti non può essere vuoto.");
        }
        this.storageFile = storageFile;
        caricaProdotti();
    }

    public Prodotto creaEInserisciProdotto(
            String codice,
            String nome,
            String descrizione,
            String utilizzo,
            int quantita,
            double costo,
            double sconto,
            CategoriaProdotto categoria
    ) {
        validaProdotto(codice, nome, descrizione, utilizzo, quantita, costo, sconto, categoria);

        ProdottoFactory factory = ProdottoFactoryProvider.getFactory(categoria);
        Prodotto prodotto = factory.creaProdotto(codice, nome, descrizione, utilizzo, quantita, costo, sconto);
        prodotti.add(prodotto);
        salvaProdotti();
        return prodotto;
    }

    public List<Prodotto> getProdotti() {
        return new ArrayList<>(prodotti);
    }

    private void salvaProdotti() {
        FileManager.salvaSuFile(storageFile, prodotti);
    }

    private void caricaProdotti() {
        List<Prodotto> prodottiSalvati = FileManager.caricaDaFile(storageFile);
        if (prodottiSalvati != null) {
            prodotti = prodottiSalvati;
        }
    }

    private void validaProdotto(
            String codice,
            String nome,
            String descrizione,
            String utilizzo,
            int quantita,
            double costo,
            double sconto,
            CategoriaProdotto categoria
    ) {
        if (isBlank(codice) || isBlank(nome) || isBlank(descrizione) || isBlank(utilizzo)) {
            throw new IllegalArgumentException("Compila tutti i campi testuali del prodotto.");
        }

        if (categoria == null) {
            throw new IllegalArgumentException("Seleziona una categoria valida.");
        }

        if (quantita < 0) {
            throw new IllegalArgumentException("La quantità non può essere negativa.");
        }

        if (costo < 0) {
            throw new IllegalArgumentException("Il costo non può essere negativo.");
        }

        if (sconto < 0 || sconto > 100) {
            throw new IllegalArgumentException("Lo sconto deve essere compreso tra 0 e 100.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
