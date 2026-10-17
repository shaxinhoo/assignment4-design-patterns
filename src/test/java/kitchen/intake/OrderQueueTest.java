package kitchen.intake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderQueueTest {

    @TempDir
    Path dir;

    private OrderQueue queue;

    @BeforeEach
    void openQueue() {
        queue = new OrderQueue(new IntakeStore(dir));
    }

    @Test
    void placingAnOrderReservesIngredients() {
        KitchenOrder order = queue.place("burger", 2, "center");

        assertEquals(KitchenOrder.Status.RESERVED, order.getStatus());
        assertEquals(8, queue.stock().get("beef"));
        assertEquals(8, queue.stock().get("bun"));
    }

    @Test
    void notEnoughIngredientsChangesNothing() {
        IntakeException e = assertThrows(IntakeException.class, () -> queue.place("steak", 10, "center"));

        assertEquals("not enough beef for 10 steak", e.getMessage());
        assertEquals(10, queue.stock().get("beef"));
        assertTrue(queue.orders().isEmpty());
    }

    @Test
    void missingZoneIsRejected() {
        assertThrows(IntakeException.class, () -> queue.place("burger", 1, " "));
        assertThrows(IntakeException.class, () -> queue.place("burger", 1, null));
    }

    @Test
    void cancellingGivesIngredientsBackOnlyOnce() {
        KitchenOrder order = queue.place("pizza", 2, "south");

        queue.cancel(order.getId());
        queue.cancel(order.getId());

        assertEquals(KitchenOrder.Status.CANCELLED, queue.find(order.getId()).getStatus());
        assertEquals(5, queue.stock().get("cheese"));
    }

    @Test
    void servedOrderCannotBeCancelled() {
        KitchenOrder order = queue.place("fries", 1, "north");

        queue.markServed(order.getId());
        queue.markServed(order.getId());

        assertThrows(IllegalStateException.class, () -> queue.cancel(order.getId()));
    }

    @Test
    void ordersAndStockSurviveReopening() {
        KitchenOrder order = queue.place("burger", 1, "center");

        OrderQueue reopened = new OrderQueue(new IntakeStore(dir));

        assertEquals("burger", reopened.find(order.getId()).getDish());
        assertEquals(9, reopened.stock().get("beef"));
    }

    @Test
    void restockAddsToStock() {
        queue.restock("beef", 5);

        assertEquals(15, queue.stock().get("beef"));
        assertThrows(IntakeException.class, () -> queue.restock("beef", 0));
    }

    @Test
    void unknownOrderIsReported() {
        assertThrows(IntakeException.class, () -> queue.find(99));
    }

    @Test
    void resetBringsBackDefaultStock() {
        queue.place("burger", 3, "center");

        queue.reset();

        assertTrue(queue.orders().isEmpty());
        assertEquals(10, queue.stock().get("beef"));
    }
}
