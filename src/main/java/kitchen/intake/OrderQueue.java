package kitchen.intake;

import java.util.List;
import java.util.Map;

public class OrderQueue {

    private final IntakeStore store;
    private final InventoryChecker checker = new InventoryChecker();

    public OrderQueue(IntakeStore store) {
        this.store = store;
    }

    public KitchenOrder place(String dish, int quantity, String zone) {
        if (zone == null || zone.isBlank()) {
            throw new IntakeException("delivery zone is missing");
        }
        Map<String, Integer> needed = checker.ingredientsFor(dish, quantity);
        Map<String, Integer> stock = store.loadStock();
        List<String> missing = checker.missingIngredients(stock, needed);
        if (!missing.isEmpty()) {
            throw new IntakeException("not enough " + String.join(", ", missing) + " for " + quantity + " " + dish);
        }
        needed.forEach((ingredient, amount) -> stock.merge(ingredient, -amount, Integer::sum));
        store.saveStock(stock);
        KitchenOrder order = new KitchenOrder(store.nextOrderId(), dish, quantity, zone, KitchenOrder.Status.RESERVED);
        store.saveOrder(order);
        return order;
    }

    public KitchenOrder cancel(int orderId) {
        KitchenOrder order = find(orderId);
        if (order.getStatus() == KitchenOrder.Status.CANCELLED) {
            return order;
        }
        KitchenOrder cancelled = order.withStatus(KitchenOrder.Status.CANCELLED);
        Map<String, Integer> stock = store.loadStock();
        checker.ingredientsFor(order.getDish(), order.getQuantity())
                .forEach((ingredient, amount) -> stock.merge(ingredient, amount, Integer::sum));
        store.saveStock(stock);
        store.saveOrder(cancelled);
        return cancelled;
    }

    public KitchenOrder markServed(int orderId) {
        KitchenOrder order = find(orderId);
        if (order.getStatus() == KitchenOrder.Status.SERVED) {
            return order;
        }
        KitchenOrder served = order.withStatus(KitchenOrder.Status.SERVED);
        store.saveOrder(served);
        return served;
    }

    public void restock(String ingredient, int amount) {
        if (amount < 1) {
            throw new IntakeException("restock amount must be positive");
        }
        Map<String, Integer> stock = store.loadStock();
        stock.merge(ingredient, amount, Integer::sum);
        store.saveStock(stock);
    }

    public KitchenOrder find(int orderId) {
        return store.findOrder(orderId).orElseThrow(() -> new IntakeException("order " + orderId + " not found"));
    }

    public List<KitchenOrder> orders() {
        return store.loadOrders();
    }

    public Map<String, Integer> stock() {
        return store.loadStock();
    }

    public List<String> menu() {
        return checker.menu();
    }

    public void reset() {
        store.reset();
    }
}
