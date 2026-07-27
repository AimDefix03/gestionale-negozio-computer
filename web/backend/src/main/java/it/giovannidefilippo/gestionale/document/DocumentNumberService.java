package it.giovannidefilippo.gestionale.document;

import it.giovannidefilippo.gestionale.company.CompanySettingsService;
import it.giovannidefilippo.gestionale.company.CompanySettingsSnapshot;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class DocumentNumberService {
    private final DocumentNumberCounterRepository repository;
    private final CompanySettingsService companySettingsService;
    private final TimeProvider timeProvider;

    DocumentNumberService(DocumentNumberCounterRepository repository, CompanySettingsService companySettingsService, TimeProvider timeProvider) {
        this.repository = repository;
        this.companySettingsService = companySettingsService;
        this.timeProvider = timeProvider;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    NumberedCompanySettings allocate(FiscalDocumentType type) {
        CompanySettingsSnapshot settings = companySettingsService.lockForDocumentNumbering();
        int fiscalYear = timeProvider.localDateTime().getYear();
        DocumentNumberCounterId id = new DocumentNumberCounterId(type, fiscalYear);
        DocumentNumberCounter counter = repository.findById(id)
                .orElseGet(() -> new DocumentNumberCounter(type, fiscalYear));
        long sequence = counter.takeNext();
        repository.save(counter);
        String prefix = type == FiscalDocumentType.SIMULATED_INVOICE ? settings.invoicePrefix() : settings.creditNotePrefix();
        String code = prefix + "-" + fiscalYear + "-" + String.format("%0" + settings.numberPadding() + "d", sequence);
        return new NumberedCompanySettings(new DocumentNumberAllocation(code, prefix, fiscalYear, sequence), settings);
    }

    record NumberedCompanySettings(DocumentNumberAllocation number, CompanySettingsSnapshot companySettings) {
    }
}
