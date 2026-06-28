package model;

import java.io.Serializable;

public abstract class Prodotto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String codice;
    private final String nome;
    private final String descrizione;
    private final String categoria;
    private final String utilizzo;
    private final int quantita;
    private final double costo;
    private final double sconto;

    public Prodotto(String codice, String nome, String descrizione, String categoria, String utilizzo, int quantita, double costo, double sconto) {
        this.codice = codice;
        this.nome = nome;
        this.descrizione = descrizione;
        this.categoria = categoria;
        this.utilizzo = utilizzo;
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

    public String getUtilizzo() {
        return utilizzo;
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
