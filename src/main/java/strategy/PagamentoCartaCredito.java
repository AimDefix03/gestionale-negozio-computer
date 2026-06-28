package strategy;

public class PagamentoCartaCredito implements PagamentoStrategy {
    @Override
    public void paga(double importo) {
        System.out.println("Pagamento con carta di credito di: " + importo + "€");
    }
}
