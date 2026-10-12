package it.giovannidefilippo.gestionale.order;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface OrderProductUsageRepository extends Repository<OrderItem, Long> {
    @Query("select count(item) > 0 from OrderItem item where lower(item.productCode) = lower(:productCode)")
    boolean existsByProductCode(@Param("productCode") String productCode);
}
