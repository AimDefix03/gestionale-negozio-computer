package model;

/**
 * Classe che rappresenta un componente hardware venduto nel negozio.
 * Estende la classe astratta {@link Prodotto}, ereditandone attributi e metodi di base.
 */
public class Hardware extends Prodotto {

    /**
     * Costruttore della classe Hardware.
     * Inizializza un oggetto Hardware con le informazioni specificate,
     * impostando automaticamente la categoria come "Hardware".
     */

    public Hardware(String codice, String nome, String descrizione, String utilizzo, int quantita, double costo, double sconto) {
        super(codice, nome, descrizione, "Hardware", utilizzo, quantita, costo, sconto);
    }

    /**
     * Metodo per visualizzare i dettagli dell'hardware.
     * Mostra il nome e il prezzo base (senza sconto) del prodotto.
     */
    public void visualizzaDettagli() {
        System.out.println("Hardware: " + nome + " - Prezzo: " + costo + "€");
    }
}


