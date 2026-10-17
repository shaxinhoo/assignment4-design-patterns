package kitchen.cooking;

import java.util.Map;
import java.util.Set;

public final class CookingTicket {

    public enum Status {
        ASSIGNED,
        COOKED,
        REJECTED,
        CANCELLED
    }

    private static final Map<Status, Set<Status>> ALLOWED = Map.of(
            Status.ASSIGNED, Set.of(Status.COOKED, Status.REJECTED, Status.CANCELLED),
            Status.COOKED, Set.of(Status.CANCELLED),
            Status.REJECTED, Set.of(),
            Status.CANCELLED, Set.of());

    private final int id;
    private final int orderId;
    private final String dish;
    private final int quantity;
    private final String station;
    private final Status status;

    public CookingTicket(int id, int orderId, String dish, int quantity, String station, Status status) {
        this.id = id;
        this.orderId = orderId;
        this.dish = dish;
        this.quantity = quantity;
        this.station = station;
        this.status = status;
    }

    public CookingTicket withStatus(Status next) {
        if (!ALLOWED.get(status).contains(next)) {
            throw new IllegalStateException("ticket " + id + " cannot go from " + status.name().toLowerCase()
                    + " to " + next.name().toLowerCase());
        }
        return new CookingTicket(id, orderId, dish, quantity, station, next);
    }

    public boolean isActive() {
        return status == Status.ASSIGNED || status == Status.COOKED;
    }

    public int getId() {
        return id;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getDish() {
        return dish;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getStation() {
        return station;
    }

    public Status getStatus() {
        return status;
    }
}
