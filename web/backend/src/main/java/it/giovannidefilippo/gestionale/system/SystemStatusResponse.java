package it.giovannidefilippo.gestionale.system;

import it.giovannidefilippo.gestionale.audit.AuditEventResponse;

import java.time.Instant;
import java.util.List;

public record SystemStatusResponse(
        String status,
        Instant timestamp,
        String application,
        String activeProfile,
        long uptimeMs,
        DatabaseStatus database,
        RuntimeStatus runtime,
        SecurityStatus security,
        AuditStatus audit,
        List<RecentApiError> recentErrors
) {
    public record DatabaseStatus(
            String status,
            long latencyMs,
            String name,
            String message
    ) {
    }

    public record RuntimeStatus(
            long usedMemoryBytes,
            long maxMemoryBytes,
            int availableProcessors
    ) {
    }

    public record SecurityStatus(
            long activeSessions,
            long lockedLoginAttempts,
            long recentLoginAttempts
    ) {
    }

    public record AuditStatus(
            long criticalLast24h,
            long warningsLast24h,
            List<AuditEventResponse> recentImportantEvents
    ) {
    }
}
