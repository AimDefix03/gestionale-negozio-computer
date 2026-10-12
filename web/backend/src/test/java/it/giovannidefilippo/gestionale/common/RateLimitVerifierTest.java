package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitVerifierTest {
    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest
    @CsvSource({
            "login, CRLF, true", "registration, CRLF, true",
            "login, LF, true", "registration, LF, true",
            "login, WRONG_DELAY, false", "registration, WRONG_DELAY, false",
            "login, MISSING_REQUEST_ID, false", "registration, MISSING_REQUEST_ID, false",
            "login, WRONG_CODE, false", "registration, WRONG_CODE, false",
            "login, NO_LIMIT, false", "registration, NO_LIMIT, false",
            "login, BAD_HEALTH, false", "registration, BAD_HEALTH, false",
            "login, UNEXPECTED_STATUS, false", "registration, UNEXPECTED_STATUS, false"
    })
    void verifiesGatewayResponseWithoutDependingOnGrepEscapeExtensions(
            String endpoint, String fixture, boolean accepted) throws Exception {
        Path curl = temporaryDirectory.resolve("curl");
        Files.copy(Path.of("src/test/resources/scripts/rate-limit-curl.sh"), curl);
        assertThat(curl.toFile().setExecutable(true, true)).isTrue();
        Path output = temporaryDirectory.resolve("result.log");
        ProcessBuilder builder = new ProcessBuilder("sh",
                Path.of("../../scripts/security/verify-" + endpoint + "-rate-limit.sh")
                        .toAbsolutePath().normalize().toString(), "http://fixture.invalid");
        builder.environment().put("PATH", temporaryDirectory + ":" + System.getenv("PATH"));
        builder.environment().put("RATE_LIMIT_FIXTURE", fixture);
        builder.environment().put("RATE_LIMIT_TEST_MAX_ATTEMPTS", "2");
        builder.environment().put("REGISTER_RATE_LIMIT_TEST_MAX_ATTEMPTS", "2");
        builder.environment().put("GESTIONALE_LOGIN_RETRY_AFTER_SECONDS", "60");
        builder.environment().put("GESTIONALE_REGISTER_RATE_RETRY_AFTER_SECONDS", "60");
        builder.redirectErrorStream(true).redirectOutput(output.toFile());
        Process process = builder.start();
        try {
            assertThat(process.waitFor(10, TimeUnit.SECONDS)).isTrue();
            assertThat(process.exitValue() == 0)
                    .as("%s %s: %s", endpoint, fixture, Files.readString(output))
                    .isEqualTo(accepted);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
