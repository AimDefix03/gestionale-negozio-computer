package decorator;

import model.Hardware;
import model.Prodotto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProdottoDecoratorTest {

    @Test
    void garanziaEstesaAggiungeNomeECostoSenzaModificareIlProdottoBase() {
        Prodotto prodotto = new Hardware("H001", "Notebook", "Notebook business", "Ufficio", 2, 1000, 10);

        Prodotto decorato = new GaranziaEstesa(prodotto);

        assertEquals("Notebook + Garanzia Estesa", decorato.getNome());
        assertEquals(950, decorato.getCostoScontato());
        assertEquals("Notebook", prodotto.getNome());
        assertEquals(900, prodotto.getCostoScontato());
    }

    @Test
    void decoratoriMultipliSiPossonoCombinare() {
        Prodotto prodotto = new Hardware("H002", "PC", "Desktop", "Lavoro", 1, 800, 0);

        Prodotto decorato = new PacchettoManutenzione(new AssemblaggioPC(prodotto));

        assertEquals("PC + Servizio di Assemblaggio + Pacchetto di Manutenzione", decorato.getNome());
        assertEquals(930, decorato.getCostoScontato());
    }
}
