package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalFixtureContractTest {
    private static final String ROOT = "db/fixtures/";
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+");
    private static final List<String> DATA_FILES = List.of(
            "manifest.json",
            "v14/base.sql",
            "v16/delta.sql",
            "v18/delta.sql",
            "volume/core.sql",
            "assertions/v14.sql",
            "assertions/v16.sql",
            "assertions/v18.sql",
            "assertions/v19.sql",
            "assertions/v20.sql",
            "assertions/v21.sql",
            "assertions/v22.sql",
            "remediation/pre-v23-canonical-resolution.sql",
            "assertions/v29.sql",
            "assertions/v30.sql",
            "assertions/v31.sql",
            "assertions/v32.sql",
            "assertions/v33.sql",
            "assertions/v34.sql",
            "assertions/v35.sql",
            "profiles/small.properties",
            "profiles/medium.properties",
            "profiles/large.properties"
    );

    @Test
    void fixtureChecksumsMatchTheVersionedManifest() throws Exception {
        List<String> checksumLines = readText("SHA256SUMS").lines()
                .filter(line -> !line.isBlank())
                .toList();

        assertThat(checksumLines).hasSize(DATA_FILES.size());
        for (String file : DATA_FILES) {
            String expected = checksumLines.stream()
                    .filter(line -> line.endsWith("  " + file))
                    .map(line -> line.substring(0, line.indexOf(' ')))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Checksum mancante per " + file));
            assertThat(sha256(readBytes(file))).isEqualTo(expected);
        }
    }

    @Test
    void fixtureManifestCoversHistoricalRiskScenarios() throws IOException {
        String manifest = readText("manifest.json");

        assertThat(manifest)
                .contains("\"version\": 14", "\"version\": 16", "\"version\": 18", "\"version\": 19", "\"version\": 20", "\"version\": 21", "\"version\": 22", "\"version\": 30", "\"version\": 31", "\"version\": 32", "\"version\": 33", "\"version\": 34", "\"version\": 35")
                .contains("all-order-statuses")
                .contains("case-insensitive-collisions")
                .contains("inventory-ledger-drift")
                .contains("paid-unfulfilled-order")
                .contains("financial-reconciliation-mismatch")
                .contains("legacy-document-numbering-collision")
                .contains("populated-upgrade-v30")
                .contains("populated-upgrade-v31")
                .contains("populated-upgrade-v32")
                .contains("populated-upgrade-v33")
                .contains("populated-upgrade-v34")
                .contains("populated-upgrade-v35")
                .contains("concurrent-login-attempt-merge")
                .contains("\"F-01\"", "\"F-02\"", "\"F-05\"", "\"F-06\"", "\"F-07\"")
                .contains("\"F-08\"", "\"F-09\"", "\"F-10\"", "\"F-11\"", "\"F-13\"")
                .contains("\"F-20A\"", "\"F-21\"", "\"F-22\"", "\"F-27\"", "\"F-28\"")
                .contains("\"F-29\"", "\"F-31\"", "\"F-31A\"", "\"F-31B\"");
    }

    @Test
    void fixtureDataIsSyntheticAndContainsNoUsableCredentials() throws IOException {
        List<String> contents = new ArrayList<>();
        for (String file : DATA_FILES) {
            contents.add(readText(file));
        }
        String combined = String.join("\n", contents);
        String normalized = combined.toLowerCase();

        assertThat(normalized)
                .doesNotContain("giovanni")
                .doesNotContain("de filippo")
                .doesNotContain("aim_defix")
                .doesNotContain("@gmail.")
                .doesNotContain("@hotmail.")
                .doesNotContain("@outlook.")
                .doesNotContain("$2a$")
                .doesNotContain("$2b$")
                .doesNotContain("$2y$");

        Matcher matcher = EMAIL.matcher(combined);
        while (matcher.find()) {
            assertThat(matcher.group()).endsWith("@example.invalid");
        }
        assertThat(readText("manifest.json")).contains("\"validCredentials\": false");
    }

    @Test
    void volumeProfilesAreOrderedAndBounded() throws IOException {
        VolumeProfile small = readProfile("small");
        VolumeProfile medium = readProfile("medium");
        VolumeProfile large = readProfile("large");

        assertThat(small.products()).isLessThan(medium.products());
        assertThat(medium.products()).isLessThan(large.products());
        assertThat(small.partners()).isLessThan(medium.partners());
        assertThat(medium.partners()).isLessThan(large.partners());
        assertThat(small.orders()).isLessThan(medium.orders());
        assertThat(medium.orders()).isLessThan(large.orders());
        assertThat(large.products()).isLessThanOrEqualTo(5_000);
        assertThat(large.partners()).isLessThanOrEqualTo(2_500);
        assertThat(large.orders()).isLessThanOrEqualTo(50_000);
    }

    @Test
    void fixtureSqlContainsRequiredWorkflowStates() throws IOException {
        assertThat(readText("v14/base.sql"))
                .contains("'DRAFT'", "'CONFIRMED'", "'FULFILLED'", "'CANCELED'")
                .contains("'SIMULATED_INVOICE'", "'SIMULATED_CREDIT_NOTE'")
                .contains("'IN_PROGRESS'", "'COMPLETED'");
        assertThat(readText("v16/delta.sql"))
                .contains("'PAID'", "'PARTIALLY_PAID'", "'PARTIALLY_REFUNDED'")
                .contains("'REQUESTED'", "'REJECTED'")
                .contains("'RECEIPT'", "'REFUND'");
        assertThat(readText("v18/delta.sql"))
                .contains("company_settings", "last_used_at");
        assertThat(readText("assertions/v19.sql"))
                .contains("operational_access_verified", "QUARANTINE_OPERATIONAL_ACCOUNT");
        assertThat(readText("assertions/v20.sql"))
                .contains("account_id", "credential_version", "username_snapshot");
        assertThat(readText("assertions/v21.sql"))
                .contains("customer_account_id", "partner_id", "ownership_status")
                .contains("linked_account_id", "legacy textual data");
        assertThat(readText("assertions/v22.sql"))
                .contains("UNRECONCILED", "document_number_counters", "maximum_sequence")
                .contains("reconciliation_reason", "preserve payment states");
        assertThat(readText("remediation/pre-v23-canonical-resolution.sql"))
                .contains("account_id = 1201", "partner_id = 1302", "keeper.quantity + duplicate.quantity");
        assertThat(readText("assertions/v29.sql"))
                .contains("canonical business identifier collisions", "authoritative inventory baseline")
                .contains("durable idempotency claims", "financial reconciliation mismatch")
                .contains("merge concurrent login-attempt history deterministically");
        assertThat(readText("assertions/v30.sql"))
                .contains("customer classification", "must not infer customer channel")
                .contains("description snapshot field");
        assertThat(readText("assertions/v31.sql"))
                .contains("account lifecycle metadata", "historical accounts must remain enabled")
                .contains("chk_user_accounts_lifecycle", "idx_user_accounts_enabled_role");
        assertThat(readText("assertions/v32.sql"))
                .contains("company settings without a time zone", "legacy document time zones as UTC")
                .contains("chk_company_settings_time_zone_not_blank", "chk_fiscal_documents_snapshot_time_zone_not_blank");
        assertThat(readText("assertions/v33.sql"))
                .contains("supplier-order relations missing", "must not infer supplier orders from historical sales")
                .contains("chk_supplier_orders_status", "chk_supplier_order_items_quantities");
        assertThat(readText("assertions/v34.sql"))
                .contains("must not infer inventory cost from historical stock or selling prices", "legacy unposted evidence")
                .contains("chk_products_purchase_costs", "chk_supplier_receipt_items_posting");
        assertThat(readText("assertions/v35.sql"))
                .contains("must not infer physical inventory sessions from historical stock", "approval separation")
                .contains("chk_physical_inventory_sessions_lifecycle", "chk_stock_movements_physical_inventory");
    }

    private VolumeProfile readProfile(String name) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = new ClassPathResource(ROOT + "profiles/" + name + ".properties").getInputStream()) {
            properties.load(input);
        }
        return new VolumeProfile(
                Integer.parseInt(properties.getProperty("PRODUCT_COUNT")),
                Integer.parseInt(properties.getProperty("PARTNER_COUNT")),
                Integer.parseInt(properties.getProperty("ORDER_COUNT"))
        );
    }

    private byte[] readBytes(String file) throws IOException {
        try (InputStream input = new ClassPathResource(ROOT + file).getInputStream()) {
            return input.readAllBytes();
        }
    }

    private String readText(String file) throws IOException {
        return new String(readBytes(file), StandardCharsets.UTF_8);
    }

    private String sha256(byte[] content) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }

    private record VolumeProfile(int products, int partners, int orders) {
    }
}
