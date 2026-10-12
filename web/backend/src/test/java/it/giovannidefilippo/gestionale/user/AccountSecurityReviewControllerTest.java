package it.giovannidefilippo.gestionale.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountSecurityReviewControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserAccountRepository accountRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private AuthSessionService authSessionService;

    @Test
    void reportAndVerificationAreReservedToReauthenticatedSuperAdmin() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String superAdmin = "security_root_" + suffix;
        String admin = "security_admin_" + suffix;
        String employee = "security_employee_" + suffix;
        String rootPassword = "Test-Bootstrap-9842!";
        userService.createAccount(superAdmin, rootPassword, UserRole.SUPER_ADMIN, "Sistema", "Bootstrap test");
        userService.createAccount(admin, "AdminSecure123!", UserRole.ADMIN, superAdmin, "Creazione da pannello admin");
        savePendingEmployee(employee);
        String rootToken = token(superAdmin, rootPassword, UserRole.SUPER_ADMIN);
        String adminToken = token(admin, "AdminSecure123!", UserRole.ADMIN);

        mockMvc.perform(get("/api/accounts/security-review")
                        .header("X-Session-Token", adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/accounts/security-review")
                        .header("X-Session-Token", rootToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[?(@.username == '" + employee + "')].classification")
                        .value("SUSPICIOUS_OPERATIONAL"));

        mockMvc.perform(post("/api/accounts/security-review/{username}/verify", employee)
                        .header("X-Session-Token", rootToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/accounts/security-review/{username}/verify", employee)
                        .header("X-Session-Token", rootToken)
                        .header("X-Reauth-Password", rootPassword))
                .andExpect(status().isNoContent());
    }

    private void savePendingEmployee(String username) {
        PasswordHasher.PasswordHash password = passwordHasher.hash("Employee123!");
        accountRepository.save(new UserAccount(
                username,
                password.salt(),
                password.hash(),
                UserRole.EMPLOYEE,
                AccountProvisioningSource.UNKNOWN,
                false,
                null,
                null
        ));
    }

    private String token(String username, String password, UserRole role) {
        return authSessionService.create(userService.login(username, password)).token();
    }
}
