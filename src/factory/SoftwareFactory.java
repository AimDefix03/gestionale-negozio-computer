package factory;

import model.Software;
import model.Prodotto;

/**
 * Factory concreta per la creazione di prodotti di tipo Software.
 */
public class SoftwareFactory implements ProdottoFactory {
    @Override
    public Prodotto creaProdotto(String codice, String nome, String descrizione, String utilizzo, int quantita, double costo, double sconto) {
        return new Software(codice, nome, descrizione, utilizzo, quantita, costo, sconto);
    }
}
