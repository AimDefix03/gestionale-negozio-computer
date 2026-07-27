package it.giovannidefilippo.gestionale.partner;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class BusinessPartnerService {
    private final BusinessPartnerRepository repository;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    BusinessPartnerService(BusinessPartnerRepository repository, AuditService auditService, TimeProvider timeProvider) {
        this.repository = repository;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    public PageResponse<BusinessPartnerResponse> search(String q, BusinessPartnerType type, Boolean active, int page, int size) {
        return PageResponse.from(repository.findAll(specification(q, type, active), PageRequests.of(page, size, Sort.by("displayName").ascending().and(Sort.by("code").ascending())))
                .map(BusinessPartnerResponse::from));
    }

    public BusinessPartnerResponse findByCode(String code) {
        return BusinessPartnerResponse.from(requirePartner(code));
    }

    public BusinessPartnerResponse requireActiveCustomer(String code) {
        BusinessPartner partner = requirePartner(code);
        if (partner.getType() != BusinessPartnerType.CUSTOMER) {
            throw new IllegalArgumentException("L'anagrafica selezionata non e un cliente.");
        }
        if (!partner.isActive()) {
            throw new IllegalArgumentException("Il cliente selezionato non e attivo.");
        }
        return BusinessPartnerResponse.from(partner);
    }

    @Transactional
    public BusinessPartnerResponse create(BusinessPartnerRequest request, String actor, String role) {
        if (repository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Esiste gia un'anagrafica con questo codice.");
        }
        BusinessPartner partner = repository.save(new BusinessPartner(request, timeProvider.localDateTime()));
        auditService.record(actor, role, "CREATE_PARTNER", partner.getCode(), "Creata anagrafica " + partner.getDisplayName() + " - tipo " + partner.getType().getLabel(), AuditCategory.PARTNER, AuditSeverity.INFO, "BUSINESS_PARTNER");
        return BusinessPartnerResponse.from(partner);
    }

    @Transactional
    public BusinessPartnerResponse update(String code, BusinessPartnerRequest request, String actor, String role) {
        BusinessPartner partner = requirePartner(code);
        validateUniqueCode(partner, request.code());
        LocalDateTime now = timeProvider.localDateTime();
        partner.update(request, now);
        partner.reactivate(now);
        auditService.record(actor, role, "UPDATE_PARTNER", partner.getCode(), "Aggiornata anagrafica " + partner.getDisplayName() + " - tipo " + partner.getType().getLabel(), AuditCategory.PARTNER, AuditSeverity.INFO, "BUSINESS_PARTNER");
        return BusinessPartnerResponse.from(partner);
    }

    @Transactional
    public void deactivate(String code, String actor, String role) {
        BusinessPartner partner = requirePartner(code);
        partner.deactivate(timeProvider.localDateTime());
        auditService.record(actor, role, "DEACTIVATE_PARTNER", partner.getCode(), "Disattivata anagrafica " + partner.getDisplayName() + " - tipo " + partner.getType().getLabel(), AuditCategory.PARTNER, AuditSeverity.WARNING, "BUSINESS_PARTNER");
    }

    private BusinessPartner requirePartner(String code) {
        return repository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Anagrafica non trovata."));
    }

    private void validateUniqueCode(BusinessPartner partner, String requestedCode) {
        repository.findByCodeIgnoreCase(requestedCode.trim())
                .filter(existing -> !existing.getId().equals(partner.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Esiste gia un'anagrafica con questo codice.");
                });
    }

    private Specification<BusinessPartner> specification(String q, BusinessPartnerType type, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(q)) {
                String term = contains(q);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("displayName")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("taxCode")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("vatNumber")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("city")), term)
                ));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
