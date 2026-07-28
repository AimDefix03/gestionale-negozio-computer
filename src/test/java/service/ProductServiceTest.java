package service;

import factory.CategoriaProdotto;
import model.Hardware;
import model.Prodotto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import utils.FileManager;

import java.nio.file.Path;
import java.util.List;

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
                "Lenovo",
                "Notebook",
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
                "Lenovo",
                "Notebook",
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
                "Lenovo",
                "Notebook",
                "Studio",
                3,
                900,
                150,
                CategoriaProdotto.HARDWARE
        ));
    }

    @Test
    void accettaUtilizzoVuotoMaRichiedeBrandETipoProdotto() {
        ProductService service = new ProductService(tempDir.resolve("prodotti.dat").toString());

        Prodotto prodotto = service.creaEInserisciProdotto(
                "HW-20",
                "RTX 5090",
                "Scheda video dedicata",
                "NVIDIA",
                "Scheda grafica",
                "",
                2,
                1900,
                0,
                CategoriaProdotto.HARDWARE
        );

        assertEquals("NVIDIA", prodotto.getBrand());
        assertEquals("Scheda grafica", prodotto.getTipoProdotto());
        assertEquals("", prodotto.getUtilizzo());

        assertThrows(IllegalArgumentException.class, () -> service.creaEInserisciProdotto(
                "HW-21",
                "Ryzen 7 9800X3D",
                "CPU desktop",
                "",
                "Processore",
                "",
                1,
                520,
                0,
                CategoriaProdotto.HARDWARE
        ));
    }

    @Test
    void normalizzaProdottiLegacySenzaBrandETipoProdotto() {
        String storageFile = tempDir.resolve("prodotti.dat").toString();
        FileManager.salvaSuFile(storageFile, List.of(new Hardware(
                "HW-OLD",
                "Intel Core i7",
                "Processore salvato con vecchio formato",
                "Editing",
                2,
                320,
                0
        )));

        ProductService service = new ProductService(storageFile);
        Prodotto prodotto = service.getProdottoByCodice("HW-OLD");

        assertEquals("Da definire", prodotto.getBrand());
        assertEquals("Da definire", prodotto.getTipoProdotto());

        service.aggiornaQuantita("HW-OLD", 4);
        assertEquals(4, service.getProdottoByCodice("HW-OLD").getQuantita());
    }

    @Test
    void aggiornaProdottoEsistente() {
        String storageFile = tempDir.resolve("prodotti.dat").toString();
        ProductService service = new ProductService(storageFile);
        service.creaEInserisciProdotto(
                "HW-10",
                "Notebook",
                "Computer portatile",
                "Lenovo",
                "Notebook",
                "Studio",
                3,
                900,
                10,
                CategoriaProdotto.HARDWARE
        );

        Prodotto aggiornato = service.aggiornaProdotto(
                "HW-10",
                "SW-10",
                "IDE",
                "Ambiente di sviluppo",
                "JetBrains",
                "IDE",
                "Programmazione",
                5,
                120,
                0,
                CategoriaProdotto.SOFTWARE
        );

        assertEquals("SW-10", aggiornato.getCodice());
        assertEquals("IDE", service.getProdotti().get(0).getNome());
        assertEquals("Software", service.getProdotti().get(0).getCategoria());
        assertEquals("JetBrains", service.getProdotti().get(0).getBrand());
        assertEquals("IDE", service.getProdotti().get(0).getTipoProdotto());
    }

    @Test
    void eliminaPiuProdottiSelezionati() {
        ProductService service = new ProductService(tempDir.resolve("prodotti.dat").toString());
        Prodotto primo = service.creaEInserisciProdotto("HW-1", "RAM", "Memoria", "Corsair", "RAM", "Gaming", 2, 80, 0, CategoriaProdotto.HARDWARE);
        Prodotto secondo = service.creaEInserisciProdotto("SW-1", "IDE", "Ambiente", "JetBrains", "IDE", "Dev", 1, 120, 0, CategoriaProdotto.SOFTWARE);

        service.eliminaProdotti(java.util.List.of(primo, secondo));

        assertEquals(0, service.getProdotti().size());
    }

    @Test
    void rifiutaCodiciDuplicati() {
        ProductService service = new ProductService(tempDir.resolve("prodotti.dat").toString());
        service.creaEInserisciProdotto("HW-1", "RAM", "Memoria", "Corsair", "RAM", "Gaming", 2, 80, 0, CategoriaProdotto.HARDWARE);

        assertThrows(IllegalArgumentException.class, () -> service.creaEInserisciProdotto(
                "HW-1",
                "SSD",
                "Disco",
                "Samsung",
                "SSD",
                "Storage",
                3,
                100,
                0,
                CategoriaProdotto.HARDWARE
        ));
    }
}
