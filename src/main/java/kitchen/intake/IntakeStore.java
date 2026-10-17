package kitchen.intake;

import kitchen.storage.CsvFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class IntakeStore {

    private static final Map<String, Integer> DEFAULT_STOCK = Map.of(
            "bun", 10, "beef", 10, "potato", 20, "dough", 5, "cheese", 5, "tomato", 5);

    private final CsvFile ordersFile;
    private final CsvFile stockFile;

    public IntakeStore(Path dataDir) {
        this.ordersFile = new CsvFile(dataDir.resolve("orders.csv"));
        this.stockFile = new CsvFile(dataDir.resolve("stock.csv"));
    }

    public List<KitchenOrder> loadOrders() {
        List<KitchenOrder> orders = new ArrayList<>();
        for (String[] row : ordersFile.readRows()) {
            orders.add(new KitchenOrder(Integer.parseInt(row[0]), row[1], Integer.parseInt(row[2]), row[3],
                    KitchenOrder.Status.valueOf(row[4])));
        }
        return orders;
    }

    public Optional<KitchenOrder> findOrder(int id) {
        return loadOrders().stream().filter(order -> order.getId() == id).findFirst();
    }

    public int nextOrderId() {
        return loadOrders().stream().mapToInt(KitchenOrder::getId).max().orElse(0) + 1;
    }

    public void saveOrder(KitchenOrder order) {
        List<String[]> rows = new ArrayList<>();
        for (KitchenOrder saved : loadOrders()) {
            if (saved.getId() != order.getId()) {
                rows.add(toRow(saved));
            }
        }
        rows.add(toRow(order));
        rows.sort((a, b) -> Integer.parseInt(a[0]) - Integer.parseInt(b[0]));
        ordersFile.writeRows(rows);
    }

    public Map<String, Integer> loadStock() {
        if (!stockFile.exists()) {
            return new TreeMap<>(DEFAULT_STOCK);
        }
        Map<String, Integer> stock = new TreeMap<>();
        for (String[] row : stockFile.readRows()) {
            stock.put(row[0], Integer.parseInt(row[1]));
        }
        return stock;
    }

    public void saveStock(Map<String, Integer> stock) {
        List<String[]> rows = new ArrayList<>();
        new TreeMap<>(stock).forEach((ingredient, amount) -> rows.add(new String[]{ingredient, String.valueOf(amount)}));
        stockFile.writeRows(rows);
    }

    public void reset() {
        ordersFile.delete();
        stockFile.delete();
    }

    private String[] toRow(KitchenOrder order) {
        return new String[]{String.valueOf(order.getId()), order.getDish(), String.valueOf(order.getQuantity()),
                order.getZone(), order.getStatus().name()};
    }
}
