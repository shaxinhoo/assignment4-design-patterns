package kitchen.facade;

import java.util.Map;
import java.util.Set;

public final class ChainRecord {

    public enum Status {
        PENDING_B,
        PENDING_C,
        COMPLETED,
        ROLLBACK_B,
        ROLLBACK_A,
        COMPENSATED
    }

    private static final Map<Status, Set<Status>> ALLOWED = Map.of(
            Status.PENDING_B, Set.of(Status.PENDING_C, Status.ROLLBACK_A),
            Status.PENDING_C, Set.of(Status.COMPLETED, Status.ROLLBACK_B),
            Status.ROLLBACK_B, Set.of(Status.ROLLBACK_A),
            Status.ROLLBACK_A, Set.of(Status.COMPENSATED),
            Status.COMPLETED, Set.of(),
            Status.COMPENSATED, Set.of());

    private final int id;
    private final int orderId;
    private final int ticketId;
    private final int billId;
    private final Status status;
    private final String reason;
    private final String history;

    public ChainRecord(int id, int orderId, int ticketId, int billId, Status status, String reason, String history) {
        this.id = id;
        this.orderId = orderId;
        this.ticketId = ticketId;
        this.billId = billId;
        this.status = status;
        this.reason = reason;
        this.history = history;
    }

    public ChainRecord moveTo(Status next, String event) {
        if (!ALLOWED.get(status).contains(next)) {
            throw new IllegalStateException("order chain " + id + " cannot go from " + status.name().toLowerCase()
                    + " to " + next.name().toLowerCase());
        }
        return new ChainRecord(id, orderId, ticketId, billId, next, reason, history + " > " + event);
    }

    public ChainRecord withTicket(int newTicketId) {
        return new ChainRecord(id, orderId, newTicketId, billId, status, reason, history);
    }

    public ChainRecord withBill(int newBillId) {
        return new ChainRecord(id, orderId, ticketId, newBillId, status, reason, history);
    }

    public ChainRecord withReason(String newReason) {
        return new ChainRecord(id, orderId, ticketId, billId, status, newReason, history);
    }

    public boolean isFinished() {
        return status == Status.COMPLETED || status == Status.COMPENSATED;
    }

    public int getId() {
        return id;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getTicketId() {
        return ticketId;
    }

    public int getBillId() {
        return billId;
    }

    public Status getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getHistory() {
        return history;
    }
}
