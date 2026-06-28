package command;

import model.Carrello;
import model.Hardware;
import model.Prodotto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AggiungiAlCarrelloCommandTest {

    @Test
    void executeAggiungeIlProdottoAlCarrello() {
        Carrello carrello = new Carrello();
        Prodotto prodotto = new Hardware("HW-1", "Scheda video", "GPU", "Gaming", 1, 450, 0);

        Command command = new AggiungiAlCarrelloCommand(carrello, prodotto);
        command.execute();

        assertEquals(1, carrello.getProdotti().size());
        assertSame(prodotto, carrello.getProdotti().get(0));
    }
}
