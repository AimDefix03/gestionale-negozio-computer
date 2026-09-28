package it.giovannidefilippo.gestionale.dashboard;

import it.giovannidefilippo.gestionale.document.DocumentOrderCapabilities;
import it.giovannidefilippo.gestionale.order.OrderResponse;

public record OrderOperationalDetailResponse(
        OrderResponse order,
        DocumentOrderCapabilities documents
) {
}
