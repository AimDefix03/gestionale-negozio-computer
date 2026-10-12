import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

public final class NvdFeedFreshnessCheck {
    public static void main(String[] args) {
        try {
            if (args.length != 1) throw new IllegalArgumentException("Expected one metadata file.");
            Properties metadata = new Properties();
            try (InputStream input = Files.newInputStream(Path.of(args[0]))) {
                metadata.load(input);
            }
            String value = metadata.getProperty("lastModifiedDate");
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing NVD feed timestamp.");
            }
            Instant updatedAt = Instant.parse(value);
            Duration age = Duration.between(updatedAt, Instant.now());
            if (age.compareTo(Duration.ofHours(48)) > 0) {
                throw new IllegalArgumentException("NVD feed is older than 48 hours.");
            }
            if (age.compareTo(Duration.ofMinutes(-5)) < 0) {
                throw new IllegalArgumentException("NVD feed timestamp is in the future.");
            }
            System.out.println("NVD feed freshness verified: " + updatedAt);
        } catch (IOException | RuntimeException failure) {
            System.err.println("NVD feed freshness check failed: " + failure.getMessage());
            System.exit(1);
        }
    }
}
