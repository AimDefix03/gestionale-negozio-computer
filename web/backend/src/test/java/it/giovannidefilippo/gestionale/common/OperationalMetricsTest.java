package it.giovannidefilippo.gestionale.common;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalMetricsTest {
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final OperationalMetrics metrics = new OperationalMetrics(meterRegistry);

    @Test
    void recordsOnlyFiniteAuthenticationAndSessionLabels() {
        metrics.recordAuthentication(OperationalMetrics.AuthenticationOutcome.SUCCESS);
        metrics.recordAuthentication(OperationalMetrics.AuthenticationOutcome.INVALID_CREDENTIALS);
        metrics.recordSessionEvent(OperationalMetrics.SessionEvent.RENEWAL_FAILED);

        assertThat(counter("gestionale.authentication.attempts", "outcome", "success")).isEqualTo(1);
        assertThat(counter("gestionale.authentication.attempts", "outcome", "invalid_credentials")).isEqualTo(1);
        assertThat(counter("gestionale.authentication.attempts", "outcome", "locked")).isZero();
        assertThat(counter("gestionale.session.events", "outcome", "renewal_failed")).isEqualTo(1);
    }

    @Test
    void recordsApiErrorsWithoutUserOrRequestLabels() {
        metrics.recordApiError(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR);

        var counter = meterRegistry.find("gestionale.api.errors")
                .tags("status", "500", "code", "internal_error")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
        assertThat(counter.getId().getTags())
                .extracting(tag -> tag.getKey())
                .containsExactlyInAnyOrder("status", "code");
    }

    private double counter(String name, String tag, String value) {
        return meterRegistry.get(name).tag(tag, value).counter().count();
    }
}
