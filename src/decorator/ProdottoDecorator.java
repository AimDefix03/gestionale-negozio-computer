package decorator;

import model.Prodotto;

/**
 * Classe astratta che rappresenta il Decorator base nel Pattern Decorator.
 * Estende la classe {@link Prodotto} e aggiunge la possibilità di decorare dinamicamente
 * un prodotto con funzionalità extra senza modificare la sua struttura originale.
 */
public abstract class ProdottoDecorator extends Prodotto {
    protected Prodotto prodottoBase; // Riferimento al prodotto originale da decorare

    /**
     * Costruttore della classe ProdottoDecorator.
     * Inizializza il decoratore copiando gli attributi del prodotto base e mantiene un riferimento ad esso.
     *
     * @param prodotto Il prodotto base da decorare
     */
    public ProdottoDecorator(Prodotto prodotto) {
        super(prodotto.getCodice(), prodotto.getNome(), prodotto.getDescrizione(),
                prodotto.getCategoria(), prodotto.getUtilizzo(), prodotto.getQuantita(),
                prodotto.getCostoScontato(), prodotto.getSconto());
        this.prodottoBase = prodotto;
    }

    /**
     * Metodo astratto per ottenere il nome del prodotto decorato.
     * Le classi concrete che estendono {@code ProdottoDecorator} devono implementare questo metodo,
     * aggiungendo una descrizione extra al nome del prodotto originale.
     *
     * @return Il nome del prodotto decorato
     */
    @Override
    public abstract String getNome();

    /**
     * Metodo astratto per calcolare il costo totale del prodotto decorato.
     * Le classi concrete che estendono {@code ProdottoDecorator} devono implementare questo metodo,
     * aggiungendo eventuali costi extra al costo del prodotto base.
     *
     * @return Il costo totale del prodotto decorato, inclusi eventuali costi aggiuntivi
     */
    @Override
    public abstract double getCostoScontato();
}

