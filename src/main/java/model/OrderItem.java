package model;

import java.io.Serializable;

public record OrderItem(
        String productCode,
        String productName,
        int quantity,
        double unitPrice,
        double lineTotal
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
