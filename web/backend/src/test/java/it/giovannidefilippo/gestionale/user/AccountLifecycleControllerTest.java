package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountLifecycleControllerTest {
    private static final String SUPER_ADMIN_PASSWORD = "Test-Bootstrap-9842!";
    private static final String CUSTOMER_PASSWORD = "Customer-Strong-123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository accountRepository;

    @Autowired
    private AuditService auditService;

    @Test
    void loginUsesServerRoleAndDoesNotRequireRoleInput() throws Exception {
        String username = registerCustomer();

        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, CUSTOMER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.enabled").value(true));

        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "role": "SUPER_ADMIN"
                                }
                                """.formatted(username, CUSTOMER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void disableRevokesSessionsPreservesHistoryAndEnableRequiresFreshLogin() throws Exception {
        String adminToken = login("test_super_admin", SUPER_ADMIN_PASSWORD);
        String username = registerCustomer();
        String customerToken = login(username, CUSTOMER_PASSWORD);

        mockMvc.perform(post("/api/accounts/{username}/disable", username)
                        .header("X-Session-Token", adminToken)
                        .header("X-Reauth-Password", SUPER_ADMIN_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Uscita dal servizio"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.disabledBy").value("test_super_admin"))
                .andExpect(jsonPath("$.disabledReason").value("Uscita dal servizio"));

        mockMvc.perform(get("/api/customer/dashboard").header("X-Session-Token", customerToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, CUSTOMER_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Credenziali non valide."));

        UserAccount stored = accountRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertThat(stored.isEnabled()).isFalse();
        assertThat(auditService.search(username, AuditCategory.ACCOUNT, null, 0, 20).content())
                .anySatisfy(event -> assertThat(event.action()).isEqualTo("DISABLE_ACCOUNT"));

        mockMvc.perform(post("/api/accounts/{username}/enable", username)
                        .header("X-Session-Token", adminToken)
                        .header("X-Reauth-Password", SUPER_ADMIN_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Rientro autorizzato"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.disabledAt").doesNotExist());

        login(username, CUSTOMER_PASSWORD);
        mockMvc.perform(get("/api/customer/dashboard").header("X-Session-Token", customerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void selfPasswordChangeRevokesAllTokensAndRequiresCurrentPassword() throws Exception {
        String username = registerCustomer();
        String firstToken = login(username, CUSTOMER_PASSWORD);
        String secondToken = login(username, CUSTOMER_PASSWORD);

        mockMvc.perform(post("/api/accounts/me/password")
                        .header("X-Session-Token", firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "errata",
                                  "newPassword": "Replacement-Strong-123!"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/accounts/me/password")
                        .header("X-Session-Token", firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "Replacement-Strong-123!"
                                }
                                """.formatted(CUSTOMER_PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/customer/dashboard").header("X-Session-Token", firstToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customer/dashboard").header("X-Session-Token", secondToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/accounts/login").contentType(MediaType.APPLICATION_JSON).content(loginBody(username, CUSTOMER_PASSWORD)))
                .andExpect(status().isBadRequest());
        login(username, "Replacement-Strong-123!");
    }

    @Test
    void administrativeResetAndExplicitRevocationInvalidateEveryPriorToken() throws Exception {
        String adminToken = login("test_super_admin", SUPER_ADMIN_PASSWORD);
        String username = registerCustomer();
        String firstToken = login(username, CUSTOMER_PASSWORD);
        String secondToken = login(username, CUSTOMER_PASSWORD);

        mockMvc.perform(post("/api/accounts/{username}/sessions/revoke", username)
                        .header("X-Session-Token", adminToken)
                        .header("X-Reauth-Password", SUPER_ADMIN_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Possibile compromissione"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/customer/dashboard").header("X-Session-Token", firstToken)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customer/dashboard").header("X-Session-Token", secondToken)).andExpect(status().isUnauthorized());

        String freshToken = login(username, CUSTOMER_PASSWORD);
        mockMvc.perform(post("/api/accounts/{username}/password-reset", username)
                        .header("X-Session-Token", adminToken)
                        .header("X-Reauth-Password", SUPER_ADMIN_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newPassword": "Administrative-Reset-123!",
                                  "reason": "Reset controllato dopo verifica identita"
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/customer/dashboard").header("X-Session-Token", freshToken)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/accounts/login").contentType(MediaType.APPLICATION_JSON).content(loginBody(username, CUSTOMER_PASSWORD)))
                .andExpect(status().isBadRequest());
        login(username, "Administrative-Reset-123!");
    }

    private String registerCustomer() throws Exception {
        String username = "lifecycle_" + UUID.randomUUID().toString().replace("-", "");
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, CUSTOMER_PASSWORD)))
                .andExpect(status().isCreated());
        return username;
    }

    private String login(String username, String password) throws Exception {
        MvcResult response = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(response.getResponse().getContentAsString()).path("token").asText();
    }

    private String loginBody(String username, String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
    }
}
