package it.giovannidefilippo.gestionale.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class FinancialReconciliationJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinancialReconciliationJob.class);
    private final FinancialReconciliationService service;

    FinancialReconciliationJob(FinancialReconciliationService service) {
        this.service = service;
    }

    @Scheduled(
            initialDelayString = "${app.financial-reconciliation.initial-delay:PT15M}",
            fixedDelayString = "${app.financial-reconciliation.fixed-delay:PT15M}"
    )
    void run() {
        try {
            FinancialReconciliationResponse report = service.reconcile();
            if (!report.balanced()) {
                LOGGER.warn("Financial reconciliation detected {} mismatches", report.mismatchCount());
            }
        } catch (RuntimeException exception) {
            service.recordFailure();
            LOGGER.error("Financial reconciliation failed", exception);
        }
    }
}
