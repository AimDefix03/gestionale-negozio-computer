package it.giovannidefilippo.gestionale.reporting;

import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.order.OrderStatus;
import it.giovannidefilippo.gestionale.order.SalesOrderItemReportSource;
import it.giovannidefilippo.gestionale.order.SalesOrderReportSource;
import it.giovannidefilippo.gestionale.order.SalesReportingUsage;
import it.giovannidefilippo.gestionale.product.InventoryProductReportSource;
import it.giovannidefilippo.gestionale.product.InventoryReportingUsage;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportingService {
    static final int MAX_REPORT_ROWS = 10_000;
    private static final int LOW_STOCK_THRESHOLD = 3;
    private static final long MAX_REPORT_DAYS = 366L * 5;

    private final SalesReportingUsage salesReportingUsage;
    private final InventoryReportingUsage inventoryReportingUsage;
    private final TimeProvider timeProvider;

    ReportingService(SalesReportingUsage salesReportingUsage, InventoryReportingUsage inventoryReportingUsage, TimeProvider timeProvider) {
        this.salesReportingUsage = salesReportingUsage;
        this.inventoryReportingUsage = inventoryReportingUsage;
        this.timeProvider = timeProvider;
    }

    public SalesReportResponse sales(LocalDate requestedFrom, LocalDate requestedTo, OrderStatus status) {
        LocalDate today = timeProvider.localDateTime().toLocalDate();
        LocalDate from = requestedFrom == null ? today.withDayOfYear(1) : requestedFrom;
        LocalDate to = requestedTo == null ? today : requestedTo;
        validatePeriod(from, to);
        List<SalesOrderReportSource> sources = salesReportingUsage.findForReport(from, to, status, MAX_REPORT_ROWS);
        List<SalesReportResponse.SalesOrderRow> rows = sources.stream()
                .map(ReportingService::toSalesRow)
                .toList();
        BigDecimal orderValue = sum(sources.stream().map(SalesOrderReportSource::total).toList());
        BigDecimal paid = sum(sources.stream().map(SalesOrderReportSource::paidAmount).toList());
        BigDecimal refunded = sum(sources.stream().map(SalesOrderReportSource::refundedAmount).toList());
        BigDecimal netCollected = sum(sources.stream().map(SalesOrderReportSource::netPaidAmount).toList());
        BigDecimal outstanding = sum(sources.stream().map(SalesOrderReportSource::outstandingAmount).toList());
        BigDecimal average = sources.isEmpty()
                ? money(BigDecimal.ZERO)
                : orderValue.divide(BigDecimal.valueOf(sources.size()), 2, RoundingMode.HALF_UP);
        String statusValue = status == null ? "ALL" : status.name();
        String statusLabel = status == null ? "Tutti gli stati" : status.getLabel();
        return new SalesReportResponse(
                timeProvider.localDateTime(),
                from,
                to,
                statusValue,
                statusLabel,
                sources.size(),
                orderValue,
                paid,
                refunded,
                netCollected,
                outstanding,
                average,
                rows,
                topProducts(sources)
        );
    }

    public InventoryReportResponse inventory(String query, ProductCategory category, String stock, Boolean discontinued) {
        List<InventoryProductReportSource> sources = inventoryReportingUsage.findForReport(query, category, stock, discontinued, MAX_REPORT_ROWS);
        List<InventoryReportResponse.InventoryProductRow> rows = sources.stream()
                .map(ReportingService::toInventoryRow)
                .toList();
        long physical = sources.stream().mapToLong(InventoryProductReportSource::quantity).sum();
        long reserved = sources.stream().mapToLong(InventoryProductReportSource::reservedQuantity).sum();
        long available = sources.stream().mapToLong(InventoryProductReportSource::availableQuantity).sum();
        BigDecimal inventoryValue = sum(rows.stream().map(InventoryReportResponse.InventoryProductRow::stockValue).toList());
        long lowStock = sources.stream().filter(source -> source.availableQuantity() > 0 && source.availableQuantity() <= LOW_STOCK_THRESHOLD).count();
        long outOfStock = sources.stream().filter(source -> source.availableQuantity() == 0).count();
        long discontinuedCount = sources.stream().filter(InventoryProductReportSource::discontinued).count();
        return new InventoryReportResponse(
                timeProvider.localDateTime(),
                sources.size(),
                physical,
                reserved,
                available,
                inventoryValue,
                lowStock,
                outOfStock,
                discontinuedCount,
                rows
        );
    }

    private static void validatePeriod(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La data iniziale non puo essere successiva alla data finale.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_REPORT_DAYS) {
            throw new IllegalArgumentException("Il periodo del report non puo superare cinque anni.");
        }
    }

    private static SalesReportResponse.SalesOrderRow toSalesRow(SalesOrderReportSource source) {
        return new SalesReportResponse.SalesOrderRow(
                source.code(),
                source.timestamp(),
                source.customer(),
                source.status().name(),
                source.status().getLabel(),
                money(source.total()),
                money(source.paidAmount()),
                money(source.refundedAmount()),
                money(source.netPaidAmount()),
                money(source.outstandingAmount())
        );
    }

    private static List<SalesReportResponse.TopProductRow> topProducts(List<SalesOrderReportSource> sources) {
        Map<String, ProductAccumulator> products = new LinkedHashMap<>();
        sources.stream()
                .flatMap(source -> source.items().stream())
                .forEach(item -> products.computeIfAbsent(item.productCode().toUpperCase(Locale.ROOT), ignored -> new ProductAccumulator(item.productCode(), item.productName()))
                        .add(item));
        return products.values().stream()
                .sorted(Comparator.comparingInt(ProductAccumulator::quantity).reversed()
                        .thenComparing(ProductAccumulator::productName)
                        .thenComparing(ProductAccumulator::productCode))
                .limit(10)
                .map(product -> new SalesReportResponse.TopProductRow(
                        product.productCode(),
                        product.productName(),
                        product.quantity(),
                        money(product.orderValue())
                ))
                .toList();
    }

    private static InventoryReportResponse.InventoryProductRow toInventoryRow(InventoryProductReportSource source) {
        StockState stockState = StockState.from(source.availableQuantity());
        BigDecimal stockValue = source.discountedPrice().multiply(BigDecimal.valueOf(source.quantity()));
        return new InventoryReportResponse.InventoryProductRow(
                source.code(),
                source.name(),
                source.category().name(),
                source.category().getLabel(),
                source.brand(),
                source.productType(),
                source.quantity(),
                source.reservedQuantity(),
                source.availableQuantity(),
                money(source.price()),
                source.discount().setScale(2, RoundingMode.HALF_UP),
                money(source.discountedPrice()),
                money(stockValue),
                source.discontinued(),
                stockState.name(),
                stockState.label
        );
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return money(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private enum StockState {
        AVAILABLE("Disponibile"),
        LOW("Scorta bassa"),
        OUT("Esaurito");

        private final String label;

        StockState(String label) {
            this.label = label;
        }

        static StockState from(int availableQuantity) {
            if (availableQuantity == 0) {
                return OUT;
            }
            return availableQuantity <= LOW_STOCK_THRESHOLD ? LOW : AVAILABLE;
        }
    }

    private static final class ProductAccumulator {
        private final String productCode;
        private final String productName;
        private int quantity;
        private BigDecimal orderValue = BigDecimal.ZERO;

        private ProductAccumulator(String productCode, String productName) {
            this.productCode = productCode;
            this.productName = productName;
        }

        private void add(SalesOrderItemReportSource item) {
            quantity += item.quantity();
            orderValue = orderValue.add(item.lineTotal());
        }

        private String productCode() {
            return productCode;
        }

        private String productName() {
            return productName;
        }

        private int quantity() {
            return quantity;
        }

        private BigDecimal orderValue() {
            return orderValue;
        }
    }
}
