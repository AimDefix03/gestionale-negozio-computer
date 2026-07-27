package it.giovannidefilippo.gestionale.document;

import it.giovannidefilippo.gestionale.company.CompanySettingsRequests;
import it.giovannidefilippo.gestionale.company.CompanySettingsResponse;
import it.giovannidefilippo.gestionale.company.CompanySettingsService;
import it.giovannidefilippo.gestionale.common.ResourceConflictException;
import it.giovannidefilippo.gestionale.order.OrderRequests;
import it.giovannidefilippo.gestionale.order.OrderResponse;
import it.giovannidefilippo.gestionale.order.OrderService;
import it.giovannidefilippo.gestionale.order.PaymentMethod;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerRequest;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerService;
import it.giovannidefilippo.gestionale.partner.BusinessPartnerType;
import it.giovannidefilippo.gestionale.product.ProductCategory;
import it.giovannidefilippo.gestionale.product.ProductRequest;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class FiscalDocumentCustomerSnapshotTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private BusinessPartnerService partnerService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FiscalDocumentService documentService;

    @Autowired
    private CompanySettingsService companySettingsService;

    @Test
    void invoiceKeepsCustomerSnapshotEvenIfRegistryChangesLater() {
        String customerCode = uniqueCode("CLI");
        createPartner(customerCode, "Cliente Originale", "CFORIG", "IT001", "originale@example.com", "Napoli");
        OrderResponse order = createFulfilledOrder(customerCode);

        FiscalDocumentResponse invoice = documentService.createInvoice(new FiscalDocumentRequests.CreateInvoiceRequest(order.code()), actor());
        partnerService.update(customerCode, partnerRequest(customerCode, "Cliente Modificato", "CFMOD", "IT999", "modificato@example.com", "Roma"), "admin", "Super Admin");

        assertThat(invoice.customer()).isEqualTo("Cliente Originale");
        assertThat(invoice.customerSnapshotCode()).isEqualTo(customerCode);
        assertThat(invoice.customerSnapshotName()).isEqualTo("Cliente Originale");
        assertThat(invoice.customerSnapshotTaxCode()).isEqualTo("CFORIG");
        assertThat(invoice.customerSnapshotVatNumber()).isEqualTo("IT001");
        assertThat(invoice.customerSnapshotEmail()).isEqualTo("originale@example.com");
        assertThat(invoice.customerSnapshotCity()).isEqualTo("Napoli");
    }

    @Test
    void creditNoteUsesInvoiceCustomerSnapshot() {
        String customerCode = uniqueCode("CLI");
        createPartner(customerCode, "Cliente Nota", "CFNOTA", "IT002", "nota@example.com", "Milano");
        OrderResponse order = createFulfilledOrder(customerCode);
        documentService.createInvoice(new FiscalDocumentRequests.CreateInvoiceRequest(order.code()), actor());
        partnerService.update(customerCode, partnerRequest(customerCode, "Cliente Cambiato", "CFCAMB", "IT888", "cambiato@example.com", "Torino"), "admin", "Super Admin");

        FiscalDocumentResponse creditNote = documentService.createCreditNote(new FiscalDocumentRequests.CreateCreditNoteRequest(order.code(), "Rettifica simulata"), actor());

        assertThat(creditNote.customer()).isEqualTo("Cliente Nota");
        assertThat(creditNote.customerSnapshotCode()).isEqualTo(customerCode);
        assertThat(creditNote.customerSnapshotName()).isEqualTo("Cliente Nota");
        assertThat(creditNote.customerSnapshotTaxCode()).isEqualTo("CFNOTA");
        assertThat(creditNote.customerSnapshotVatNumber()).isEqualTo("IT002");
        assertThat(creditNote.customerSnapshotEmail()).isEqualTo("nota@example.com");
        assertThat(creditNote.customerSnapshotCity()).isEqualTo("Milano");
    }

    @Test
    void documentsKeepIssuerAndVatSnapshotWhenCompanySettingsChange() {
        CompanySettingsResponse current = companySettingsService.current();
        CompanySettingsResponse original = companySettingsService.update(companyRequest(
                current, "Azienda Originale", "CFORIG", "IT001", new BigDecimal("0.1000")
        ), actor());
        String customerCode = uniqueCode("CLI");
        createPartner(customerCode, "Cliente Snapshot", "CFCLI", "ITCLI", "cliente@example.com", "Napoli");
        OrderResponse order = createFulfilledOrder(customerCode);

        FiscalDocumentResponse invoice = documentService.createInvoice(
                new FiscalDocumentRequests.CreateInvoiceRequest(order.code()), actor()
        );
        CompanySettingsResponse changed = companySettingsService.update(companyRequest(
                original, "Azienda Modificata", "CFMOD", "IT999", new BigDecimal("0.2200")
        ), actor());
        FiscalDocumentResponse creditNote = documentService.createCreditNote(
                new FiscalDocumentRequests.CreateCreditNoteRequest(order.code(), "Rettifica simulata"), actor()
        );

        assertThat(invoice.companySnapshotLegalName()).isEqualTo("Azienda Originale");
        assertThat(invoice.companySnapshotTaxCode()).isEqualTo("CFORIG");
        assertThat(invoice.companySnapshotVatNumber()).isEqualTo("IT001");
        assertThat(invoice.vatRate()).isEqualByComparingTo("0.1");
        assertThat(creditNote.companySnapshotLegalName()).isEqualTo("Azienda Originale");
        assertThat(creditNote.companySnapshotTaxCode()).isEqualTo("CFORIG");
        assertThat(creditNote.vatRate()).isEqualByComparingTo("0.1");

        assertThatThrownBy(() -> companySettingsService.update(new CompanySettingsRequests.UpdateRequest(
                changed.version(), changed.legalName(), changed.taxCode(), changed.vatNumber(), changed.email(), changed.phone(),
                changed.address(), changed.postalCode(), changed.city(), changed.province(), changed.countryCode(),
                changed.defaultVatRate(), "FI", changed.creditNotePrefix(), changed.numberPadding()
        ), actor()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("primo documento");
    }

    private OrderResponse createFulfilledOrder(String customerCode) {
        String productCode = uniqueCode("DOC-PROD");
        productService.create(new ProductRequest(
                productCode,
                "Prodotto snapshot",
                "Prodotto creato per verificare snapshot cliente nei documenti.",
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
                        "cliente libero",
                        customerCode,
                        PaymentMethod.BANK_TRANSFER,
                        List.of(new OrderRequests.CreateOrderItemRequest(productCode, 1))
                ),
                "cliente libero",
                actor()
        );
        orderService.confirm(order.code(), actor());
        return orderService.fulfill(order.code(), actor());
    }

    private void createPartner(String code, String name, String taxCode, String vatNumber, String email, String city) {
        partnerService.create(partnerRequest(code, name, taxCode, vatNumber, email, city), "admin", "Super Admin");
    }

    private BusinessPartnerRequest partnerRequest(String code, String name, String taxCode, String vatNumber, String email, String city) {
        return new BusinessPartnerRequest(
                code,
                BusinessPartnerType.CUSTOMER,
                name,
                taxCode,
                vatNumber,
                email,
                "0810000000",
                "Via Test 1",
                city,
                "Cliente test"
        );
    }

    private static String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static CompanySettingsRequests.UpdateRequest companyRequest(
            CompanySettingsResponse current,
            String legalName,
            String taxCode,
            String vatNumber,
            BigDecimal vatRate
    ) {
        return new CompanySettingsRequests.UpdateRequest(
                current.version(), legalName, taxCode, vatNumber, "amministrazione@example.com", "+39 0000000000",
                "Via Test 1", "80100", "Napoli", "NA", "IT", vatRate,
                current.invoicePrefix(), current.creditNotePrefix(), current.numberPadding()
        );
    }

    private static AuthenticatedUser actor() {
        return new AuthenticatedUser("admin", UserRole.SUPER_ADMIN);
    }
}
