package strategy;

/**
 * Interfaccia che definisce la strategia di pagamento.
 * Fa parte del Pattern Strategy, consentendo di definire una famiglia di algoritmi di pagamento
 * e di renderli intercambiabili senza modificare il codice del client.
 */
public interface PagamentoStrategy {

    /**
     * Metodo astratto per effettuare un pagamento.
     * Le classi concrete che implementano questa interfaccia definiranno il comportamento specifico
     * del metodo di pagamento (es. contanti, carta di credito, bancomat).
     *
     * @param importo L'importo totale da pagare
     */
    void paga(double importo);
}
