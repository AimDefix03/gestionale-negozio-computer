package model;

public class Hardware extends Prodotto {
    public Hardware(String codice, String nome, String descrizione, String utilizzo, int quantita, double costo, double sconto) {
        super(codice, nome, descrizione, "Hardware", utilizzo, quantita, costo, sconto);
    }

    public void visualizzaDettagli() {
        System.out.println("Hardware: " + getNome() + " - Prezzo: " + getCosto() + "€");
    }
}

