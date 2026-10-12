package it.giovannidefilippo.gestionale.common;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class TimeProvider {
    private final Clock clock;

    TimeProvider(Clock clock) {
        this.clock = clock;
    }

    public Instant instant() {
        return clock.instant();
    }

    public LocalDateTime localDateTime() {
        return LocalDateTime.ofInstant(instant(), ZoneOffset.UTC);
    }
}
