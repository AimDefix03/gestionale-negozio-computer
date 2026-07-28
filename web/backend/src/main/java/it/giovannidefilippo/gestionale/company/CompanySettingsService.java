package it.giovannidefilippo.gestionale.company;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.ResourceConflictException;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanySettingsService {
    private final CompanySettingsRepository repository;
    private final DocumentNumberingUsage documentNumberingUsage;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    CompanySettingsService(CompanySettingsRepository repository, DocumentNumberingUsage documentNumberingUsage, AuditService auditService, TimeProvider timeProvider) {
        this.repository = repository;
        this.documentNumberingUsage = documentNumberingUsage;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    public CompanySettingsResponse current() {
        return CompanySettingsResponse.from(requireCurrent());
    }

    @Transactional
    public CompanySettingsResponse update(CompanySettingsRequests.UpdateRequest request, AuthenticatedUser actor) {
        CompanySettings settings = requireCurrentForUpdate();
        if (settings.getVersion() != request.version()) {
            throw new ResourceConflictException("La configurazione aziendale e stata modificata da un'altra sessione. Ricarica i dati e riprova.");
        }
        validate(request);
        int fiscalYear = timeProvider.localDateTime().getYear();
        boolean numberingChanged = !settings.getInvoicePrefix().equalsIgnoreCase(request.invoicePrefix())
                || !settings.getCreditNotePrefix().equalsIgnoreCase(request.creditNotePrefix())
                || settings.getNumberPadding() != request.numberPadding();
        if (numberingChanged && documentNumberingUsage.existsForFiscalYear(fiscalYear)) {
            throw new ResourceConflictException("Prefissi e lunghezza dei progressivi non possono cambiare dopo l'emissione del primo documento dell'esercizio " + fiscalYear + ".");
        }
        settings.update(request, actor.username(), timeProvider.localDateTime());
        CompanySettings saved = repository.saveAndFlush(settings);
        auditService.record(actor.username(), actor.roleLabel(), "UPDATE_COMPANY_SETTINGS", "COMPANY_SETTINGS", "Configurazione aziendale aggiornata; IVA predefinita " + saved.getDefaultVatRate() + ", prefissi " + saved.getInvoicePrefix() + "/" + saved.getCreditNotePrefix(), AuditCategory.SYSTEM, AuditSeverity.WARNING, "COMPANY_SETTINGS");
        return CompanySettingsResponse.from(saved);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public CompanySettingsSnapshot lockForDocumentNumbering() {
        return requireCurrentForUpdate().snapshot();
    }

    private CompanySettings requireCurrent() {
        return repository.findById(CompanySettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Configurazione aziendale non inizializzata."));
    }

    private CompanySettings requireCurrentForUpdate() {
        return repository.findByIdForUpdate(CompanySettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Configurazione aziendale non inizializzata."));
    }

    private static void validate(CompanySettingsRequests.UpdateRequest request) {
        String invoicePrefix = request.invoicePrefix().trim();
        String creditPrefix = request.creditNotePrefix().trim();
        if (invoicePrefix.equalsIgnoreCase(creditPrefix)) {
            throw new IllegalArgumentException("I prefissi di fatture e note credito devono essere diversi.");
        }
        if (request.defaultVatRate().scale() > 4) {
            throw new IllegalArgumentException("L'aliquota IVA puo avere al massimo quattro decimali.");
        }
    }
}
