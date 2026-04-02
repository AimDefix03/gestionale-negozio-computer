package strategy;

/**
 * Classe che rappresenta una modalità di pagamento tramite carta di credito.
 * Implementa l'interfaccia {@link PagamentoStrategy}, definendo il comportamento specifico per i pagamenti con carta di credito.
 * Fa parte del Pattern Strategy, che consente di gestire diverse modalità di pagamento in modo flessibile.
 */
public class PagamentoCartaCredito implements PagamentoStrategy {

    /**
     * Esegue il pagamento utilizzando una carta di credito.
     * Questo metodo simula il pagamento stampando un messaggio di conferma dell'importo pagato.
     *
     * @param importo L'importo totale da pagare con la carta di credito
     */
    @Override
    public void paga(double importo) {
        System.out.println("Pagamento con carta di credito di: " + importo + "€"); // Simulazione del pagamento con carta di credito
    }
}

