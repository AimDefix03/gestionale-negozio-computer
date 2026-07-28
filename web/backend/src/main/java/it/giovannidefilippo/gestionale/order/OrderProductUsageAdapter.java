package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.product.ProductOrderUsage;
import org.springframework.stereotype.Component;

@Component
class OrderProductUsageAdapter implements ProductOrderUsage {
    private final OrderProductUsageRepository repository;

    OrderProductUsageAdapter(OrderProductUsageRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByProductCode(String productCode) {
        return repository.existsByProductCode(productCode);
    }
}
