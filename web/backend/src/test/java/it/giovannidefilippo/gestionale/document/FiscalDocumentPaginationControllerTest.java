package it.giovannidefilippo.gestionale.document;

import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.common.TestCompanySettings;
import it.giovannidefilippo.gestionale.company.CompanySettingsService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import it.giovannidefilippo.gestionale.order.OrderRequests;
import it.giovannidefilippo.gestionale.order.OrderResponse;
import it.giovannidefilippo.gestionale.order.OrderService;
import it.giovannidefilippo.gestionale.order.PaymentMethod;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FiscalDocumentPaginationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FiscalDocumentService documentService;

    @Autowired
    private CompanySettingsService companySettingsService;

    @BeforeEach
    void configureCompany() {
        TestCompanySettings.configure(companySettingsService, actor());
    }

    @Test
    void documentsEndpointReturnsPagedAndFilteredResults() throws Exception {
        String customer = "cliente_documenti_" + UUID.randomUUID().toString().replace("-", "");
        createInvoice(customer, uniqueCode("DOC-A"));
        createInvoice(customer, uniqueCode("DOC-B"));
        createInvoice(customer, uniqueCode("DOC-C"));
        String token = login();

        String response = mockMvc.perform(get("/api/documents")
                        .header("X-Session-Token", token)
                        .param("page", "0")
                        .param("size", "2")
                        .param("q", customer)
                        .param("type", "SIMULATED_INVOICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode content = objectMapper.readTree(response).path("content");
        assertThat(content).allMatch(document -> document.path("customer").asText().equals(customer));
        assertThat(content).allMatch(document -> document.path("type").asText().equals("SIMULATED_INVOICE"));
    }

    @Test
    void invoiceCapabilitySeesCreditNoteExcludedByCurrentFilter() throws Exception {
        String customer = "cliente_capability_" + UUID.randomUUID().toString().replace("-", "");
        OrderResponse order = createInvoice(customer, uniqueCode("DOC-CAP"));
        documentService.createCreditNote(
                new FiscalDocumentRequests.CreateCreditNoteRequest(order.code(), "Rettifica gia registrata"),
                actor()
        );
        String token = login();

        mockMvc.perform(get("/api/documents")
                        .header("X-Session-Token", token)
                        .param("page", "0")
                        .param("size", "1")
                        .param("q", customer)
                        .param("type", "SIMULATED_INVOICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].relatedOrderCode").value(order.code()))
                .andExpect(jsonPath("$.content[0].type").value("SIMULATED_INVOICE"))
                .andExpect(jsonPath("$.content[0].capabilities.canCreateCreditNote").value(false));
    }

    private OrderResponse createInvoice(String customer, String productCode) {
        productService.create(new ProductRequest(
                productCode,
                "Prodotto paginazione documenti",
                "Prodotto creato per verificare la paginazione dei documenti.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Scheda test",
                "",
                new BigDecimal("120.00"),
                new BigDecimal("0.00")
        ));
        inventoryService.initialBalance(productCode, 5, "Saldo iniziale paginazione documenti", "test", "Test");
        OrderResponse order = orderService.create(
                new OrderRequests.CreateOrderRequest(
                        customer,
                        PaymentMethod.CARD,
                        List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
                ),
                customer,
                actor()
        );
        orderService.confirm(order.code(), actor());
        orderService.fulfill(order.code(), actor());
        documentService.createInvoice(new FiscalDocumentRequests.CreateInvoiceRequest(order.code()), actor());
        return order;
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

    private static AuthenticatedUser actor() {
        return new AuthenticatedUser("test_super_admin", UserRole.SUPER_ADMIN);
    }

    private static String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
