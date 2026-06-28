package strategy;

public final class PagamentoStrategyFactory {
    private static final String CONTANTI = "Contanti";
    private static final String CARTA_CREDITO = "Carta di Credito";
    private static final String BANCOMAT = "Bancomat";

    private PagamentoStrategyFactory() {
    }

    public static String[] getMetodiPagamento() {
        return new String[]{CONTANTI, CARTA_CREDITO, BANCOMAT};
    }

    public static PagamentoStrategy crea(String metodoPagamento) {
        if (metodoPagamento == null) {
            return new PagamentoContanti();
        }

        return switch (metodoPagamento) {
            case CARTA_CREDITO -> new PagamentoCartaCredito();
            case BANCOMAT -> new PagamentoBancomat();
            case CONTANTI -> new PagamentoContanti();
            default -> throw new IllegalArgumentException("Metodo di pagamento non supportato: " + metodoPagamento);
        };
    }
}
