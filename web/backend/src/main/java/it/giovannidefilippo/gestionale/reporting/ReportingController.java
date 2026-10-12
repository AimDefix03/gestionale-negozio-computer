package it.giovannidefilippo.gestionale.reporting;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.order.OrderStatus;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
class ReportingController {
    private final ReportingService reportingService;
    private final ReportExportService exportService;
    private final AuthSessionService authSessionService;
    private final AuditService auditService;

    ReportingController(ReportingService reportingService, ReportExportService exportService, AuthSessionService authSessionService, AuditService auditService) {
        this.reportingService = reportingService;
        this.exportService = exportService;
        this.authSessionService = authSessionService;
        this.auditService = auditService;
    }

    @GetMapping("/sales")
    SalesReportResponse sales(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) OrderStatus status
    ) {
        authSessionService.requirePermission(token, UserPermission.VIEW_REPORTS);
        return reportingService.sales(from, to, status);
    }

    @GetMapping("/sales/export")
    ResponseEntity<byte[]> exportSales(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "CSV") ReportFormat format
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.VIEW_REPORTS);
        SalesReportResponse report = reportingService.sales(from, to, status);
        ReportFile file = exportService.sales(report, format);
        auditExport(actor, "EXPORT_SALES_REPORT", file, "Periodo " + report.from() + " - " + report.to() + ", stato " + report.status());
        return download(file);
    }

    @GetMapping("/inventory")
    InventoryReportResponse inventory(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(defaultValue = "ALL") String stock,
            @RequestParam(required = false) Boolean discontinued
    ) {
        authSessionService.requirePermission(token, UserPermission.VIEW_REPORTS);
        return reportingService.inventory(q, category, stock, discontinued);
    }

    @GetMapping("/inventory/export")
    ResponseEntity<byte[]> exportInventory(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(defaultValue = "ALL") String stock,
            @RequestParam(required = false) Boolean discontinued,
            @RequestParam(defaultValue = "CSV") ReportFormat format
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.VIEW_REPORTS);
        InventoryReportResponse report = reportingService.inventory(q, category, stock, discontinued);
        ReportFile file = exportService.inventory(report, format);
        auditExport(actor, "EXPORT_INVENTORY_REPORT", file, "Prodotti esportati: " + report.productCount());
        return download(file);
    }

    private void auditExport(AuthenticatedUser actor, String action, ReportFile file, String details) {
        auditService.record(
                actor.username(),
                actor.roleLabel(),
                action,
                file.filename(),
                details,
                AuditCategory.REPORT,
                AuditSeverity.INFO,
                "REPORT"
        );
    }

    private static ResponseEntity<byte[]> download(ReportFile file) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.content().length)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.content());
    }
}
