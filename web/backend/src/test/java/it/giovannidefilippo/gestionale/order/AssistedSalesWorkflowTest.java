package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.common.ForbiddenException;
import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerRequest;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerResponse;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerService;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerType;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AssistedSalesWorkflowTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private BusinessPartnerService partnerService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void employeeCreatesRegisteredCustomerDraftUsingStablePartnerIdAndFrozenLineSnapshots() {
        String productCode = createProduct("Descrizione originale", "149.90", 4);
        BusinessPartnerResponse partner = createCustomer();

        OrderResponse created = orderService.create(registeredRequest(partner.id(), productCode, 2), null, employee());

        assertThat(created.customer()).isEqualTo(partner.displayName());
        assertThat(created.customerCode()).isEqualTo(partner.code());
        assertThat(created.partnerId()).isEqualTo(partner.id());
        assertThat(created.customerType()).isEqualTo(OrderCustomerType.REGISTERED);
        assertThat(created.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(created.items()).singleElement().satisfies(item -> {
            assertThat(item.productDescription()).isEqualTo("Descrizione originale");
            assertThat(item.unitPrice()).isEqualByComparingTo("149.90");
            assertThat(item.quantity()).isEqualTo(2);
        });

        productService.update(productCode, productRequest(productCode, "Descrizione aggiornata", "199.90"));
        OrderResponse historical = orderService.findByCode(created.code(), employee());

        assertThat(historical.items()).singleElement().satisfies(item -> {
            assertThat(item.productDescription()).isEqualTo("Descrizione originale");
            assertThat(item.unitPrice()).isEqualByComparingTo("149.90");
        });
        assertThat(jdbc.queryForObject(
                "select count(*) from audit_events where actor = ? and action = 'CREATE_ASSISTED_ORDER' and target = ?",
                Long.class,
                employee().username(),
                created.code()
        )).isEqualTo(1L);
    }

    @Test
    void employeeCreatesExplicitWalkInDraftWithoutFakePartner() {
        String productCode = createProduct("Prodotto occasionale", "49.90", 2);
        OrderRequests.CreateOrderRequest request = new OrderRequests.CreateOrderRequest(
                null,
                null,
                OrderCustomerType.WALK_IN,
                null,
                "Cliente occasionale prova",
                PaymentMethod.CASH,
                List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
        );

        OrderResponse created = orderService.create(request, null, employee());

        assertThat(created.customer()).isEqualTo("Cliente occasionale prova");
        assertThat(created.customerCode()).isNullOrEmpty();
        assertThat(created.partnerId()).isNull();
        assertThat(created.customerAccountId()).isNull();
        assertThat(created.customerType()).isEqualTo(OrderCustomerType.WALK_IN);
    }

    @Test
    void customerCannotSelectAnotherPartnerOrWalkInIdentity() {
        String productCode = createProduct("Prodotto self service", "79.90", 2);
        BusinessPartnerResponse partner = createCustomer();

        assertThatThrownBy(() -> orderService.create(registeredRequest(partner.id(), productCode, 1), null, customer()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("proprio account");
    }

    @Test
    void assistedDraftRejectsMissingCustomerAndInsufficientStock() {
        String productCode = createProduct("Prodotto limitato", "99.90", 1);
        OrderRequests.CreateOrderRequest missingCustomer = new OrderRequests.CreateOrderRequest(
                null, null, null, null, null, PaymentMethod.CARD,
                List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
        );
        OrderRequests.CreateOrderRequest excessiveQuantity = new OrderRequests.CreateOrderRequest(
                null, null, OrderCustomerType.WALK_IN, null, "Cliente stock", PaymentMethod.CARD,
                List.of(new OrderRequests.CreateOrderItemRequest(productCode, 2))
        );

        assertThatThrownBy(() -> orderService.create(missingCustomer, null, employee()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Seleziona un cliente");
        assertThatThrownBy(() -> orderService.create(excessiveQuantity, null, employee()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Scorte insufficienti");
    }

    private OrderRequests.CreateOrderRequest registeredRequest(long partnerId, String productCode, int quantity) {
        return new OrderRequests.CreateOrderRequest(
                null,
                null,
                OrderCustomerType.REGISTERED,
                partnerId,
                null,
                PaymentMethod.CARD,
                List.of(new OrderRequests.CreateOrderItemRequest(productCode, quantity))
        );
    }

    private BusinessPartnerResponse createCustomer() {
        String code = unique("CLI").toUpperCase();
        return partnerService.create(new BusinessPartnerRequest(
                code,
                BusinessPartnerType.CUSTOMER,
                "Cliente censito " + code,
                "",
                "",
                "cliente@unit.test",
                "",
                "",
                "Napoli",
                ""
        ), employee().username(), employee().roleLabel());
    }

    private String createProduct(String description, String price, int quantity) {
        String code = unique("SALE").toUpperCase();
        productService.create(productRequest(code, description, price));
        inventoryService.initialBalance(code, quantity, "Saldo iniziale vendita assistita", "test", "Test");
        return code;
    }

    private ProductRequest productRequest(String code, String description, String price) {
        return new ProductRequest(
                code,
                "Prodotto vendita assistita",
                description,
                ProductCategory.HARDWARE,
                "TestBrand",
                "Componente",
                "",
                new BigDecimal(price),
                BigDecimal.ZERO
        );
    }

    private static AuthenticatedUser employee() {
        return new AuthenticatedUser(9001L, "operatore_vendite", UserRole.EMPLOYEE);
    }

    private static AuthenticatedUser customer() {
        return new AuthenticatedUser(9002L, "cliente_self_service", UserRole.CUSTOMER);
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
