package decorator;

import model.Prodotto;

public abstract class ProdottoDecorator extends Prodotto {
    protected final Prodotto prodottoDecorato;

    public ProdottoDecorator(Prodotto prodotto) {
        super(prodotto.getCodice(), prodotto.getNome(), prodotto.getDescrizione(),
                prodotto.getCategoria(), prodotto.getBrand(), prodotto.getTipoProdotto(), prodotto.getUtilizzo(), prodotto.getQuantita(),
                prodotto.getCosto(), prodotto.getSconto());
        this.prodottoDecorato = prodotto;
    }

    @Override
    public String getCodice() {
        return prodottoDecorato.getCodice();
    }

    @Override
    public String getDescrizione() {
        return prodottoDecorato.getDescrizione();
    }

    @Override
    public String getCategoria() {
        return prodottoDecorato.getCategoria();
    }

    @Override
    public String getUtilizzo() {
        return prodottoDecorato.getUtilizzo();
    }

    @Override
    public String getBrand() {
        return prodottoDecorato.getBrand();
    }

    @Override
    public String getTipoProdotto() {
        return prodottoDecorato.getTipoProdotto();
    }

    @Override
    public int getQuantita() {
        return prodottoDecorato.getQuantita();
    }

    @Override
    public double getSconto() {
        return prodottoDecorato.getSconto();
    }

    @Override
    public double getCosto() {
        return prodottoDecorato.getCosto();
    }

    @Override
    public abstract String getNome();

    @Override
    public abstract double getCostoScontato();
}
