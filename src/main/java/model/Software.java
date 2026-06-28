package model;

public class Software extends Prodotto {
    public Software(String codice, String nome, String descrizione, String utilizzo, int quantita, double costo, double sconto) {
        super(codice, nome, descrizione, "Software", utilizzo, quantita, costo, sconto);
    }

    public void visualizzaDettagli() {
        System.out.println("Software: " + getNome() + " - Prezzo: " + getCosto() + "€");
    }
}
