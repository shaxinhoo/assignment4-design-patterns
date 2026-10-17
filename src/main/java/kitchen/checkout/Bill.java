package kitchen.checkout;

import java.util.Map;
import java.util.Set;

public final class Bill {

    public enum Status {
        OPEN,
        DISPATCHED,
        DELIVERED,
        VOIDED
    }

    private static final Map<Status, Set<Status>> ALLOWED = Map.of(
            Status.OPEN, Set.of(Status.DISPATCHED, Status.VOIDED),
            Status.DISPATCHED, Set.of(Status.DELIVERED),
            Status.DELIVERED, Set.of(),
            Status.VOIDED, Set.of());

    private final int id;
    private final int orderId;
    private final int amount;
    private final String zone;
    private final String courier;
    private final Status status;

    public Bill(int id, int orderId, int amount, String zone, String courier, Status status) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.zone = zone;
        this.courier = courier;
        this.status = status;
    }

    public Bill withStatus(Status next) {
        if (!ALLOWED.get(status).contains(next)) {
            throw new IllegalStateException("bill " + id + " cannot go from " + status.name().toLowerCase()
                    + " to " + next.name().toLowerCase());
        }
        return new Bill(id, orderId, amount, zone, courier, next);
    }

    public Bill dispatchedBy(String courierName) {
        Bill dispatched = withStatus(Status.DISPATCHED);
        return new Bill(id, orderId, amount, zone, courierName, dispatched.status);
    }

    public int getId() {
        return id;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getAmount() {
        return amount;
    }

    public String getZone() {
        return zone;
    }

    public String getCourier() {
        return courier;
    }

    public Status getStatus() {
        return status;
    }
}
