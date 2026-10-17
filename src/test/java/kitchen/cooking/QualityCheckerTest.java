package kitchen.cooking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualityCheckerTest {

    private final QualityChecker checker = new QualityChecker();

    @Test
    void dishPassesFromItsMinimumTemperature() {
        assertEquals(70, checker.minimumFor("burger"));
        assertTrue(checker.passes("burger", 70));
        assertFalse(checker.passes("burger", 69));
        assertTrue(checker.passes("steak", 63));
    }

    @Test
    void unknownDishHasNoRule() {
        assertThrows(CookingException.class, () -> checker.passes("sushi", 100));
    }
}
