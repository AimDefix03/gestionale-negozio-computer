package it.giovannidefilippo.gestionale.partner;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.common.DatabaseConstraintViolations;
import it.giovannidefilippo.gestionale.common.ResourceConflictException;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.user.UserResponse;
import it.giovannidefilippo.gestionale.user.UserService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final UserService userService;

    BusinessPartnerService(BusinessPartnerRepository repository, AuditService auditService, TimeProvider timeProvider, UserService userService) {
        this.repository = repository;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
        this.userService = userService;
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

    public BusinessPartnerResponse requireActiveCustomer(long id) {
        BusinessPartner partner = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Anagrafica cliente non trovata."));
        if (partner.getType() != BusinessPartnerType.CUSTOMER) {
            throw new IllegalArgumentException("L'anagrafica selezionata non e un cliente.");
        }
        if (!partner.isActive()) {
            throw new IllegalArgumentException("Il cliente selezionato non e attivo.");
        }
        return BusinessPartnerResponse.from(partner);
    }

    public BusinessPartnerResponse requireActiveSupplier(long id) {
        BusinessPartner partner = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Anagrafica fornitore non trovata."));
        if (partner.getType() != BusinessPartnerType.SUPPLIER) {
            throw new IllegalArgumentException("L'anagrafica selezionata non e un fornitore.");
        }
        if (!partner.isActive()) {
            throw new IllegalArgumentException("Il fornitore selezionato non e attivo.");
        }
        return BusinessPartnerResponse.from(partner);
    }

    public BusinessPartnerResponse findActiveCustomerByLinkedAccount(long accountId) {
        return repository.findByLinkedAccountId(accountId)
                .filter(BusinessPartner::isActive)
                .filter(partner -> partner.getType() == BusinessPartnerType.CUSTOMER)
                .map(BusinessPartnerResponse::from)
                .orElse(null);
    }

    @Transactional
    public BusinessPartnerResponse create(BusinessPartnerRequest request, String actor, String role) {
        BusinessPartner partner = saveAndFlush(new BusinessPartner(request, timeProvider.localDateTime()));
        auditService.record(actor, role, "CREATE_PARTNER", partner.getCode(), "Creata anagrafica " + partner.getDisplayName() + " - tipo " + partner.getType().getLabel(), AuditCategory.PARTNER, AuditSeverity.INFO, "BUSINESS_PARTNER");
        return BusinessPartnerResponse.from(partner);
    }

    @Transactional
    public BusinessPartnerResponse update(String code, BusinessPartnerRequest request, String actor, String role) {
        BusinessPartner partner = requirePartner(code);
        LocalDateTime now = timeProvider.localDateTime();
        partner.update(request, now);
        partner.reactivate(now);
        flushCanonicalCode();
        auditService.record(actor, role, "UPDATE_PARTNER", partner.getCode(), "Aggiornata anagrafica " + partner.getDisplayName() + " - tipo " + partner.getType().getLabel(), AuditCategory.PARTNER, AuditSeverity.INFO, "BUSINESS_PARTNER");
        return BusinessPartnerResponse.from(partner);
    }

    @Transactional
    public void deactivate(String code, String actor, String role) {
        BusinessPartner partner = requirePartner(code);
        partner.deactivate(timeProvider.localDateTime());
        auditService.record(actor, role, "DEACTIVATE_PARTNER", partner.getCode(), "Disattivata anagrafica " + partner.getDisplayName() + " - tipo " + partner.getType().getLabel(), AuditCategory.PARTNER, AuditSeverity.WARNING, "BUSINESS_PARTNER");
    }

    @Transactional
    public BusinessPartnerResponse linkCustomerAccount(String code, long accountId, String actor, String role) {
        BusinessPartner partner = requirePartner(code);
        UserResponse account = userService.requireCustomerAccount(accountId);
        repository.findByLinkedAccountId(accountId)
                .filter(existing -> !existing.getId().equals(partner.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("L'account cliente e gia collegato a un'altra anagrafica.");
                });
        partner.linkAccount(account.id(), timeProvider.localDateTime());
        auditService.record(actor, role, "LINK_PARTNER_ACCOUNT", partner.getCode(), "Collegato account cliente ID " + account.id(), AuditCategory.PARTNER, AuditSeverity.WARNING, "BUSINESS_PARTNER");
        return BusinessPartnerResponse.from(partner);
    }

    @Transactional
    public BusinessPartnerResponse unlinkCustomerAccount(String code, String actor, String role) {
        BusinessPartner partner = requirePartner(code);
        Long accountId = partner.getLinkedAccountId();
        partner.unlinkAccount(timeProvider.localDateTime());
        auditService.record(actor, role, "UNLINK_PARTNER_ACCOUNT", partner.getCode(), "Rimosso collegamento account cliente ID " + accountId, AuditCategory.PARTNER, AuditSeverity.WARNING, "BUSINESS_PARTNER");
        return BusinessPartnerResponse.from(partner);
    }

    private BusinessPartner requirePartner(String code) {
        return repository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Anagrafica non trovata."));
    }

    private BusinessPartner saveAndFlush(BusinessPartner partner) {
        try {
            return repository.saveAndFlush(partner);
        } catch (DataIntegrityViolationException exception) {
            throw translatePartnerConstraint(exception);
        }
    }

    private void flushCanonicalCode() {
        try {
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw translatePartnerConstraint(exception);
        }
    }

    private RuntimeException translatePartnerConstraint(DataIntegrityViolationException exception) {
        if (DatabaseConstraintViolations.matches(exception, "uk_business_partners_code", "uk_business_partners_code_canonical")) {
            return new ResourceConflictException("Esiste gia un'anagrafica con questo codice anagrafica.");
        }
        return exception;
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
