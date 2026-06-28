package strategy;

public class PagamentoContanti implements PagamentoStrategy {
    @Override
    public void paga(double importo) {
        System.out.println("Pagamento in contanti di: " + importo + "€");
    }
}
