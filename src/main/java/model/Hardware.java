package model;

public class Hardware extends Prodotto {
    private static final long serialVersionUID = 0xc7acbda0041b1b32L;

    public Hardware(String codice, String nome, String descrizione, String utilizzo, int quantita, double costo, double sconto) {
        super(codice, nome, descrizione, "Hardware", utilizzo, quantita, costo, sconto);
    }

    public Hardware(
            String codice,
            String nome,
            String descrizione,
            String brand,
            String tipoProdotto,
            String utilizzo,
            int quantita,
            double costo,
            double sconto
    ) {
        super(codice, nome, descrizione, "Hardware", brand, tipoProdotto, utilizzo, quantita, costo, sconto);
    }

    public void visualizzaDettagli() {
        System.out.println("Hardware: " + getNome() + " - Prezzo: " + getCosto() + "€");
    }
}
