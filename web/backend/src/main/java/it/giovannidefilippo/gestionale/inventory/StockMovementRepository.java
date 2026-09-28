package it.giovannidefilippo.gestionale.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {
    @Query("select count(m) > 0 from StockMovement m where m.productId = :productId and m.baselineMarker = true")
    boolean hasInitialBalance(Long productId);

    boolean existsBySupplierOrderReceiptItemId(Long supplierOrderReceiptItemId);

    boolean existsByPhysicalInventoryItemId(Long physicalInventoryItemId);

    List<StockMovement> findAllByOrderByTimestampAscIdAsc();
}
