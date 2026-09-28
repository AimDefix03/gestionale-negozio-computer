package it.giovannidefilippo.gestionale.order;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
class FinancialReconciliationMetrics {
    private final Map<FinancialMismatchType, AtomicLong> mismatchGauges = new EnumMap<>(FinancialMismatchType.class);
    private final AtomicLong totalMismatches = new AtomicLong();
    private final AtomicLong lastSuccessEpochSeconds = new AtomicLong();
    private final Counter successfulRuns;
    private final Counter failedRuns;
    private final TimeProvider timeProvider;

    FinancialReconciliationMetrics(MeterRegistry meterRegistry, TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
        for (FinancialMismatchType type : FinancialMismatchType.values()) {
            AtomicLong gauge = new AtomicLong();
            mismatchGauges.put(type, gauge);
            Gauge.builder("gestionale.financial.reconciliation.mismatches", gauge, AtomicLong::get)
                    .tag("type", type.name())
                    .register(meterRegistry);
        }
        meterRegistry.gauge("gestionale.financial.reconciliation.detected.total", totalMismatches);
        meterRegistry.gauge("gestionale.financial.reconciliation.last.success.epoch.seconds", lastSuccessEpochSeconds);
        successfulRuns = Counter.builder("gestionale.financial.reconciliation.runs").tag("outcome", "success").register(meterRegistry);
        failedRuns = Counter.builder("gestionale.financial.reconciliation.runs").tag("outcome", "failure").register(meterRegistry);
    }

    void recordSuccess(FinancialReconciliationResponse report) {
        mismatchGauges.forEach((type, gauge) -> gauge.set(report.counts().getOrDefault(type, 0L)));
        totalMismatches.set(report.mismatchCount());
        lastSuccessEpochSeconds.set(timeProvider.instant().getEpochSecond());
        successfulRuns.increment();
    }

    void recordFailure() {
        failedRuns.increment();
    }
}
