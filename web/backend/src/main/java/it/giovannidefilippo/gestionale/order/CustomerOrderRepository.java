package it.giovannidefilippo.gestionale.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long>, JpaSpecificationExecutor<CustomerOrder> {
    @Override
    @EntityGraph(attributePaths = "payment")
    List<CustomerOrder> findAll();

    @Override
    @EntityGraph(attributePaths = "payment")
    Page<CustomerOrder> findAll(@Nullable Specification<CustomerOrder> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"payment", "items"})
    List<CustomerOrder> findAll(@Nullable Specification<CustomerOrder> specification, Sort sort);

    Optional<CustomerOrder> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where lower(o.code) = lower(:code)")
    Optional<CustomerOrder> findByCodeForUpdate(@Param("code") String code);

    boolean existsByCodeIgnoreCase(String code);

    long countByCustomerIgnoreCase(String customer);

    @Query("select coalesce(sum(customerOrder.total), 0) from CustomerOrder customerOrder")
    BigDecimal sumTotal();

    @Query("select coalesce(sum(customerOrder.total), 0) from CustomerOrder customerOrder where lower(customerOrder.customer) = lower(:customer)")
    BigDecimal sumTotalByCustomer(@Param("customer") String customer);

    @EntityGraph(attributePaths = "payment")
    List<CustomerOrder> findByCustomerIgnoreCase(String customer);
}
