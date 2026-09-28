package it.giovannidefilippo.gestionale.common;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRulesException;

public final class BusinessTime {
    private BusinessTime() {
    }

    public static ZoneId requireZoneId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Il fuso orario aziendale e obbligatorio.");
        }
        try {
            return ZoneId.of(value.trim());
        } catch (ZoneRulesException exception) {
            throw new IllegalArgumentException("Il fuso orario aziendale non e valido: " + value.trim() + ".");
        }
    }

    public static int yearAt(Instant instant, String zoneId) {
        return instant.atZone(requireZoneId(zoneId)).getYear();
    }

    public static OffsetDateTime offsetFromUtc(LocalDateTime utcDateTime, String zoneId) {
        if (utcDateTime == null) {
            return null;
        }
        return utcDateTime.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(requireZoneId(zoneId))
                .toOffsetDateTime();
    }

    public static OffsetDateTime utcOffset(LocalDateTime utcDateTime) {
        return utcDateTime == null ? null : utcDateTime.atOffset(ZoneOffset.UTC);
    }
}
