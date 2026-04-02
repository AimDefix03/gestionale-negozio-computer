package model;

import command.Comando;

/**
 * Classe che rappresenta un comando per aggiungere un prodotto al carrello.
 * Implementa il pattern Command, consentendo di incapsulare l'operazione di aggiunta del prodotto
 * come un oggetto che può essere eseguito in modo flessibile.
 */
public class AggiungiAlCarrello implements Comando {
    private Carrello carrello;   // Riferimento al carrello su cui verrà eseguita l'operazione
    private Prodotto prodotto;   // Prodotto da aggiungere al carrello

    /**
     * Costruttore della classe AggiungiAlCarrello.
     * Inizializza il comando con il carrello di destinazione e il prodotto da aggiungere.
     *
     * @param carrello Il carrello su cui aggiungere il prodotto
     * @param prodotto Il prodotto da aggiungere al carrello
     */
    public AggiungiAlCarrello(Carrello carrello, Prodotto prodotto) {
        this.carrello = carrello;
        this.prodotto = prodotto;
    }

    /**
     * Esegue il comando di aggiunta del prodotto al carrello.
     * Utilizza il metodo {@code aggiungiProdotto()} del carrello per completare l'operazione.
     * Viene stampato un messaggio di conferma a console.
     */
    @Override
    public void esegui() {
        carrello.aggiungiProdotto(prodotto);  // Aggiunge il prodotto al carrello
        System.out.println("Prodotto aggiunto al carrello: " + prodotto.nome); // Messaggio di conferma
    }
}

