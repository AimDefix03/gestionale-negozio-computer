package it.giovannidefilippo.gestionale.product;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    ProductService(ProductRepository repository, AuditService auditService, ProductOrderUsage productOrderUsage) {
        this.repository = repository;
        this.auditService = auditService;
        this.productOrderUsage = productOrderUsage;
    }

    public List<ProductResponse> findAll() {
        return repository.findAll().stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    public PageResponse<ProductResponse> search(String q, ProductCategory category, String brand, String productType, String stock, String sort, int page, int size) {
        return PageResponse.from(repository.findAll(specification(q, category, brand, productType, stock), PageRequests.of(page, size, sort(sort)))
                .map(ProductMapper::toResponse));
    }

    public List<ProductLookupResponse> lookup() {
        return repository.findAll(Sort.by("name").ascending().and(Sort.by("code").ascending())).stream()
                .map(ProductLookupResponse::from)
                .toList();
    }

    public ProductResponse findByCode(String code) {
        return ProductMapper.toResponse(requireProduct(code));
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
        return new ProductDashboardSummary(
                repository.count(),
                repository.sumDiscountedInventoryValue().setScale(2, java.math.RoundingMode.HALF_UP),
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
        if (repository.existsByCodeIgnoreCase(request.code().trim())) {
            throw new IllegalArgumentException("Esiste gia un prodotto con questo codice.");
        }
        Product product = repository.save(new Product(request));
        return ProductMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request, String actor, String role) {
        ProductResponse response = create(request);
        auditService.record(actor, role, "CREATE_PRODUCT", response.code(), "Creato " + response.name() + " - brand " + response.brand() + " - tipo " + response.productType() + " - quantita " + response.quantity() + " - prezzo " + response.price(), AuditCategory.PRODUCT, AuditSeverity.INFO, "PRODUCT");
        return response;
    }

    @Transactional
    public ProductResponse update(String code, ProductRequest request) {
        Product product = requireProduct(code);
        validateCodeChangeAllowed(product, request.code());
        validateUniqueCode(product, request.code());
        product.update(request);
        return ProductMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse update(String code, ProductRequest request, String actor, String role) {
        Product product = requireProduct(code);
        ProductResponse before = ProductMapper.toResponse(product);
        validateCodeChangeAllowed(product, request.code());
        validateUniqueCode(product, request.code());
        product.update(request);
        ProductResponse response = ProductMapper.toResponse(product);
        auditService.record(actor, role, "UPDATE_PRODUCT", response.code(), "Aggiornato " + response.name() + " - modifiche: " + changeSummary(before, response), AuditCategory.PRODUCT, AuditSeverity.INFO, "PRODUCT");
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
    public StockAdjustment adjustStock(String code, int delta) {
        Product product = repository.findByCodeForStockAdjustment(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato."));
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
        return new StockAdjustment(product.getCode(), product.getName(), previousQuantity, newQuantity);
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
        return new StockAdjustment(product.getCode(), product.getName(), previousQuantity, product.getQuantity());
    }

    private Specification<Product> specification(String q, ProductCategory category, String brand, String productType, String stock) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
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

    private void validateUniqueCode(Product product, String requestedCode) {
        repository.findByCodeIgnoreCase(requestedCode.trim())
                .filter(existing -> !existing.getId().equals(product.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Esiste gia un prodotto con questo codice.");
                });
    }

    private void validateCodeChangeAllowed(Product product, String requestedCode) {
        if (!product.getCode().equalsIgnoreCase(requestedCode.trim()) && productOrderUsage.existsByProductCode(product.getCode())) {
            throw new IllegalArgumentException("Il codice prodotto non puo essere modificato perche il prodotto e gia presente in uno o piu ordini.");
        }
    }

    private void validateDeletionAllowed(Product product) {
        if (product.getReservedQuantity() > 0) {
            throw new IllegalArgumentException("Il prodotto non puo essere eliminato perche ha stock riservato da ordini confermati.");
        }
        if (productOrderUsage.existsByProductCode(product.getCode())) {
            throw new IllegalArgumentException("Il prodotto non puo essere eliminato perche e gia presente in uno o piu ordini. Disattivalo per impedirne nuovi acquisti mantenendo lo storico.");
        }
    }

    private static String changeSummary(ProductResponse before, ProductResponse after) {
        List<String> changes = new ArrayList<>();
        addChange(changes, "codice", before.code(), after.code());
        addChange(changes, "nome", before.name(), after.name());
        addChange(changes, "categoria", before.category(), after.category());
        addChange(changes, "brand", before.brand(), after.brand());
        addChange(changes, "tipo", before.productType(), after.productType());
        addChange(changes, "utilizzo", before.usageContext(), after.usageContext());
        addChange(changes, "quantita", before.quantity(), after.quantity());
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
