package it.giovannidefilippo.gestionale.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportingControllerTest {
    private static final String SUPER_ADMIN_PASSWORD = "RootSecure123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void reportEndpointsRequireAnAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/api/reports/sales"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
        mockMvc.perform(get("/api/reports/inventory/export").param("format", "CSV"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotReadOrExportOperationalReports() throws Exception {
        String username = "report_customer_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        registerCustomer(username);
        String token = login(username, "CustomerStrong123!", "CUSTOMER");

        mockMvc.perform(get("/api/reports/sales").header("X-Session-Token", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
        mockMvc.perform(get("/api/reports/inventory/export")
                        .param("format", "PDF")
                        .header("X-Session-Token", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void authorizedUserReceivesJsonReportsAndAllRealExportFormats() throws Exception {
        String token = login("admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");

        mockMvc.perform(get("/api/reports/sales")
                        .param("status", "FULFILLED")
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").isNumber())
                .andExpect(jsonPath("$.netCollectedAmount").isNumber())
                .andExpect(jsonPath("$.topProducts").isArray());

        mockMvc.perform(get("/api/reports/inventory").header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCount").isNumber())
                .andExpect(jsonPath("$.inventoryValue").isNumber())
                .andExpect(jsonPath("$.products").isArray());

        MvcResult csv = export(token, "/api/reports/sales/export", "CSV", "text/csv");
        MvcResult xlsx = export(token, "/api/reports/sales/export", "XLSX", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MvcResult pdf = export(token, "/api/reports/inventory/export", "PDF", "application/pdf");

        assertThat(csv.getResponse().getContentAsByteArray()).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(xlsx.getResponse().getContentAsByteArray()).startsWith((byte) 'P', (byte) 'K');
        assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void exportIsRecordedInTheAuditLog() throws Exception {
        String token = login("admin", SUPER_ADMIN_PASSWORD, "SUPER_ADMIN");
        export(token, "/api/reports/sales/export", "CSV", "text/csv");

        mockMvc.perform(get("/api/audit")
                        .param("q", "EXPORT_SALES_REPORT")
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("EXPORT_SALES_REPORT"))
                .andExpect(jsonPath("$.content[0].category").value("REPORT"))
                .andExpect(jsonPath("$.content[0].entityType").value("REPORT"));
    }

    private MvcResult export(String token, String endpoint, String format, String contentType) throws Exception {
        return mockMvc.perform(get(endpoint)
                        .param("format", format)
                        .header("X-Session-Token", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith(contentType)))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn();
    }

    private void registerCustomer(String username) throws Exception {
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "CustomerStrong123!",
                                  "role": "CUSTOMER"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated());
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
}
