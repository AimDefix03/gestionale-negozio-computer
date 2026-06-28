package strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PagamentoStrategyFactoryTest {

    @Test
    void creaStrategiaContantiDiDefault() {
        PagamentoStrategy strategy = PagamentoStrategyFactory.crea(null);

        assertInstanceOf(PagamentoContanti.class, strategy);
    }

    @Test
    void creaStrategiaCartaCredito() {
        PagamentoStrategy strategy = PagamentoStrategyFactory.crea("Carta di Credito");

        assertInstanceOf(PagamentoCartaCredito.class, strategy);
    }

    @Test
    void creaStrategiaBancomat() {
        PagamentoStrategy strategy = PagamentoStrategyFactory.crea("Bancomat");

        assertInstanceOf(PagamentoBancomat.class, strategy);
    }
}
