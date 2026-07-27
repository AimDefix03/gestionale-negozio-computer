package it.giovannidefilippo.gestionale.document;

import it.giovannidefilippo.gestionale.common.ResourceConflictException;
import it.giovannidefilippo.gestionale.order.OrderRequests;
import it.giovannidefilippo.gestionale.order.OrderResponse;
import it.giovannidefilippo.gestionale.order.OrderService;
import it.giovannidefilippo.gestionale.order.PaymentMethod;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class FiscalDocumentUniquenessTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FiscalDocumentService documentService;

    @Autowired
    private FiscalDocumentRepository repository;

    @Test
    void invoiceCannotBeCreatedTwiceForSameOrder() {
        OrderResponse order = createFulfilledOrder();

        documentService.createInvoice(new FiscalDocumentRequests.CreateInvoiceRequest(order.code()), actor());

        assertThatThrownBy(() -> documentService.createInvoice(new FiscalDocumentRequests.CreateInvoiceRequest(order.code()), actor()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("fattura simulata");
    }

    @Test
    void creditNoteCannotBeCreatedTwiceForSameOrder() {
        OrderResponse order = createFulfilledOrder();
        documentService.createInvoice(new FiscalDocumentRequests.CreateInvoiceRequest(order.code()), actor());

        documentService.createCreditNote(new FiscalDocumentRequests.CreateCreditNoteRequest(order.code(), "Rettifica simulata"), actor());

        assertThatThrownBy(() -> documentService.createCreditNote(new FiscalDocumentRequests.CreateCreditNoteRequest(order.code(), "Seconda rettifica"), actor()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("nota credito simulata");
    }

    @Test
    void databaseRejectsDuplicateDocumentTypeForSameOrder() {
        String orderCode = uniqueCode("ORD-DB");
        long firstSequence = Math.abs(UUID.randomUUID().getMostSignificantBits());
        repository.saveAndFlush(document(uniqueCode("FS"), FiscalDocumentType.SIMULATED_INVOICE, orderCode, firstSequence));

        assertThatThrownBy(() -> repository.saveAndFlush(document(uniqueCode("FS"), FiscalDocumentType.SIMULATED_INVOICE, orderCode, firstSequence + 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private OrderResponse createFulfilledOrder() {
        String productCode = uniqueCode("DOC-PROD");
        productService.create(new ProductRequest(
                productCode,
                "Prodotto documenti",
                "Prodotto creato per verificare i vincoli documentali.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Scheda di test",
                "",
                3,
                new BigDecimal("120.00"),
                new BigDecimal("0.00")
        ));
        OrderResponse order = orderService.create(
                new OrderRequests.CreateOrderRequest(
                        "cliente_documenti",
                        PaymentMethod.BANK_TRANSFER,
                        List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
                ),
                "cliente_documenti",
                actor()
        );
        orderService.confirm(order.code(), actor());
        return orderService.fulfill(order.code(), actor());
    }

    private FiscalDocument document(String code, FiscalDocumentType type, String orderCode, long sequenceNumber) {
        return new FiscalDocument(
                new DocumentNumberAllocation(code, "FS", 2026, sequenceNumber),
                type,
                orderCode,
                new CompanySnapshot("", "", "", "", "", "", "", "", "", "IT"),
                CustomerSnapshot.minimal("Cliente test", "CLI-TEST"),
                "Bonifico",
                List.of(new FiscalDocumentLine("P-TEST", "Prodotto test", 1, new BigDecimal("100.00"), new BigDecimal("100.00"))),
                new BigDecimal("81.97"),
                new BigDecimal("0.22"),
                new BigDecimal("18.03"),
                new BigDecimal("100.00"),
                "admin",
                "Super admin",
                "Documento test",
                "DOCUMENTO SIMULATO - NON VALIDO AI FINI FISCALI",
                LocalDateTime.of(2026, 1, 1, 10, 0)
        );
    }

    private static AuthenticatedUser actor() {
        return new AuthenticatedUser("admin", UserRole.SUPER_ADMIN);
    }

    private static String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
