package service;

import model.FiscalDocument;
import model.FiscalDocumentType;
import model.Order;
import model.OrderItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiscalDocumentServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void generaFatturaSimulataDaOrdine() {
        FiscalDocumentService service = service();

        FiscalDocument document = service.createInvoiceFromOrder(order(), "admin", "Admin");

        assertEquals("FS-0001", document.code());
        assertEquals(FiscalDocumentType.SIMULATED_INVOICE, document.type());
        assertEquals("ORD-0001", document.relatedOrderCode());
        assertEquals(131.15, document.taxableAmount());
        assertEquals(28.85, document.vatAmount());
        assertEquals(160, document.totalAmount());
        assertTrue(document.disclaimer().contains("NON VALIDO AI FINI FISCALI"));
    }

    @Test
    void impedisceFatturaDuplicataPerLoStessoOrdine() {
        FiscalDocumentService service = service();
        Order order = order();

        service.createInvoiceFromOrder(order, "admin", "Admin");

        assertThrows(IllegalArgumentException.class, () -> service.createInvoiceFromOrder(order, "admin", "Admin"));
    }

    @Test
    void generaNotaCreditoSoloDopoFatturaSimulata() {
        FiscalDocumentService service = service();
        Order order = order();

        assertThrows(IllegalArgumentException.class, () -> service.createCreditNoteFromOrder(order, "Reso", "admin", "Admin"));

        service.createInvoiceFromOrder(order, "admin", "Admin");
        FiscalDocument creditNote = service.createCreditNoteFromOrder(order, "Reso cliente", "admin", "Admin");

        assertEquals("NC-0001", creditNote.code());
        assertEquals(FiscalDocumentType.SIMULATED_CREDIT_NOTE, creditNote.type());
        assertEquals("Reso cliente", creditNote.reason());
    }

    @Test
    void esportaDocumentoConAvvisoDiSimulazione() {
        FiscalDocumentService service = service();
        FiscalDocument document = service.createInvoiceFromOrder(order(), "admin", "Admin");

        String text = service.buildDocumentText(document);

        assertTrue(text.contains("DOCUMENTO SIMULATO - NON VALIDO AI FINI FISCALI"));
        assertTrue(text.contains("Fattura simulata FS-0001"));
        assertTrue(text.contains("Imponibile simulato: 131,15 euro"));
        assertTrue(text.contains("Sistema di Interscambio"));
    }

    private FiscalDocumentService service() {
        return new FiscalDocumentService(tempDir.resolve("documenti.dat").toString());
    }

    private Order order() {
        return new Order(
                "ORD-0001",
                "cliente",
                LocalDateTime.of(2026, 7, 3, 12, 0),
                "Contanti",
                List.of(new OrderItem("GPU-1", "Scheda grafica", 2, 80, 160)),
                160
        );
    }
}
