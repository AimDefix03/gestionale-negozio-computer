package it.giovannidefilippo.gestionale.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Query("select product from Product product where product.codeCanonical = lower(trim(:code))")
    Optional<Product> findByCodeIgnoreCase(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.codeCanonical = lower(trim(:code))")
    Optional<Product> findByCodeForStockAdjustment(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id in :ids order by product.id")
    List<Product> findAllByIdForStockAdjustment(@Param("ids") List<Long> ids);

    @Query("""
            select product from Product product
            where (product.quantity - product.reservedQuantity) > 0
              and (product.quantity - product.reservedQuantity) <= :threshold
            order by product.name asc, product.code asc
            """)
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    @Query("""
            select count(product) from Product product
            where (product.quantity - product.reservedQuantity) > 0
              and (product.quantity - product.reservedQuantity) <= :threshold
            """)
    long countLowStockProducts(@Param("threshold") int threshold);

    @Query("""
            select count(product) from Product product
            where (product.quantity - product.reservedQuantity) = 0
            """)
    long countOutOfStockProducts();

    @Query("""
            select coalesce(sum(product.price * (100 - product.discount) / 100 * product.quantity), 0)
            from Product product
            """)
    java.math.BigDecimal sumPotentialRetailStockValue();

    @Query("select coalesce(sum(product.averagePurchaseCost * product.costedQuantity), 0) from Product product")
    java.math.BigDecimal sumKnownInventoryCostValue();

    @Query("select coalesce(sum((product.price * (100 - product.discount) / 100 - product.averagePurchaseCost) * product.costedQuantity), 0) from Product product")
    java.math.BigDecimal sumPotentialGrossMarginOnCostedStock();

    @Query("select coalesce(sum(product.costedQuantity), 0) from Product product")
    long sumCostedQuantity();

    @Query("select coalesce(sum(product.quantity - product.costedQuantity), 0) from Product product")
    long sumUncostedQuantity();

}
