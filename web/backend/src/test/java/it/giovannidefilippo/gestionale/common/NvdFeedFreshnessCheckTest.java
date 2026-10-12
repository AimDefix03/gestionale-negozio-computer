package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class NvdFeedFreshnessCheckTest {
    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest
    @CsvSource({
            "recent, true", "escaped, true", "within-limit, true",
            "stale, false", "future, false", "missing-timestamp, false",
            "invalid-timestamp, false", "missing-file, false"
    })
    void rejectsUnavailableOrUntrustworthyFreshnessEvidence(String fixture, boolean accepted) throws Exception {
        Path metadata = temporaryDirectory.resolve("cache.properties");
        Instant now = Instant.now();
        String value = switch (fixture) {
            case "within-limit" -> now.minus(47, ChronoUnit.HOURS).toString();
            case "stale" -> now.minus(49, ChronoUnit.HOURS).toString();
            case "future" -> now.plus(1, ChronoUnit.HOURS).toString();
            case "invalid-timestamp" -> "not-a-timestamp";
            default -> now.minus(24, ChronoUnit.HOURS).toString();
        };
        if (fixture.equals("escaped")) value = value.replace(":", "\\:");
        if (!fixture.equals("missing-file")) {
            Files.writeString(metadata, fixture.equals("missing-timestamp") ? "prefix=nvdcve-\n"
                    : "lastModifiedDate=" + value + "\n");
        }
        Path output = temporaryDirectory.resolve("result.log");
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                Path.of("../../scripts/security/NvdFeedFreshnessCheck.java").toAbsolutePath().normalize().toString(),
                metadata.toString())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertThat(process.waitFor(20, TimeUnit.SECONDS)).isTrue();
            assertThat(process.exitValue() == 0).as("%s: %s", fixture, Files.readString(output)).isEqualTo(accepted);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
