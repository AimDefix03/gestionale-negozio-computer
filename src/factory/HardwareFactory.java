package factory;

import model.Hardware;
import model.Prodotto;

/**
 * Factory concreta per la creazione di prodotti di tipo Hardware.
 */
public class HardwareFactory implements ProdottoFactory {
    @Override
    public Prodotto creaProdotto(String codice, String nome, String descrizione, String utilizzo, int quantita, double costo, double sconto) {
        return new Hardware(codice, nome, descrizione, utilizzo, quantita, costo, sconto);
    }
}
