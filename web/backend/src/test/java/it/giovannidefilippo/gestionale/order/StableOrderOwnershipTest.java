package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerRequest;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerResponse;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerService;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerType;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserResponse;
import it.giovannidefilippo.gestionale.user.UserRole;
import it.giovannidefilippo.gestionale.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class StableOrderOwnershipTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private BusinessPartnerService partnerService;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void usernameRenameDoesNotChangeOwnership() {
        UserResponse account = customer("rename-owner");
        AuthenticatedUser original = actor(account);
        OrderResponse order = createSelfServiceOrder(original, createProduct(3));

        String renamed = unique("renamed");
        jdbc.update(
                "update user_accounts set username = ?, username_canonical = ? where id = ?",
                renamed,
                renamed.toLowerCase(Locale.ROOT),
                account.id()
        );
        AuthenticatedUser afterRename = new AuthenticatedUser(account.id(), renamed, UserRole.CUSTOMER);

        assertThat(orderService.searchForAccount(null, account.id(), 0, 25).content())
                .extracting(OrderResponse::code)
                .contains(order.code());
        assertThat(orderService.findByCode(order.code(), afterRename).customerAccountId()).isEqualTo(account.id());
        assertThat(orderService.cancel(order.code(), cancellation(), afterRename).status()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void duplicatedDisplayNamesCannotCrossAccountOwnership() {
        UserResponse first = customer("first-owner");
        UserResponse second = customer("second-owner");
        BusinessPartnerResponse firstPartner = partner("CLI-A", "Cliente omonimo");
        BusinessPartnerResponse secondPartner = partner("CLI-B", "Cliente omonimo");
        partnerService.linkCustomerAccount(firstPartner.code(), first.id(), "admin", "Super admin");
        partnerService.linkCustomerAccount(secondPartner.code(), second.id(), "admin", "Super admin");
        String productCode = createProduct(5);

        OrderResponse firstOrder = createStaffOrder(firstPartner.code(), productCode);
        OrderResponse secondOrder = createStaffOrder(secondPartner.code(), productCode);

        assertThat(orderService.searchForAccount(null, first.id(), 0, 25).content())
                .extracting(OrderResponse::code)
                .containsExactly(firstOrder.code());
        assertThat(orderService.searchForAccount(null, second.id(), 0, 25).content())
                .extracting(OrderResponse::code)
                .containsExactly(secondOrder.code());
        assertThatThrownBy(() -> orderService.findByCode(secondOrder.code(), actor(first)))
                .isInstanceOf(it.giovannidefilippo.gestionale.common.ForbiddenException.class);
    }

    @Test
    void partnerDisplayNameRenameKeepsOwnerAndOrderSnapshot() {
        UserResponse account = customer("partner-rename");
        BusinessPartnerResponse partner = partner("CLI-RENAME", "Nome originale");
        partnerService.linkCustomerAccount(partner.code(), account.id(), "admin", "Super admin");
        OrderResponse order = createSelfServiceOrder(actor(account), createProduct(2));

        partnerService.update(partner.code(), partnerRequest(partner.code(), "Nome aggiornato"), "admin", "Super admin");

        OrderResponse historical = orderService.findByCode(order.code(), actor(account));
        assertThat(historical.customer()).isEqualTo("Nome originale");
        assertThat(historical.customerAccountId()).isEqualTo(account.id());
        assertThat(historical.partnerId()).isEqualTo(partner.id());
    }

    @Test
    void unresolvedTextualOrderIsInvisibleAndCannotBeClaimedByMatchingUsername() {
        UserResponse account = customer("unresolved-name");
        String productCode = createProduct(2);
        OrderResponse unresolved = orderService.create(
                request(account.username(), null, productCode),
                account.username(),
                admin()
        );

        assertThat(unresolved.ownershipStatus()).isEqualTo(OrderOwnershipStatus.UNRESOLVED);
        assertThat(orderService.searchForAccount(null, account.id(), 0, 25).content()).isEmpty();
        assertThatThrownBy(() -> orderService.confirm(unresolved.code(), actor(account)))
                .isInstanceOf(it.giovannidefilippo.gestionale.common.ForbiddenException.class);
    }

    @Test
    void customerTransitionsRequireStableOwnerId() {
        UserResponse owner = customer("transition-owner");
        UserResponse stranger = customer("transition-stranger");
        String productCode = createProduct(8);
        OrderResponse cancellable = createSelfServiceOrder(actor(owner), productCode);

        assertThatThrownBy(() -> orderService.confirm(cancellable.code(), actor(stranger)))
                .isInstanceOf(it.giovannidefilippo.gestionale.common.ForbiddenException.class);
        assertThat(orderService.confirm(cancellable.code(), actor(owner)).status()).isEqualTo(OrderStatus.CONFIRMED);

        OrderResponse returnable = createSelfServiceOrder(actor(owner), productCode);
        orderService.confirm(returnable.code(), admin());
        orderService.fulfill(returnable.code(), admin());

        OrderResponse returned = orderService.requestReturn(
                returnable.code(),
                new OrderOperationRequests.ReturnRequest(
                        "Prodotto non adatto",
                        List.of(new OrderOperationRequests.ReturnItemRequest(productCode, 1))
                ),
                actor(owner)
        );
        assertThat(returned.returns()).hasSize(1);
        assertThatThrownBy(() -> orderService.requestReturn(
                returnable.code(),
                new OrderOperationRequests.ReturnRequest(
                        "Tentativo estraneo",
                        List.of(new OrderOperationRequests.ReturnItemRequest(productCode, 1))
                ),
                actor(stranger)
        )).isInstanceOf(it.giovannidefilippo.gestionale.common.ForbiddenException.class);
    }

    @Test
    void onlyCustomerAccountsCanBeLinkedAndEachAccountHasOnePartner() {
        UserResponse first = customer("linked-once");
        UserResponse second = customer("linked-other");
        Long adminAccountId = jdbc.queryForObject(
                "select id from user_accounts where username = 'test_super_admin'",
                Long.class
        );
        BusinessPartnerResponse firstPartner = partner("CLI-LINK-A", "Primo cliente");
        BusinessPartnerResponse secondPartner = partner("CLI-LINK-B", "Secondo cliente");

        assertThat(partnerService.linkCustomerAccount(firstPartner.code(), first.id(), "admin", "Super admin").linkedAccountId())
                .isEqualTo(first.id());
        assertThatThrownBy(() -> partnerService.linkCustomerAccount(secondPartner.code(), first.id(), "admin", "Super admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gia collegato");
        assertThat(partnerService.linkCustomerAccount(secondPartner.code(), second.id(), "admin", "Super admin").linkedAccountId())
                .isEqualTo(second.id());
        assertThatThrownBy(() -> partnerService.linkCustomerAccount(firstPartner.code(), adminAccountId, "admin", "Super admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cliente");
        assertThatThrownBy(() -> partnerService.update(
                firstPartner.code(),
                new BusinessPartnerRequest(
                        firstPartner.code(),
                        BusinessPartnerType.SUPPLIER,
                        firstPartner.displayName(),
                        "", "", "", "", "", "", ""
                ),
                "admin",
                "Super admin"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Scollega");
    }

    private UserResponse customer(String prefix) {
        String username = unique(prefix);
        return userService.registerPublic(username, "StrongPassword123!");
    }

    private BusinessPartnerResponse partner(String prefix, String displayName) {
        String code = unique(prefix).toUpperCase();
        return partnerService.create(partnerRequest(code, displayName), "admin", "Super admin");
    }

    private BusinessPartnerRequest partnerRequest(String code, String displayName) {
        return new BusinessPartnerRequest(code, BusinessPartnerType.CUSTOMER, displayName, "", "", "", "", "", "", "");
    }

    private OrderResponse createSelfServiceOrder(AuthenticatedUser owner, String productCode) {
        return orderService.create(request("ignored", null, productCode), owner.username(), owner);
    }

    private OrderResponse createStaffOrder(String customerCode, String productCode) {
        return orderService.create(request("ignored", customerCode, productCode), "ignored", admin());
    }

    private OrderRequests.CreateOrderRequest request(String customer, String customerCode, String productCode) {
        return new OrderRequests.CreateOrderRequest(
                customer,
                customerCode,
                PaymentMethod.CARD,
                List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
        );
    }

    private String createProduct(int quantity) {
        String code = unique("OWN").toUpperCase();
        productService.create(new ProductRequest(
                code,
                "Prodotto ownership",
                "Prodotto sintetico per i test di ownership stabile.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Componente",
                "",
                new BigDecimal("100.00"),
                BigDecimal.ZERO
        ));
        inventoryService.initialBalance(code, quantity, "Saldo iniziale ownership", "test", "Test");
        return code;
    }

    private static AuthenticatedUser actor(UserResponse account) {
        return new AuthenticatedUser(account.id(), account.username(), UserRole.CUSTOMER);
    }

    private static AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, "admin", UserRole.SUPER_ADMIN);
    }

    private static OrderOperationRequests.CancellationRequest cancellation() {
        return new OrderOperationRequests.CancellationRequest("", "Annullamento richiesto dal test");
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
