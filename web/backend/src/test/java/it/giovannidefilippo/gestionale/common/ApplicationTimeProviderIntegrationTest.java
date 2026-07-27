package it.giovannidefilippo.gestionale.common;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.document.FiscalDocumentRequests;
import it.giovannidefilippo.gestionale.document.FiscalDocumentResponse;
import it.giovannidefilippo.gestionale.document.FiscalDocumentService;
import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.inventory.StockMovementRequest;
import it.giovannidefilippo.gestionale.inventory.StockMovementResponse;
import it.giovannidefilippo.gestionale.inventory.StockMovementType;
import it.giovannidefilippo.gestionale.order.OrderRequests;
import it.giovannidefilippo.gestionale.order.OrderResponse;
import it.giovannidefilippo.gestionale.order.OrderService;
import it.giovannidefilippo.gestionale.order.PaymentMethod;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerRequest;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerResponse;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerService;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerType;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthSessionResponse;
import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserResponse;
import it.giovannidefilippo.gestionale.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ApplicationTimeProviderIntegrationTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-12T10:15:30Z");
    private static final LocalDateTime FIXED_LOCAL_DATE_TIME = LocalDateTime.of(2026, 7, 12, 10, 15, 30);

    @Autowired
    private TimeProvider timeProvider;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FiscalDocumentService fiscalDocumentService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private BusinessPartnerService partnerService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    @Qualifier("applicationClock")
    private Clock applicationClock;

    @Test
    void backendUsesUtcApplicationClockForOperationalTimestamps() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String productCode = "TIME-" + suffix;
        AuthenticatedUser actor = new AuthenticatedUser("superadmin", UserRole.SUPER_ADMIN);

        productService.create(new ProductRequest(
                productCode,
                "Prodotto tempo",
                "Prodotto usato per verificare il clock applicativo.",
                ProductCategory.HARDWARE,
                "ClockBrand",
                "Scheda di test",
                "",
                5,
                new BigDecimal("100.00"),
                new BigDecimal("0.00")
        ));

        StockMovementResponse movement = inventoryService.register(
                new StockMovementRequest(productCode, StockMovementType.LOAD, 2, "Verifica timestamp UTC"),
                actor
        );
        BusinessPartnerResponse partner = partnerService.create(new BusinessPartnerRequest(
                "CLI-" + suffix,
                BusinessPartnerType.CUSTOMER,
                "Cliente tempo",
                "",
                "",
                "",
                "",
                "",
                "Napoli",
                ""
        ), actor.username(), actor.roleLabel());
        OrderResponse draft = orderService.create(new OrderRequests.CreateOrderRequest(
                partner.code(),
                PaymentMethod.CARD,
                List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
        ), "cliente_tempo", actor);
        orderService.confirm(draft.code(), actor);
        OrderResponse fulfilled = orderService.fulfill(draft.code(), actor);
        FiscalDocumentResponse invoice = fiscalDocumentService.createInvoice(
                new FiscalDocumentRequests.CreateInvoiceRequest(fulfilled.code()),
                actor
        );
        String auditTarget = "TIMEZONE_MARK_" + suffix;
        auditService.record(actor.username(), actor.roleLabel(), "TIMEZONE_TEST", auditTarget, "Verifica clock UTC", AuditCategory.SYSTEM, AuditSeverity.INFO, "SYSTEM");
        AuthSessionResponse session = authSessionService.create(new UserResponse(1L, "cliente_tempo", UserRole.CUSTOMER, UserRole.CUSTOMER.getLabel(), UserRole.CUSTOMER.getPermissions()));

        assertThat(timeProvider.instant()).isEqualTo(FIXED_INSTANT);
        assertThat(timeProvider.localDateTime()).isEqualTo(FIXED_LOCAL_DATE_TIME);
        assertThat(applicationClock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(movement.timestamp()).isEqualTo(FIXED_LOCAL_DATE_TIME);
        assertThat(partner.createdAt()).isEqualTo(FIXED_LOCAL_DATE_TIME);
        assertThat(partner.updatedAt()).isEqualTo(FIXED_LOCAL_DATE_TIME);
        assertThat(draft.timestamp()).isEqualTo(FIXED_LOCAL_DATE_TIME);
        assertThat(draft.statusChangedAt()).isEqualTo(FIXED_LOCAL_DATE_TIME);
        assertThat(fulfilled.statusChangedAt()).isEqualTo(FIXED_LOCAL_DATE_TIME);
        assertThat(invoice.createdAt()).isEqualTo(FIXED_LOCAL_DATE_TIME);
        assertThat(session.expiresAt()).isEqualTo(FIXED_INSTANT.plus(Duration.ofMinutes(45)));
        assertThat(auditService.search(auditTarget, null, null, 0, 5).content())
                .anySatisfy(event -> assertThat(event.timestamp()).isEqualTo(FIXED_LOCAL_DATE_TIME));
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        }
    }
}
