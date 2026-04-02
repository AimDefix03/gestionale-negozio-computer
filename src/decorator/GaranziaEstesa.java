package decorator;

import model.Prodotto;

/**
 * Classe che rappresenta un decoratore concreto per aggiungere una garanzia estesa a un prodotto.
 * Estende la classe astratta {@link ProdottoDecorator}, implementando il Pattern Decorator per aggiungere funzionalità extra
 * senza modificare la struttura della classe originale {@link Prodotto}.
 */
public class GaranziaEstesa extends ProdottoDecorator {

    /**
     * Costruttore della classe GaranziaEstesa.
     * Inizializza il decoratore con il prodotto di base a cui verrà aggiunta la garanzia estesa.
     *
     * @param prodotto Il prodotto base a cui applicare la garanzia estesa
     */
    public GaranziaEstesa(Prodotto prodotto) {
        super(prodotto);
    }

    /**
     * Restituisce il nome del prodotto decorato, aggiungendo la descrizione della garanzia estesa.
     *
     * @return Nome del prodotto con l'indicazione della garanzia estesa
     */
    @Override
    public String getNome() {
        return prodottoBase.getNome() + " + Garanzia Estesa";
    }

    /**
     * Calcola il costo totale del prodotto, includendo un costo aggiuntivo fisso per la garanzia estesa.
     *
     * @return Costo del prodotto con la garanzia estesa inclusa
     */
    @Override
    public double getCostoScontato() {
        return prodottoBase.getCostoScontato() + 50; // Aggiunge 50€ per la garanzia estesa
    }
}


