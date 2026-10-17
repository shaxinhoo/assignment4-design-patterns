package kitchen.intake;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class InventoryChecker {

    private static final int MAX_PORTIONS = 10;

    private static final Map<String, Map<String, Integer>> RECIPES = Map.of(
            "burger", Map.of("bun", 1, "beef", 1),
            "steak", Map.of("beef", 2),
            "fries", Map.of("potato", 2),
            "pizza", Map.of("dough", 1, "cheese", 1, "tomato", 1));

    public List<String> menu() {
        return new ArrayList<>(new TreeMap<>(RECIPES).keySet());
    }

    public Map<String, Integer> ingredientsFor(String dish, int quantity) {
        if (!RECIPES.containsKey(dish)) {
            throw new IntakeException(dish + " is not on the menu");
        }
        if (quantity < 1 || quantity > MAX_PORTIONS) {
            throw new IntakeException("quantity must be from 1 to " + MAX_PORTIONS);
        }
        Map<String, Integer> needed = new TreeMap<>();
        RECIPES.get(dish).forEach((ingredient, amount) -> needed.put(ingredient, amount * quantity));
        return needed;
    }

    public List<String> missingIngredients(Map<String, Integer> stock, Map<String, Integer> needed) {
        List<String> missing = new ArrayList<>();
        needed.forEach((ingredient, amount) -> {
            if (stock.getOrDefault(ingredient, 0) < amount) {
                missing.add(ingredient);
            }
        });
        return missing;
    }
}
