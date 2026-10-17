package kitchen.cooking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookingTicketTest {

    private final CookingTicket assigned =
            new CookingTicket(1, 1, "burger", 1, "grill", CookingTicket.Status.ASSIGNED);

    @Test
    void cookedTicketCanStillBeCancelled() {
        CookingTicket cooked = assigned.withStatus(CookingTicket.Status.COOKED);

        assertEquals(CookingTicket.Status.CANCELLED, cooked.withStatus(CookingTicket.Status.CANCELLED).getStatus());
        assertTrue(cooked.isActive());
    }

    @Test
    void rejectedTicketIsFinal() {
        CookingTicket rejected = assigned.withStatus(CookingTicket.Status.REJECTED);

        assertFalse(rejected.isActive());
        assertThrows(IllegalStateException.class, () -> rejected.withStatus(CookingTicket.Status.CANCELLED));
        assertThrows(IllegalStateException.class, () -> rejected.withStatus(CookingTicket.Status.COOKED));
    }
}
