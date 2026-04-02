package strategy;

/**
 * Classe che rappresenta una modalità di pagamento tramite bancomat.
 * Implementa l'interfaccia {@link PagamentoStrategy}, definendo il comportamento specifico per i pagamenti con bancomat.
 * Fa parte del Pattern Strategy, che consente di gestire diverse modalità di pagamento in modo flessibile.
 */
public class PagamentoBancomat implements PagamentoStrategy {

    /**
     * Esegue il pagamento utilizzando il bancomat.
     * Questo metodo simula il pagamento stampando un messaggio di conferma dell'importo pagato.
     *
     * @param importo L'importo totale da pagare con il bancomat
     */
    @Override
    public void paga(double importo) {
        System.out.println("Pagamento con bancomat di: " + importo + "€"); // Simulazione del pagamento con bancomat
    }
}

