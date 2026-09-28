package it.giovannidefilippo.gestionale.inventory;

import tools.jackson.databind.ObjectMapper;
import it.giovannidefilippo.gestionale.common.PostgreSqlIntegrationTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("postgresql")
class InventoryLedgerControllerTest extends PostgreSqlIntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void ledgerCommandsRequireAnAuthenticatedInventoryOperator() throws Exception {
        mockMvc.perform(post("/api/inventory/initial-balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productCode":"UNAUTHORIZED","quantity":1,"reason":"Non autorizzato"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/inventory/reconciliation"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productEndpointsCannotChangeStockAndInventoryCommandsRemainReconciled() throws Exception {
        String token = login();
        String code = "API-LED-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/products")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(code, "Prodotto ledger", 99)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(0));

        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productCode":"%s","delta":1,"reason":"Rettifica prematura"}
                                """.formatted(code)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/inventory/initial-balance")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productCode":"%s","quantity":5,"reason":"Inventario iniziale"}
                                """.formatted(code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INITIAL_BALANCE"))
                .andExpect(jsonPath("$.deltaQuantity").value(5))
                .andExpect(jsonPath("$.newQuantity").value(5));

        mockMvc.perform(post("/api/inventory/adjustments")
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productCode":"%s","delta":-2,"reason":"Rettifica inventario"}
                                """.formatted(code)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("sessione di conteggio approvata")));

        mockMvc.perform(put("/api/products/{code}", code)
                        .header("X-Session-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(code, "Prodotto rinominato", 500)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Prodotto rinominato"))
                .andExpect(jsonPath("$.quantity").value(5));

        mockMvc.perform(get("/api/inventory/reconciliation")
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.productCode == '%s')].status".formatted(code)).value(hasItem("BALANCED")));
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

    private String productJson(String code, String name, int ignoredQuantity) {
        return """
                {
                  "code": "%s",
                  "name": "%s",
                  "description": "Prodotto per il contratto ledger.",
                  "category": "HARDWARE",
                  "brand": "LedgerBrand",
                  "productType": "Componente",
                  "usageContext": "",
                  "quantity": %d,
                  "price": 100.00,
                  "discount": 0.00
                }
                """.formatted(code, name, ignoredQuantity);
    }
}
