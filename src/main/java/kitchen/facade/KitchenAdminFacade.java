package kitchen.facade;

import kitchen.checkout.Bill;
import kitchen.checkout.BillingModule;
import kitchen.checkout.CheckoutStore;
import kitchen.cooking.CookingLine;
import kitchen.cooking.CookingStore;
import kitchen.cooking.CookingTicket;
import kitchen.intake.KitchenOrder;
import kitchen.intake.OrderQueue;

import java.util.ArrayList;
import java.util.List;

public class KitchenAdminFacade {

    private static final String MANAGER_CANCEL = "cancelled by manager";

    private final KitchenFacade kitchen;
    private final OrderQueue orders;
    private final CookingLine cooking;
    private final BillingModule billing;
    private final ChainStore chains;

    public KitchenAdminFacade(KitchenFacade kitchen, OrderQueue orders, CookingLine cooking, BillingModule billing,
                              ChainStore chains) {
        this.kitchen = kitchen;
        this.orders = orders;
        this.cooking = cooking;
        this.billing = billing;
        this.chains = chains;
    }

    public List<ChainRecord> unfinishedOrders() {
        return chains.unfinished();
    }

    public List<ChainRecord> resumeUnfinished() {
        List<ChainRecord> results = new ArrayList<>();
        for (ChainRecord chain : chains.unfinished()) {
            results.add(kitchen.finishOrder(chain.getId()));
        }
        return results;
    }

    public ChainRecord cancelOrder(int chainId) {
        ChainRecord chain = chains.find(chainId);
        switch (chain.getStatus()) {
            case PENDING_B -> chains.save(chain.withReason(MANAGER_CANCEL)
                    .moveTo(ChainRecord.Status.ROLLBACK_A, MANAGER_CANCEL));
            case PENDING_C -> chains.save(chain.withReason(MANAGER_CANCEL)
                    .moveTo(ChainRecord.Status.ROLLBACK_B, MANAGER_CANCEL));
            case ROLLBACK_A, ROLLBACK_B -> {
            }
            default -> throw new IllegalStateException("order chain " + chainId + " is already "
                    + chain.getStatus().name().toLowerCase());
        }
        return kitchen.finishOrder(chainId);
    }

    public void breakStation(String name) {
        cooking.breakStation(name);
    }

    public void repairStation(String name) {
        cooking.repairStation(name);
    }

    public void setStationTemperature(String name, int temperature) {
        cooking.setTemperature(name, temperature);
    }

    public void setCourierFree(String name, boolean free) {
        billing.setCourierFree(name, free);
    }

    public void finishDelivery(int billId) {
        billing.finishDelivery(billId);
    }

    public void restock(String ingredient, int amount) {
        orders.restock(ingredient, amount);
    }

    public void resetAllData() {
        chains.reset();
        billing.reset();
        cooking.reset();
        orders.reset();
    }

    public String stateReport() {
        List<String> lines = new ArrayList<>();
        List<String> stock = new ArrayList<>();
        orders.stock().forEach((ingredient, amount) -> stock.add(ingredient + " " + amount));
        lines.add("stock: " + String.join(", ", stock));
        lines.add("orders:");
        for (KitchenOrder order : orders.orders()) {
            lines.add("  #" + order.getId() + " " + order.getQuantity() + " x " + order.getDish() + " to "
                    + order.getZone() + ", " + lower(order.getStatus()));
        }
        lines.add("cooking tickets:");
        for (CookingTicket ticket : cooking.tickets()) {
            lines.add("  #" + ticket.getId() + " order " + ticket.getOrderId() + ", " + ticket.getDish() + " on "
                    + ticket.getStation() + ", " + lower(ticket.getStatus()));
        }
        List<String> stations = new ArrayList<>();
        for (CookingStore.Station station : cooking.stations()) {
            stations.add(station.name() + " " + station.temperature() + " c "
                    + (station.working() ? "working" : "out of order"));
        }
        lines.add("stations: " + String.join(", ", stations));
        lines.add("bills:");
        for (Bill bill : billing.bills()) {
            lines.add("  #" + bill.getId() + " order " + bill.getOrderId() + ", " + bill.getAmount() + " tg, "
                    + bill.getZone() + ", " + lower(bill.getStatus())
                    + (bill.getCourier().isEmpty() ? "" : " with " + bill.getCourier()));
        }
        List<String> couriers = new ArrayList<>();
        for (CheckoutStore.Courier courier : billing.couriers()) {
            couriers.add(courier.name() + " (" + courier.zone() + ") " + (courier.free() ? "free" : "busy"));
        }
        lines.add("couriers: " + String.join(", ", couriers));
        lines.add("order chains:");
        for (ChainRecord chain : chains.loadAll()) {
            lines.add("  " + describe(chain));
        }
        return String.join("\n", lines);
    }

    public static String describe(ChainRecord chain) {
        String reason = chain.getReason().isEmpty() ? "" : " (" + chain.getReason() + ")";
        return "#" + chain.getId() + " order " + chain.getOrderId() + ", " + lower(chain.getStatus()) + reason
                + ": " + chain.getHistory();
    }

    private static String lower(Enum<?> status) {
        return status.name().toLowerCase();
    }
}
