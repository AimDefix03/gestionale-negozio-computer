package factory;

import java.util.EnumMap;
import java.util.Map;

public final class ProdottoFactoryProvider {
    private static final Map<CategoriaProdotto, ProdottoFactory> FACTORIES = new EnumMap<>(CategoriaProdotto.class);

    static {
        FACTORIES.put(CategoriaProdotto.HARDWARE, new HardwareFactory());
        FACTORIES.put(CategoriaProdotto.SOFTWARE, new SoftwareFactory());
    }

    private ProdottoFactoryProvider() {
    }

    public static ProdottoFactory getFactory(CategoriaProdotto categoria) {
        ProdottoFactory factory = FACTORIES.get(categoria);
        if (factory == null) {
            throw new IllegalArgumentException("Factory non configurata per la categoria: " + categoria);
        }
        return factory;
    }
}
