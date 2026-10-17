package kitchen.intake;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryCheckerTest {

    private final InventoryChecker checker = new InventoryChecker();

    @Test
    void menuIsSortedByName() {
        assertEquals(List.of("burger", "fries", "pizza", "steak"), checker.menu());
    }

    @Test
    void ingredientsGrowWithQuantity() {
        assertEquals(Map.of("beef", 3, "bun", 3), checker.ingredientsFor("burger", 3));
    }

    @Test
    void rejectsUnknownDishAndBadQuantity() {
        assertThrows(IntakeException.class, () -> checker.ingredientsFor("sushi", 1));
        assertThrows(IntakeException.class, () -> checker.ingredientsFor("burger", 0));
        assertThrows(IntakeException.class, () -> checker.ingredientsFor("burger", 11));
    }

    @Test
    void findsMissingIngredients() {
        Map<String, Integer> stock = Map.of("beef", 1, "bun", 5);

        assertEquals(List.of("beef"), checker.missingIngredients(stock, Map.of("beef", 2, "bun", 2)));
        assertEquals(List.of(), checker.missingIngredients(stock, Map.of("bun", 5)));
    }
}
