package kitchen.checkout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void smallOrderPaysForDelivery() {
        assertEquals(5500, calculator.total("burger", 2));
    }

    @Test
    void bigOrderGetsFreeDelivery() {
        assertEquals(12000, calculator.total("steak", 2));
        assertEquals(10000, calculator.total("burger", 4));
    }

    @Test
    void unknownDishHasNoPrice() {
        assertThrows(CheckoutException.class, () -> calculator.total("sushi", 1));
    }
}
