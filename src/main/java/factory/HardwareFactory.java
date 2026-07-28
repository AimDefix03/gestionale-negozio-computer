package factory;

import model.Hardware;
import model.Prodotto;

public class HardwareFactory implements ProdottoFactory {
    @Override
    public Prodotto creaProdotto(
            String codice,
            String nome,
            String descrizione,
            String brand,
            String tipoProdotto,
            String utilizzo,
            int quantita,
            double costo,
            double sconto
    ) {
        return new Hardware(codice, nome, descrizione, brand, tipoProdotto, utilizzo, quantita, costo, sconto);
    }
}
