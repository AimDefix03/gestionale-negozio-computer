package it.giovannidefilippo.gestionale.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class OperationalMetrics {
    private final MeterRegistry meterRegistry;
    private final Map<AuthenticationOutcome, Counter> authenticationCounters;
    private final Map<SessionEvent, Counter> sessionCounters;

    OperationalMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.authenticationCounters = counters("gestionale.authentication.attempts", AuthenticationOutcome.values());
        this.sessionCounters = counters("gestionale.session.events", SessionEvent.values());
    }

    public void recordAuthentication(AuthenticationOutcome outcome) {
        authenticationCounters.get(outcome).increment();
    }

    public void recordSessionEvent(SessionEvent event) {
        sessionCounters.get(event).increment();
    }

    public void recordApiError(HttpStatus status, ApiErrorCode code) {
        meterRegistry.counter(
                "gestionale.api.errors",
                "status", Integer.toString(status.value()),
                "code", code.name().toLowerCase()
        ).increment();
    }

    private <E extends Enum<E>> Map<E, Counter> counters(String name, E[] values) {
        Map<E, Counter> counters = new EnumMap<>(values[0].getDeclaringClass());
        for (E value : values) {
            counters.put(value, Counter.builder(name)
                    .tag("outcome", value.name().toLowerCase())
                    .register(meterRegistry));
        }
        return counters;
    }

    public enum AuthenticationOutcome {
        SUCCESS,
        INVALID_CREDENTIALS,
        LOCKED
    }

    public enum SessionEvent {
        RENEWED,
        RENEWAL_FAILED,
        LOGOUT
    }
}
