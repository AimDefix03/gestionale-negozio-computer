package model;
import java.io.Serializable;

/**
 * Classe astratta che rappresenta un prodotto generico venduto nel negozio,
 * come hardware o software. Definisce gli attributi comuni e i metodi di base
 * per la gestione dei prodotti.
 */
public abstract class Prodotto implements Serializable {
    private static final long serialVersionUID = 1L; // Identificatore per la serializzazione, evita problemi di compatibilità durante la deserializzazione

    // Attributi del prodotto
    protected String codice;          // Codice univoco del prodotto
    protected String nome;            // Nome del prodotto
    protected String descrizione;     // Descrizione dettagliata del prodotto
    protected String categoria;       // Categoria del prodotto (es. hardware, software)
    protected String utilizzo;        // Scopo o utilizzo del prodotto (es. multimediale, elaborazione testi)
    protected int quantita;           // Quantità disponibile in magazzino
    protected double costo;           // Costo base del prodotto
    protected double sconto;          // Percentuale di sconto applicata al prodotto (es. 10% = 10.0)

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

    // Getter per accedere agli attributi del prodotto

    /**
     * Restituisce il codice univoco del prodotto.
     * @return codice del prodotto
     */
    public String getCodice() { return codice; }

    /**
     * Restituisce il nome del prodotto.
     * @return nome del prodotto
     */
    public String getNome() { return nome; }

    /**
     * Restituisce la descrizione del prodotto.
     * @return descrizione del prodotto
     */
    public String getDescrizione() { return descrizione; }

    /**
     * Restituisce la categoria del prodotto.
     * @return categoria del prodotto
     */
    public String getCategoria() { return categoria; }

    /**
     * Restituisce l'utilizzo del prodotto (es. multimediale, elaborazione testi).
     * @return utilizzo del prodotto
     */
    public String getUtilizzo() { return utilizzo; }

    /**
     * Restituisce la quantità disponibile in magazzino.
     * @return quantità disponibile
     */
    public int getQuantita() { return quantita; }

    /**
     * Restituisce la percentuale di sconto applicata al prodotto.
     * @return percentuale di sconto
     */
    public double getSconto() { return sconto; }

    /**
     * Calcola e restituisce il costo del prodotto applicando lo sconto.
     *
     * @return costo scontato del prodotto
     */
    public double getCostoScontato() {
        return costo * (1 - sconto / 100);
    }
}

