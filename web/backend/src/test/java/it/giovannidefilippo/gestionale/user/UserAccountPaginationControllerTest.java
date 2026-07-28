package it.giovannidefilippo.gestionale.user;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserAccountPaginationControllerTest {
    private static final String SUPER_ADMIN_PASSWORD = "RootSecure123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void superAdminCanSearchAndFilterPaginatedAccounts() throws Exception {
        String token = loginSuperAdmin();
        String prefix = "paged_emp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        createAccount(token, prefix + "_01");
        createAccount(token, prefix + "_02");
        createAccount(token, prefix + "_03");

        MvcResult result = mockMvc.perform(get("/api/accounts")
                        .header("X-Session-Token", token)
                        .param("q", prefix)
                        .param("role", "EMPLOYEE")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).path("content");
        assertThat(content).allSatisfy(account -> {
            assertThat(account.path("username").asText()).contains(prefix);
            assertThat(account.path("role").asText()).isEqualTo("EMPLOYEE");
        });
    }

    private String loginSuperAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "%s",
                                  "role": "SUPER_ADMIN"
                                }
                                """.formatted(SUPER_ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private void createAccount(String token, String username) throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .header("X-Session-Token", token)
                        .header("X-Reauth-Password", SUPER_ADMIN_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "EmployeeStrong123!",
                                  "role": "EMPLOYEE"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated());
    }
}
