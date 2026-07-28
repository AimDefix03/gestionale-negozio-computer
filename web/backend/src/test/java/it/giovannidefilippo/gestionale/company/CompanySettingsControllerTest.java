package it.giovannidefilippo.gestionale.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.giovannidefilippo.gestionale.user.UserRole;
import it.giovannidefilippo.gestionale.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompanySettingsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Test
    void superAdminCanReadCompanySettings() throws Exception {
        mockMvc.perform(get("/api/company-settings")
                        .header("X-Session-Token", login("admin", "RootSecure123!", UserRole.SUPER_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultVatRate").value(0.22))
                .andExpect(jsonPath("$.invoicePrefix").value("FS"))
                .andExpect(jsonPath("$.creditNotePrefix").value("NC"));
    }

    @Test
    void regularAdminCannotReadCompanySettings() throws Exception {
        String username = "company_admin_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Violet-Quartz-928!";
        userService.createAccount(username, password, UserRole.ADMIN, "admin", "Account test configurazione aziendale");

        mockMvc.perform(get("/api/company-settings")
                        .header("X-Session-Token", login(username, password, UserRole.ADMIN)))
                .andExpect(status().isForbidden());
    }

    private String login(String username, String password, UserRole role) throws Exception {
        String response = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password, role))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }

    private record LoginRequest(String username, String password, UserRole role) {
    }
}
