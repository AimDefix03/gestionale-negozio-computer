package service;

import model.Order;
import model.OrderItem;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class OrderReceiptService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public String buildReceipt(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("L'ordine è obbligatorio.");
        }

        StringBuilder receipt = new StringBuilder();
        receipt.append("GESTIONALE NEGOZIO COMPUTER").append(System.lineSeparator());
        receipt.append("RIEPILOGO ORDINE - NON VALIDO AI FINI FISCALI").append(System.lineSeparator());
        receipt.append("Ricevuta ordine ").append(order.code()).append(System.lineSeparator());
        receipt.append(System.lineSeparator());
        receipt.append("Data: ").append(order.timestamp().format(FORMATTER)).append(System.lineSeparator());
        receipt.append("Cliente: ").append(order.customer()).append(System.lineSeparator());
        receipt.append("Pagamento: ").append(order.paymentMethod()).append(System.lineSeparator());
        receipt.append(System.lineSeparator());
        receipt.append("Prodotti").append(System.lineSeparator());

        for (OrderItem item : order.items()) {
            receipt.append("- ")
                    .append(item.productCode())
                    .append(" | ")
                    .append(item.productName())
                    .append(" | Quantita: ")
                    .append(item.quantity())
                    .append(" | Prezzo: ")
                    .append(formatCurrency(item.unitPrice()))
                    .append(" | Subtotale: ")
                    .append(formatCurrency(item.lineTotal()))
                    .append(System.lineSeparator());
        }

        receipt.append(System.lineSeparator());
        receipt.append("Totale: ").append(formatCurrency(order.total())).append(System.lineSeparator());
        return receipt.toString();
    }

    private String formatCurrency(double value) {
        return String.format(Locale.ITALY, "%.2f euro", value);
    }
}
