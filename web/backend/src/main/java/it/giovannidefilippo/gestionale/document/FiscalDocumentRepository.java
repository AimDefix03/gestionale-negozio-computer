package it.giovannidefilippo.gestionale.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface FiscalDocumentRepository extends JpaRepository<FiscalDocument, Long>, JpaSpecificationExecutor<FiscalDocument> {
    Optional<FiscalDocument> findByRelatedOrderCodeIgnoreCaseAndType(String relatedOrderCode, FiscalDocumentType type);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByFiscalYear(int fiscalYear);

    @Query("select document.relatedOrderCode from FiscalDocument document where document.type = :type and document.relatedOrderCode in :orderCodes")
    List<String> findRelatedOrderCodesByTypeAndRelatedOrderCodeIn(
            @Param("type") FiscalDocumentType type,
            @Param("orderCodes") Collection<String> orderCodes
    );
}
