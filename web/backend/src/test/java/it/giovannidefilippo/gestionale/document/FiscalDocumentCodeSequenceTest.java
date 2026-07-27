package it.giovannidefilippo.gestionale.document;

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
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FiscalDocumentCodeSequenceTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FiscalDocumentService documentService;

    @Test
    void concurrentInvoicesReceiveUniqueSequenceCodes() throws Exception {
        int documents = 6;
        List<String> orderCodes = new ArrayList<>();
        for (int index = 0; index < documents; index++) {
            orderCodes.add(createOrder(index).code());
        }

        CountDownLatch ready = new CountDownLatch(documents);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(documents);
        try {
            List<Future<FiscalDocumentResponse>> futures = new ArrayList<>();
            for (String orderCode : orderCodes) {
                futures.add(executor.submit(createInvoiceTask(orderCode, ready, start)));
            }
            await(ready);
            start.countDown();

            List<FiscalDocumentResponse> responses = new ArrayList<>();
            for (Future<FiscalDocumentResponse> future : futures) {
                responses.add(future.get(10, TimeUnit.SECONDS));
            }

            List<String> codes = responses.stream().map(FiscalDocumentResponse::code).toList();
            Set<String> uniqueCodes = codes.stream().collect(Collectors.toSet());
            Set<Long> uniqueSequences = responses.stream().map(FiscalDocumentResponse::sequenceNumber).collect(Collectors.toSet());
            assertThat(uniqueCodes).hasSize(documents);
            assertThat(uniqueSequences).hasSize(documents);
            assertThat(responses).allSatisfy(document -> {
                assertThat(document.code()).matches("FS-\\d{4}-\\d{4,}");
                assertThat(document.documentPrefix()).isEqualTo("FS");
                assertThat(document.fiscalYear()).isEqualTo(Year.now().getValue());
            });
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void creditNotesUseDedicatedSequenceCodes() {
        OrderResponse order = createOrder(99);
        documentService.createInvoice(new FiscalDocumentRequests.CreateInvoiceRequest(order.code()), actor());

        FiscalDocumentResponse creditNote = documentService.createCreditNote(
                new FiscalDocumentRequests.CreateCreditNoteRequest(order.code(), "Rettifica simulata"),
                actor()
        );

        assertThat(creditNote.code()).matches("NC-\\d{4}-\\d{4,}");
        assertThat(creditNote.documentPrefix()).isEqualTo("NC");
        assertThat(creditNote.fiscalYear()).isEqualTo(Year.now().getValue());
    }

    private Callable<FiscalDocumentResponse> createInvoiceTask(String orderCode, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            await(start);
            return documentService.createInvoice(new FiscalDocumentRequests.CreateInvoiceRequest(orderCode), actor());
        };
    }

    private OrderResponse createOrder(int index) {
        String productCode = "SEQ-DOC-" + UUID.randomUUID().toString().substring(0, 8);
        productService.create(productRequest(productCode));
        String customer = "document_sequence_customer_" + index;
        OrderResponse order = orderService.create(
                new OrderRequests.CreateOrderRequest(
                        customer,
                        PaymentMethod.BANK_TRANSFER,
                        List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
                ),
                customer,
                actor()
        );
        orderService.confirm(order.code(), actor());
        return orderService.fulfill(order.code(), actor());
    }

    private ProductRequest productRequest(String code) {
        return new ProductRequest(
                code,
                "Prodotto sequenza documento",
                "Prodotto creato per verificare la generazione concorrente dei codici documento.",
                ProductCategory.HARDWARE,
                "TestBrand",
                "Scheda di test",
                "",
                2,
                new BigDecimal("100.00"),
                new BigDecimal("0.00")
        );
    }

    private static AuthenticatedUser actor() {
        return new AuthenticatedUser("admin", UserRole.SUPER_ADMIN);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout durante il test concorrente.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test concorrente interrotto.", exception);
        }
    }
}
