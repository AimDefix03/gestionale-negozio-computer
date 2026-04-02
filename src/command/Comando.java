// Interfaccia Command
package command;

/**
 * Interfaccia che rappresenta il Command nel Pattern Command.
 * Definisce un metodo astratto {@code esegui()} che deve essere implementato
 * dalle classi concrete per eseguire un'azione specifica.

 * Il Pattern Command consente di incapsulare una richiesta come oggetto,
 * permettendo di parametrizzare i client con diverse operazioni, supportare code di richieste e operazioni annullabili.
 */
public interface Comando {

    /**
     * Metodo astratto per eseguire un'operazione specifica.
     * Le classi che implementano questa interfaccia definiranno il comportamento concreto di questa operazione,
     * come ad esempio aggiungere un prodotto al carrello o gestire azioni utente.
     */
    void esegui();
}

