package kitchen.checkout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BillTest {

    private final Bill open = new Bill(1, 1, 5500, "center", "", Bill.Status.OPEN);

    @Test
    void openBillIsDispatchedAndThenDelivered() {
        Bill dispatched = open.dispatchedBy("aidar");

        assertEquals("aidar", dispatched.getCourier());
        assertEquals(Bill.Status.DELIVERED, dispatched.withStatus(Bill.Status.DELIVERED).getStatus());
    }

    @Test
    void voidedBillIsFinal() {
        Bill voided = open.withStatus(Bill.Status.VOIDED);

        assertThrows(IllegalStateException.class, () -> voided.dispatchedBy("aidar"));
        assertThrows(IllegalStateException.class, () -> open.withStatus(Bill.Status.DELIVERED));
    }
}
