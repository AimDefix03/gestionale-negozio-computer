package service;

import model.Order;
import model.OrderItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderReceiptServiceTest {
    private final OrderReceiptService service = new OrderReceiptService();

    @Test
    void generaRicevutaConDettaglioProdottiETotale() {
        Order order = new Order(
                "ORD-0001",
                "cliente",
                LocalDateTime.of(2026, 6, 30, 18, 15),
                "Contanti",
                List.of(new OrderItem("HW-1", "RAM", 2, 80, 160)),
                160
        );

        String receipt = service.buildReceipt(order);

        assertTrue(receipt.contains("Ricevuta ordine ORD-0001"));
        assertTrue(receipt.contains("NON VALIDO AI FINI FISCALI"));
        assertTrue(receipt.contains("Cliente: cliente"));
        assertTrue(receipt.contains("HW-1 | RAM | Quantita: 2"));
        assertTrue(receipt.contains("Totale: 160,00 euro"));
    }

    @Test
    void rifiutaOrdineNullo() {
        assertThrows(IllegalArgumentException.class, () -> service.buildReceipt(null));
    }
}
