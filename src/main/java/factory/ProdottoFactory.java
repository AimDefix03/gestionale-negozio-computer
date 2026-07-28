package factory;

import model.Prodotto;

public interface ProdottoFactory {
    Prodotto creaProdotto(
            String codice,
            String nome,
            String descrizione,
            String brand,
            String tipoProdotto,
            String utilizzo,
            int quantita,
            double costo,
            double sconto
    );
}
