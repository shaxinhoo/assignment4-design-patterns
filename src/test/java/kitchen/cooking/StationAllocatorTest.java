package kitchen.cooking;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StationAllocatorTest {

    private final StationAllocator allocator = new StationAllocator();

    @Test
    void eachDishGoesToItsStation() {
        Set<String> all = Set.of("grill", "fryer", "oven");

        assertEquals("grill", allocator.allocate("burger", all));
        assertEquals("grill", allocator.allocate("steak", all));
        assertEquals("fryer", allocator.allocate("fries", all));
        assertEquals("oven", allocator.allocate("pizza", all));
    }

    @Test
    void brokenStationCannotBeUsed() {
        CookingException e = assertThrows(CookingException.class,
                () -> allocator.allocate("burger", Set.of("fryer", "oven")));

        assertEquals("grill is out of order", e.getMessage());
    }

    @Test
    void unknownDishHasNoStation() {
        assertThrows(CookingException.class, () -> allocator.allocate("sushi", Set.of("grill")));
    }
}
