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
    Optional<Product> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("select product from Product product where lower(product.code) = lower(:code)")
    Optional<Product> findByCodeForStockAdjustment(@Param("code") String code);

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
    java.math.BigDecimal sumDiscountedInventoryValue();

    boolean existsByCodeIgnoreCase(String code);
}
