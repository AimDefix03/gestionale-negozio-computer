package it.giovannidefilippo.gestionale.purchase;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface SupplierOrderRepository extends JpaRepository<SupplierOrder, Long>, JpaSpecificationExecutor<SupplierOrder> {
    Optional<SupplierOrder> findByCodeIgnoreCase(String code);

    @Query("select (count(item) > 0) from SupplierOrderItem item where item.productId = :productId")
    boolean existsItemByProductId(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select supplierOrder from SupplierOrder supplierOrder where lower(supplierOrder.code) = lower(:code)")
    Optional<SupplierOrder> findByCodeForUpdate(@Param("code") String code);
}
