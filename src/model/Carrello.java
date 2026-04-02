package model;

import strategy.PagamentoStrategy;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe che rappresenta un carrello della spesa per un cliente.
 * Permette di aggiungere, visualizzare e acquistare prodotti utilizzando diverse strategie di pagamento.
 */
public class Carrello implements Serializable {
    private static final long serialVersionUID = 1L; // Identificatore per la serializzazione, utile per la compatibilità durante la deserializzazione

    private List<Prodotto> prodotti = new ArrayList<>(); // Lista dei prodotti aggiunti al carrello

    /**
     * Aggiunge un prodotto al carrello.
     *
     * @param prodotto Il prodotto da aggiungere al carrello
     */
    public void aggiungiProdotto(Prodotto prodotto) {
        prodotti.add(prodotto);
    }

    /**
     * Restituisce una copia della lista dei prodotti presenti nel carrello.
     * Restituire una copia aiuta a proteggere l'integrità dei dati interni,
     * evitando modifiche accidentali da parte di classi esterne.
     *
     * @return Lista di prodotti nel carrello (copia)
     */
    public List<Prodotto> getProdotti() {
        return new ArrayList<>(prodotti); // Ritorna una copia per evitare modifiche esterne
    }

    /**
     * Svuota il carrello, rimuovendo tutti i prodotti presenti.
     */
    public void svuotaCarrello() {
        prodotti.clear();
    }

    /**
     * Esegue l'acquisto dei prodotti presenti nel carrello.
     * Calcola il totale del carrello applicando gli sconti sui prodotti
     * e utilizza la strategia di pagamento selezionata dall'utente.
     * Dopo l'acquisto, il carrello viene svuotato.
     *
     * @param pagamentoStrategy Strategia di pagamento da utilizzare (es. contanti, carta di credito, bancomat)
     */
    public void acquista(PagamentoStrategy pagamentoStrategy) {
        // Calcolo del totale del carrello, applicando gli sconti su ciascun prodotto
        double totale = prodotti.stream().mapToDouble(p -> p.getCostoScontato()).sum();

        // Esecuzione del pagamento utilizzando la strategia scelta dall'utente
        pagamentoStrategy.paga(totale);

        // Svuotamento del carrello dopo il completamento dell'acquisto
        svuotaCarrello();
    }
}
