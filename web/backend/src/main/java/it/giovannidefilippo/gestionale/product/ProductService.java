package it.giovannidefilippo.gestionale.product;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.common.DatabaseConstraintViolations;
import it.giovannidefilippo.gestionale.common.ResourceConflictException;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository repository;
    private final AuditService auditService;
    private final ProductOrderUsage productOrderUsage;
    private final ProductProcurementUsage productProcurementUsage;

    ProductService(ProductRepository repository, AuditService auditService, ProductOrderUsage productOrderUsage, ProductProcurementUsage productProcurementUsage) {
        this.repository = repository;
        this.auditService = auditService;
        this.productOrderUsage = productOrderUsage;
        this.productProcurementUsage = productProcurementUsage;
    }

    public List<ProductResponse> findAll() {
        return repository.findAll().stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    public PageResponse<ProductResponse> search(String q, ProductCategory category, String brand, String productType, String stock, String sort, int page, int size) {
        return PageResponse.from(repository.findAll(specification(q, category, brand, productType, stock, false), PageRequests.of(page, size, sort(sort)))
                .map(ProductMapper::toResponse));
    }

    public PageResponse<ProductResponse> search(String q, ProductCategory category, String brand, String productType, String stock, String sort, int page, int size, AuthenticatedUser actor) {
        return PageResponse.from(repository.findAll(specification(q, category, brand, productType, stock, false), PageRequests.of(page, size, sort(sort)))
                .map(product -> ProductMapper.toResponse(product, capabilities(product, actor))));
    }

    public PageResponse<CustomerProductResponse> searchCustomerCatalog(String q, ProductCategory category, String brand, String productType, String stock, String sort, int page, int size) {
        return PageResponse.from(repository.findAll(specification(q, category, brand, productType, stock, true), PageRequests.of(page, size, customerSort(sort)))
                .map(CustomerProductResponse::from));
    }

    public List<ProductLookupResponse> lookup() {
        return repository.findAll(Sort.by("name").ascending().and(Sort.by("code").ascending())).stream()
                .map(ProductLookupResponse::from)
                .toList();
    }

    public ProductResponse findByCode(String code) {
        return ProductMapper.toResponse(requireProduct(code));
    }

    public ProductResponse findByCode(String code, AuthenticatedUser actor) {
        Product product = requireProduct(code);
        return ProductMapper.toResponse(product, capabilities(product, actor));
    }

    public List<ProductResponse> findLowStockProducts(int threshold) {
        validateThreshold(threshold);
        return repository.findLowStockProducts(threshold).stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    public long countLowStockProducts(int threshold) {
        validateThreshold(threshold);
        return repository.countLowStockProducts(threshold);
    }

    public ProductDashboardSummary dashboardSummary(int lowStockThreshold) {
        validateThreshold(lowStockThreshold);
        long costedUnits = repository.sumCostedQuantity();
        long uncostedUnits = repository.sumUncostedQuantity();
        long physicalUnits = costedUnits + uncostedUnits;
        BigDecimal coverage = physicalUnits == 0
                ? BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.valueOf(costedUnits).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(physicalUnits), 2, java.math.RoundingMode.HALF_UP);
        return new ProductDashboardSummary(
                repository.count(),
                repository.sumPotentialRetailStockValue().setScale(2, java.math.RoundingMode.HALF_UP),
                repository.sumKnownInventoryCostValue().setScale(2, java.math.RoundingMode.HALF_UP),
                repository.sumPotentialGrossMarginOnCostedStock().setScale(2, java.math.RoundingMode.HALF_UP),
                costedUnits,
                uncostedUnits,
                coverage,
                repository.countLowStockProducts(lowStockThreshold),
                repository.countOutOfStockProducts()
        );
    }

    public Product requireProduct(String code) {
        return repository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato."));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = saveAndFlush(new Product(request));
        return ProductMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request, String actor, String role) {
        ProductResponse response = create(request);
        auditService.record(actor, role, "CREATE_PRODUCT", response.code(), "Creata anagrafica " + response.name() + " - brand " + response.brand() + " - tipo " + response.productType() + " - prezzo " + response.price(), AuditCategory.PRODUCT, AuditSeverity.INFO, "PRODUCT");
        return response;
    }

    @Transactional
    public ProductResponse create(ProductRequest request, AuthenticatedUser actor) {
        Product product = saveAndFlush(new Product(request));
        ProductResponse response = ProductMapper.toResponse(product, capabilities(product, actor));
        auditService.record(actor.username(), actor.roleLabel(), "CREATE_PRODUCT", response.code(), "Creata anagrafica " + response.name() + " - brand " + response.brand() + " - tipo " + response.productType() + " - prezzo " + response.price(), AuditCategory.PRODUCT, AuditSeverity.INFO, "PRODUCT");
        return response;
    }

    @Transactional
    public ProductResponse update(String code, ProductRequest request) {
        Product product = requireProduct(code);
        validateCodeChangeAllowed(product, request.code());
        product.update(request);
        flushCanonicalCode();
        return ProductMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse update(String code, ProductRequest request, String actor, String role) {
        Product product = requireProduct(code);
        ProductResponse before = ProductMapper.toResponse(product);
        validateCodeChangeAllowed(product, request.code());
        product.update(request);
        flushCanonicalCode();
        ProductResponse response = ProductMapper.toResponse(product);
        auditService.record(actor, role, "UPDATE_PRODUCT", response.code(), "Aggiornato " + response.name() + " - modifiche: " + changeSummary(before, response), AuditCategory.PRODUCT, AuditSeverity.INFO, "PRODUCT");
        return response;
    }

    @Transactional
    public ProductResponse update(String code, ProductRequest request, AuthenticatedUser actor) {
        Product product = requireProduct(code);
        ProductResponse before = ProductMapper.toResponse(product);
        validateCodeChangeAllowed(product, request.code());
        product.update(request);
        flushCanonicalCode();
        ProductResponse response = ProductMapper.toResponse(product, capabilities(product, actor));
        auditService.record(actor.username(), actor.roleLabel(), "UPDATE_PRODUCT", response.code(), "Aggiornato " + response.name() + " - modifiche: " + changeSummary(before, response), AuditCategory.PRODUCT, AuditSeverity.INFO, "PRODUCT");
        return response;
    }

    @Transactional
    public void delete(String code) {
        Product product = requireProduct(code);
        validateDeletionAllowed(product);
        repository.delete(product);
    }

    @Transactional
    public void delete(String code, String actor, String role) {
        Product product = requireProduct(code);
        validateDeletionAllowed(product);
        repository.delete(product);
        auditService.record(actor, role, "DELETE_PRODUCT", product.getCode(), "Eliminato " + product.getName() + " - brand " + product.getBrand() + " - tipo " + product.getProductType() + " - quantita residua " + product.getQuantity(), AuditCategory.PRODUCT, AuditSeverity.WARNING, "PRODUCT");
    }

    @Transactional
    public void deleteMany(List<String> codes) {
        codes.forEach(this::delete);
    }

    @Transactional
    public void deleteMany(List<String> codes, String actor, String role) {
        codes.forEach(code -> delete(code, actor, role));
    }

    @Transactional
    public ProductResponse discontinue(String code) {
        Product product = requireProduct(code);
        product.discontinue();
        return ProductMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse discontinue(String code, String actor, String role) {
        Product product = requireProduct(code);
        product.discontinue();
        ProductResponse response = ProductMapper.toResponse(product);
        auditService.record(actor, role, "DISCONTINUE_PRODUCT", product.getCode(), "Disattivato " + product.getName() + " - non acquistabile per nuovi ordini", AuditCategory.PRODUCT, AuditSeverity.WARNING, "PRODUCT");
        return response;
    }

    @Transactional
    public ProductResponse discontinue(String code, AuthenticatedUser actor) {
        Product product = requireProduct(code);
        product.discontinue();
        ProductResponse response = ProductMapper.toResponse(product, capabilities(product, actor));
        auditService.record(actor.username(), actor.roleLabel(), "DISCONTINUE_PRODUCT", product.getCode(), "Disattivato " + product.getName() + " - non acquistabile per nuovi ordini", AuditCategory.PRODUCT, AuditSeverity.WARNING, "PRODUCT");
        return response;
    }

    @Transactional
    public StockAdjustment adjustStock(String code, int delta) {
        Product product = repository.findByCodeForStockAdjustment(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato."));
        return adjustLockedProduct(product, delta);
    }

    @Transactional
    public StockAdjustment adjustStock(Long productId, int delta) {
        Product product = repository.findAllByIdForStockAdjustment(List.of(productId)).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato."));
        return adjustLockedProduct(product, delta);
    }

    @Transactional
    public List<InventoryProductSnapshot> lockInventorySnapshots(List<String> productCodes) {
        List<Long> ids;
        if (productCodes == null || productCodes.isEmpty()) {
            ids = repository.findAll(Sort.by("id").ascending()).stream().map(Product::getId).toList();
        } else {
            List<String> normalized = productCodes.stream()
                    .filter(ProductService::hasText)
                    .map(String::trim)
                    .toList();
            long canonicalCodes = normalized.stream()
                    .map(code -> code.toLowerCase(Locale.ROOT))
                    .distinct()
                    .count();
            if (normalized.size() != productCodes.size() || canonicalCodes != normalized.size()) {
                throw new IllegalArgumentException("I prodotti della sessione devono essere valorizzati e non duplicati.");
            }
            ids = normalized.stream()
                    .map(code -> repository.findByCodeIgnoreCase(code)
                            .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato: " + code + "."))
                            .getId())
                    .sorted()
                    .toList();
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("La sessione di inventario richiede almeno un prodotto.");
        }
        return repository.findAllByIdForStockAdjustment(ids).stream()
                .map(ProductService::inventorySnapshot)
                .toList();
    }

    @Transactional
    public InventoryProductSnapshot lockInventorySnapshot(Long productId) {
        return repository.findAllByIdForStockAdjustment(List.of(productId)).stream()
                .findFirst()
                .map(ProductService::inventorySnapshot)
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato."));
    }

    private StockAdjustment adjustLockedProduct(Product product, int delta) {
        int previousQuantity = product.getQuantity();
        int newQuantity = previousQuantity + delta;
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Lo scarico supera la quantità disponibile.");
        }
        if (newQuantity < product.getReservedQuantity()) {
            throw new IllegalArgumentException("Lo scarico supera la disponibilità vendibile.");
        }
        product.updateQuantity(newQuantity);
        repository.flush();
        return new StockAdjustment(product.getId(), product.getCode(), product.getName(), previousQuantity, newQuantity);
    }

    private static InventoryProductSnapshot inventorySnapshot(Product product) {
        return new InventoryProductSnapshot(
                product.getId(), product.getCode(), product.getName(), product.getQuantity(),
                product.getReservedQuantity(), product.getVersion()
        );
    }

    @Transactional
    public CostedStockReceipt receivePurchaseStock(String code, int quantity, BigDecimal unitCost) {
        Product product = repository.findByCodeForStockAdjustment(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato."));
        CostedStockReceipt receipt = product.receivePurchase(quantity, unitCost);
        repository.flush();
        return receipt;
    }

    @Transactional
    public StockAdjustment initializeStock(String code, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Il saldo iniziale non può essere negativo.");
        }
        Product product = repository.findByCodeForStockAdjustment(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato."));
        product.initializeQuantity(quantity);
        repository.flush();
        return new StockAdjustment(product.getId(), product.getCode(), product.getName(), 0, quantity);
    }

    @Transactional
    public void reserveStock(String code, int quantity) {
        Product product = repository.findByCodeForStockAdjustment(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato."));
        validatePositiveQuantity(quantity);
        if (product.isDiscontinued()) {
            throw new IllegalArgumentException("Il prodotto " + product.getCode() + " e disattivato e non puo essere acquistato.");
        }
        product.reserveQuantity(quantity);
        repository.flush();
    }

    @Transactional
    public void releaseReservedStock(String code, int quantity) {
        Product product = repository.findByCodeForStockAdjustment(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato."));
        validatePositiveQuantity(quantity);
        product.releaseReservedQuantity(quantity);
        repository.flush();
    }

    @Transactional
    public StockAdjustment fulfillReservedStock(String code, int quantity) {
        Product product = repository.findByCodeForStockAdjustment(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato."));
        validatePositiveQuantity(quantity);
        int previousQuantity = product.getQuantity();
        product.fulfillReservedQuantity(quantity);
        repository.flush();
        return new StockAdjustment(product.getId(), product.getCode(), product.getName(), previousQuantity, product.getQuantity());
    }

    private Specification<Product> specification(String q, ProductCategory category, String brand, String productType, String stock, boolean activeOnly) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (activeOnly) {
                predicates.add(criteriaBuilder.isFalse(root.get("discontinued")));
            }
            if (hasText(q)) {
                String term = contains(q);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("brand")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("productType")), term),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("usageContext")), term)
                ));
            }
            if (category != null) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            if (hasText(brand) && !"ALL".equalsIgnoreCase(brand)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("brand")), brand.trim().toLowerCase(Locale.ROOT)));
            }
            if (hasText(productType) && !"ALL".equalsIgnoreCase(productType)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("productType")), productType.trim().toLowerCase(Locale.ROOT)));
            }
            if (hasText(stock) && !"ALL".equalsIgnoreCase(stock)) {
                String normalizedStock = stock.trim().toUpperCase(Locale.ROOT);
                Expression<Integer> availableQuantity = criteriaBuilder.diff(root.<Integer>get("quantity"), root.<Integer>get("reservedQuantity"));
                if ("AVAILABLE".equals(normalizedStock)) {
                    predicates.add(criteriaBuilder.greaterThan(availableQuantity, 3));
                } else if ("LOW".equals(normalizedStock)) {
                    predicates.add(criteriaBuilder.and(
                            criteriaBuilder.greaterThan(availableQuantity, 0),
                            criteriaBuilder.lessThanOrEqualTo(availableQuantity, 3)
                    ));
                } else if ("OUT".equals(normalizedStock)) {
                    predicates.add(criteriaBuilder.equal(availableQuantity, 0));
                } else {
                    throw new IllegalArgumentException("Filtro stock non valido.");
                }
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Sort sort(String sort) {
        if (!hasText(sort)) {
            return Sort.by("name").ascending().and(Sort.by("code").ascending());
        }
        return switch (sort.trim().toUpperCase(Locale.ROOT)) {
            case "PRICE_ASC" -> Sort.by("price").ascending();
            case "PRICE_DESC" -> Sort.by("price").descending();
            case "QTY_ASC" -> Sort.by("quantity").ascending();
            case "QTY_DESC" -> Sort.by("quantity").descending();
            default -> Sort.by("name").ascending().and(Sort.by("code").ascending());
        };
    }

    private Sort customerSort(String sort) {
        if (!hasText(sort)) {
            return Sort.by("name").ascending().and(Sort.by("code").ascending());
        }
        return switch (sort.trim().toUpperCase(Locale.ROOT)) {
            case "PRICE_ASC" -> Sort.by("price").ascending();
            case "PRICE_DESC" -> Sort.by("price").descending();
            case "NAME_ASC" -> Sort.by("name").ascending().and(Sort.by("code").ascending());
            default -> throw new IllegalArgumentException("Ordinamento catalogo cliente non valido.");
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantità deve essere maggiore di zero.");
        }
    }

    private static void validateThreshold(int threshold) {
        if (threshold <= 0) {
            throw new IllegalArgumentException("La soglia scorte basse deve essere maggiore di zero.");
        }
    }

    private Product saveAndFlush(Product product) {
        try {
            return repository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            throw translateProductConstraint(exception);
        }
    }

    private void flushCanonicalCode() {
        try {
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw translateProductConstraint(exception);
        }
    }

    private RuntimeException translateProductConstraint(DataIntegrityViolationException exception) {
        if (DatabaseConstraintViolations.matches(exception, "uk_products_code", "uk_products_code_canonical")) {
            return new ResourceConflictException("Esiste gia un prodotto con questo codice prodotto.");
        }
        return exception;
    }

    private void validateCodeChangeAllowed(Product product, String requestedCode) {
        if (!product.getCode().equalsIgnoreCase(requestedCode.trim()) && isUsedInOrders(product)) {
            throw new IllegalArgumentException("Il codice prodotto non puo essere modificato perche il prodotto e gia presente in uno o piu ordini.");
        }
    }

    private void validateDeletionAllowed(Product product) {
        if (product.getReservedQuantity() > 0) {
            throw new IllegalArgumentException("Il prodotto non puo essere eliminato perche ha stock riservato da ordini confermati.");
        }
        if (isUsedInOrders(product)) {
            throw new IllegalArgumentException("Il prodotto non puo essere eliminato perche e gia presente in uno o piu ordini. Disattivalo per impedirne nuovi acquisti mantenendo lo storico.");
        }
        if (product.getQuantity() != 0) {
            throw new IllegalArgumentException("Il prodotto non puo essere eliminato finche la giacenza fisica non viene rettificata a zero.");
        }
    }

    private ProductCapabilities capabilities(Product product, AuthenticatedUser actor) {
        boolean canManageProducts = actor.hasPermission(UserPermission.MANAGE_PRODUCTS);
        boolean usedInOrders = isUsedInOrders(product);
        return new ProductCapabilities(
                canManageProducts,
                canManageProducts && !usedInOrders,
                canManageProducts && !usedInOrders && product.getReservedQuantity() == 0 && product.getQuantity() == 0,
                canManageProducts && !product.isDiscontinued(),
                actor.hasPermission(UserPermission.MANAGE_INVENTORY)
        );
    }

    private boolean isUsedInOrders(Product product) {
        return productOrderUsage.existsByProductCode(product.getCode())
                || productProcurementUsage.existsByProductId(product.getId());
    }

    private static String changeSummary(ProductResponse before, ProductResponse after) {
        List<String> changes = new ArrayList<>();
        addChange(changes, "codice", before.code(), after.code());
        addChange(changes, "nome", before.name(), after.name());
        addChange(changes, "categoria", before.category(), after.category());
        addChange(changes, "brand", before.brand(), after.brand());
        addChange(changes, "tipo", before.productType(), after.productType());
        addChange(changes, "utilizzo", before.usageContext(), after.usageContext());
        addChange(changes, "prezzo", before.price(), after.price());
        addChange(changes, "sconto", before.discount(), after.discount());
        return changes.isEmpty() ? "nessuna variazione rilevante" : String.join("; ", changes);
    }

    private static void addChange(List<String> changes, String label, Object before, Object after) {
        if (!normalized(before).equals(normalized(after))) {
            changes.add(label + " " + normalized(before) + " -> " + normalized(after));
        }
    }

    private static String normalized(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return Objects.toString(value, "-").isBlank() ? "-" : Objects.toString(value, "-");
    }
}
