package it.giovannidefilippo.gestionale.inventory;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.BusinessCodeGenerator;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.common.ResourceConflictException;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import it.giovannidefilippo.gestionale.product.InventoryProductSnapshot;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PhysicalInventoryService {
    private final PhysicalInventorySessionRepository sessionRepository;
    private final PhysicalInventoryItemRepository itemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final BusinessCodeGenerator codeGenerator;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    PhysicalInventoryService(
            PhysicalInventorySessionRepository sessionRepository,
            PhysicalInventoryItemRepository itemRepository,
            StockMovementRepository stockMovementRepository,
            ProductService productService,
            InventoryService inventoryService,
            BusinessCodeGenerator codeGenerator,
            AuditService auditService,
            TimeProvider timeProvider
    ) {
        this.sessionRepository = sessionRepository;
        this.itemRepository = itemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    public PageResponse<PhysicalInventorySessionResponse> search(PhysicalInventoryStatus status, int page, int size, AuthenticatedUser actor) {
        Specification<PhysicalInventorySession> specification = status == null
                ? (root, query, builder) -> builder.conjunction()
                : (root, query, builder) -> builder.equal(root.get("status"), status);
        return PageResponse.from(sessionRepository.findAll(
                        specification,
                        PageRequests.of(page, size, Sort.by("createdAt").descending())
                )
                .map(session -> response(session, actor, false)));
    }

    public PhysicalInventorySessionResponse find(Long id, AuthenticatedUser actor) {
        return response(requireDetailed(id), actor, true);
    }

    @Transactional
    public PhysicalInventorySessionResponse create(PhysicalInventoryRequests.Create request, AuthenticatedUser actor) {
        List<InventoryProductSnapshot> snapshots = productService.lockInventorySnapshots(request.productCodes());
        for (InventoryProductSnapshot snapshot : snapshots) {
            if (!stockMovementRepository.hasInitialBalance(snapshot.productId())) {
                throw new ResourceConflictException("Registra il saldo iniziale del prodotto " + snapshot.productCode() + " prima del conteggio fisico.");
            }
            if (itemRepository.existsByProductIdAndActiveMarkerTrue(snapshot.productId())) {
                throw new ResourceConflictException("Il prodotto " + snapshot.productCode() + " appartiene già a una sessione di inventario aperta.");
            }
        }
        PhysicalInventorySession session = new PhysicalInventorySession(
                codeGenerator.nextPhysicalInventoryCode(), request.reason(), actor.username(), actor.roleLabel(), timeProvider.localDateTime()
        );
        snapshots.forEach(snapshot -> session.addItem(new PhysicalInventoryItem(
                session, snapshot.productId(), snapshot.productCode(), snapshot.productName(), snapshot.quantity(),
                snapshot.reservedQuantity(), snapshot.version()
        )));
        try {
            sessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException("Uno dei prodotti appartiene già a una sessione di inventario attiva.");
        }
        auditService.record(
                actor.username(), actor.roleLabel(), "PHYSICAL_INVENTORY_CREATED", session.getCode(),
                "Sessione creata con " + session.getItems().size() + " prodotti - " + session.getReason(),
                AuditCategory.INVENTORY, AuditSeverity.INFO, "PHYSICAL_INVENTORY_SESSION"
        );
        return response(session, actor, true);
    }

    @Transactional
    public PhysicalInventorySessionResponse recordCount(Long sessionId, Long itemId, PhysicalInventoryRequests.Count request, AuthenticatedUser actor) {
        PhysicalInventorySession session = requireForUpdate(sessionId);
        session.requireOpen();
        PhysicalInventoryItem item = session.requireItem(itemId);
        InventoryProductSnapshot product = productService.lockInventorySnapshot(item.getProductId());
        item.recordCount(
                request.countedQuantity(), product.quantity(), product.reservedQuantity(), product.version(),
                actor.username(), actor.roleLabel(), request.note(), timeProvider.localDateTime()
        );
        sessionRepository.flush();
        auditService.record(
                actor.username(), actor.roleLabel(), "PHYSICAL_INVENTORY_COUNT_RECORDED", session.getCode(),
                item.getProductCodeSnapshot() + " - teorico " + product.quantity() + " - contato " + request.countedQuantity()
                        + " - differenza " + item.getDifferenceQuantity(),
                AuditCategory.INVENTORY, item.getDifferenceQuantity() == 0 ? AuditSeverity.INFO : AuditSeverity.WARNING,
                "PHYSICAL_INVENTORY_ITEM"
        );
        return response(session, actor, true);
    }

    @Transactional
    public PhysicalInventorySessionResponse submit(Long sessionId, AuthenticatedUser actor) {
        PhysicalInventorySession session = requireForUpdate(sessionId);
        session.submit(actor.username(), actor.roleLabel(), timeProvider.localDateTime());
        sessionRepository.flush();
        auditService.record(
                actor.username(), actor.roleLabel(), "PHYSICAL_INVENTORY_SUBMITTED", session.getCode(),
                "Conteggio inviato con " + differenceItems(session) + " differenze da approvare.",
                AuditCategory.INVENTORY, differenceItems(session) == 0 ? AuditSeverity.INFO : AuditSeverity.WARNING,
                "PHYSICAL_INVENTORY_SESSION"
        );
        return response(session, actor, true);
    }

    @Transactional
    public PhysicalInventorySessionResponse approve(Long sessionId, PhysicalInventoryRequests.Decision request, AuthenticatedUser actor) {
        PhysicalInventorySession session = requireForUpdate(sessionId);
        if (session.getStatus() != PhysicalInventoryStatus.SUBMITTED) {
            throw new IllegalStateException("La sessione non è disponibile per l'approvazione.");
        }
        if (session.getSubmittedBy() != null && session.getSubmittedBy().equalsIgnoreCase(actor.username())) {
            throw new IllegalStateException("Chi invia il conteggio non può approvare la stessa sessione.");
        }
        List<PhysicalInventoryItem> items = session.getItems().stream()
                .sorted(Comparator.comparing(PhysicalInventoryItem::getProductId))
                .toList();
        for (PhysicalInventoryItem item : items) {
            PhysicalInventoryAdjustmentResult result = inventoryService.applyPhysicalInventoryAdjustment(
                    new PhysicalInventoryAdjustmentCommand(
                            session.getId(), session.getCode(), item.getId(), item.getProductId(),
                            item.getProductCodeSnapshot(), item.getProductNameSnapshot(), item.getDifferenceQuantity(),
                            request.reason(), actor.username(), actor.roleLabel()
                    )
            );
            item.recordApproval(result.previousQuantity(), result.newQuantity(), result.reservedQuantity(), result.stockMovementId());
        }
        session.approve(actor.username(), actor.roleLabel(), request.reason(), timeProvider.localDateTime());
        sessionRepository.flush();
        auditService.record(
                actor.username(), actor.roleLabel(), "PHYSICAL_INVENTORY_APPROVED", session.getCode(),
                request.reason().trim() + " - " + differenceItems(session) + " differenze approvate.",
                AuditCategory.INVENTORY, differenceItems(session) == 0 ? AuditSeverity.INFO : AuditSeverity.WARNING,
                "PHYSICAL_INVENTORY_SESSION"
        );
        return response(session, actor, true);
    }

    @Transactional
    public PhysicalInventorySessionResponse cancel(Long sessionId, PhysicalInventoryRequests.Decision request, AuthenticatedUser actor) {
        PhysicalInventorySession session = requireForUpdate(sessionId);
        session.cancel(actor.username(), actor.roleLabel(), request.reason(), timeProvider.localDateTime());
        sessionRepository.flush();
        auditService.record(
                actor.username(), actor.roleLabel(), "PHYSICAL_INVENTORY_CANCELED", session.getCode(),
                request.reason().trim(), AuditCategory.INVENTORY, AuditSeverity.WARNING, "PHYSICAL_INVENTORY_SESSION"
        );
        return response(session, actor, true);
    }

    private PhysicalInventorySession requireDetailed(Long id) {
        return sessionRepository.findDetailedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sessione di inventario non trovata."));
    }

    private PhysicalInventorySession requireForUpdate(Long id) {
        PhysicalInventorySession session = sessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Sessione di inventario non trovata."));
        session.getItems().size();
        return session;
    }

    private PhysicalInventorySessionResponse response(PhysicalInventorySession session, AuthenticatedUser actor, boolean includeItems) {
        boolean manage = actor.hasPermission(UserPermission.MANAGE_INVENTORY);
        boolean canApprove = actor.hasPermission(UserPermission.APPROVE_INVENTORY_COUNTS)
                && session.getStatus() == PhysicalInventoryStatus.SUBMITTED
                && (session.getSubmittedBy() == null || !session.getSubmittedBy().equalsIgnoreCase(actor.username()));
        PhysicalInventoryCapabilities capabilities = new PhysicalInventoryCapabilities(
                manage && session.getStatus() == PhysicalInventoryStatus.OPEN,
                manage && session.getStatus() == PhysicalInventoryStatus.OPEN
                        && session.getItems().stream().allMatch(PhysicalInventoryItem::isCounted),
                canApprove,
                manage && (session.getStatus() == PhysicalInventoryStatus.OPEN || session.getStatus() == PhysicalInventoryStatus.SUBMITTED)
        );
        return PhysicalInventorySessionResponse.from(session, capabilities, includeItems);
    }

    private static long differenceItems(PhysicalInventorySession session) {
        return session.getItems().stream()
                .filter(item -> item.getDifferenceQuantity() != null && item.getDifferenceQuantity() != 0)
                .count();
    }
}
