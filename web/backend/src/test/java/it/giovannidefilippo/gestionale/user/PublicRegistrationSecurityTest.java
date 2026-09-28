package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicRegistrationSecurityTest {
    private static final String SUPER_ADMIN_PASSWORD = "Test-Bootstrap-9842!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AuditService auditService;

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"EMPLOYEE", "ADMIN", "SUPER_ADMIN"})
    void anonymousRegistrationRejectsAnyRequestedOperationalRole(UserRole requestedRole) throws Exception {
        String username = uniqueUsername("public_role");

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "PublicCustomer-Strong-123!",
                                  "role": "%s"
                                }
                                """.formatted(username, requestedRole)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(userAccountRepository.findByUsernameIgnoreCase(username)).isEmpty();
    }

    @Test
    void anonymousRegistrationCreatesOnlyCustomerAndAuditsSelfServiceActor() throws Exception {
        String username = uniqueUsername("public_customer");

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "PublicCustomer-Strong-123!"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.permissions[?(@ == 'MANAGE_PRODUCTS')]").isEmpty())
                .andExpect(jsonPath("$.permissions[?(@ == 'MANAGE_ACCOUNTS')]").isEmpty());

        assertThat(userAccountRepository.findByUsernameIgnoreCase(username))
                .get()
                .extracting(UserAccount::getRole)
                .isEqualTo(UserRole.CUSTOMER);
        assertThat(auditService.search(username, AuditCategory.ACCOUNT, null, 0, 20).content())
                .anySatisfy(event -> {
                    assertThat(event.actor()).isEqualTo("SELF_SERVICE");
                    assertThat(event.action()).isEqualTo("CREATE_ACCOUNT");
                    assertThat(event.target()).isEqualTo(username);
                    assertThat(event.source()).isEqualTo("POST /api/accounts/register");
                });
    }

    @Test
    void authenticatedAdministrativeFlowStillCreatesEmployee() throws Exception {
        String token = loginSuperAdmin();
        String username = uniqueUsername("staff_employee");

        mockMvc.perform(post("/api/accounts")
                        .header("X-Session-Token", token)
                        .header("X-Reauth-Password", SUPER_ADMIN_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "Employee-Strong-123!",
                                  "role": "EMPLOYEE"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    private String loginSuperAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "test_super_admin",
                                  "password": "%s",
                                  "role": "SUPER_ADMIN"
                                }
                                """.formatted(SUPER_ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return new tools.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .path("token")
                .asText();
    }

    private static String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
