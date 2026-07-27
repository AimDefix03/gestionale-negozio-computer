package it.giovannidefilippo.gestionale.security;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedEndpointsRejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/products").header("X-Request-Id", "req-missing-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", "req-missing-token"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Sessione mancante o non valida."))
                .andExpect(jsonPath("$.path").value("/api/products"))
                .andExpect(jsonPath("$.requestId").value("req-missing-token"));
    }

    @Test
    void availabilityProbesArePublicWithoutExposingDetails() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void genericAndSensitiveActuatorEndpointsAreNotPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void authenticatedUsersCannotBypassActuatorDenyRule() throws Exception {
        String token = login("admin", "RootSecure123!", "SUPER_ADMIN");

        mockMvc.perform(get("/actuator/health").header("X-Session-Token", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void successfulResponsesPropagateClientRequestId() throws Exception {
        mockMvc.perform(post("/api/accounts/login")
                        .header("X-Request-Id", "ui-login-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "RootSecure123!",
                                  "role": "SUPER_ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "ui-login-001"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Pragma", "no-cache"));
    }

    @Test
    void renewalRotatesTokenAndRejectsReplayOfPreviousToken() throws Exception {
        String username = "renew_" + UUID.randomUUID().toString().replace("-", "");
        register(username, "Client123!", "CUSTOMER");
        String originalToken = login(username, "Client123!", "CUSTOMER");

        String response = mockMvc.perform(post("/api/accounts/session/renew")
                        .header("X-Session-Token", originalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "password": "Client123!" }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String rotatedToken = objectMapper.readTree(response).path("token").asText();
        assertThat(rotatedToken).isNotBlank().isNotEqualTo(originalToken);

        mockMvc.perform(get("/api/products").header("X-Session-Token", originalToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
        mockMvc.perform(get("/api/products").header("X-Session-Token", rotatedToken))
                .andExpect(status().isOk());
    }

    @Test
    void failedRenewalKeepsCurrentTokenValid() throws Exception {
        String username = "renew_failure_" + UUID.randomUUID().toString().replace("-", "");
        register(username, "Client123!", "CUSTOMER");
        String token = login(username, "Client123!", "CUSTOMER");

        mockMvc.perform(post("/api/accounts/session/renew")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "password": "PasswordErrata123!" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALID"));

        mockMvc.perform(get("/api/products").header("X-Session-Token", token))
                .andExpect(status().isOk());
    }

    @Test
    void renewalRequiresAuthenticatedSession() throws Exception {
        mockMvc.perform(post("/api/accounts/session/renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "password": "Client123!" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void missingRequestIdIsGeneratedAndReturnedConsistently() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andReturn();

        String responseRequestId = result.getResponse().getHeader("X-Request-Id");
        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(error.path("requestId").asText()).isEqualTo(responseRequestId);
    }

    @Test
    void superAdminCanAccessAccountsAndAudit() throws Exception {
        String token = login("admin", "RootSecure123!", "SUPER_ADMIN");

        mockMvc.perform(get("/api/accounts").header("X-Session-Token", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit").header("X-Session-Token", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/system/status").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database.status").value("UP"))
                .andExpect(jsonPath("$.security.activeSessions").isNumber())
                .andExpect(jsonPath("$.audit.recentImportantEvents").isArray())
                .andExpect(jsonPath("$.recentErrors").isArray());
    }

    @Test
    void customerCannotAccessAdministrativeAreas() throws Exception {
        String username = "customer_" + UUID.randomUUID().toString().replace("-", "");
        register(username, "Client123!", "CUSTOMER");
        String token = login(username, "Client123!", "CUSTOMER");

        mockMvc.perform(get("/api/products").header("X-Session-Token", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts").header("X-Session-Token", token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/documents").header("X-Session-Token", token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/system/status").header("X-Session-Token", token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders/ORD-NOT-FOUND/payments/receipts")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 10.00, "reason": "Operazione non consentita" }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders/ORD-NOT-FOUND/returns/RES-NOT-FOUND/approve")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "note": "Operazione non consentita" }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCannotCreateAccountsEvenWithValidSession() throws Exception {
        String username = "employee_" + UUID.randomUUID().toString().replace("-", "");
        register(username, "Employee123!", "EMPLOYEE");
        String token = login(username, "Employee123!", "EMPLOYEE");

        mockMvc.perform(post("/api/accounts")
                        .header("X-Session-Token", token)
                        .header("X-Reauth-Password", "Employee123!")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "blocked_admin",
                                  "password": "RootSecure123!",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginResponseContainsGranularPermissions() throws Exception {
        String response = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "RootSecure123!",
                                  "role": "SUPER_ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode permissions = objectMapper.readTree(response).path("user").path("permissions");
        assertThat(permissions).anyMatch(permission -> permission.asText().equals("MANAGE_ACCOUNTS"));
        assertThat(permissions).anyMatch(permission -> permission.asText().equals("VIEW_PARTNERS"));
        assertThat(permissions).anyMatch(permission -> permission.asText().equals("MANAGE_PARTNERS"));
        assertThat(permissions).anyMatch(permission -> permission.asText().equals("CONFIRM_ORDERS"));
        assertThat(permissions).anyMatch(permission -> permission.asText().equals("FULFILL_ORDERS"));
        assertThat(permissions).anyMatch(permission -> permission.asText().equals("CANCEL_ORDERS"));
        assertThat(permissions).anyMatch(permission -> permission.asText().equals("RECORD_PAYMENTS"));
        assertThat(permissions).anyMatch(permission -> permission.asText().equals("REFUND_PAYMENTS"));
        assertThat(permissions).anyMatch(permission -> permission.asText().equals("REQUEST_RETURNS"));
        assertThat(permissions).anyMatch(permission -> permission.asText().equals("MANAGE_RETURNS"));
        assertThat(permissions).anyMatch(permission -> permission.asText().equals("VIEW_AUDIT"));
    }

    @Test
    void validationErrorsUseStableContract() throws Exception {
        String token = login("admin", "RootSecure123!", "SUPER_ADMIN");

        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "",
                                  "name": "",
                                  "description": "",
                                  "category": "HARDWARE",
                                  "brand": "",
                                  "productType": "",
                                  "usageContext": "",
                                  "quantity": 0,
                                  "price": 0.00,
                                  "discount": 0.00
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Dati non validi."))
                .andExpect(jsonPath("$.path").value("/api/products"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void operationalRequestsUseSessionActorInsteadOfClientPayload() throws Exception {
        String token = login("admin", "RootSecure123!", "SUPER_ADMIN");
        String code = "SEC-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "Prodotto test sicurezza",
                                  "description": "Prodotto creato per verificare actor e role derivati dalla sessione.",
                                  "category": "HARDWARE",
                                  "brand": "TestBrand",
                                  "productType": "Scheda di test",
                                  "usageContext": "",
                                  "quantity": 1,
                                  "price": 100.00,
                                  "discount": 0.00
                                }
                                """.formatted(code)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventory/movements")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "%s",
                                  "type": "LOAD",
                                  "quantity": 2,
                                  "reason": "Verifica session actor",
                                  "actor": "utente_falsificato",
                                  "role": "Cliente"
                                }
                                """.formatted(code)))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(get("/api/inventory/movements")
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode movements = objectMapper.readTree(response);
        assertThat(movements.path("content")).anyMatch(movement -> movement.path("productCode").asText().equals(code)
                && movement.path("actor").asText().equals("admin")
                && movement.path("role").asText().equals("Super admin"));
        assertThat(movements.path("content")).noneMatch(movement -> movement.path("actor").asText().equals("utente_falsificato"));
    }

    @Test
    void auditEventsIncludeRequestContextAndEntityType() throws Exception {
        String token = login("admin", "RootSecure123!", "SUPER_ADMIN");
        String code = "AUD-" + UUID.randomUUID().toString().substring(0, 8);
        String requestId = "audit-" + UUID.randomUUID();

        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", token)
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "Prodotto audit",
                                  "description": "Prodotto creato per verificare il contesto audit.",
                                  "category": "HARDWARE",
                                  "brand": "TestBrand",
                                  "productType": "Scheda audit",
                                  "usageContext": "Test",
                                  "quantity": 2,
                                  "price": 100.00,
                                  "discount": 0.00
                                }
                                """.formatted(code)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/audit")
                        .header("X-Session-Token", token)
                        .param("q", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requestId").value(requestId))
                .andExpect(jsonPath("$.content[0].source").value("POST /api/products"))
                .andExpect(jsonPath("$.content[0].entityType").value("PRODUCT"))
                .andExpect(jsonPath("$.content[0].action").value("CREATE_PRODUCT"))
                .andExpect(jsonPath("$.content[0].target").value(code));
    }

    @Test
    void productsEndpointSupportsServerSidePaginationAndFilters() throws Exception {
        String token = login("admin", "RootSecure123!", "SUPER_ADMIN");
        String prefix = "PAGE-" + UUID.randomUUID().toString().substring(0, 8);

        createProduct(token, prefix + "-A", prefix + " Alpha", "NVIDIA", "Scheda grafica");
        createProduct(token, prefix + "-B", prefix + " Beta", "NVIDIA", "Scheda grafica");
        createProduct(token, prefix + "-C", prefix + " Gamma", "AMD", "Processore");

        mockMvc.perform(get("/api/products")
                        .header("X-Session-Token", token)
                        .param("q", prefix)
                        .param("category", "HARDWARE")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    private void register(String username, String password, String role) throws Exception {
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "role": "%s"
                                }
                                """.formatted(username, password, role)))
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

    private void createProduct(String token, String code, String name, String brand, String productType) throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "%s",
                                  "description": "Prodotto per test paginazione e filtri.",
                                  "category": "HARDWARE",
                                  "brand": "%s",
                                  "productType": "%s",
                                  "usageContext": "Test",
                                  "quantity": 4,
                                  "price": 100.00,
                                  "discount": 0.00
                                }
                                """.formatted(code, name, brand, productType)))
                .andExpect(status().isCreated());
    }
}
