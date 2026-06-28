package decorator;

import model.Prodotto;

public class PacchettoManutenzione extends ProdottoDecorator {
    private static final double COSTO_EXTRA = 30;

    public PacchettoManutenzione(Prodotto prodotto) {
        super(prodotto);
    }

    @Override
    public String getNome() {
        return prodottoDecorato.getNome() + " + Pacchetto di Manutenzione";
    }

    @Override
    public double getCostoScontato() {
        return prodottoDecorato.getCostoScontato() + COSTO_EXTRA;
    }
}
