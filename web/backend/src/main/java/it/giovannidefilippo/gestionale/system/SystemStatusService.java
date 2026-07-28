package it.giovannidefilippo.gestionale.system;

import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.user.SecurityMetricsService;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@Transactional(readOnly = true)
public class SystemStatusService {
    private final DataSource dataSource;
    private final Environment environment;
    private final AuditService auditService;
    private final SecurityMetricsService securityMetricsService;
    private final ApiErrorMonitor apiErrorMonitor;
    private final TimeProvider timeProvider;

    SystemStatusService(DataSource dataSource, Environment environment, AuditService auditService, SecurityMetricsService securityMetricsService, ApiErrorMonitor apiErrorMonitor, TimeProvider timeProvider) {
        this.dataSource = dataSource;
        this.environment = environment;
        this.auditService = auditService;
        this.securityMetricsService = securityMetricsService;
        this.apiErrorMonitor = apiErrorMonitor;
        this.timeProvider = timeProvider;
    }

    public SystemStatusResponse currentStatus() {
        SystemStatusResponse.DatabaseStatus database = databaseStatus();
        Runtime runtime = Runtime.getRuntime();
        SecurityMetricsService.SecurityMetrics securityMetrics = securityMetricsService.snapshot();
        LocalDateTime last24Hours = timeProvider.localDateTime().minusHours(24);

        return new SystemStatusResponse(
                "UP".equals(database.status()) ? "UP" : "DEGRADED",
                timeProvider.instant(),
                environment.getProperty("spring.application.name", "gestionale-api"),
                activeProfile(),
                ManagementFactory.getRuntimeMXBean().getUptime(),
                database,
                new SystemStatusResponse.RuntimeStatus(runtime.totalMemory() - runtime.freeMemory(), runtime.maxMemory(), runtime.availableProcessors()),
                new SystemStatusResponse.SecurityStatus(securityMetrics.activeSessions(), securityMetrics.lockedLoginAttempts(), securityMetrics.recentLoginAttempts()),
                new SystemStatusResponse.AuditStatus(
                        auditService.countBySeveritySince(AuditSeverity.CRITICAL, last24Hours),
                        auditService.countBySeveritySince(AuditSeverity.WARNING, last24Hours),
                        auditService.recentImportantEvents(8)
                ),
                apiErrorMonitor.recentErrors()
        );
    }

    private SystemStatusResponse.DatabaseStatus databaseStatus() {
        long started = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            String databaseName = connection.getCatalog() == null || connection.getCatalog().isBlank() ? "-" : connection.getCatalog();
            return new SystemStatusResponse.DatabaseStatus(valid ? "UP" : "DOWN", elapsedMs(started), databaseName, valid ? "Connessione disponibile" : "Connessione non valida");
        } catch (Exception exception) {
            return new SystemStatusResponse.DatabaseStatus("DOWN", elapsedMs(started), "-", exception.getClass().getSimpleName());
        }
    }

    private long elapsedMs(long started) {
        return Math.max(1, (System.nanoTime() - started) / 1_000_000);
    }

    private String activeProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return "default";
        }
        return String.join(", ", Arrays.stream(activeProfiles).sorted().toList());
    }
}
