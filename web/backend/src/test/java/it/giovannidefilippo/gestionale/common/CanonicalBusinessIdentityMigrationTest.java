package it.giovannidefilippo.gestionale.common;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanonicalBusinessIdentityMigrationTest {
    @Test
    void upgradesCleanHistoricalIdentifiersWithoutChangingDisplayValues() {
        TestDatabase database = database("canonical_clean_");
        database.migrateTo22();
        database.insertPartner(801, "  Partner-Display  ");

        Flyway upgrade = database.upgrade();

        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("23");
        assertThat(database.jdbc().queryForObject("select code from business_partners where id = 801", String.class))
                .isEqualTo("  Partner-Display  ");
        assertThat(database.jdbc().queryForObject("select code_canonical from business_partners where id = 801", String.class))
                .isEqualTo("partner-display");
    }

    @Test
    void refusesUpgradeWhenHistoricalIdentifiersCollideCanonically() {
        TestDatabase database = database("canonical_collision_");
        database.migrateTo22();
        database.insertPartner(811, "Partner-Collision");
        database.insertPartner(812, "  partner-collision  ");

        assertThatThrownBy(database::upgrade)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("V23");
    }

    private TestDatabase database(String prefix) {
        String name = prefix + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        return new TestDatabase(dataSource, new JdbcTemplate(dataSource));
    }

    private record TestDatabase(DriverManagerDataSource dataSource, JdbcTemplate jdbc) {
        void migrateTo22() {
            Flyway.configure().dataSource(dataSource).target("22").load().migrate();
        }

        Flyway upgrade() {
            Flyway flyway = Flyway.configure().dataSource(dataSource).target("23").load();
            flyway.migrate();
            return flyway;
        }

        void insertPartner(long id, String code) {
            jdbc.update(
                    "insert into business_partners (id, code, type, display_name, tax_code, vat_number, email, phone, address, city, notes, active, created_at, updated_at) values (?, ?, 'SUPPLIER', 'Fixture', '', '', '', '', '', '', '', true, ?, ?)",
                    id,
                    code,
                    LocalDateTime.of(2026, 1, 1, 10, 0),
                    LocalDateTime.of(2026, 1, 1, 10, 0)
            );
        }
    }
}
