package model;

import java.io.Serializable;

public abstract class Prodotto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String codice;
    private final String nome;
    private final String descrizione;
    private final String categoria;
    private final String brand;
    private final String tipoProdotto;
    private final String utilizzo;
    private final int quantita;
    private final double costo;
    private final double sconto;

    public Prodotto(String codice, String nome, String descrizione, String categoria, String utilizzo, int quantita, double costo, double sconto) {
        this(codice, nome, descrizione, categoria, "", "", utilizzo, quantita, costo, sconto);
    }

    public Prodotto(
            String codice,
            String nome,
            String descrizione,
            String categoria,
            String brand,
            String tipoProdotto,
            String utilizzo,
            int quantita,
            double costo,
            double sconto
    ) {
        this.codice = codice;
        this.nome = nome;
        this.descrizione = descrizione;
        this.categoria = categoria;
        this.brand = brand;
        this.tipoProdotto = tipoProdotto;
        this.utilizzo = utilizzo == null ? "" : utilizzo;
        this.quantita = quantita;
        this.costo = costo;
        this.sconto = sconto;
    }

    public String getCodice() {
        return codice;
    }

    public String getNome() {
        return nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getBrand() {
        return brand == null ? "" : brand;
    }

    public String getTipoProdotto() {
        return tipoProdotto == null ? "" : tipoProdotto;
    }

    public String getUtilizzo() {
        return utilizzo == null ? "" : utilizzo;
    }

    public int getQuantita() {
        return quantita;
    }

    public double getSconto() {
        return sconto;
    }

    public double getCosto() {
        return costo;
    }

    public double getCostoScontato() {
        return costo * (1 - sconto / 100);
    }
}
