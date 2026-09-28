package it.giovannidefilippo.gestionale.purchase;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import it.giovannidefilippo.gestionale.user.UserRole;
import it.giovannidefilippo.gestionale.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SupplierOrderControllerTest {
    private static final String SUPER_ADMIN_PASSWORD = "Test-Bootstrap-9842!";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserService userService;

    @Test
    void createIsIdempotentAndCustomerCannotAccessPurchases() throws Exception {
        String token = login("test_super_admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long supplierId = createSupplier(token, "FOR-" + suffix);
        String productCode = "PC-" + suffix;
        createProduct(token, productCode);
        String key = "purchase-create-" + suffix;
        String payload = purchasePayload(supplierId, productCode);

        JsonNode first = createPurchase(token, key, payload);
        JsonNode replay = createPurchase(token, key, payload);

        assertThat(replay.path("code").asText()).isEqualTo(first.path("code").asText());
        assertThat(jdbc.queryForObject("select count(*) from supplier_orders where code = ?", Long.class, first.path("code").asText())).isEqualTo(1L);

        String customerUsername = "purchase_customer_" + suffix;
        String customerPassword = "Customer123!";
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(customerUsername, customerPassword)))
                .andExpect(status().isCreated());
        String customerToken = login(customerUsername, customerPassword, "CUSTOMER");
        mockMvc.perform(get("/api/purchase-orders").header("X-Session-Token", customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void employeeCanRunPurchaseWorkflowAndRetryReceiptWithoutDuplication() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "purchase_employee_" + suffix;
        String password = "Employee123!";
        userService.createAccount(username, password, UserRole.EMPLOYEE, "test_super_admin", "Test ordini fornitore");
        String employeeToken = login(username, password, "EMPLOYEE");
        String superToken = login("test_super_admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        long supplierId = createSupplier(superToken, "FOR-EMP-" + suffix);
        String productCode = "EMP-" + suffix;
        createProduct(superToken, productCode);
        JsonNode created = createPurchase(employeeToken, "create-" + suffix, purchasePayload(supplierId, productCode));
        String code = created.path("code").asText();

        MvcResult sentResult = mockMvc.perform(post("/api/purchase-orders/" + code + "/send")
                        .header("X-Session-Token", employeeToken)
                        .header("Idempotency-Key", "send-" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andReturn();
        long lineId = objectMapper.readTree(sentResult.getResponse().getContentAsString()).path("items").get(0).path("id").asLong();
        String receiptPayload = """
                {"reason":"Consegna di prova","items":[{"lineId":%d,"quantity":2,"unitCost":75.1234}]}
                """.formatted(lineId);

        JsonNode firstReceipt = receive(employeeToken, code, "receipt-" + suffix, receiptPayload);
        JsonNode replay = receive(employeeToken, code, "receipt-" + suffix, receiptPayload);

        assertThat(firstReceipt.path("receipts").size()).isEqualTo(1);
        assertThat(replay.path("receipts").size()).isEqualTo(1);
        assertThat(replay.path("items").get(0).path("receivedQuantity").asInt()).isEqualTo(2);
        assertThat(replay.path("receipts").get(0).path("items").get(0).path("actualUnitCost").decimalValue()).isEqualByComparingTo("75.1234");
        assertThat(replay.path("receipts").get(0).path("items").get(0).path("inventoryPostingStatus").asText()).isEqualTo("POSTED");
        assertThat(jdbc.queryForObject("select count(*) from supplier_order_receipts where supplier_order_id = (select id from supplier_orders where code = ?)", Long.class, code)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select quantity from products where code = ?", Integer.class, productCode)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select average_purchase_cost from products where code = ?", java.math.BigDecimal.class, productCode)).isEqualByComparingTo("75.1234");
        assertThat(jdbc.queryForObject("select count(*) from stock_movements where supplier_order_receipt_item_id is not null and product_code = ?", Long.class, productCode)).isEqualTo(1L);
    }

    private JsonNode createPurchase(String token, String key, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/purchase-orders")
                        .header("X-Session-Token", token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode receive(String token, String code, String key, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/purchase-orders/" + code + "/receipts")
                        .header("X-Session-Token", token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long createSupplier(String token, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/partners")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","type":"SUPPLIER","displayName":"Fornitore %s","taxCode":"","vatNumber":"IT12345678901","email":"supplier@example.com","phone":"","address":"Via Test 1","city":"Napoli","notes":""}
                                """.formatted(code, code)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private void createProduct(String token, String code) throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Prodotto acquisto","description":"Prodotto per test API ordini fornitore.","category":"HARDWARE","brand":"Test","productType":"Componente","usageContext":"","price":100.00,"discount":0.00}
                                """.formatted(code)))
                .andExpect(status().isCreated());
    }

    private String purchasePayload(long supplierId, String productCode) {
        return """
                {"supplierId":%d,"expectedDeliveryDate":"%s","notes":"Ordine API","items":[{"productCode":"%s","quantity":2,"unitPrice":80.00}]}
                """.formatted(supplierId, LocalDate.now().plusDays(3), productCode);
    }

    private String login(String username, String password, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","role":"%s"}
                                """.formatted(username, password, role)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }
}
