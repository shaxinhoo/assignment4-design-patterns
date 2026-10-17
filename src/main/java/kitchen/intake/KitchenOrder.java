package kitchen.intake;

public final class KitchenOrder {

    public enum Status {
        RESERVED,
        CANCELLED,
        SERVED
    }

    private final int id;
    private final String dish;
    private final int quantity;
    private final String zone;
    private final Status status;

    public KitchenOrder(int id, String dish, int quantity, String zone, Status status) {
        this.id = id;
        this.dish = dish;
        this.quantity = quantity;
        this.zone = zone;
        this.status = status;
    }

    public KitchenOrder withStatus(Status next) {
        if (status != Status.RESERVED || next == Status.RESERVED) {
            throw new IllegalStateException("order " + id + " cannot go from " + status.name().toLowerCase()
                    + " to " + next.name().toLowerCase());
        }
        return new KitchenOrder(id, dish, quantity, zone, next);
    }

    public int getId() {
        return id;
    }

    public String getDish() {
        return dish;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getZone() {
        return zone;
    }

    public Status getStatus() {
        return status;
    }
}
