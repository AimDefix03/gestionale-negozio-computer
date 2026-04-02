package model;

/**
 * Classe che rappresenta un prodotto software venduto nel negozio.
 * Estende la classe astratta {@link Prodotto}, ereditandone attributi e metodi di base.
 */
public class Software extends Prodotto {

    /**
     * Costruttore della classe Software.
     * Inizializza un oggetto Software con le informazioni specificate,
     * impostando automaticamente la categoria come "Software".

     */
    public Software(String codice, String nome, String descrizione, String utilizzo, int quantita, double costo, double sconto) {
        super(codice, nome, descrizione, "Software", utilizzo, quantita, costo, sconto);
    }

    /**
     * Metodo per visualizzare i dettagli del software.
     * Mostra il nome e il prezzo base (senza sconto) del software.
     */
    public void visualizzaDettagli() {
        System.out.println("Software: " + nome + " - Prezzo: " + costo + "€");
    }
}