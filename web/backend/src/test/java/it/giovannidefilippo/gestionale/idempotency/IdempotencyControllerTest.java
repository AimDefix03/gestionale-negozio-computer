package it.giovannidefilippo.gestionale.idempotency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyControllerTest {
    private static final String SUPER_ADMIN_PASSWORD = "RootSecure123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void replayedOrderCreationReturnsSameResponse() throws Exception {
        String token = loginSuperAdmin();
        String productCode = uniqueCode("IDEM-PROD");
        createProduct(token, productCode);
        String idempotencyKey = uniqueCode("idem-order");
        String payload = orderPayload("Cliente idempotenza", productCode);

        JsonNode first = postOrder(token, idempotencyKey, payload);
        JsonNode second = postOrder(token, idempotencyKey, payload);

        assertThat(second.path("code").asText()).isEqualTo(first.path("code").asText());
        assertThat(second.path("id").asLong()).isEqualTo(first.path("id").asLong());
        assertThat(first.path("payment").path("method").asText()).isEqualTo("CARD");
        assertThat(first.path("payment").path("status").asText()).isEqualTo("PENDING");
        assertThat(first.path("payment").path("outstandingAmount").decimalValue()).isEqualByComparingTo("100.00");
    }

    @Test
    void unsupportedPaymentMethodIsRejectedByTheApi() throws Exception {
        String token = loginSuperAdmin();
        String productCode = uniqueCode("BAD-PAY");
        createProduct(token, productCode);

        mockMvc.perform(post("/api/orders")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload("Cliente pagamento", productCode).replace("Carta", "Criptovaluta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_MALFORMED"));
    }

    @Test
    void sameKeyWithDifferentPayloadIsRejected() throws Exception {
        String token = loginSuperAdmin();
        String firstProduct = uniqueCode("IDEM-A");
        String secondProduct = uniqueCode("IDEM-B");
        createProduct(token, firstProduct);
        createProduct(token, secondProduct);
        String idempotencyKey = uniqueCode("idem-conflict");

        postOrder(token, idempotencyKey, orderPayload("Cliente idempotenza", firstProduct));

        mockMvc.perform(post("/api/orders")
                        .header("X-Session-Token", token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload("Cliente idempotenza", secondProduct)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void replayedInvoiceCreationReturnsSameDocument() throws Exception {
        String token = loginSuperAdmin();
        String productCode = uniqueCode("IDEM-DOC");
        createProduct(token, productCode);
        String orderCode = createFulfilledOrder(token, productCode);
        String idempotencyKey = uniqueCode("idem-invoice");
        String payload = """
                {
                  "orderCode": "%s"
                }
                """.formatted(orderCode);

        JsonNode first = postInvoice(token, idempotencyKey, payload);
        JsonNode second = postInvoice(token, idempotencyKey, payload);

        assertThat(second.path("code").asText()).isEqualTo(first.path("code").asText());
        assertThat(second.path("relatedOrderCode").asText()).isEqualTo(orderCode);
    }

    @Test
    void duplicateInvoiceWithoutIdempotencyKeyReturnsConflict() throws Exception {
        String token = loginSuperAdmin();
        String productCode = uniqueCode("DUP-DOC");
        createProduct(token, productCode);
        String orderCode = createFulfilledOrder(token, productCode);
        String payload = """
                {
                  "orderCode": "%s"
                }
                """.formatted(orderCode);

        mockMvc.perform(post("/api/documents/invoice")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/documents/invoice")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Esiste già una fattura simulata per questo ordine."));
    }

    @Test
    void replayedReceiptCreatesOneFinancialMovement() throws Exception {
        String token = loginSuperAdmin();
        String productCode = uniqueCode("IDEM-PAY");
        createProduct(token, productCode);
        JsonNode draft = postOrder(token, uniqueCode("idem-payment-order"), orderPayload("Cliente incasso", productCode));
        String orderCode = draft.path("code").asText();
        mockMvc.perform(post("/api/orders/" + orderCode + "/confirm").header("X-Session-Token", token)).andExpect(status().isOk());
        String key = uniqueCode("idem-receipt");
        String payload = """
                {
                  "amount": 40.00,
                  "reference": "POS-TEST",
                  "reason": "Acconto idempotente"
                }
                """;

        JsonNode first = postReceipt(token, orderCode, key, payload);
        JsonNode replay = postReceipt(token, orderCode, key, payload);

        assertThat(first.path("payment").path("paidAmount").decimalValue()).isEqualByComparingTo("40.00");
        assertThat(replay.path("payment").path("paidAmount").decimalValue()).isEqualByComparingTo("40.00");
        assertThat(replay.path("payment").path("transactions").size()).isEqualTo(1);
    }

    private JsonNode postOrder(String token, String idempotencyKey, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("X-Session-Token", token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode postInvoice(String token, String idempotencyKey, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/documents/invoice")
                        .header("X-Session-Token", token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode postReceipt(String token, String orderCode, String idempotencyKey, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders/" + orderCode + "/payments/receipts")
                        .header("X-Session-Token", token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String createFulfilledOrder(String token, String productCode) throws Exception {
        JsonNode order = postOrder(token, uniqueCode("idem-draft"), orderPayload("Cliente documento", productCode));
        String orderCode = order.path("code").asText();
        mockMvc.perform(post("/api/orders/" + orderCode + "/confirm")
                        .header("X-Session-Token", token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders/" + orderCode + "/fulfill")
                        .header("X-Session-Token", token))
                .andExpect(status().isOk());
        return orderCode;
    }

    private void createProduct(String token, String code) throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload(code)))
                .andExpect(status().isCreated());
    }

    private String loginSuperAdmin() throws Exception {
        String response = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "%s",
                                  "role": "SUPER_ADMIN"
                                }
                                """.formatted(SUPER_ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }

    private String orderPayload(String customer, String productCode) {
        return """
                {
                  "customer": "%s",
                  "paymentMethod": "Carta",
                  "items": [
                    { "productCode": "%s", "quantity": 1 }
                  ]
                }
                """.formatted(customer, productCode);
    }

    private String productPayload(String code) {
        return """
                {
                  "code": "%s",
                  "name": "Prodotto idempotenza %s",
                  "description": "Prodotto creato per verificare la protezione anti doppio invio.",
                  "category": "HARDWARE",
                  "brand": "TestBrand",
                  "productType": "Scheda di test",
                  "usageContext": "Test",
                  "quantity": 5,
                  "price": 100.00,
                  "discount": 0.00
                }
                """.formatted(code, code);
    }

    private String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
