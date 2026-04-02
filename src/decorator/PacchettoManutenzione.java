package decorator;

import model.Prodotto;

/**
 * Classe che rappresenta un decoratore concreto per aggiungere un pacchetto di manutenzione a un prodotto.
 * Estende la classe astratta {@link ProdottoDecorator}, implementando il Pattern Decorator per aggiungere funzionalità extra
 * senza modificare la struttura della classe originale {@link Prodotto}.
 */
public class PacchettoManutenzione extends ProdottoDecorator {

    /**
     * Costruttore della classe PacchettoManutenzione.
     * Inizializza il decoratore con il prodotto di base a cui verrà aggiunto il pacchetto di manutenzione.
     *
     * @param prodotto Il prodotto base a cui applicare il pacchetto di manutenzione
     */
    public PacchettoManutenzione(Prodotto prodotto) {
        super(prodotto);
    }

    /**
     * Restituisce il nome del prodotto decorato, aggiungendo la descrizione del pacchetto di manutenzione.
     *
     * @return Nome del prodotto con l'indicazione del pacchetto di manutenzione
     */
    @Override
    public String getNome() {
        return prodottoBase.getNome() + " + Pacchetto di Manutenzione";
    }

    /**
     * Calcola il costo totale del prodotto, includendo un costo aggiuntivo fisso per il pacchetto di manutenzione.
     *
     * @return Costo del prodotto con il pacchetto di manutenzione incluso
     */
    @Override
    public double getCostoScontato() {
        return prodottoBase.getCostoScontato() + 30; // Costo extra per la manutenzione
    }
}

