package it.giovannidefilippo.gestionale.reporting;

import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.order.OrderStatus;
import it.giovannidefilippo.gestionale.order.SalesOrderItemReportSource;
import it.giovannidefilippo.gestionale.order.SalesOrderReportSource;
import it.giovannidefilippo.gestionale.order.SalesReportingUsage;
import it.giovannidefilippo.gestionale.product.InventoryProductReportSource;
import it.giovannidefilippo.gestionale.product.InventoryReportingUsage;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 14, 10, 30);

    @Mock
    private SalesReportingUsage salesUsage;

    @Mock
    private InventoryReportingUsage inventoryUsage;

    @Mock
    private TimeProvider timeProvider;

    private ReportingService service;

    @BeforeEach
    void setUp() {
        service = new ReportingService(salesUsage, inventoryUsage, timeProvider);
    }

    @Test
    void salesReportKeepsOrderValuePaymentsRefundsAndOutstandingDistinct() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 7, 14);
        when(timeProvider.localDateTime()).thenReturn(NOW);
        when(salesUsage.findForReport(from, to, OrderStatus.FULFILLED, ReportingService.MAX_REPORT_ROWS)).thenReturn(List.of(
                order("ORD-1", "Cliente Uno", "GPU-1", "Scheda grafica", 2, "200.00", "150.00", "20.00", "130.00", "50.00"),
                order("ORD-2", "Cliente Due", "GPU-1", "Scheda grafica", 1, "100.00", "100.00", "0.00", "100.00", "0.00")
        ));

        SalesReportResponse report = service.sales(from, to, OrderStatus.FULFILLED);

        assertThat(report.orderCount()).isEqualTo(2);
        assertThat(report.orderValue()).isEqualByComparingTo("300.00");
        assertThat(report.paidAmount()).isEqualByComparingTo("250.00");
        assertThat(report.refundedAmount()).isEqualByComparingTo("20.00");
        assertThat(report.netCollectedAmount()).isEqualByComparingTo("230.00");
        assertThat(report.outstandingAmount()).isEqualByComparingTo("50.00");
        assertThat(report.averageOrderValue()).isEqualByComparingTo("150.00");
        assertThat(report.topProducts()).singleElement().satisfies(product -> {
            assertThat(product.productCode()).isEqualTo("GPU-1");
            assertThat(product.quantity()).isEqualTo(3);
            assertThat(product.orderValue()).isEqualByComparingTo("300.00");
        });
    }

    @Test
    void salesReportDefaultsToCurrentYearAndForwardsTheRowLimit() {
        when(timeProvider.localDateTime()).thenReturn(NOW);
        when(salesUsage.findForReport(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 14), null, ReportingService.MAX_REPORT_ROWS)).thenReturn(List.of());

        SalesReportResponse report = service.sales(null, null, null);

        assertThat(report.from()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(report.to()).isEqualTo(LocalDate.of(2026, 7, 14));
        assertThat(report.status()).isEqualTo("ALL");
        assertThat(report.averageOrderValue()).isEqualByComparingTo("0.00");
        verify(salesUsage).findForReport(report.from(), report.to(), null, ReportingService.MAX_REPORT_ROWS);
    }

    @Test
    void salesReportRejectsInvalidOrExcessivePeriods() {
        when(timeProvider.localDateTime()).thenReturn(NOW);

        assertThatThrownBy(() -> service.sales(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data iniziale");
        assertThatThrownBy(() -> service.sales(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 2), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cinque anni");
    }

    @Test
    void inventoryReportCalculatesPhysicalReservedAvailableAndDiscountedValue() {
        when(timeProvider.localDateTime()).thenReturn(NOW);
        when(inventoryUsage.findForReport("gpu", ProductCategory.HARDWARE, "ALL", false, ReportingService.MAX_REPORT_ROWS)).thenReturn(List.of(
                product("GPU-1", 5, 2, "100.00", "10.00", false),
                product("GPU-2", 2, 2, "50.00", "0.00", false)
        ));

        InventoryReportResponse report = service.inventory("gpu", ProductCategory.HARDWARE, "ALL", false);

        assertThat(report.productCount()).isEqualTo(2);
        assertThat(report.physicalUnits()).isEqualTo(7);
        assertThat(report.reservedUnits()).isEqualTo(4);
        assertThat(report.availableUnits()).isEqualTo(3);
        assertThat(report.inventoryValue()).isEqualByComparingTo("550.00");
        assertThat(report.lowStockCount()).isEqualTo(1);
        assertThat(report.outOfStockCount()).isEqualTo(1);
        assertThat(report.products()).extracting(InventoryReportResponse.InventoryProductRow::stockStatus)
                .containsExactly("LOW", "OUT");
    }

    private static SalesOrderReportSource order(
            String code,
            String customer,
            String productCode,
            String productName,
            int quantity,
            String total,
            String paid,
            String refunded,
            String net,
            String outstanding
    ) {
        BigDecimal orderTotal = new BigDecimal(total);
        return new SalesOrderReportSource(
                code,
                customer,
                NOW.minusDays(1),
                OrderStatus.FULFILLED,
                orderTotal,
                new BigDecimal(paid),
                new BigDecimal(refunded),
                new BigDecimal(net),
                new BigDecimal(outstanding),
                List.of(new SalesOrderItemReportSource(productCode, productName, quantity, orderTotal.divide(BigDecimal.valueOf(quantity)), orderTotal))
        );
    }

    private static InventoryProductReportSource product(String code, int quantity, int reserved, String price, String discount, boolean discontinued) {
        BigDecimal grossPrice = new BigDecimal(price);
        BigDecimal discountValue = new BigDecimal(discount);
        BigDecimal discounted = grossPrice.multiply(BigDecimal.ONE.subtract(discountValue.divide(BigDecimal.valueOf(100))));
        return new InventoryProductReportSource(
                code,
                "Prodotto " + code,
                ProductCategory.HARDWARE,
                "Brand",
                "Scheda grafica",
                quantity,
                reserved,
                quantity - reserved,
                grossPrice,
                discountValue,
                discounted,
                discontinued
        );
    }
}
