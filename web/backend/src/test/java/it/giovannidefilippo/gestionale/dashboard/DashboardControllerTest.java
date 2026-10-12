package it.giovannidefilippo.gestionale.dashboard;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {
    private static final String SUPER_ADMIN_PASSWORD = "Test-Bootstrap-9842!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void dashboardRequiresSession() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void superAdminReceivesAggregatedDashboardWithRecentMovements() throws Exception {
        String token = login("test_super_admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        String productCode = uniqueCode("DASH-ADM");
        createProduct(token, productCode, 2);
        createMovement(token, productCode);

        mockMvc.perform(get("/api/dashboard").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.potentialRetailStockValue").isNumber())
                .andExpect(jsonPath("$.knownInventoryCostValue").isNumber())
                .andExpect(jsonPath("$.potentialGrossMarginOnCostedStock").isNumber())
                .andExpect(jsonPath("$.costedUnits").isNumber())
                .andExpect(jsonPath("$.uncostedUnits").isNumber())
                .andExpect(jsonPath("$.costCoveragePercentage").isNumber())
                .andExpect(jsonPath("$.lowStock").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.outOfStock").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.orders.totalOrders").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.orders.draftOrderValue").isNumber())
                .andExpect(jsonPath("$.orders.confirmedOrderValue").isNumber())
                .andExpect(jsonPath("$.orders.fulfilledOrderValue").isNumber())
                .andExpect(jsonPath("$.orders.grossCollected").isNumber())
                .andExpect(jsonPath("$.orders.refunded").isNumber())
                .andExpect(jsonPath("$.orders.netCollected").isNumber())
                .andExpect(jsonPath("$.recentOrders").isArray())
                .andExpect(jsonPath("$.recentMovements").isArray())
                .andExpect(jsonPath("$.recentMovements[0].productCode").value(productCode));
    }

    @Test
    void customerDashboardContainsOnlyOwnOrdersAndNoInventoryMovements() throws Exception {
        String adminToken = login("test_super_admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        String productCode = uniqueCode("DASH-CUST");
        String customer = "dash_customer_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String otherCustomer = "dash_other_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        createProduct(adminToken, productCode, 5);
        register(customer, "CustomerStrong123!");
        register(otherCustomer, "CustomerStrong123!");
        String customerToken = login(customer, "CustomerStrong123!", "CUSTOMER");
        String otherCustomerToken = login(otherCustomer, "CustomerStrong123!", "CUSTOMER");
        createOrder(customerToken, productCode);
        createOrder(otherCustomerToken, productCode);

        mockMvc.perform(get("/api/dashboard").header("X-Session-Token", customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/customer/dashboard").header("X-Session-Token", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(1))
                .andExpect(jsonPath("$.draftOrders").value(1))
                .andExpect(jsonPath("$.confirmedOrders").value(0))
                .andExpect(jsonPath("$.fulfilledOrders").value(0))
                .andExpect(jsonPath("$.canceledOrders").value(0))
                .andExpect(jsonPath("$.recentOrders", hasSize(1)))
                .andExpect(jsonPath("$.recentOrders[0].code").isString())
                .andExpect(jsonPath("$.recentOrders[0].total").value(120.00))
                .andExpect(jsonPath("$.recentOrders[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.recentOrders[0].customer").doesNotExist())
                .andExpect(jsonPath("$.products").doesNotExist())
                .andExpect(jsonPath("$.potentialRetailStockValue").doesNotExist())
                .andExpect(jsonPath("$.knownInventoryCostValue").doesNotExist())
                .andExpect(jsonPath("$.potentialGrossMarginOnCostedStock").doesNotExist())
                .andExpect(jsonPath("$.lowStock").doesNotExist())
                .andExpect(jsonPath("$.outOfStock").doesNotExist())
                .andExpect(jsonPath("$.recentMovements").doesNotExist());
    }

    @Test
    void staffCannotUseCustomerDashboardProjection() throws Exception {
        String adminToken = login("test_super_admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");

        mockMvc.perform(get("/api/customer/dashboard").header("X-Session-Token", adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    private String login(String username, String password, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "role": "%s"
                                }
                                """.formatted(username, password, role)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isCreated());
    }

    private void createProduct(String token, String code, int quantity) throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "Prodotto dashboard",
                                  "description": "Prodotto creato per verificare la dashboard aggregata.",
                                  "category": "HARDWARE",
                                  "brand": "DashboardBrand",
                                  "productType": "Scheda di test",
                                  "usageContext": "",
                                  "price": 120.00,
                                  "discount": 0.00
                                }
                                """.formatted(code)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventory/initial-balance")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "%s",
                                  "quantity": %d,
                                  "reason": "Saldo iniziale test dashboard"
                                }
                                """.formatted(code, quantity)))
                .andExpect(status().isCreated());
    }

    private void createMovement(String token, String productCode) throws Exception {
        mockMvc.perform(post("/api/inventory/movements")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "%s",
                                  "type": "LOAD",
                                  "quantity": 1,
                                  "reason": "Verifica dashboard"
                                }
                                """.formatted(productCode)))
                .andExpect(status().isCreated());
    }

    private void createOrder(String token, String productCode) throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer": "ignored",
                                  "paymentMethod": "Carta",
                                  "items": [
                                    { "productCode": "%s", "quantity": 1 }
                                  ]
                                }
                                """.formatted(productCode)))
                .andExpect(status().isCreated());
    }

    private static String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
