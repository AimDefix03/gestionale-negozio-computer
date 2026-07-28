package it.giovannidefilippo.gestionale.common;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperationalMetricsIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void recordsSuccessfulInvalidAndLockedAuthenticationOutcomes() throws Exception {
        String username = "metrics_" + UUID.randomUUID().toString().replace("-", "");
        register(username);

        double successBefore = authenticationCount("success");
        double invalidBefore = authenticationCount("invalid_credentials");
        double lockedBefore = authenticationCount("locked");

        login(username, "Client123!").andExpect(status().isOk());
        for (int attempt = 0; attempt < 5; attempt++) {
            login(username, "PasswordErrata123!").andExpect(status().isBadRequest());
        }
        login(username, "Client123!").andExpect(status().isUnauthorized());

        assertThat(authenticationCount("success") - successBefore).isEqualTo(1);
        assertThat(authenticationCount("invalid_credentials") - invalidBefore).isEqualTo(5);
        assertThat(authenticationCount("locked") - lockedBefore).isEqualTo(1);
    }

    @Test
    void recordsSecurityErrorsWithoutSensitiveLabels() throws Exception {
        double before = meterRegistry.find("gestionale.api.errors")
                .tags("status", "401", "code", "auth_unauthorized")
                .counter() == null
                ? 0
                : meterRegistry.get("gestionale.api.errors")
                        .tags("status", "401", "code", "auth_unauthorized")
                        .counter()
                        .count();

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());

        var counter = meterRegistry.get("gestionale.api.errors")
                .tags("status", "401", "code", "auth_unauthorized")
                .counter();
        assertThat(counter.count() - before).isEqualTo(1);
        assertThat(counter.getId().getTags())
                .extracting(tag -> tag.getKey())
                .containsExactlyInAnyOrder("status", "code");
    }

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "Client123!",
                                  "role": "CUSTOMER"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/accounts/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "CUSTOMER"
                        }
                        """.formatted(username, password)));
    }

    private double authenticationCount(String outcome) {
        return meterRegistry.get("gestionale.authentication.attempts")
                .tag("outcome", outcome)
                .counter()
                .count();
    }
}
