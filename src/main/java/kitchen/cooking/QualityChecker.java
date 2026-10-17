package kitchen.cooking;

import java.util.Map;

public class QualityChecker {

    private static final Map<String, Integer> MIN_CORE_TEMPERATURE = Map.of(
            "burger", 70,
            "steak", 63,
            "fries", 70,
            "pizza", 75);

    public int minimumFor(String dish) {
        Integer minimum = MIN_CORE_TEMPERATURE.get(dish);
        if (minimum == null) {
            throw new CookingException("no quality rule for " + dish);
        }
        return minimum;
    }

    public boolean passes(String dish, int coreTemperature) {
        return coreTemperature >= minimumFor(dish);
    }
}
