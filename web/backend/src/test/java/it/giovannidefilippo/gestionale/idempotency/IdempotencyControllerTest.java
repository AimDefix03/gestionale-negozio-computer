package it.giovannidefilippo.gestionale.idempotency;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import it.giovannidefilippo.gestionale.common.PostgreSqlIntegrationTestSupport;
import it.giovannidefilippo.gestionale.common.TestCompanySettings;
import it.giovannidefilippo.gestionale.company.CompanySettingsService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "gestionale.idempotency.wait-timeout-ms=120",
        "gestionale.idempotency.poll-interval-ms=10"
})
@AutoConfigureMockMvc
@Tag("postgresql")
class IdempotencyControllerTest extends PostgreSqlIntegrationTestSupport {
    private static final String SUPER_ADMIN_PASSWORD = "Test-Bootstrap-9842!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyClaimService claimService;

    @Autowired
    private IdempotencyMaintenanceService maintenanceService;

    @Autowired
    private CompanySettingsService companySettingsService;

    @Test
    void concurrentRequestsWithTheSameIntentCreateOneOrderAndReplayOneResponse() throws Exception {
        String token = loginSuperAdmin();
        String productCode = uniqueCode("IDEM-RACE");
        createProduct(token, productCode);
        String key = uniqueCode("idem-race-order");
        String customer = uniqueCode("Cliente-concorrente");
        String payload = orderPayload(customer, productCode);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<JsonNode>> responses = List.of(
                    executor.submit(() -> concurrentPostOrder(token, key, payload, ready, start)),
                    executor.submit(() -> concurrentPostOrder(token, key, payload, ready, start))
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            JsonNode first = responses.get(0).get(10, TimeUnit.SECONDS);
            JsonNode second = responses.get(1).get(10, TimeUnit.SECONDS);

            assertThat(second.path("id").asLong()).isEqualTo(first.path("id").asLong());
            assertThat(second.path("code").asText()).isEqualTo(first.path("code").asText());
            assertThat(jdbc.queryForObject(
                    "select count(*) from customer_orders where customer = ?",
                    Long.class,
                    customer
            )).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedMutationCanBeRetriedWithTheSameKey() {
        AuthenticatedUser actor = stableSuperAdmin();
        String key = uniqueCode("idem-retryable");
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> idempotencyService.execute(
                key,
                actor,
                "POST /api/test/retryable",
                "payload",
                String.class,
                org.springframework.http.HttpStatus.CREATED,
                () -> {
                    attempts.incrementAndGet();
                    throw new IllegalStateException("fallimento simulato");
                }
        )).isInstanceOf(IllegalStateException.class).hasMessage("fallimento simulato");

        String response = idempotencyService.execute(
                key,
                actor,
                "POST /api/test/retryable",
                "payload",
                String.class,
                org.springframework.http.HttpStatus.CREATED,
                () -> {
                    attempts.incrementAndGet();
                    return "completata";
                }
        );

        assertThat(response).isEqualTo("completata");
        assertThat(attempts).hasValue(2);
    }

    @Test
    void activeClaimTimesOutWithoutExecutingTheSecondMutation() {
        AuthenticatedUser actor = stableSuperAdmin();
        String key = uniqueCode("idem-active");
        String operation = "POST /api/test/active";
        String payload = objectMapper.valueToTree("payload").toString();
        String requestHash = hashForTest("v2\nactor:" + actor.accountId() + "\nendpoint:" + operation + "\npayload:" + payload);
        claimService.create(key, actor.accountId(), actor.username(), operation, requestHash, UUID.randomUUID().toString());
        AtomicInteger executions = new AtomicInteger();

        assertThatThrownBy(() -> idempotencyService.execute(
                key,
                actor,
                operation,
                "payload",
                String.class,
                org.springframework.http.HttpStatus.OK,
                () -> {
                    executions.incrementAndGet();
                    return "duplicata";
                }
        )).isInstanceOf(IdempotencyInProgressException.class);
        assertThat(executions).hasValue(0);
    }

    @Test
    void cleanupRemovesOnlyExpiredOrAbandonedClaims() {
        AuthenticatedUser actor = stableSuperAdmin();
        String expiredKey = uniqueCode("idem-expired");
        String activeKey = uniqueCode("idem-live");
        claimService.create(expiredKey, actor.accountId(), actor.username(), "POST /api/test/cleanup", "a".repeat(64), UUID.randomUUID().toString());
        claimService.create(activeKey, actor.accountId(), actor.username(), "POST /api/test/cleanup", "b".repeat(64), UUID.randomUUID().toString());
        LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
        jdbc.update(
                "update idempotency_records set expires_at = ?, lease_expires_at = ? where idem_key = ?",
                utcNow.minusHours(2),
                utcNow.minusHours(1),
                expiredKey
        );

        maintenanceService.cleanup();

        assertThat(idempotencyRecordCount(expiredKey)).isZero();
        assertThat(idempotencyRecordCount(activeKey)).isEqualTo(1L);
    }

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
        TestCompanySettings.configure(companySettingsService, stableSuperAdmin());
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
        TestCompanySettings.configure(companySettingsService, stableSuperAdmin());
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

    @Test
    void replayedCancellationCreatesOneReversalAndReleasesStockOnce() throws Exception {
        String token = loginSuperAdmin();
        String productCode = uniqueCode("IDEM-CANCEL");
        createProduct(token, productCode);
        JsonNode draft = postOrder(token, uniqueCode("idem-cancel-order"), orderPayload("Cliente annullamento", productCode));
        String orderCode = draft.path("code").asText();
        mockMvc.perform(post("/api/orders/" + orderCode + "/confirm").header("X-Session-Token", token)).andExpect(status().isOk());
        postReceipt(token, orderCode, uniqueCode("idem-cancel-receipt"), """
                {
                  "amount": 100.00,
                  "reference": "POS-CANCEL",
                  "reason": "Saldo prima dell'annullamento"
                }
                """);
        String key = uniqueCode("idem-cancel");
        String payload = """
                {
                  "reference": "STORNO-IDEMPOTENTE",
                  "reason": "Ordine duplicato verificato"
                }
                """;

        JsonNode first = postCancellation(token, orderCode, key, payload);
        JsonNode replay = postCancellation(token, orderCode, key, payload);

        assertThat(first.path("status").asText()).isEqualTo("CANCELED");
        assertThat(replay.path("status").asText()).isEqualTo("CANCELED");
        assertThat(replay.path("payment").path("transactions").size()).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "select count(*) from payment_transactions movement join order_payments payment on payment.id = movement.payment_id join customer_orders customer_order on customer_order.id = payment.order_id where customer_order.code = ? and movement.type = 'REVERSAL'",
                Long.class,
                orderCode
        )).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select reserved_quantity from products where code = ?", Integer.class, productCode)).isZero();
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

    private JsonNode concurrentPostOrder(
            String token,
            String idempotencyKey,
            String payload,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Avvio concorrente non ricevuto.");
        }
        return postOrder(token, idempotencyKey, payload);
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

    private JsonNode postCancellation(String token, String orderCode, String idempotencyKey, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders/" + orderCode + "/cancel")
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

        mockMvc.perform(post("/api/inventory/initial-balance")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "%s",
                                  "quantity": 5,
                                  "reason": "Saldo iniziale test idempotenza"
                                }
                                """.formatted(code)))
                .andExpect(status().isCreated());
    }

    private String loginSuperAdmin() throws Exception {
        String response = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "test_super_admin",
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
                  "customerType": "WALK_IN",
                  "walkInCustomerName": "%s",
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
                  "price": 100.00,
                  "discount": 0.00
                }
                """.formatted(code, code);
    }

    private String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private AuthenticatedUser stableSuperAdmin() {
        Long accountId = jdbc.queryForObject(
                "select id from user_accounts where username = 'test_super_admin'",
                Long.class
        );
        return new AuthenticatedUser(accountId, "test_super_admin", UserRole.SUPER_ADMIN);
    }

    private long idempotencyRecordCount(String key) {
        return jdbc.queryForObject(
                "select count(*) from idempotency_records where idem_key = ?",
                Long.class,
                key
        );
    }

    private String hashForTest(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
