package strategy;

public class PagamentoBancomat implements PagamentoStrategy {
    @Override
    public void paga(double importo) {
        System.out.println("Pagamento con bancomat di: " + importo + "€");
    }
}
