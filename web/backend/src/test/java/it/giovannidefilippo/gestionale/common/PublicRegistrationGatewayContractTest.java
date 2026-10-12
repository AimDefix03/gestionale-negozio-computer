package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PublicRegistrationGatewayContractTest {
    @Test
    void registrationUsesAnIndependentConfigurableNginxRateLimit() throws Exception {
        String nginx = Files.readString(Path.of("../frontend/nginx.conf"));
        String entrypoint = Files.readString(Path.of("../frontend/docker-entrypoint.sh"));
        String dockerfile = Files.readString(Path.of("../frontend/Dockerfile"));
        String compose = Files.readString(Path.of("../../docker-compose.prod-like.yml"));
        String environmentExample = Files.readString(Path.of("../../.env.docker.example"));
        String runtimeVerification = Files.readString(Path.of("../../scripts/security/verify-registration-rate-limit.sh"));

        assertThat(nginx)
                .contains("zone=register_per_ip:10m rate=${NGINX_REGISTER_RATE}")
                .contains("location = /api/accounts/register")
                .contains("limit_req zone=register_per_ip burst=${NGINX_REGISTER_BURST} nodelay")
                .contains("error_page 429 = @register_rate_limited")
                .contains("location @register_rate_limited")
                .contains("\"path\":\"/api/accounts/register\"");
        assertThat(entrypoint)
                .contains("NGINX_REGISTER_RATE")
                .contains("NGINX_REGISTER_BURST")
                .contains("NGINX_REGISTER_RETRY_AFTER_SECONDS");
        assertThat(dockerfile)
                .contains("NGINX_REGISTER_RATE")
                .contains("NGINX_REGISTER_BURST")
                .contains("NGINX_REGISTER_RETRY_AFTER_SECONDS");
        assertThat(compose)
                .contains("GESTIONALE_REGISTER_RATE_LIMIT")
                .contains("GESTIONALE_REGISTER_RATE_BURST")
                .contains("GESTIONALE_REGISTER_RATE_RETRY_AFTER_SECONDS");
        assertThat(environmentExample)
                .contains("GESTIONALE_REGISTER_RATE_LIMIT=")
                .contains("GESTIONALE_REGISTER_RATE_BURST=")
                .contains("GESTIONALE_REGISTER_RATE_RETRY_AFTER_SECONDS=");
        assertThat(runtimeVerification)
                .contains("/api/accounts/register")
                .contains("\"status\":429")
                .contains("\"code\":\"RATE_LIMIT_EXCEEDED\"");
    }
}
