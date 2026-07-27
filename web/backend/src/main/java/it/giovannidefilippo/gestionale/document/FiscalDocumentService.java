package it.giovannidefilippo.gestionale.document;

import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.common.ResourceConflictException;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.order.CustomerOrder;
import it.giovannidefilippo.gestionale.order.OrderItem;
import it.giovannidefilippo.gestionale.order.OrderService;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerResponse;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class FiscalDocumentService {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final String DISCLAIMER = "DOCUMENTO SIMULATO - NON VALIDO AI FINI FISCALI";
    private final FiscalDocumentRepository repository;
    private final OrderService orderService;
    private final BusinessPartnerService partnerService;
    private final AuditService auditService;
    private final DocumentNumberService documentNumberService;
    private final TimeProvider timeProvider;

    FiscalDocumentService(FiscalDocumentRepository repository, OrderService orderService, BusinessPartnerService partnerService, AuditService auditService, DocumentNumberService documentNumberService, TimeProvider timeProvider) {
        this.repository = repository;
        this.orderService = orderService;
        this.partnerService = partnerService;
        this.auditService = auditService;
        this.documentNumberService = documentNumberService;
        this.timeProvider = timeProvider;
    }

    public List<FiscalDocumentResponse> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(FiscalDocument::getCreatedAt).reversed())
                .map(FiscalDocumentResponse::from)
                .toList();
    }

    public PageResponse<FiscalDocumentResponse> search(String q, FiscalDocumentType type, int page, int size) {
        return PageResponse.from(repository.findAll(
                specification(q, type),
                PageRequests.of(page, size, Sort.by("createdAt").descending())
        ).map(FiscalDocumentResponse::from));
    }

    @Transactional
    public FiscalDocumentResponse createInvoice(FiscalDocumentRequests.CreateInvoiceRequest request, AuthenticatedUser actor) {
        CustomerOrder order = orderService.requireOrder(request.orderCode());
        if (!order.isFulfilled()) {
            throw new IllegalArgumentException("Puoi generare la fattura simulata solo dopo aver evaso l'ordine.");
        }
        if (repository.findByRelatedOrderCodeIgnoreCaseAndType(order.getCode(), FiscalDocumentType.SIMULATED_INVOICE).isPresent()) {
            throw new ResourceConflictException("Esiste già una fattura simulata per questo ordine.");
        }
        DocumentNumberService.NumberedCompanySettings numberedSettings = documentNumberService.allocate(FiscalDocumentType.SIMULATED_INVOICE);
        FiscalDocument document = saveUniqueDocument(
                buildInvoice(order, snapshotFromOrder(order), actor.username(), actor.roleLabel(), "Documento generato da ordine " + order.getCode(), numberedSettings),
                "Esiste già una fattura simulata per questo ordine."
        );
        auditService.record(actor.username(), actor.roleLabel(), "CREATE_DOCUMENT", document.getCode(), "Fattura simulata generata da ordine " + order.getCode() + " - totale " + order.getTotal(), AuditCategory.DOCUMENT, AuditSeverity.WARNING, "FISCAL_DOCUMENT");
        return FiscalDocumentResponse.from(document);
    }

    @Transactional
    public FiscalDocumentResponse createCreditNote(FiscalDocumentRequests.CreateCreditNoteRequest request, AuthenticatedUser actor) {
        CustomerOrder order = orderService.requireOrder(request.orderCode());
        FiscalDocument invoice = repository.findByRelatedOrderCodeIgnoreCaseAndType(order.getCode(), FiscalDocumentType.SIMULATED_INVOICE)
                .orElseThrow(() -> new IllegalArgumentException("Genera prima una fattura simulata per questo ordine."));
        if (repository.findByRelatedOrderCodeIgnoreCaseAndType(order.getCode(), FiscalDocumentType.SIMULATED_CREDIT_NOTE).isPresent()) {
            throw new ResourceConflictException("Esiste già una nota credito simulata per questo ordine.");
        }
        DocumentNumberService.NumberedCompanySettings numberedSettings = documentNumberService.allocate(FiscalDocumentType.SIMULATED_CREDIT_NOTE);
        FiscalDocument document = saveUniqueDocument(
                buildCreditNote(order, invoice, actor.username(), actor.roleLabel(), request.reason().trim(), numberedSettings.number()),
                "Esiste già una nota credito simulata per questo ordine."
        );
        auditService.record(actor.username(), actor.roleLabel(), "CREATE_DOCUMENT", document.getCode(), "Nota credito simulata generata da ordine " + order.getCode() + " - motivo " + request.reason().trim(), AuditCategory.DOCUMENT, AuditSeverity.CRITICAL, "FISCAL_DOCUMENT");
        return FiscalDocumentResponse.from(document);
    }

    private FiscalDocument saveUniqueDocument(FiscalDocument document, String duplicateMessage) {
        try {
            return repository.saveAndFlush(document);
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException(duplicateMessage);
        }
    }

    private FiscalDocument buildInvoice(CustomerOrder order, CustomerSnapshot customerSnapshot, String actor, String role, String reason, DocumentNumberService.NumberedCompanySettings numberedSettings) {
        BigDecimal vatRate = numberedSettings.companySettings().defaultVatRate();
        BigDecimal taxableAmount = order.getTotal().divide(ONE.add(vatRate), 2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = order.getTotal().subtract(taxableAmount).setScale(2, RoundingMode.HALF_UP);
        return new FiscalDocument(
                numberedSettings.number(),
                FiscalDocumentType.SIMULATED_INVOICE,
                order.getCode(),
                CompanySnapshot.from(numberedSettings.companySettings()),
                customerSnapshot,
                order.getPayment().getMethod().getLabel(),
                order.getItems().stream().map(this::lineFromOrder).toList(),
                taxableAmount,
                vatRate,
                vatAmount,
                order.getTotal(),
                actor,
                role,
                reason,
                DISCLAIMER,
                timeProvider.localDateTime()
        );
    }

    private FiscalDocument buildCreditNote(CustomerOrder order, FiscalDocument invoice, String actor, String role, String reason, DocumentNumberAllocation number) {
        return new FiscalDocument(
                number,
                FiscalDocumentType.SIMULATED_CREDIT_NOTE,
                order.getCode(),
                CompanySnapshot.from(invoice),
                snapshotFromDocument(invoice),
                order.getPayment().getMethod().getLabel(),
                order.getItems().stream().map(this::lineFromOrder).toList(),
                invoice.getTaxableAmount(),
                invoice.getVatRate(),
                invoice.getVatAmount(),
                invoice.getTotalAmount(),
                actor,
                role,
                reason,
                DISCLAIMER,
                timeProvider.localDateTime()
        );
    }

    private CustomerSnapshot snapshotFromOrder(CustomerOrder order) {
        if (hasText(order.getCustomerCode())) {
            try {
                return snapshotFromPartner(partnerService.findByCode(order.getCustomerCode()));
            } catch (IllegalArgumentException ignored) {
                return CustomerSnapshot.minimal(order.getCustomer(), order.getCustomerCode());
            }
        }
        return CustomerSnapshot.minimal(order.getCustomer(), "");
    }

    private static CustomerSnapshot snapshotFromPartner(BusinessPartnerResponse partner) {
        return new CustomerSnapshot(
                optional(partner.code()),
                clean(partner.displayName()),
                optional(partner.taxCode()),
                optional(partner.vatNumber()),
                optional(partner.email()),
                optional(partner.phone()),
                optional(partner.address()),
                optional(partner.city())
        );
    }

    private static CustomerSnapshot snapshotFromDocument(FiscalDocument document) {
        return new CustomerSnapshot(
                optional(document.getCustomerSnapshotCode()),
                clean(document.getCustomerSnapshotName()),
                optional(document.getCustomerSnapshotTaxCode()),
                optional(document.getCustomerSnapshotVatNumber()),
                optional(document.getCustomerSnapshotEmail()),
                optional(document.getCustomerSnapshotPhone()),
                optional(document.getCustomerSnapshotAddress()),
                optional(document.getCustomerSnapshotCity())
        );
    }

    private FiscalDocumentLine lineFromOrder(OrderItem item) {
        return new FiscalDocumentLine(item.getProductCode(), item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
    }

    private Specification<FiscalDocument> specification(String q, FiscalDocumentType type) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (hasText(q)) {
                String term = contains(q);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("relatedOrderCode")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customer")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customerSnapshotCode")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customerSnapshotName")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customerSnapshotVatNumber")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customerSnapshotTaxCode")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customerSnapshotCity")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("createdBy")), term)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
