package it.giovannidefilippo.gestionale.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
    private static final String SUPER_ADMIN_PASSWORD = "RootSecure123!";

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
        String token = login("admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        String productCode = uniqueCode("DASH-ADM");
        createProduct(token, productCode, 2);
        createMovement(token, productCode);

        mockMvc.perform(get("/api/dashboard").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.inventoryValue").isNumber())
                .andExpect(jsonPath("$.lowStock").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.outOfStock").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.orders").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.revenue").isNumber())
                .andExpect(jsonPath("$.recentOrders").isArray())
                .andExpect(jsonPath("$.recentMovements").isArray())
                .andExpect(jsonPath("$.recentMovements[0].productCode").value(productCode));
    }

    @Test
    void customerDashboardContainsOnlyOwnOrdersAndNoInventoryMovements() throws Exception {
        String adminToken = login("admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        String productCode = uniqueCode("DASH-CUST");
        String customer = "dash_customer_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        createProduct(adminToken, productCode, 5);
        register(customer, "CustomerStrong123!");
        String customerToken = login(customer, "CustomerStrong123!", "CUSTOMER");
        createOrder(customerToken, productCode);

        mockMvc.perform(get("/api/dashboard").header("X-Session-Token", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").value(1))
                .andExpect(jsonPath("$.revenue").value(120.00))
                .andExpect(jsonPath("$.recentOrders", hasSize(1)))
                .andExpect(jsonPath("$.recentOrders[0].customer").value(customer))
                .andExpect(jsonPath("$.recentMovements", hasSize(0)));
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
                                  "password": "%s",
                                  "role": "CUSTOMER"
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
                                  "quantity": %d,
                                  "price": 120.00,
                                  "discount": 0.00
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
