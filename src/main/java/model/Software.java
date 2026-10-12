package model;

public class Software extends Prodotto {
    private static final long serialVersionUID = 0xa855bc2045ed9686L;

    public Software(String codice, String nome, String descrizione, String utilizzo, int quantita, double costo, double sconto) {
        super(codice, nome, descrizione, "Software", utilizzo, quantita, costo, sconto);
    }

    public Software(
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
        super(codice, nome, descrizione, "Software", brand, tipoProdotto, utilizzo, quantita, costo, sconto);
    }

    public void visualizzaDettagli() {
        System.out.println("Software: " + getNome() + " - Prezzo: " + getCosto() + "€");
    }
}
