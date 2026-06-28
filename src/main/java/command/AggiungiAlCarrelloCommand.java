package command;

import model.Carrello;
import model.Prodotto;

public class AggiungiAlCarrelloCommand implements Command {
    private final Carrello carrello;
    private final Prodotto prodotto;

    public AggiungiAlCarrelloCommand(Carrello carrello, Prodotto prodotto) {
        this.carrello = carrello;
        this.prodotto = prodotto;
    }

    @Override
    public void execute() {
        carrello.aggiungiProdotto(prodotto);
    }
}
