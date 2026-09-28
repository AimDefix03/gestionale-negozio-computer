package it.giovannidefilippo.gestionale.inventory;

import it.giovannidefilippo.gestionale.common.PostgreSqlIntegrationTestSupport;
import it.giovannidefilippo.gestionale.common.ResourceConflictException;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Tag("postgresql")
class PhysicalInventoryServiceTest extends PostgreSqlIntegrationTestSupport {
    private static final AuthenticatedUser OPERATOR = new AuthenticatedUser("inventory_operator", UserRole.EMPLOYEE);
    private static final AuthenticatedUser APPROVER = new AuthenticatedUser("inventory_approver", UserRole.ADMIN);

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PhysicalInventoryService physicalInventoryService;

    @Test
    void approvalCompensatesMovementsRecordedAfterThePhysicalCount() {
        String code = createProductWithStock(10);
        PhysicalInventorySessionResponse session = createSession(code);
        Long itemId = session.items().get(0).id();

        PhysicalInventorySessionResponse counted = physicalInventoryService.recordCount(
                session.id(), itemId, new PhysicalInventoryRequests.Count(8, "Conteggio scaffale"), OPERATOR
        );
        inventoryService.register(code, StockMovementType.LOAD, 3, "Merce ricevuta durante il conteggio", "warehouse", "Dipendente");
        physicalInventoryService.submit(session.id(), OPERATOR);

        PhysicalInventorySessionResponse approved = physicalInventoryService.approve(
                session.id(), new PhysicalInventoryRequests.Decision("Differenza verificata dal responsabile"), APPROVER
        );

        PhysicalInventoryItemResponse item = approved.items().get(0);
        assertThat(counted.items().get(0).differenceQuantity()).isEqualTo(-2);
        assertThat(item.quantityBeforeApproval()).isEqualTo(13);
        assertThat(item.quantityAfterApproval()).isEqualTo(11);
        assertThat(item.compensatedMovementDelta()).isEqualTo(3);
        assertThat(item.stockMovementId()).isNotNull();
        assertThat(productService.findByCode(code).quantity()).isEqualTo(11);
        assertThat(inventoryService.recentForProduct(code, 10))
                .anySatisfy(movement -> {
                    assertThat(movement.type()).isEqualTo(StockMovementType.PHYSICAL_INVENTORY_DECREASE);
                    assertThat(movement.origin()).isEqualTo(StockMovementOrigin.PHYSICAL_INVENTORY);
                    assertThat(movement.physicalInventorySessionId()).isEqualTo(session.id());
                    assertThat(movement.physicalInventoryItemId()).isEqualTo(itemId);
                });
    }

    @Test
    void submitterCannotApproveAndAnApprovedSessionCannotBeApprovedTwice() {
        String code = createProductWithStock(5);
        PhysicalInventorySessionResponse session = createSession(code);
        physicalInventoryService.recordCount(
                session.id(), session.items().get(0).id(), new PhysicalInventoryRequests.Count(6, null), OPERATOR
        );
        physicalInventoryService.submit(session.id(), OPERATOR);

        assertThatThrownBy(() -> physicalInventoryService.approve(
                session.id(), new PhysicalInventoryRequests.Decision("Auto approvazione"), OPERATOR
        )).isInstanceOf(IllegalStateException.class).hasMessageContaining("non può approvare");

        physicalInventoryService.approve(
                session.id(), new PhysicalInventoryRequests.Decision("Approvazione separata"), APPROVER
        );

        assertThatThrownBy(() -> physicalInventoryService.approve(
                session.id(), new PhysicalInventoryRequests.Decision("Seconda approvazione"), APPROVER
        )).isInstanceOf(IllegalStateException.class).hasMessageContaining("non è disponibile");
        assertThat(productService.findByCode(code).quantity()).isEqualTo(6);
    }

    @Test
    void approvalCannotReduceStockBelowReservedQuantity() {
        String code = createProductWithStock(10);
        PhysicalInventorySessionResponse session = createSession(code);
        physicalInventoryService.recordCount(
                session.id(), session.items().get(0).id(), new PhysicalInventoryRequests.Count(4, "Conteggio inferiore"), OPERATOR
        );
        productService.reserveStock(code, 7);
        physicalInventoryService.submit(session.id(), OPERATOR);

        assertThatThrownBy(() -> physicalInventoryService.approve(
                session.id(), new PhysicalInventoryRequests.Decision("Tentativo non compatibile"), APPROVER
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("disponibilità vendibile");

        PhysicalInventorySessionResponse unchanged = physicalInventoryService.find(session.id(), APPROVER);
        assertThat(unchanged.status()).isEqualTo(PhysicalInventoryStatus.SUBMITTED);
        assertThat(unchanged.items().get(0).stockMovementId()).isNull();
        assertThat(productService.findByCode(code).quantity()).isEqualTo(10);
        assertThat(productService.findByCode(code).reservedQuantity()).isEqualTo(7);
    }

    @Test
    void productCannotBelongToTwoActivePhysicalInventorySessions() {
        String code = createProductWithStock(3);
        createSession(code);

        assertThatThrownBy(() -> createSession(code))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("sessione di inventario aperta");
    }

    @Test
    void sessionRejectsTheSameProductCodeWithDifferentLetterCase() {
        String code = createProductWithStock(3);

        assertThatThrownBy(() -> physicalInventoryService.create(
                new PhysicalInventoryRequests.Create(
                        "Verifica inventario periodica",
                        List.of(code.toLowerCase(), code.toUpperCase())
                ),
                OPERATOR
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non duplicati");
    }

    private PhysicalInventorySessionResponse createSession(String code) {
        return physicalInventoryService.create(
                new PhysicalInventoryRequests.Create("Verifica inventario periodica", List.of(code)), OPERATOR
        );
    }

    private String createProductWithStock(int quantity) {
        String code = "PHY-" + UUID.randomUUID().toString().substring(0, 8);
        InventoryTestSupport.createProductWithStock(productService, inventoryService, request(code), quantity);
        return code;
    }

    private ProductRequest request(String code) {
        return new ProductRequest(
                code,
                "Prodotto inventario fisico",
                "Prodotto creato per verificare il conteggio fisico.",
                ProductCategory.HARDWARE,
                "InventoryBrand",
                "Componente",
                "",
                new BigDecimal("100.00"),
                BigDecimal.ZERO
        );
    }
}
