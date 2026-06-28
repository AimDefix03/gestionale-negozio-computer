package decorator;

import model.Prodotto;

public class GaranziaEstesa extends ProdottoDecorator {
    private static final double COSTO_EXTRA = 50;

    public GaranziaEstesa(Prodotto prodotto) {
        super(prodotto);
    }

    @Override
    public String getNome() {
        return prodottoDecorato.getNome() + " + Garanzia Estesa";
    }

    @Override
    public double getCostoScontato() {
        return prodottoDecorato.getCostoScontato() + COSTO_EXTRA;
    }
}
