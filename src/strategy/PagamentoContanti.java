package strategy;

/**
 * Classe che rappresenta una modalità di pagamento in contanti.
 * Implementa l'interfaccia {@link PagamentoStrategy}, definendo il comportamento specifico per i pagamenti in contanti.
 * Fa parte del Pattern Strategy, che consente di gestire diverse modalità di pagamento in modo flessibile.
 */
public class PagamentoContanti implements PagamentoStrategy {

    /**
     * Esegue il pagamento in contanti.
     * Questo metodo simula il pagamento stampando un messaggio di conferma dell'importo pagato.
     *
     * @param importo L'importo totale da pagare in contanti
     */
    @Override
    public void paga(double importo) {
        System.out.println("Pagamento in contanti di: " + importo + "€"); // Simulazione del pagamento in contanti
    }
}

