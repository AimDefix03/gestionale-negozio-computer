package it.giovannidefilippo.gestionale.purchase;

import it.giovannidefilippo.gestionale.product.ProductProcurementUsage;
import org.springframework.stereotype.Component;

@Component
class SupplierOrderProductUsageAdapter implements ProductProcurementUsage {
    private final SupplierOrderRepository repository;

    SupplierOrderProductUsageAdapter(SupplierOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByProductId(Long productId) {
        return repository.existsItemByProductId(productId);
    }
}
