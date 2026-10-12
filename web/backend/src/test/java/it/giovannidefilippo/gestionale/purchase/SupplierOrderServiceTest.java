package it.giovannidefilippo.gestionale.purchase;

import it.giovannidefilippo.gestionale.partner.BusinessPartnerRequest;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerResponse;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerService;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerType;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductResponse;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SupplierOrderServiceTest {
    private static final AuthenticatedUser BUYER = new AuthenticatedUser(9001L, "buyer_test", UserRole.EMPLOYEE);

    @Autowired
    private SupplierOrderService service;

    @Autowired
    private BusinessPartnerService partnerService;

    @Autowired
    private ProductService productService;

    @Test
    void partialAndFinalReceiptUpdatePhysicalStockAndWeightedAverageCost() {
        BusinessPartnerResponse supplier = createPartner(BusinessPartnerType.SUPPLIER);
        ProductResponse product = createProduct("Scheda video test");
        SupplierOrderResponse created = createOrder(supplier.id(), product.code(), 3);

        SupplierOrderResponse sent = service.send(created.code(), BUYER);
        SupplierOrderResponse.ItemResponse line = sent.items().get(0);
        SupplierOrderResponse partial = service.receive(sent.code(), new SupplierOrderRequests.ReceiveRequest(
                "Consegna parziale DDT 1",
                List.of(new SupplierOrderRequests.ReceiveItemRequest(line.id(), 1, new BigDecimal("100.00")))
        ), BUYER);

        assertThat(partial.status()).isEqualTo(SupplierOrderStatus.PARTIALLY_RECEIVED);
        assertThat(partial.items().get(0).receivedQuantity()).isEqualTo(1);
        assertThat(partial.items().get(0).remainingQuantity()).isEqualTo(2);
        assertThat(partial.receipts()).singleElement().satisfies(receipt -> {
            assertThat(receipt.reason()).isEqualTo("Consegna parziale DDT 1");
            assertThat(receipt.items()).singleElement().satisfies(item -> {
                assertThat(item.quantity()).isEqualTo(1);
                assertThat(item.expectedUnitCost()).isEqualByComparingTo("150.0000");
                assertThat(item.actualUnitCost()).isEqualByComparingTo("100.0000");
                assertThat(item.unitCostVariance()).isEqualByComparingTo("-50.0000");
                assertThat(item.totalCost()).isEqualByComparingTo("100.0000");
                assertThat(item.inventoryPostingStatus()).isEqualTo("POSTED");
                assertThat(item.stockMovementId()).isPositive();
            });
        });
        ProductResponse afterPartial = productService.findByCode(product.code());
        assertThat(afterPartial.quantity()).isEqualTo(1);
        assertThat(afterPartial.costedQuantity()).isEqualTo(1);
        assertThat(afterPartial.averagePurchaseCost()).isEqualByComparingTo("100.0000");

        SupplierOrderResponse completed = service.receive(sent.code(), new SupplierOrderRequests.ReceiveRequest(
                "Saldo consegna DDT 2",
                List.of(new SupplierOrderRequests.ReceiveItemRequest(line.id(), 2, new BigDecimal("200.00")))
        ), BUYER);

        assertThat(completed.status()).isEqualTo(SupplierOrderStatus.RECEIVED);
        assertThat(completed.items().get(0).remainingQuantity()).isZero();
        assertThat(completed.receipts()).hasSize(2);
        assertThat(completed.supplierCode()).isEqualTo(supplier.code());
        assertThat(completed.items().get(0).productCode()).isEqualTo(product.code());
        ProductResponse stocked = productService.findByCode(product.code());
        assertThat(stocked.quantity()).isEqualTo(3);
        assertThat(stocked.lastPurchaseCost()).isEqualByComparingTo("200.0000");
        assertThat(stocked.averagePurchaseCost()).isEqualByComparingTo("166.6667");
        assertThat(stocked.costedQuantity()).isEqualTo(3);
        assertThat(stocked.uncostedQuantity()).isZero();
        assertThat(stocked.costCoveragePercentage()).isEqualByComparingTo("100.00");
        assertThat(stocked.knownInventoryCost()).isEqualByComparingTo("500.00");
        assertThat(stocked.potentialGrossMarginOnCostedStock()).isEqualByComparingTo("100.00");
    }

    @Test
    void cancellationClosesOnlyResidualAndPreservesAlreadyReceivedQuantity() {
        BusinessPartnerResponse supplier = createPartner(BusinessPartnerType.SUPPLIER);
        ProductResponse product = createProduct("Processore test");
        SupplierOrderResponse sent = service.send(createOrder(supplier.id(), product.code(), 4).code(), BUYER);
        long lineId = sent.items().get(0).id();
        service.receive(sent.code(), new SupplierOrderRequests.ReceiveRequest("Prima consegna", List.of(new SupplierOrderRequests.ReceiveItemRequest(lineId, 2))), BUYER);

        SupplierOrderResponse canceled = service.cancel(sent.code(), new SupplierOrderRequests.CancelRequest("Fornitore non consegna il residuo"), BUYER);

        assertThat(canceled.status()).isEqualTo(SupplierOrderStatus.CANCELED);
        assertThat(canceled.items().get(0).receivedQuantity()).isEqualTo(2);
        assertThat(canceled.items().get(0).remainingQuantity()).isEqualTo(2);
        assertThat(canceled.cancellationReason()).contains("residuo");
        assertThatThrownBy(() -> service.receive(sent.code(), new SupplierOrderRequests.ReceiveRequest("Tentativo tardivo", List.of(new SupplierOrderRequests.ReceiveItemRequest(lineId, 1))), BUYER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ordini inviati");
    }

    @Test
    void invalidSupplierDuplicateProductAndOverReceiptAreRejected() {
        BusinessPartnerResponse customer = createPartner(BusinessPartnerType.CUSTOMER);
        ProductResponse product = createProduct("Memoria test");

        assertThatThrownBy(() -> createOrder(customer.id(), product.code(), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non e un fornitore");

        BusinessPartnerResponse supplier = createPartner(BusinessPartnerType.SUPPLIER);
        LocalDate expected = LocalDate.now().plusDays(5);
        SupplierOrderRequests.CreateItemRequest line = new SupplierOrderRequests.CreateItemRequest(product.code(), 1, new BigDecimal("20.00"), null);
        assertThatThrownBy(() -> service.create(new SupplierOrderRequests.CreateRequest(supplier.id(), expected, "Duplicato", List.of(line, line)), BUYER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("una sola volta");

        SupplierOrderResponse sent = service.send(createOrder(supplier.id(), product.code(), 1).code(), BUYER);
        long lineId = sent.items().get(0).id();
        assertThatThrownBy(() -> service.receive(sent.code(), new SupplierOrderRequests.ReceiveRequest("Quantita errata", List.of(new SupplierOrderRequests.ReceiveItemRequest(sent.items().get(0).id(), 2))), BUYER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("supera il residuo");
        assertThatThrownBy(() -> service.receive(sent.code(), new SupplierOrderRequests.ReceiveRequest(
                "Riga duplicata",
                List.of(new SupplierOrderRequests.ReceiveItemRequest(lineId, 1), new SupplierOrderRequests.ReceiveItemRequest(lineId, 1))
        ), BUYER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("una sola volta");
        assertThatThrownBy(() -> service.receive(sent.code(), new SupplierOrderRequests.ReceiveRequest(
                "Costo non valido",
                List.of(new SupplierOrderRequests.ReceiveItemRequest(lineId, 1, new BigDecimal("-0.01")))
        ), BUYER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("costo");
    }

    @Test
    void productReferencedBySupplierOrderCannotBeRenamedOrDeleted() {
        BusinessPartnerResponse supplier = createPartner(BusinessPartnerType.SUPPLIER);
        ProductResponse product = createProduct("Prodotto con storico acquisti");
        createOrder(supplier.id(), product.code(), 1);

        ProductResponse visible = productService.findByCode(product.code(), BUYER);
        assertThat(visible.capabilities().canChangeCode()).isFalse();
        assertThat(visible.capabilities().canDelete()).isFalse();

        ProductRequest renamed = new ProductRequest(
                product.code() + "-NEW", product.name(), product.description(), product.category(), product.brand(),
                product.productType(), product.usageContext(), product.price(), product.discount()
        );
        assertThatThrownBy(() -> productService.update(product.code(), renamed, BUYER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gia presente in uno o piu ordini");
        assertThatThrownBy(() -> productService.delete(product.code()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Disattivalo");
    }

    private SupplierOrderResponse createOrder(long supplierId, String productCode, int quantity) {
        return service.create(new SupplierOrderRequests.CreateRequest(
                supplierId,
                LocalDate.now().plusDays(5),
                "Ordine test",
                List.of(new SupplierOrderRequests.CreateItemRequest(productCode, quantity, new BigDecimal("150.00"), null))
        ), BUYER);
    }

    private BusinessPartnerResponse createPartner(BusinessPartnerType type) {
        String code = (type == BusinessPartnerType.SUPPLIER ? "FOR" : "CLI") + "-" + UUID.randomUUID().toString().substring(0, 8);
        return partnerService.create(new BusinessPartnerRequest(code, type, "Partner " + code, "", type == BusinessPartnerType.SUPPLIER ? "IT12345678901" : "", "partner@example.com", "", "Via Test 1", "Napoli", ""), BUYER.username(), BUYER.roleLabel());
    }

    private ProductResponse createProduct(String name) {
        String code = "PUR-" + UUID.randomUUID().toString().substring(0, 8);
        return productService.create(new ProductRequest(code, name, "Prodotto usato nei test degli ordini fornitore.", ProductCategory.HARDWARE, "TestBrand", "Componente", "", new BigDecimal("200.00"), BigDecimal.ZERO), BUYER);
    }
}
