package kitchen.intake;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KitchenOrderTest {

    private final KitchenOrder reserved = new KitchenOrder(1, "burger", 2, "center", KitchenOrder.Status.RESERVED);

    @Test
    void reservedOrderCanBeServedOrCancelled() {
        assertEquals(KitchenOrder.Status.SERVED, reserved.withStatus(KitchenOrder.Status.SERVED).getStatus());
        assertEquals(KitchenOrder.Status.CANCELLED, reserved.withStatus(KitchenOrder.Status.CANCELLED).getStatus());
    }

    @Test
    void finishedOrderCannotChange() {
        KitchenOrder served = reserved.withStatus(KitchenOrder.Status.SERVED);

        assertThrows(IllegalStateException.class, () -> served.withStatus(KitchenOrder.Status.CANCELLED));
        assertThrows(IllegalStateException.class, () -> reserved.withStatus(KitchenOrder.Status.RESERVED));
    }
}
