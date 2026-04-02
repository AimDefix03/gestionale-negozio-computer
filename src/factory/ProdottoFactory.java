package factory;

import model.Prodotto;

/**
 * Interfaccia Factory per la creazione di prodotti.
 */
public interface ProdottoFactory {
    Prodotto creaProdotto(String codice, String nome, String descrizione, String utilizzo, int quantita, double costo, double sconto);
}
