package model;

import strategy.PagamentoStrategy;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Carrello implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Prodotto> prodotti = new ArrayList<>();

    public void aggiungiProdotto(Prodotto prodotto) {
        prodotti.add(prodotto);
    }

    public void aggiungiProdotti(List<Prodotto> prodottiDaAggiungere) {
        prodotti.addAll(prodottiDaAggiungere);
    }

    public List<Prodotto> getProdotti() {
        return new ArrayList<>(prodotti);
    }

    public void svuotaCarrello() {
        prodotti.clear();
    }

    public double calcolaTotale() {
        return prodotti.stream().mapToDouble(Prodotto::getCostoScontato).sum();
    }

    public void acquista(PagamentoStrategy pagamentoStrategy) {
        double totale = calcolaTotale();
        pagamentoStrategy.paga(totale);
        svuotaCarrello();
    }
}
