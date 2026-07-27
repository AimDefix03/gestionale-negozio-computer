package it.giovannidefilippo.gestionale.document;

import it.giovannidefilippo.gestionale.company.DocumentNumberingUsage;
import org.springframework.stereotype.Component;

@Component
class FiscalDocumentCompanyUsage implements DocumentNumberingUsage {
    private final FiscalDocumentRepository repository;

    FiscalDocumentCompanyUsage(FiscalDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsForFiscalYear(int fiscalYear) {
        return repository.existsByFiscalYear(fiscalYear);
    }
}
