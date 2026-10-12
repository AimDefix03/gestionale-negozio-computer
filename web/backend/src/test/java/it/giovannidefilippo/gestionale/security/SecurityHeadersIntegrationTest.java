package it.giovannidefilippo.gestionale.security;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityHeadersIntegrationTest.ErrorProbeController.class)
class SecurityHeadersIntegrationTest {
    private static final String SUPER_ADMIN_USERNAME = "test_super_admin";
    private static final String SUPER_ADMIN_PASSWORD = "Test-Bootstrap-9842!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void securityHeadersAreWrittenForSuccessfulResponses() throws Exception {
        assertSecurityHeaders(mockMvc.perform(get("/actuator/health/liveness")))
                .andExpect(status().isOk());
    }

    @Test
    void securityHeadersAreWrittenForClientErrors() throws Exception {
        assertSecurityHeaders(mockMvc.perform(get("/api/products")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void securityHeadersAreWrittenForServerErrors() throws Exception {
        String token = login();

        assertSecurityHeaders(mockMvc.perform(get("/test/security/internal-error")
                        .header("X-Session-Token", token)))
                .andExpect(status().isInternalServerError());
    }

    private org.springframework.test.web.servlet.ResultActions assertSecurityHeaders(
            org.springframework.test.web.servlet.ResultActions result
    ) throws Exception {
        return result
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "role": "SUPER_ADMIN"
                                }
                                """.formatted(SUPER_ADMIN_USERNAME, SUPER_ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }

    @RestController
    static class ErrorProbeController {
        @GetMapping("/test/security/internal-error")
        String fail() {
            throw new NullPointerException("Errore intenzionale del test header.");
        }
    }
}
