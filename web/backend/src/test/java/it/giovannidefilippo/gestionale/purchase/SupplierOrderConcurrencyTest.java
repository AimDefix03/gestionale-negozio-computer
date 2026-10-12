package it.giovannidefilippo.gestionale.purchase;

import it.giovannidefilippo.gestionale.common.PostgreSqlIntegrationTestSupport;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerRequest;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerResponse;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerService;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerType;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductResponse;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("postgresql")
class SupplierOrderConcurrencyTest extends PostgreSqlIntegrationTestSupport {
    private static final AuthenticatedUser BUYER = new AuthenticatedUser(9100L, "concurrent_buyer", UserRole.EMPLOYEE);

    @Autowired SupplierOrderService service;
    @Autowired BusinessPartnerService partnerService;
    @Autowired ProductService productService;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void concurrentReceiptOfLastUnitIsAppliedOnce() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        BusinessPartnerResponse supplier = partnerService.create(new BusinessPartnerRequest("FOR-" + suffix, BusinessPartnerType.SUPPLIER, "Fornitore concorrenza", "", "IT12345678901", "supplier@example.com", "", "Via Test", "Napoli", ""), BUYER.username(), BUYER.roleLabel());
        ProductResponse product = productService.create(new ProductRequest("PURC-" + suffix, "Prodotto concorrente", "Test lock ricezione ordine fornitore.", ProductCategory.HARDWARE, "Test", "Componente", "", new BigDecimal("10.00"), BigDecimal.ZERO), BUYER);
        SupplierOrderResponse order = service.create(new SupplierOrderRequests.CreateRequest(supplier.id(), LocalDate.now().plusDays(2), "", List.of(new SupplierOrderRequests.CreateItemRequest(product.code(), 1, new BigDecimal("8.00"), null))), BUYER);
        order = service.send(order.code(), BUYER);
        String orderCode = order.code();
        long lineId = order.items().get(0).id();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> receive(orderCode, lineId, "Sessione A", ready, start));
            Future<Boolean> second = executor.submit(() -> receive(orderCode, lineId, "Sessione B", ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
            SupplierOrderResponse result = service.findByCode(orderCode, BUYER);
            assertThat(result.status()).isEqualTo(SupplierOrderStatus.RECEIVED);
            assertThat(result.items().get(0).receivedQuantity()).isEqualTo(1);
            assertThat(result.receipts()).hasSize(1);
            ProductResponse stocked = productService.findByCode(product.code());
            assertThat(stocked.quantity()).isEqualTo(1);
            assertThat(stocked.costedQuantity()).isEqualTo(1);
            assertThat(stocked.averagePurchaseCost()).isEqualByComparingTo("8.0000");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentReceiptsFromDifferentOrdersKeepOneBaselineAndWeightedCost() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        BusinessPartnerResponse supplier = partnerService.create(new BusinessPartnerRequest("FOR-M-" + suffix, BusinessPartnerType.SUPPLIER, "Fornitore costo concorrente", "", "IT12345678902", "supplier-cost@example.com", "", "Via Test", "Napoli", ""), BUYER.username(), BUYER.roleLabel());
        ProductResponse product = productService.create(new ProductRequest("PURM-" + suffix, "Prodotto costo concorrente", "Test media ponderata concorrente.", ProductCategory.HARDWARE, "Test", "Componente", "", new BigDecimal("20.00"), BigDecimal.ZERO), BUYER);
        SupplierOrderResponse firstOrder = sentOrder(supplier.id(), product.code(), new BigDecimal("8.00"));
        SupplierOrderResponse secondOrder = sentOrder(supplier.id(), product.code(), new BigDecimal("12.00"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> receive(firstOrder.code(), firstOrder.items().get(0).id(), "Ricezione ordine A", new BigDecimal("8.00"), ready, start));
            Future<Boolean> second = executor.submit(() -> receive(secondOrder.code(), secondOrder.items().get(0).id(), "Ricezione ordine B", new BigDecimal("12.00"), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(10, TimeUnit.SECONDS)).isTrue();

            ProductResponse stocked = productService.findByCode(product.code());
            assertThat(stocked.quantity()).isEqualTo(2);
            assertThat(stocked.costedQuantity()).isEqualTo(2);
            assertThat(stocked.averagePurchaseCost()).isEqualByComparingTo("10.0000");
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from stock_movements where product_id = ? and type = 'PURCHASE_RECEIPT'",
                    Integer.class,
                    product.id()
            )).isEqualTo(2);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from stock_movements where product_id = ? and baseline_marker = true",
                    Integer.class,
                    product.id()
            )).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private SupplierOrderResponse sentOrder(long supplierId, String productCode, BigDecimal unitPrice) {
        SupplierOrderResponse order = service.create(new SupplierOrderRequests.CreateRequest(
                supplierId,
                LocalDate.now().plusDays(2),
                "",
                List.of(new SupplierOrderRequests.CreateItemRequest(productCode, 1, unitPrice, null))
        ), BUYER);
        return service.send(order.code(), BUYER);
    }

    private boolean receive(String code, long lineId, String reason, CountDownLatch ready, CountDownLatch start) {
        return receive(code, lineId, reason, null, ready, start);
    }

    private boolean receive(String code, long lineId, String reason, BigDecimal unitCost, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            service.receive(code, new SupplierOrderRequests.ReceiveRequest(reason, List.of(new SupplierOrderRequests.ReceiveItemRequest(lineId, 1, unitCost))), BUYER);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
