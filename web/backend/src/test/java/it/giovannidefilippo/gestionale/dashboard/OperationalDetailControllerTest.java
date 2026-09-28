package it.giovannidefilippo.gestionale.dashboard;

import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.inventory.StockMovementType;
import it.giovannidefilippo.gestionale.order.OrderRequests;
import it.giovannidefilippo.gestionale.order.OrderResponse;
import it.giovannidefilippo.gestionale.order.OrderService;
import it.giovannidefilippo.gestionale.order.PaymentMethod;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperationalDetailControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Test
    void productDetailUsesTargetedHistoryWhenRelevantRecordsAreOutsideGlobalFirstPage() throws Exception {
        String targetCode = uniqueCode("DETAIL-TARGET");
        String unrelatedCode = uniqueCode("DETAIL-OTHER");
        createProduct(targetCode);
        createProduct(unrelatedCode);
        inventoryService.initialBalance(targetCode, 10, "Saldo iniziale prodotto dettaglio", "test", "Test");
        inventoryService.register(targetCode, StockMovementType.LOAD, 1, "Carico storico prodotto dettaglio", "test", "Test");
        OrderResponse targetOrder = createOrder(targetCode, "cliente_dettaglio");

        inventoryService.initialBalance(unrelatedCode, 20, "Saldo iniziale prodotto estraneo", "test", "Test");
        for (int index = 0; index < 6; index++) {
            inventoryService.register(unrelatedCode, StockMovementType.LOAD, 1, "Carico estraneo " + index, "test", "Test");
            createOrder(unrelatedCode, "cliente_estraneo_" + index);
        }

        mockMvc.perform(get("/api/products/{code}/detail", targetCode)
                        .header("X-Session-Token", login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.code").value(targetCode))
                .andExpect(jsonPath("$.product.capabilities.canMoveStock").value(true))
                .andExpect(jsonPath("$.recentMovements.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.recentMovements[*].productCode", everyItem(is(targetCode))))
                .andExpect(jsonPath("$.recentOrders.length()").value(1))
                .andExpect(jsonPath("$.recentOrders[0].code").value(targetOrder.code()));
    }

    @Test
    void orderDetailPublishesTransitionAndDocumentCapabilities() throws Exception {
        String productCode = uniqueCode("ORDER-DETAIL");
        createProduct(productCode);
        inventoryService.initialBalance(productCode, 5, "Saldo iniziale dettaglio ordine", "test", "Test");
        OrderResponse order = createOrder(productCode, "cliente_ordine_dettaglio");

        mockMvc.perform(get("/api/orders/{code}/detail", order.code())
                        .header("X-Session-Token", login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.code").value(order.code()))
                .andExpect(jsonPath("$.order.capabilities.canConfirm").value(true))
                .andExpect(jsonPath("$.order.capabilities.canFulfill").value(false))
                .andExpect(jsonPath("$.documents.hasInvoice").value(false))
                .andExpect(jsonPath("$.documents.canCreateInvoice").value(false));
    }

    private void createProduct(String code) {
        productService.create(new ProductRequest(
                code,
                "Prodotto dettaglio operativo",
                "Prodotto creato per verificare lo storico operativo mirato.",
                ProductCategory.HARDWARE,
                "DetailBrand",
                "Componente test",
                "",
                new BigDecimal("120.00"),
                BigDecimal.ZERO
        ));
    }

    private OrderResponse createOrder(String productCode, String customer) {
        return orderService.create(
                new OrderRequests.CreateOrderRequest(
                        customer,
                        PaymentMethod.CARD,
                        List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
                ),
                customer,
                actor()
        );
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "test_super_admin",
                                  "password": "Test-Bootstrap-9842!",
                                  "role": "SUPER_ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }

    private static AuthenticatedUser actor() {
        return new AuthenticatedUser("test_super_admin", UserRole.SUPER_ADMIN);
    }

    private static String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
