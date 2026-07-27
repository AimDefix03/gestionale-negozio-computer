package factory;

import model.Software;
import model.Prodotto;

public class SoftwareFactory implements ProdottoFactory {
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
        return new Software(codice, nome, descrizione, brand, tipoProdotto, utilizzo, quantita, costo, sconto);
    }
}
