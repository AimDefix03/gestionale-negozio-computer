package it.giovannidefilippo.gestionale.common;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.document.FiscalDocumentRequests;
import it.giovannidefilippo.gestionale.document.FiscalDocumentResponse;
import it.giovannidefilippo.gestionale.document.FiscalDocumentService;
import it.giovannidefilippo.gestionale.company.CompanySettingsRequests;
import it.giovannidefilippo.gestionale.company.CompanySettingsResponse;
import it.giovannidefilippo.gestionale.company.CompanySettingsService;
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
import it.giovannidefilippo.gestionale.user.UserService;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ApplicationTimeProviderIntegrationTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-12-31T23:30:00Z");
    private static final LocalDateTime FIXED_LOCAL_DATE_TIME = LocalDateTime.of(2026, 12, 31, 23, 30);

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
    private UserService userService;

    @Autowired
    private CompanySettingsService companySettingsService;

    @Autowired
    @Qualifier("applicationClock")
    private Clock applicationClock;

    @Test
    void backendUsesUtcApplicationClockForOperationalTimestamps() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String productCode = "TIME-" + suffix;
        AuthenticatedUser actor = new AuthenticatedUser("superadmin", UserRole.SUPER_ADMIN);
        CompanySettingsResponse company = TestCompanySettings.configure(companySettingsService, actor);

        productService.create(new ProductRequest(
                productCode,
                "Prodotto tempo",
                "Prodotto usato per verificare il clock applicativo.",
                ProductCategory.HARDWARE,
                "ClockBrand",
                "Scheda di test",
                "",
                new BigDecimal("100.00"),
                new BigDecimal("0.00")
        ));
        inventoryService.initialBalance(productCode, 0, "Saldo iniziale test clock", actor.username(), actor.roleLabel());

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
        String sessionUsername = "cliente_tempo_" + suffix;
        UserResponse sessionUser = userService.registerPublic(sessionUsername, "Client123!");
        AuthSessionResponse session = authSessionService.create(
                userService.login(sessionUser.username(), "Client123!")
        );

        assertThat(timeProvider.instant()).isEqualTo(FIXED_INSTANT);
        assertThat(timeProvider.localDateTime()).isEqualTo(FIXED_LOCAL_DATE_TIME);
        assertThat(applicationClock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(movement.timestamp()).isEqualTo(OffsetDateTime.parse("2026-12-31T23:30:00Z"));
        assertThat(partner.createdAt()).isEqualTo(OffsetDateTime.parse("2026-12-31T23:30:00Z"));
        assertThat(partner.updatedAt()).isEqualTo(OffsetDateTime.parse("2026-12-31T23:30:00Z"));
        assertThat(draft.timestamp()).isEqualTo(OffsetDateTime.parse("2026-12-31T23:30:00Z"));
        assertThat(draft.statusChangedAt()).isEqualTo(OffsetDateTime.parse("2026-12-31T23:30:00Z"));
        assertThat(fulfilled.statusChangedAt()).isEqualTo(OffsetDateTime.parse("2026-12-31T23:30:00Z"));
        assertThat(invoice.createdAt()).isEqualTo(OffsetDateTime.parse("2027-01-01T00:30:00+01:00"));
        assertThat(invoice.fiscalYear()).isEqualTo(2027);
        assertThat(invoice.code()).startsWith("FS-2027-");
        assertThat(invoice.companySnapshotTimeZone()).isEqualTo("Europe/Rome");
        assertThat(invoice.disclaimer()).isEqualTo("DOCUMENTO SIMULATO - NON VALIDO AI FINI FISCALI");
        assertThat(session.expiresAt()).isEqualTo(FIXED_INSTANT.plus(Duration.ofMinutes(45)));
        assertThat(auditService.search(auditTarget, null, null, 0, 5).content())
                .anySatisfy(event -> assertThat(event.timestamp()).isEqualTo(OffsetDateTime.parse("2026-12-31T23:30:00Z")));
        assertThatThrownBy(() -> companySettingsService.update(new CompanySettingsRequests.UpdateRequest(
                company.version(), company.legalName(), company.taxCode(), company.vatNumber(), company.email(), company.phone(),
                company.address(), company.postalCode(), company.city(), company.province(), company.countryCode(), "UTC",
                company.defaultVatRate(), company.invoicePrefix(), company.creditNotePrefix(), company.numberPadding()
        ), actor))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("fuso orario");
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
