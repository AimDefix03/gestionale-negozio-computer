package it.giovannidefilippo.gestionale.product;

import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
class ProductController {
    private final ProductService service;
    private final AuthSessionService authSessionService;

    ProductController(ProductService service, AuthSessionService authSessionService) {
        this.service = service;
        this.authSessionService = authSessionService;
    }

    @GetMapping
    PageResponse<ProductResponse> findAll(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String stock,
            @RequestParam(required = false) String sort
    ) {
        authSessionService.requirePermission(token, UserPermission.VIEW_CATALOG);
        return service.search(q, category, brand, productType, stock, sort, page, size);
    }

    @GetMapping("/lookup")
    List<ProductLookupResponse> lookup(@RequestHeader(value = "X-Session-Token", required = false) String token) {
        authSessionService.requirePermission(token, UserPermission.VIEW_CATALOG);
        return service.lookup();
    }

    @GetMapping("/{code}")
    ProductResponse findByCode(@PathVariable String code, @RequestHeader(value = "X-Session-Token", required = false) String token) {
        authSessionService.requirePermission(token, UserPermission.VIEW_CATALOG);
        return service.findByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProductResponse create(@Valid @RequestBody ProductRequest request, @RequestHeader(value = "X-Session-Token", required = false) String token) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_PRODUCTS);
        return service.create(request, actor.username(), actor.roleLabel());
    }

    @PutMapping("/{code}")
    ProductResponse update(@PathVariable String code, @Valid @RequestBody ProductRequest request, @RequestHeader(value = "X-Session-Token", required = false) String token) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_PRODUCTS);
        return service.update(code, request, actor.username(), actor.roleLabel());
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String code, @RequestHeader(value = "X-Session-Token", required = false) String token) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_PRODUCTS);
        service.delete(code, actor.username(), actor.roleLabel());
    }

    @PostMapping("/{code}/discontinue")
    ProductResponse discontinue(@PathVariable String code, @RequestHeader(value = "X-Session-Token", required = false) String token) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_PRODUCTS);
        return service.discontinue(code, actor.username(), actor.roleLabel());
    }

    @PostMapping("/bulk-delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMany(@Valid @RequestBody DeleteProductsRequest request, @RequestHeader(value = "X-Session-Token", required = false) String token) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_PRODUCTS);
        service.deleteMany(request.codes(), actor.username(), actor.roleLabel());
    }
}
