package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record Order(
        String code,
        String customer,
        LocalDateTime timestamp,
        String paymentMethod,
        List<OrderItem> items,
        double total
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
