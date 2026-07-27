package it.giovannidefilippo.gestionale.order;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@Transactional(readOnly = true)
class OrderSalesReportingAdapter implements SalesReportingUsage {
    private final CustomerOrderRepository repository;

    OrderSalesReportingAdapter(CustomerOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SalesOrderReportSource> findForReport(LocalDate from, LocalDate to, OrderStatus status, int maxRows) {
        Specification<CustomerOrder> specification = specification(from, to, status);
        long totalRows = repository.count(specification);
        if (totalRows > maxRows) {
            throw new IllegalArgumentException("Il report contiene " + totalRows + " ordini. Restringi i filtri a un massimo di " + maxRows + " righe.");
        }
        return repository.findAll(specification, Sort.by("timestamp").descending())
                .stream()
                .map(OrderSalesReportingAdapter::toSource)
                .toList();
    }

    private static Specification<CustomerOrder> specification(LocalDate from, LocalDate to, OrderStatus status) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), start));
            predicates.add(criteriaBuilder.lessThan(root.get("timestamp"), endExclusive));
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static SalesOrderReportSource toSource(CustomerOrder order) {
        OrderPayment payment = order.getPayment();
        List<SalesOrderItemReportSource> items = order.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getProductName).thenComparing(OrderItem::getProductCode))
                .map(item -> new SalesOrderItemReportSource(
                        item.getProductCode(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();
        return new SalesOrderReportSource(
                order.getCode(),
                order.getCustomer(),
                order.getTimestamp(),
                order.getStatus(),
                order.getTotal(),
                payment.getPaidAmount(),
                payment.getRefundedAmount(),
                payment.getNetPaidAmount(),
                payment.getOutstandingAmount(),
                items
        );
    }
}
