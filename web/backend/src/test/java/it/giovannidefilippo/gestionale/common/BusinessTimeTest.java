package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessTimeTest {
    @Test
    void companyYearUsesConfiguredZoneAtNewYear() {
        Instant instant = Instant.parse("2026-12-31T23:30:00Z");

        assertThat(BusinessTime.yearAt(instant, "UTC")).isEqualTo(2026);
        assertThat(BusinessTime.yearAt(instant, "Europe/Rome")).isEqualTo(2027);
    }

    @Test
    void offsetReflectsRomeDaylightSavingTransition() {
        assertThat(BusinessTime.offsetFromUtc(LocalDateTime.parse("2026-03-29T00:30:00"), "Europe/Rome").getOffset())
                .isEqualTo(ZoneOffset.ofHours(1));
        assertThat(BusinessTime.offsetFromUtc(LocalDateTime.parse("2026-03-29T01:30:00"), "Europe/Rome").getOffset())
                .isEqualTo(ZoneOffset.ofHours(2));
    }
}
