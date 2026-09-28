package it.giovannidefilippo.gestionale.order;

record OrderCancellationResult(OrderStatus previousStatus, PaymentTransaction reversal) {
}
