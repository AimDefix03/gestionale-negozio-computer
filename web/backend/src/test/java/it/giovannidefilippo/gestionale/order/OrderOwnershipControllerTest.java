package it.giovannidefilippo.gestionale.order;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderOwnershipControllerTest {
    private static final String SUPER_ADMIN_PASSWORD = "Test-Bootstrap-9842!";
    private static final String CUSTOMER_PASSWORD = "CustomerStrong123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void disabledAccountPreservesOrderOwnershipAndUsernameCannotBeReused() throws Exception {
        String adminToken = login("test_super_admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        String username = unique("ownership-recreated");
        String productCode = unique("OWN-API").toUpperCase();
        register(username);
        String originalToken = login(username, CUSTOMER_PASSWORD, "CUSTOMER");
        createProduct(adminToken, productCode, 3);
        JsonNode order = createOrder(originalToken, productCode);
        long originalAccountId = order.path("customerAccountId").asLong();

        mockMvc.perform(delete("/api/accounts/{username}", username)
                        .header("X-Session-Token", adminToken)
                        .header("X-Reauth-Password", SUPER_ADMIN_PASSWORD))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, CUSTOMER_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));

        mockMvc.perform(get("/api/products")
                        .header("X-Session-Token", originalToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));

        JsonNode disabledAccount = accountByUsername(adminToken, username);
        assertThat(disabledAccount.path("id").asLong()).isEqualTo(originalAccountId);
        assertThat(disabledAccount.path("enabled").asBoolean()).isFalse();
    }

    @Test
    void legacyCustomerPathCannotBeUsedToReadAnotherCustomersOrders() throws Exception {
        String adminToken = login("test_super_admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        String owner = unique("ownership-owner");
        String stranger = unique("ownership-stranger");
        String productCode = unique("OWN-PATH").toUpperCase();
        register(owner);
        register(stranger);
        String ownerToken = login(owner, CUSTOMER_PASSWORD, "CUSTOMER");
        String strangerToken = login(stranger, CUSTOMER_PASSWORD, "CUSTOMER");
        createProduct(adminToken, productCode, 3);
        JsonNode order = createOrder(ownerToken, productCode);

        mockMvc.perform(get("/api/orders/customer/{customer}", owner)
                        .header("X-Session-Token", strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/orders/customer/{customer}", stranger)
                        .header("X-Session-Token", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value(order.path("code").asText()));
    }

    @Test
    void selfServiceCustomerCannotSelectAnotherCustomerIdentity() throws Exception {
        String adminToken = login("test_super_admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        String username = unique("ownership-spoof");
        String productCode = unique("OWN-SPOOF").toUpperCase();
        register(username);
        String customerToken = login(username, CUSTOMER_PASSWORD, "CUSTOMER");
        createProduct(adminToken, productCode, 2);

        mockMvc.perform(post("/api/orders")
                        .header("X-Session-Token", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerType": "WALK_IN",
                                  "walkInCustomerName": "Identita diversa",
                                  "paymentMethod": "CARD",
                                  "items": [
                                    { "productCode": "%s", "quantity": 1 }
                                  ]
                                }
                                """.formatted(productCode)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, CUSTOMER_PASSWORD)))
                .andExpect(status().isCreated());
    }

    private String login(String username, String password, String role) throws Exception {
        String response = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "role": "%s"
                                }
                                """.formatted(username, password, role)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }

    private void createProduct(String token, String code, int quantity) throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "Prodotto ownership API",
                                  "description": "Prodotto sintetico per verificare l'ownership stabile via API.",
                                  "category": "HARDWARE",
                                  "brand": "TestBrand",
                                  "productType": "Componente",
                                  "usageContext": "",
                                  "price": 100.00,
                                  "discount": 0.00
                                }
                                """.formatted(code)))
                .andExpect(status().isCreated());

        initializeStock(token, code, quantity);
    }

    private void initializeStock(String token, String code, int quantity) throws Exception {
        mockMvc.perform(post("/api/inventory/initial-balance")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "%s",
                                  "quantity": %d,
                                  "reason": "Saldo iniziale test ownership"
                                }
                                """.formatted(code, quantity)))
                .andExpect(status().isCreated());
    }

    private JsonNode createOrder(String token, String productCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer": "ignored",
                                  "paymentMethod": "CARD",
                                  "items": [
                                    { "productCode": "%s", "quantity": 1 }
                                  ]
                                }
                                """.formatted(productCode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownershipStatus").value("ACCOUNT"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode accountByUsername(String token, String username) throws Exception {
        String response = mockMvc.perform(get("/api/accounts")
                        .header("X-Session-Token", token)
                        .param("q", username)
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode content = objectMapper.readTree(response).path("content");
        for (JsonNode account : content) {
            if (username.equals(account.path("username").asText())) {
                return account;
            }
        }
        throw new AssertionError("Account storico non trovato: " + username);
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
