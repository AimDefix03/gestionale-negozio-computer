package decorator;

import model.Prodotto;

public class AssemblaggioPC extends ProdottoDecorator {
    private static final double COSTO_EXTRA = 100;

    public AssemblaggioPC(Prodotto prodotto) {
        super(prodotto);
    }

    @Override
    public String getNome() {
        return prodottoDecorato.getNome() + " + Servizio di Assemblaggio";
    }

    @Override
    public double getCostoScontato() {
        return prodottoDecorato.getCostoScontato() + COSTO_EXTRA;
    }
}
