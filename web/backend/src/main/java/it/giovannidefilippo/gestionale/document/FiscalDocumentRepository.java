package it.giovannidefilippo.gestionale.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

interface FiscalDocumentRepository extends JpaRepository<FiscalDocument, Long>, JpaSpecificationExecutor<FiscalDocument> {
    Optional<FiscalDocument> findByRelatedOrderCodeIgnoreCaseAndType(String relatedOrderCode, FiscalDocumentType type);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByFiscalYear(int fiscalYear);
}
