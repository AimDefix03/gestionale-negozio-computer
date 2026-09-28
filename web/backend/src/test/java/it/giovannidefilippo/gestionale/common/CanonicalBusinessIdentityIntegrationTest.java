package it.giovannidefilippo.gestionale.common;

import it.giovannidefilippo.gestionale.partner.BusinessPartnerRequest;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerService;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerType;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductResponse;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.UserRole;
import it.giovannidefilippo.gestionale.user.UserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("postgresql")
class CanonicalBusinessIdentityIntegrationTest extends PostgreSqlIntegrationTestSupport {
    private static final String PASSWORD = "Valid-Credential-9842!";

    @Autowired
    private ProductService productService;

    @Autowired
    private BusinessPartnerService partnerService;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preservesDisplayValuesButResolvesAndRejectsCanonicalDuplicates() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String productCode = "Display-Product-" + suffix;
        String partnerCode = "Display-Partner-" + suffix;
        String username = "Display_User_" + suffix;

        ProductResponse product = productService.create(productRequest("  " + productCode + "  "));
        partnerService.create(partnerRequest("  " + partnerCode + "  "), "test-actor", "Admin");
        userService.registerPublic("  " + username + "  ", PASSWORD);

        assertThat(product.code()).isEqualTo(productCode);
        assertThat(productService.findByCode("  " + productCode.toUpperCase() + "  ").code()).isEqualTo(productCode);
        assertThat(partnerService.findByCode("  " + partnerCode.toUpperCase() + "  ").code()).isEqualTo(partnerCode);
        assertThat(userService.login("  " + username.toUpperCase() + "  ", PASSWORD).username()).isEqualTo(username);
        assertThat(canonical("products", "code", productCode)).isEqualTo(productCode.toLowerCase());
        assertThat(canonical("business_partners", "code", partnerCode)).isEqualTo(partnerCode.toLowerCase());
        assertThat(canonical("user_accounts", "username", username)).isEqualTo(username.toLowerCase());

        assertThatThrownBy(() -> productService.create(productRequest(" " + productCode.toUpperCase() + " ")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("codice prodotto");
        assertThatThrownBy(() -> partnerService.create(partnerRequest(" " + partnerCode.toUpperCase() + " "), "test-actor", "Admin"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("codice anagrafica");
        assertThatThrownBy(() -> userService.registerPublic(" " + username.toUpperCase() + " ", PASSWORD))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("username");
    }

    @Test
    void databaseAllowsOnlyOneConcurrentProductCodeVariant() throws Exception {
        String code = "CONCURRENT-PRODUCT-" + UUID.randomUUID().toString().substring(0, 8);

        assertSingleWinner(
                () -> productService.create(productRequest(code)),
                () -> productService.create(productRequest(" " + code.toLowerCase() + " "))
        );
    }

    @Test
    void databaseAllowsOnlyOneConcurrentPartnerCodeVariant() throws Exception {
        String code = "CONCURRENT-PARTNER-" + UUID.randomUUID().toString().substring(0, 8);

        assertSingleWinner(
                () -> partnerService.create(partnerRequest(code), "test-actor", "Admin"),
                () -> partnerService.create(partnerRequest(" " + code.toLowerCase() + " "), "test-actor", "Admin")
        );
    }

    @Test
    void databaseAllowsOnlyOneConcurrentUsernameVariant() throws Exception {
        String username = "concurrent_user_" + UUID.randomUUID().toString().substring(0, 8);

        assertSingleWinner(
                () -> userService.registerPublic(username, PASSWORD),
                () -> userService.registerPublic(" " + username.toUpperCase() + " ", PASSWORD)
        );
    }

    @Test
    void canonicalDuplicateIsExposedAsUniformHttpConflict() throws Exception {
        String username = "http_conflict_" + UUID.randomUUID().toString().substring(0, 8);
        userService.registerPublic(username, PASSWORD);

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "  %s  ",
                                  "password": "%s"
                                }
                                """.formatted(username.toUpperCase(), PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Esiste gia un account con questo username."));
    }

    private String canonical(String table, String displayColumn, String displayValue) {
        String canonicalColumn = displayColumn + "_canonical";
        return jdbc.queryForObject(
                "select " + canonicalColumn + " from " + table + " where " + displayColumn + " = ?",
                String.class,
                displayValue
        );
    }

    private void assertSingleWinner(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Outcome> firstResult = executor.submit(concurrent(first, ready, start));
            Future<Outcome> secondResult = executor.submit(concurrent(second, ready, start));
            ready.await();
            start.countDown();

            assertThat(List.of(firstResult.get(), secondResult.get()))
                    .containsExactlyInAnyOrder(Outcome.CREATED, Outcome.CONFLICT);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Outcome> concurrent(Callable<?> operation, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                operation.call();
                return Outcome.CREATED;
            } catch (ResourceConflictException exception) {
                return Outcome.CONFLICT;
            }
        };
    }

    private ProductRequest productRequest(String code) {
        return new ProductRequest(
                code,
                "Canonical product",
                "Canonical identity integration test",
                ProductCategory.HARDWARE,
                "Test brand",
                "Test type",
                "",
                new BigDecimal("100.00"),
                BigDecimal.ZERO
        );
    }

    private BusinessPartnerRequest partnerRequest(String code) {
        return new BusinessPartnerRequest(
                code,
                BusinessPartnerType.SUPPLIER,
                "Canonical partner",
                "",
                "",
                "canonical@example.test",
                "",
                "",
                "",
                ""
        );
    }

    private enum Outcome {
        CREATED,
        CONFLICT
    }
}
