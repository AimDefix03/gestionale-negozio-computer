package it.giovannidefilippo.gestionale.security;

import com.fasterxml.jackson.databind.JsonNode;
import it.giovannidefilippo.gestionale.common.OperationalMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "management.endpoints.web.exposure.include=health,prometheus",
                "management.prometheus.metrics.export.enabled=true"
        }
)
class ActuatorPortIsolationIntegrationTest {
    @LocalServerPort
    private int applicationPort;

    @Value("${local.management.port}")
    private int managementPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OperationalMetrics operationalMetrics;

    @Test
    void applicationPortDoesNotExposeActuator() {
        ResponseEntity<JsonNode> response = get(applicationPort, "/actuator/health/liveness");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asText()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void managementPortPublishesOnlyDetailFreeAvailabilityProbes() {
        ResponseEntity<JsonNode> liveness = get(managementPort, "/actuator/health/liveness");
        ResponseEntity<JsonNode> readiness = get(managementPort, "/actuator/health/readiness");
        ResponseEntity<JsonNode> genericHealth = get(managementPort, "/actuator/health");

        assertThat(liveness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(liveness.getBody()).isNotNull();
        assertThat(liveness.getBody().path("status").asText()).isEqualTo("UP");
        assertThat(liveness.getBody().has("components")).isFalse();

        assertThat(readiness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readiness.getBody()).isNotNull();
        assertThat(readiness.getBody().path("status").asText()).isEqualTo("UP");
        assertThat(readiness.getBody().has("components")).isFalse();

        assertThat(genericHealth.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }

    @Test
    void managementPortPublishesPrometheusMetricsWithoutExposingThemOnApplicationPort() {
        operationalMetrics.recordAuthentication(OperationalMetrics.AuthenticationOutcome.SUCCESS);

        ResponseEntity<String> managementResponse = restTemplate.getForEntity(
                "http://127.0.0.1:" + managementPort + "/actuator/prometheus",
                String.class
        );
        ResponseEntity<JsonNode> applicationResponse = get(applicationPort, "/actuator/prometheus");

        assertThat(managementResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(managementResponse.getBody()).contains("jvm_memory_used_bytes");
        assertThat(managementResponse.getBody()).contains("gestionale_authentication_attempts_total");
        assertThat(applicationResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<JsonNode> get(int port, String path) {
        return restTemplate.getForEntity("http://127.0.0.1:" + port + path, JsonNode.class);
    }
}
