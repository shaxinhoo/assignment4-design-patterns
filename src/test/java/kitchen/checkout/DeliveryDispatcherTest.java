package kitchen.checkout;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryDispatcherTest {

    private final DeliveryDispatcher dispatcher = new DeliveryDispatcher();

    @Test
    void knowsItsZones() {
        assertTrue(dispatcher.deliversTo("north"));
        assertFalse(dispatcher.deliversTo("mars"));
    }

    @Test
    void picksTheFirstFreeCourierByName() {
        assertEquals("aidar", dispatcher.chooseCourier("center", List.of("dana", "aidar")));
    }

    @Test
    void failsWithoutFreeCourierOrForUnknownZone() {
        CheckoutException e = assertThrows(CheckoutException.class,
                () -> dispatcher.chooseCourier("north", List.of()));

        assertEquals("no free courier in north", e.getMessage());
        assertThrows(CheckoutException.class, () -> dispatcher.chooseCourier("mars", List.of("aidar")));
    }
}
