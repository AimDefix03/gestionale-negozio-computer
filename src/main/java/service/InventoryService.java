package service;

import model.Prodotto;
import model.StockMovement;
import model.StockMovementType;
import repository.DataRepository;
import repository.FileDataRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InventoryService {
    public static final int LOW_STOCK_THRESHOLD = 3;
    private static final String FILE_MOVIMENTI = "movimenti-magazzino.dat";
    private final ProductService productService;
    private final DataRepository<List<StockMovement>> movementRepository;
    private List<StockMovement> movements = new ArrayList<>();

    public InventoryService(ProductService productService) {
        this(productService, new FileDataRepository<>(FILE_MOVIMENTI));
    }

    public InventoryService(ProductService productService, String storageFile) {
        this(productService, createRepository(storageFile));
    }

    public InventoryService(ProductService productService, DataRepository<List<StockMovement>> movementRepository) {
        if (productService == null) {
            throw new IllegalArgumentException("Il servizio prodotti è obbligatorio.");
        }
        if (movementRepository == null) {
            throw new IllegalArgumentException("Il repository movimenti è obbligatorio.");
        }
        this.productService = productService;
        this.movementRepository = movementRepository;
        loadMovements();
    }

    private static DataRepository<List<StockMovement>> createRepository(String storageFile) {
        if (isBlank(storageFile)) {
            throw new IllegalArgumentException("Il file di persistenza dei movimenti non può essere vuoto.");
        }
        return new FileDataRepository<>(storageFile);
    }

    public StockMovement registerMovement(
            String productCode,
            StockMovementType type,
            int quantity,
            String reason,
            String actor,
            String role
    ) {
        if (type == null) {
            throw new IllegalArgumentException("Seleziona un tipo movimento valido.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantità del movimento deve essere maggiore di zero.");
        }
        if (isBlank(reason)) {
            throw new IllegalArgumentException("Inserisci una causale per il movimento.");
        }

        Prodotto product = productService.getProdottoByCodice(productCode);
        int previousQuantity = product.getQuantita();
        int newQuantity = switch (type) {
            case CARICO -> previousQuantity + quantity;
            case SCARICO -> previousQuantity - quantity;
        };

        if (newQuantity < 0) {
            throw new IllegalArgumentException("Lo scarico supera la quantità disponibile.");
        }

        Prodotto updatedProduct = productService.aggiornaQuantita(productCode, newQuantity);
        StockMovement movement = new StockMovement(
                LocalDateTime.now(),
                sanitize(actor),
                sanitize(role),
                updatedProduct.getCodice(),
                updatedProduct.getNome(),
                type,
                quantity,
                previousQuantity,
                newQuantity,
                reason.trim()
        );
        movements.add(movement);
        saveMovements();
        return movement;
    }

    public List<StockMovement> getMovements() {
        List<StockMovement> reversedMovements = new ArrayList<>(movements);
        Collections.reverse(reversedMovements);
        return reversedMovements;
    }

    public List<Prodotto> getLowStockProducts() {
        return productService.getProdotti().stream()
                .filter(product -> product.getQuantita() <= LOW_STOCK_THRESHOLD)
                .toList();
    }

    public void validateAvailability(Map<String, Integer> requiredQuantities) {
        if (requiredQuantities == null || requiredQuantities.isEmpty()) {
            throw new IllegalArgumentException("Nessuna quantità da verificare.");
        }

        for (Map.Entry<String, Integer> entry : requiredQuantities.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Quantità richiesta non valida.");
            }

            Prodotto product = productService.getProdottoByCodice(entry.getKey());
            if (product.getQuantita() < entry.getValue()) {
                throw new IllegalArgumentException("Scorte insufficienti per il prodotto " + product.getCodice() + ".");
            }
        }
    }

    private void saveMovements() {
        movementRepository.save(movements);
    }

    private void loadMovements() {
        List<StockMovement> savedMovements = movementRepository.load();
        if (savedMovements != null) {
            movements = savedMovements;
        }
    }

    private String sanitize(String value) {
        if (isBlank(value)) {
            return "-";
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
