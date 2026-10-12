package service;

import model.Order;
import model.OrderItem;
import model.Prodotto;
import model.StockMovementType;
import repository.DataRepository;
import repository.FileDataRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderService {
    private static final String FILE_ORDINI = "ordini.dat";
    private final DataRepository<List<Order>> orderRepository;
    private List<Order> orders = new ArrayList<>();

    public OrderService() {
        this(new FileDataRepository<>(FILE_ORDINI));
    }

    public OrderService(String storageFile) {
        this(createRepository(storageFile));
    }

    public OrderService(DataRepository<List<Order>> orderRepository) {
        if (orderRepository == null) {
            throw new IllegalArgumentException("Il repository ordini è obbligatorio.");
        }
        this.orderRepository = orderRepository;
        loadOrders();
    }

    private static DataRepository<List<Order>> createRepository(String storageFile) {
        if (isBlank(storageFile)) {
            throw new IllegalArgumentException("Il file di persistenza degli ordini non può essere vuoto.");
        }
        return new FileDataRepository<>(storageFile);
    }

    public Order createOrder(
            String customer,
            List<Prodotto> products,
            String paymentMethod,
            InventoryService inventoryService,
            String actor,
            String role
    ) {
        if (isBlank(customer)) {
            throw new IllegalArgumentException("Il cliente dell'ordine è obbligatorio.");
        }
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Il carrello è vuoto.");
        }
        if (isBlank(paymentMethod)) {
            throw new IllegalArgumentException("Seleziona un metodo di pagamento.");
        }
        if (inventoryService == null) {
            throw new IllegalArgumentException("Il servizio magazzino è obbligatorio.");
        }

        Map<String, Integer> requiredQuantities = countQuantitiesByProductCode(products);
        inventoryService.validateAvailability(requiredQuantities);

        String orderCode = generateOrderCode();
        List<OrderItem> items = buildItems(products);
        double total = items.stream().mapToDouble(OrderItem::lineTotal).sum();

        for (Map.Entry<String, Integer> entry : requiredQuantities.entrySet()) {
            inventoryService.registerMovement(
                    entry.getKey(),
                    StockMovementType.SCARICO,
                    entry.getValue(),
                    "Ordine " + orderCode,
                    actor,
                    role
            );
        }

        Order order = new Order(orderCode, customer.trim(), LocalDateTime.now(), paymentMethod, items, total);
        orders.add(order);
        saveOrders();
        return order;
    }

    public List<Order> getOrders() {
        List<Order> reversedOrders = new ArrayList<>(orders);
        Collections.reverse(reversedOrders);
        return reversedOrders;
    }

    public List<Order> getOrdersByCustomer(String customer) {
        if (isBlank(customer)) {
            return List.of();
        }
        String normalizedCustomer = customer.trim();
        return getOrders().stream()
                .filter(order -> order.customer().equalsIgnoreCase(normalizedCustomer))
                .toList();
    }

    private List<OrderItem> buildItems(List<Prodotto> products) {
        Map<String, OrderItemAccumulator> itemsByKey = new LinkedHashMap<>();
        for (Prodotto product : products) {
            String key = product.getCodice() + "|" + product.getNome() + "|" + product.getCostoScontato();
            OrderItemAccumulator accumulator = itemsByKey.computeIfAbsent(
                    key,
                    ignored -> new OrderItemAccumulator(product.getCodice(), product.getNome(), product.getCostoScontato())
            );
            accumulator.increment();
        }
        return itemsByKey.values().stream()
                .map(OrderItemAccumulator::toOrderItem)
                .toList();
    }

    private Map<String, Integer> countQuantitiesByProductCode(List<Prodotto> products) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (Prodotto product : products) {
            quantities.merge(product.getCodice(), 1, Integer::sum);
        }
        return quantities;
    }

    private String generateOrderCode() {
        return "ORD-" + String.format("%04d", orders.size() + 1);
    }

    private void saveOrders() {
        orderRepository.save(orders);
    }

    private void loadOrders() {
        List<Order> savedOrders = orderRepository.load();
        if (savedOrders != null) {
            orders = savedOrders;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class OrderItemAccumulator {
        private final String productCode;
        private final String productName;
        private final double unitPrice;
        private int quantity;

        private OrderItemAccumulator(String productCode, String productName, double unitPrice) {
            this.productCode = productCode;
            this.productName = productName;
            this.unitPrice = unitPrice;
        }

        private void increment() {
            quantity++;
        }

        private OrderItem toOrderItem() {
            return new OrderItem(productCode, productName, quantity, unitPrice, unitPrice * quantity);
        }
    }
}
