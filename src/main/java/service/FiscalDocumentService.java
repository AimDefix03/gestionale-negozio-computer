package service;

import model.FiscalDocument;
import model.FiscalDocumentLine;
import model.FiscalDocumentStatus;
import model.FiscalDocumentType;
import model.Order;
import model.OrderItem;
import repository.DataRepository;
import repository.FileDataRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class FiscalDocumentService {
    private static final String FILE_DOCUMENTI = "documenti-fiscali-simulati.dat";
    private static final double VAT_RATE = 0.22;
    private static final String DISCLAIMER = "DOCUMENTO SIMULATO - NON VALIDO AI FINI FISCALI";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final DataRepository<List<FiscalDocument>> documentRepository;
    private List<FiscalDocument> documents = new ArrayList<>();

    public FiscalDocumentService() {
        this(new FileDataRepository<>(FILE_DOCUMENTI));
    }

    public FiscalDocumentService(String storageFile) {
        this(createRepository(storageFile));
    }

    public FiscalDocumentService(DataRepository<List<FiscalDocument>> documentRepository) {
        if (documentRepository == null) {
            throw new IllegalArgumentException("Il repository documenti è obbligatorio.");
        }
        this.documentRepository = documentRepository;
        loadDocuments();
    }

    private static DataRepository<List<FiscalDocument>> createRepository(String storageFile) {
        if (isBlank(storageFile)) {
            throw new IllegalArgumentException("Il file di persistenza dei documenti non può essere vuoto.");
        }
        return new FileDataRepository<>(storageFile);
    }

    public FiscalDocument createInvoiceFromOrder(Order order, String actor, String role) {
        validateOrder(order);
        if (findByOrderAndType(order.code(), FiscalDocumentType.SIMULATED_INVOICE).isPresent()) {
            throw new IllegalArgumentException("Esiste già una fattura simulata per questo ordine.");
        }

        FiscalDocument document = buildDocument(
                FiscalDocumentType.SIMULATED_INVOICE,
                order,
                actor,
                role,
                "Documento generato da ordine " + order.code()
        );
        documents.add(document);
        saveDocuments();
        return document;
    }

    public FiscalDocument createCreditNoteFromOrder(Order order, String reason, String actor, String role) {
        validateOrder(order);
        if (findByOrderAndType(order.code(), FiscalDocumentType.SIMULATED_INVOICE).isEmpty()) {
            throw new IllegalArgumentException("Genera prima una fattura simulata per questo ordine.");
        }
        if (findByOrderAndType(order.code(), FiscalDocumentType.SIMULATED_CREDIT_NOTE).isPresent()) {
            throw new IllegalArgumentException("Esiste già una nota credito simulata per questo ordine.");
        }
        if (isBlank(reason)) {
            throw new IllegalArgumentException("Inserisci una causale per la nota credito simulata.");
        }

        FiscalDocument document = buildDocument(
                FiscalDocumentType.SIMULATED_CREDIT_NOTE,
                order,
                actor,
                role,
                reason.trim()
        );
        documents.add(document);
        saveDocuments();
        return document;
    }

    public List<FiscalDocument> getDocuments() {
        List<FiscalDocument> reversedDocuments = new ArrayList<>(documents);
        Collections.reverse(reversedDocuments);
        return reversedDocuments;
    }

    public List<FiscalDocument> getDocumentsByCustomer(String customer) {
        if (isBlank(customer)) {
            return List.of();
        }
        String normalizedCustomer = customer.trim();
        return getDocuments().stream()
                .filter(document -> document.customer().equalsIgnoreCase(normalizedCustomer))
                .toList();
    }

    public Optional<FiscalDocument> findByOrderAndType(String orderCode, FiscalDocumentType type) {
        if (isBlank(orderCode) || type == null) {
            return Optional.empty();
        }
        return documents.stream()
                .filter(document -> document.relatedOrderCode().equalsIgnoreCase(orderCode.trim()))
                .filter(document -> document.type() == type)
                .findFirst();
    }

    public String buildDocumentText(FiscalDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("Il documento è obbligatorio.");
        }

        StringBuilder text = new StringBuilder();
        text.append("GESTIONALE NEGOZIO COMPUTER").append(System.lineSeparator());
        text.append(document.disclaimer()).append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append(document.type().getLabel()).append(" ").append(document.code()).append(System.lineSeparator());
        text.append("Stato: ").append(document.status().getLabel()).append(System.lineSeparator());
        text.append("Data: ").append(document.createdAt().format(FORMATTER)).append(System.lineSeparator());
        text.append("Ordine collegato: ").append(document.relatedOrderCode()).append(System.lineSeparator());
        text.append("Cliente: ").append(document.customer()).append(System.lineSeparator());
        text.append("Pagamento: ").append(document.paymentMethod()).append(System.lineSeparator());
        text.append("Operatore: ").append(document.createdBy()).append(" (").append(document.createdByRole()).append(")").append(System.lineSeparator());
        text.append("Causale: ").append(document.reason()).append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Righe").append(System.lineSeparator());

        for (FiscalDocumentLine line : document.lines()) {
            text.append("- ")
                    .append(line.productCode())
                    .append(" | ")
                    .append(line.description())
                    .append(" | Quantita: ")
                    .append(line.quantity())
                    .append(" | Prezzo: ")
                    .append(formatCurrency(line.unitPrice()))
                    .append(" | Totale riga: ")
                    .append(formatCurrency(line.lineTotal()))
                    .append(System.lineSeparator());
        }

        text.append(System.lineSeparator());
        text.append("Imponibile simulato: ").append(formatCurrency(document.taxableAmount())).append(System.lineSeparator());
        text.append("IVA simulata: ").append(formatPercent(document.vatRate())).append(" - ").append(formatCurrency(document.vatAmount())).append(System.lineSeparator());
        text.append("Totale simulato: ").append(formatCurrency(document.totalAmount())).append(System.lineSeparator());
        text.append(System.lineSeparator());
        text.append("Questo documento non sostituisce fattura elettronica, documento commerciale, registratore telematico o trasmissione al Sistema di Interscambio.").append(System.lineSeparator());
        return text.toString();
    }

    private FiscalDocument buildDocument(
            FiscalDocumentType type,
            Order order,
            String actor,
            String role,
            String reason
    ) {
        double taxableAmount = round(order.total() / (1 + VAT_RATE));
        double vatAmount = round(order.total() - taxableAmount);
        return new FiscalDocument(
                generateCode(type),
                type,
                FiscalDocumentStatus.ISSUED,
                LocalDateTime.now(),
                order.code(),
                order.customer(),
                order.paymentMethod(),
                buildLines(order),
                taxableAmount,
                VAT_RATE,
                vatAmount,
                round(order.total()),
                sanitize(actor),
                sanitize(role),
                reason,
                DISCLAIMER
        );
    }

    private List<FiscalDocumentLine> buildLines(Order order) {
        return order.items().stream()
                .map(this::toDocumentLine)
                .toList();
    }

    private FiscalDocumentLine toDocumentLine(OrderItem item) {
        return new FiscalDocumentLine(
                item.productCode(),
                item.productName(),
                item.quantity(),
                round(item.unitPrice()),
                round(item.lineTotal())
        );
    }

    private String generateCode(FiscalDocumentType type) {
        long documentCount = documents.stream()
                .filter(document -> document.type() == type)
                .count();
        return type.getPrefix() + "-" + String.format("%04d", documentCount + 1);
    }

    private void validateOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Seleziona un ordine valido.");
        }
    }

    private void saveDocuments() {
        documentRepository.save(documents);
    }

    private void loadDocuments() {
        List<FiscalDocument> savedDocuments = documentRepository.load();
        if (savedDocuments != null) {
            documents = savedDocuments;
        }
    }

    private static String sanitize(String value) {
        if (isBlank(value)) {
            return "-";
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String formatCurrency(double value) {
        return String.format(Locale.ITALY, "%.2f euro", value);
    }

    private String formatPercent(double value) {
        return String.format(Locale.ITALY, "%.0f%%", value * 100);
    }
}
