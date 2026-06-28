package service;

import factory.CategoriaProdotto;
import model.Hardware;
import model.Prodotto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void creaProdottoConFactoryESalvaSuFile() {
        String storageFile = tempDir.resolve("prodotti.dat").toString();
        ProductService service = new ProductService(storageFile);

        Prodotto prodotto = service.creaEInserisciProdotto(
                "HW-10",
                "Notebook",
                "Computer portatile",
                "Studio",
                3,
                900,
                10,
                CategoriaProdotto.HARDWARE
        );

        ProductService reloadedService = new ProductService(storageFile);

        assertInstanceOf(Hardware.class, prodotto);
        assertEquals(1, reloadedService.getProdotti().size());
        assertEquals("Notebook", reloadedService.getProdotti().get(0).getNome());
        assertEquals(810, reloadedService.getProdotti().get(0).getCostoScontato());
    }

    @Test
    void rifiutaProdottiConValoriNonValidi() {
        ProductService service = new ProductService(tempDir.resolve("prodotti.dat").toString());

        assertThrows(IllegalArgumentException.class, () -> service.creaEInserisciProdotto(
                "",
                "Notebook",
                "Computer portatile",
                "Studio",
                3,
                900,
                10,
                CategoriaProdotto.HARDWARE
        ));

        assertThrows(IllegalArgumentException.class, () -> service.creaEInserisciProdotto(
                "HW-10",
                "Notebook",
                "Computer portatile",
                "Studio",
                3,
                900,
                150,
                CategoriaProdotto.HARDWARE
        ));
    }
}
