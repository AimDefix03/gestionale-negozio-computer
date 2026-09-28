package it.giovannidefilippo.gestionale.product;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerCatalogControllerTest {
    private static final String SUPER_ADMIN_PASSWORD = "Test-Bootstrap-9842!";
    private static final String CUSTOMER_PASSWORD = "CustomerStrong123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void customerCatalogRequiresSession() throws Exception {
        mockMvc.perform(get("/api/customer/catalog"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void customerReceivesOnlyCommercialProductProjection() throws Exception {
        String adminToken = login("test_super_admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        String customerToken = registerAndLoginCustomer();
        String productCode = uniqueCode("CUSTOMER-LIMITED");
        createProduct(adminToken, productCode, 2);

        mockMvc.perform(get("/api/customer/catalog")
                        .header("X-Session-Token", customerToken)
                        .param("q", productCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(productCode))
                .andExpect(jsonPath("$.content[0].name").value("Prodotto cliente"))
                .andExpect(jsonPath("$.content[0].availability").value("LIMITED"))
                .andExpect(jsonPath("$.content[0].availabilityLabel").value("Disponibilita limitata"))
                .andExpect(jsonPath("$.content[0].discountedPrice").value(120.00))
                .andExpect(jsonPath("$.content[0].id").doesNotExist())
                .andExpect(jsonPath("$.content[0].quantity").doesNotExist())
                .andExpect(jsonPath("$.content[0].reservedQuantity").doesNotExist())
                .andExpect(jsonPath("$.content[0].availableQuantity").doesNotExist())
                .andExpect(jsonPath("$.content[0].lastPurchaseCost").doesNotExist())
                .andExpect(jsonPath("$.content[0].averagePurchaseCost").doesNotExist())
                .andExpect(jsonPath("$.content[0].knownInventoryCost").doesNotExist())
                .andExpect(jsonPath("$.content[0].potentialGrossMarginOnCostedStock").doesNotExist())
                .andExpect(jsonPath("$.content[0].discontinued").doesNotExist());
    }

    @Test
    void commercialAvailabilityUsesDiscreteLevelsAndHidesDiscontinuedProducts() throws Exception {
        String adminToken = login("test_super_admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        String customerToken = registerAndLoginCustomer();
        String availableCode = uniqueCode("CUSTOMER-AVAILABLE");
        String unavailableCode = uniqueCode("CUSTOMER-OUT");
        String discontinuedCode = uniqueCode("CUSTOMER-OFF");
        createProduct(adminToken, availableCode, 8);
        createProduct(adminToken, unavailableCode, 0);
        createProduct(adminToken, discontinuedCode, 2);
        discontinueProduct(adminToken, discontinuedCode);

        mockMvc.perform(get("/api/customer/catalog").header("X-Session-Token", customerToken).param("q", availableCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].availability").value("AVAILABLE"));

        mockMvc.perform(get("/api/customer/catalog").header("X-Session-Token", customerToken).param("q", unavailableCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].availability").value("UNAVAILABLE"));

        mockMvc.perform(get("/api/customer/catalog").header("X-Session-Token", customerToken).param("q", discontinuedCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void responseShapeIsAuthorizedServerSideForEveryRole() throws Exception {
        String superAdminToken = login("test_super_admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        String adminToken = createAndLoginStaff(superAdminToken, "ADMIN");
        String employeeToken = createAndLoginStaff(superAdminToken, "EMPLOYEE");
        String customerToken = registerAndLoginCustomer();
        String productCode = uniqueCode("CUSTOMER-ROLES");
        createProduct(superAdminToken, productCode, 4);

        mockMvc.perform(get("/api/products").header("X-Session-Token", customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/dashboard").header("X-Session-Token", customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/customer/catalog").header("X-Session-Token", customerToken).param("q", productCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.content[0].quantity").doesNotExist());

        mockMvc.perform(get("/api/customer/dashboard").header("X-Session-Token", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.potentialRetailStockValue").doesNotExist());

        for (String staffToken : List.of(superAdminToken, adminToken, employeeToken)) {
            mockMvc.perform(get("/api/customer/catalog").header("X-Session-Token", staffToken))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/customer/dashboard").header("X-Session-Token", staffToken))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/products").header("X-Session-Token", staffToken).param("q", productCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].quantity").value(4))
                    .andExpect(jsonPath("$.content[0].reservedQuantity").value(0))
                    .andExpect(jsonPath("$.content[0].availableQuantity").value(4))
                    .andExpect(jsonPath("$.content[0].costedQuantity").value(0))
                    .andExpect(jsonPath("$.content[0].knownInventoryCost").value(0));
        }
    }

    @Test
    void customerCannotRequestQuantityBasedSorting() throws Exception {
        String customerToken = registerAndLoginCustomer();

        mockMvc.perform(get("/api/customer/catalog")
                        .header("X-Session-Token", customerToken)
                        .param("sort", "QTY_DESC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALID"));
    }

    private String registerAndLoginCustomer() throws Exception {
        String username = "catalog_customer_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, CUSTOMER_PASSWORD)))
                .andExpect(status().isCreated());
        return login(username, CUSTOMER_PASSWORD, "CUSTOMER");
    }

    private String createAndLoginStaff(String superAdminToken, String role) throws Exception {
        String username = role.toLowerCase() + "_catalog_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "StaffStrong123!";
        mockMvc.perform(post("/api/accounts")
                        .header("X-Session-Token", superAdminToken)
                        .header("X-Reauth-Password", SUPER_ADMIN_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "role": "%s"
                                }
                                """.formatted(username, password, role)))
                .andExpect(status().isCreated());
        return login(username, password, role);
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

    private void createProduct(String token, String code, int quantity) throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "Prodotto cliente",
                                  "description": "Prodotto per il contratto catalogo cliente.",
                                  "category": "HARDWARE",
                                  "brand": "CustomerBrand",
                                  "productType": "Componente",
                                  "usageContext": "Professionale",
                                  "price": 120.00,
                                  "discount": 0.00
                                }
                                """.formatted(code)))
                .andExpect(status().isCreated());
        if (quantity > 0) {
            mockMvc.perform(post("/api/inventory/initial-balance")
                            .header("X-Session-Token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "productCode": "%s",
                                      "quantity": %d,
                                      "reason": "Saldo iniziale test projection cliente"
                                    }
                                    """.formatted(code, quantity)))
                    .andExpect(status().isCreated());
        }
    }

    private void discontinueProduct(String token, String code) throws Exception {
        mockMvc.perform(post("/api/products/{code}/discontinue", code)
                        .header("X-Session-Token", token))
                .andExpect(status().isOk());
    }

    private static String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
