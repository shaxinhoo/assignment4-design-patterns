package kitchen.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingModuleTest {

    @TempDir
    Path dir;

    private BillingModule billing;

    @BeforeEach
    void openBilling() {
        billing = new BillingModule(new CheckoutStore(dir));
    }

    @Test
    void checkoutBillsTheOrderAndSendsAFreeCourier() {
        Bill bill = billing.checkout(1, "burger", 2, "center");

        assertEquals(Bill.Status.DISPATCHED, bill.getStatus());
        assertEquals(5500, bill.getAmount());
        assertEquals("aidar", bill.getCourier());
        assertFalse(courierIsFree("aidar"));
    }

    @Test
    void noFreeCourierVoidsTheBill() {
        billing.setCourierFree("dana", false);

        assertThrows(CheckoutException.class, () -> billing.checkout(1, "pizza", 1, "north"));

        assertEquals(Bill.Status.VOIDED, billing.bills().get(0).getStatus());
    }

    @Test
    void checkoutOfTheSameOrderAgainReturnsTheSameBill() {
        Bill first = billing.checkout(4, "fries", 1, "south");

        Bill second = billing.checkout(4, "fries", 1, "south");

        assertEquals(first.getId(), second.getId());
        assertEquals(1, billing.bills().size());
    }

    @Test
    void openBillLeftByACrashIsDispatched() {
        new CheckoutStore(dir).saveBill(new Bill(1, 2, 2500, "center", "", Bill.Status.OPEN));

        Bill bill = billing.checkout(2, "burger", 1, "center");

        assertEquals(1, bill.getId());
        assertEquals(Bill.Status.DISPATCHED, bill.getStatus());
    }

    @Test
    void finishingADeliveryFreesTheCourier() {
        Bill bill = billing.checkout(1, "steak", 1, "south");

        Bill delivered = billing.finishDelivery(bill.getId());

        assertEquals(Bill.Status.DELIVERED, delivered.getStatus());
        assertTrue(courierIsFree("timur"));
    }

    @Test
    void unknownCourierAndBillAreReported() {
        assertThrows(CheckoutException.class, () -> billing.setCourierFree("bob", true));
        assertThrows(CheckoutException.class, () -> billing.find(9));
        assertFalse(billing.deliversTo("mars"));
    }

    @Test
    void billsSurviveReopeningAndResetClearsThem() {
        billing.checkout(1, "burger", 1, "center");

        BillingModule reopened = new BillingModule(new CheckoutStore(dir));
        assertEquals(1, reopened.bills().size());

        reopened.reset();
        assertTrue(reopened.bills().isEmpty());
        assertTrue(courierIsFree("aidar"));
        assertEquals(3, reopened.couriers().size());
    }

    private boolean courierIsFree(String name) {
        return new CheckoutStore(dir).findCourier(name).orElseThrow().free();
    }
}
