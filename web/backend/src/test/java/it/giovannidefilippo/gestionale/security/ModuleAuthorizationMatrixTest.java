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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ModuleAuthorizationMatrixTest {
    private static final String SUPER_ADMIN_PASSWORD = "RootSecure123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedModuleListsRejectMissingSession() throws Exception {
        List<String> protectedEndpoints = List.of(
                "/api/products",
                "/api/partners",
                "/api/inventory/movements",
                "/api/orders",
                "/api/documents",
                "/api/reports/sales",
                "/api/accounts",
                "/api/audit"
        );

        for (String endpoint : protectedEndpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"))
                    .andExpect(jsonPath("$.requestId").isNotEmpty());
        }
    }

    @Test
    void customerCanBrowseCatalogAndOrdersButCannotManageOperationalModules() throws Exception {
        Account customer = createPublicAccount("customer", "CUSTOMER");

        mockMvc.perform(get("/api/products").header("X-Session-Token", customer.token()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders").header("X-Session-Token", customer.token()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/partners").header("X-Session-Token", customer.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", customer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload(uniqueCode("CUST-PROD"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/inventory/movements").header("X-Session-Token", customer.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/documents").header("X-Session-Token", customer.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/sales").header("X-Session-Token", customer.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/accounts").header("X-Session-Token", customer.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit").header("X-Session-Token", customer.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCanOperateCatalogInventoryAndDocumentsButCannotAccessAdministration() throws Exception {
        Account employee = createPublicAccount("employee", "EMPLOYEE");
        String productCode = uniqueCode("EMP-PROD");

        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", employee.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload(productCode)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventory/movements")
                        .header("X-Session-Token", employee.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "%s",
                                  "type": "LOAD",
                                  "quantity": 2,
                                  "reason": "Test permessi dipendente"
                                }
                                """.formatted(productCode)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/partners")
                        .header("X-Session-Token", employee.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partnerPayload(uniqueCode("EMP-CLI"), "CUSTOMER", "Cliente permessi")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/documents").header("X-Session-Token", employee.token()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/inventory").header("X-Session-Token", employee.token()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts").header("X-Session-Token", employee.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit").header("X-Session-Token", employee.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotInspectOrdersOwnedByAnotherCustomer() throws Exception {
        Account owner = createPublicAccount("owner", "CUSTOMER");
        Account intruder = createPublicAccount("intruder", "CUSTOMER");

        mockMvc.perform(get("/api/orders/customer/" + owner.username())
                        .header("X-Session-Token", intruder.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void customerOrderCreationUsesAuthenticatedCustomerInsteadOfPayloadCustomer() throws Exception {
        String productCode = uniqueCode("ORDER-PROD");
        createProduct(loginSuperAdmin(), productCode);
        Account customer = createPublicAccount("buyer", "CUSTOMER");

        mockMvc.perform(post("/api/orders")
                        .header("X-Session-Token", customer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer": "forged_customer",
                                  "paymentMethod": "Carta",
                                  "items": [
                                    { "productCode": "%s", "quantity": 1 }
                                  ]
                                }
                """.formatted(productCode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customer").value(customer.username()))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(get("/api/orders/customer/" + customer.username())
                        .header("X-Session-Token", customer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customer").value(customer.username()));
    }

    @Test
    void documentCreationRequiresDocumentPermission() throws Exception {
        String productCode = uniqueCode("DOC-PROD");
        String superToken = loginSuperAdmin();
        createProduct(superToken, productCode);
        Account employee = createPublicAccount("doc_employee", "EMPLOYEE");
        Account customer = createPublicAccount("doc_customer", "CUSTOMER");
        String orderCode = createFulfilledOrder(employee.token(), "document_customer", productCode);

        mockMvc.perform(post("/api/documents/invoice")
                        .header("X-Session-Token", customer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderCode": "%s"
                                }
                                """.formatted(orderCode)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/documents/invoice")
                        .header("X-Session-Token", employee.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderCode": "%s"
                                }
                                """.formatted(orderCode)))
                .andExpect(status().isCreated());
    }

    @Test
    void customerCanConfirmOwnDraftButCannotFulfillOrder() throws Exception {
        String productCode = uniqueCode("FLOW-PROD");
        String superToken = loginSuperAdmin();
        createProduct(superToken, productCode);
        Account customer = createPublicAccount("flow_customer", "CUSTOMER");
        Account employee = createPublicAccount("flow_employee", "EMPLOYEE");
        String orderCode = createDraftOrder(customer.token(), "forged_customer", productCode);

        mockMvc.perform(post("/api/orders/" + orderCode + "/confirm")
                        .header("X-Session-Token", customer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/orders/" + orderCode + "/fulfill")
                        .header("X-Session-Token", customer.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));

        mockMvc.perform(post("/api/orders/" + orderCode + "/fulfill")
                        .header("X-Session-Token", employee.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));
    }

    @Test
    void employeeOrderCanUseStructuredCustomerRegistry() throws Exception {
        String productCode = uniqueCode("REG-PROD");
        String customerCode = uniqueCode("REG-CLI");
        String superToken = loginSuperAdmin();
        createProduct(superToken, productCode);
        Account employee = createPublicAccount("registry_employee", "EMPLOYEE");
        createPartner(employee.token(), customerCode, "CUSTOMER", "Cliente Anagrafica");

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("X-Session-Token", employee.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer": "cliente_libero",
                                  "customerCode": "%s",
                                  "paymentMethod": "Carta",
                                  "items": [
                                    { "productCode": "%s", "quantity": 1 }
                                  ]
                                }
                                """.formatted(customerCode, productCode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerCode").value(customerCode))
                .andExpect(jsonPath("$.customer").value("Cliente Anagrafica"))
                .andReturn();

        assertThat(objectMapper.readTree(result.getResponse().getContentAsString()).path("code").asText()).startsWith("ORD-");
    }

    @Test
    void onlySuperAdminCanCreateOrDeleteAdminAccounts() throws Exception {
        String superToken = loginSuperAdmin();
        Account admin = createAdmin(superToken, "matrix_admin");
        Account anotherAdmin = createAdmin(superToken, "matrix_admin_target");

        mockMvc.perform(post("/api/accounts")
                        .header("X-Session-Token", admin.token())
                        .header("X-Reauth-Password", admin.password())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "AdminUser123!",
                                  "role": "ADMIN"
                                }
                                """.formatted(uniqueUsername("blocked_admin"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Solo il super admin può creare altri account admin."));

        mockMvc.perform(delete("/api/accounts/" + anotherAdmin.username())
                        .header("X-Session-Token", admin.token())
                        .header("X-Reauth-Password", admin.password()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Solo il super admin può eliminare account admin."));

        mockMvc.perform(delete("/api/accounts/" + anotherAdmin.username())
                        .header("X-Session-Token", superToken)
                        .header("X-Reauth-Password", SUPER_ADMIN_PASSWORD))
                .andExpect(status().isNoContent());
    }

    @Test
    void accountManagerCannotDeleteCurrentSessionAccount() throws Exception {
        String superToken = loginSuperAdmin();
        Account admin = createAdmin(superToken, "self_delete_admin");

        mockMvc.perform(delete("/api/accounts/" + admin.username())
                        .header("X-Session-Token", admin.token())
                        .header("X-Reauth-Password", admin.password()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Non puoi eliminare il tuo account mentre sei autenticato."));
    }

    private Account createPublicAccount(String prefix, String role) throws Exception {
        String username = uniqueUsername(prefix);
        String password = "User123!";
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
        return new Account(username, password, role, login(username, password, role));
    }

    private Account createAdmin(String superToken, String prefix) throws Exception {
        String username = uniqueUsername(prefix);
        String password = "AdminUser123!";
        mockMvc.perform(post("/api/accounts")
                        .header("X-Session-Token", superToken)
                        .header("X-Reauth-Password", SUPER_ADMIN_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "role": "ADMIN"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isCreated());
        return new Account(username, password, "ADMIN", login(username, password, "ADMIN"));
    }

    private String loginSuperAdmin() throws Exception {
        return login("admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
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

    private void createProduct(String token, String code) throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload(code)))
                .andExpect(status().isCreated());
    }

    private void createPartner(String token, String code, String type, String displayName) throws Exception {
        mockMvc.perform(post("/api/partners")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partnerPayload(code, type, displayName)))
                .andExpect(status().isCreated());
    }

    private String createFulfilledOrder(String token, String customer, String productCode) throws Exception {
        String orderCode = createDraftOrder(token, customer, productCode);
        mockMvc.perform(post("/api/orders/" + orderCode + "/confirm")
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(post("/api/orders/" + orderCode + "/fulfill")
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));
        return orderCode;
    }

    private String createDraftOrder(String token, String customer, String productCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer": "%s",
                                  "paymentMethod": "Carta",
                                  "items": [
                                    { "productCode": "%s", "quantity": 1 }
                                  ]
                                }
                                """.formatted(customer, productCode)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode order = objectMapper.readTree(result.getResponse().getContentAsString());
        return order.path("code").asText();
    }

    private String productPayload(String code) {
        return """
                {
                  "code": "%s",
                  "name": "Prodotto autorizzazione %s",
                  "description": "Prodotto creato per testare la matrice permessi.",
                  "category": "HARDWARE",
                  "brand": "TestBrand",
                  "productType": "Scheda grafica",
                  "usageContext": "Test",
                  "quantity": 5,
                  "price": 100.00,
                  "discount": 0.00
                }
                """.formatted(code, code);
    }

    private String partnerPayload(String code, String type, String displayName) {
        return """
                {
                  "code": "%s",
                  "type": "%s",
                  "displayName": "%s",
                  "taxCode": "RSSMRA80A01F839X",
                  "vatNumber": "",
                  "email": "cliente@example.com",
                  "phone": "0810000000",
                  "address": "Via Test 1",
                  "city": "Napoli",
                  "notes": "Anagrafica per test autorizzazioni"
                }
                """.formatted(code, type, displayName);
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private record Account(String username, String password, String role, String token) {
    }
}
