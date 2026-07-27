package factory;

import model.Hardware;
import model.Prodotto;
import model.Software;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ProdottoFactoryProviderTest {

    @Test
    void creaProdottoHardwareConFactoryCorretta() {
        ProdottoFactory factory = ProdottoFactoryProvider.getFactory(CategoriaProdotto.HARDWARE);

        Prodotto prodotto = factory.creaProdotto("H001", "RAM", "Memoria RAM", "Corsair", "RAM", "Gaming", 5, 80, 10);

        assertInstanceOf(Hardware.class, prodotto);
    }

    @Test
    void creaProdottoSoftwareConFactoryCorretta() {
        ProdottoFactory factory = ProdottoFactoryProvider.getFactory(CategoriaProdotto.SOFTWARE);

        Prodotto prodotto = factory.creaProdotto("S001", "IDE", "Ambiente di sviluppo", "JetBrains", "IDE", "Programmazione", 3, 120, 0);

        assertInstanceOf(Software.class, prodotto);
    }
}
