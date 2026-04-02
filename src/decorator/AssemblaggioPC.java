package decorator;

import model.Prodotto;

/**
 * Classe che rappresenta un decoratore concreto per aggiungere il servizio di assemblaggio PC a un prodotto.
 * Estende la classe astratta {@link ProdottoDecorator}, implementando il Pattern Decorator per aggiungere funzionalità extra
 * senza modificare la struttura della classe originale {@link Prodotto}.
 */
public class AssemblaggioPC extends ProdottoDecorator {

    /**
     * Costruttore della classe AssemblaggioPC.
     * Inizializza il decoratore con il prodotto di base a cui verrà aggiunto il servizio di assemblaggio.
     *
     * @param prodotto Il prodotto base a cui applicare il servizio di assemblaggio
     */
    public AssemblaggioPC(Prodotto prodotto) {
        super(prodotto);
    }

    /**
     * Restituisce il nome del prodotto decorato, aggiungendo la descrizione del servizio di assemblaggio.
     *
     * @return Nome del prodotto con l'indicazione del servizio di assemblaggio
     */
    @Override
    public String getNome() {
        return prodottoBase.getNome() + " + Servizio di Assemblaggio";
    }

    /**
     * Calcola il costo totale del prodotto, includendo un costo aggiuntivo fisso per il servizio di assemblaggio.
     *
     * @return Costo del prodotto con il servizio di assemblaggio incluso
     */
    @Override
    public double getCostoScontato() {
        return prodottoBase.getCostoScontato() + 100; // Costo extra per assemblaggio
    }
}

