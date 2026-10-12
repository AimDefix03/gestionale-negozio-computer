package it.giovannidefilippo.gestionale.document;

import it.giovannidefilippo.gestionale.company.CompanySettingsService;
import it.giovannidefilippo.gestionale.company.CompanySettingsSnapshot;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.common.BusinessTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class DocumentNumberService {
    private static final int MAX_COLLISION_SKIPS = 10_000;

    private final DocumentNumberCounterRepository repository;
    private final FiscalDocumentRepository fiscalDocumentRepository;
    private final CompanySettingsService companySettingsService;
    private final TimeProvider timeProvider;

    DocumentNumberService(DocumentNumberCounterRepository repository, FiscalDocumentRepository fiscalDocumentRepository, CompanySettingsService companySettingsService, TimeProvider timeProvider) {
        this.repository = repository;
        this.fiscalDocumentRepository = fiscalDocumentRepository;
        this.companySettingsService = companySettingsService;
        this.timeProvider = timeProvider;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    NumberedCompanySettings allocate(FiscalDocumentType type) {
        CompanySettingsSnapshot settings = companySettingsService.lockForDocumentNumbering();
        int fiscalYear = BusinessTime.yearAt(timeProvider.instant(), settings.timeZone());
        DocumentNumberCounterId id = new DocumentNumberCounterId(type, fiscalYear);
        DocumentNumberCounter counter = repository.findById(id)
                .orElseGet(() -> new DocumentNumberCounter(type, fiscalYear));
        String prefix = type == FiscalDocumentType.SIMULATED_INVOICE ? settings.invoicePrefix() : settings.creditNotePrefix();
        for (int attempt = 0; attempt < MAX_COLLISION_SKIPS; attempt++) {
            long sequence = counter.takeNext();
            String code = prefix + "-" + fiscalYear + "-" + String.format("%0" + settings.numberPadding() + "d", sequence);
            if (!fiscalDocumentRepository.existsByCodeIgnoreCase(code)) {
                repository.save(counter);
                return new NumberedCompanySettings(new DocumentNumberAllocation(code, prefix, fiscalYear, sequence), settings);
            }
        }
        throw new IllegalStateException("Numerazione documentale bloccata: troppe collisioni storiche consecutive.");
    }

    record NumberedCompanySettings(DocumentNumberAllocation number, CompanySettingsSnapshot companySettings) {
    }
}
