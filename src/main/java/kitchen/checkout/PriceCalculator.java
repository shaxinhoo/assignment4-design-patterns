package kitchen.checkout;

import java.util.Map;

public class PriceCalculator {

    private static final int DELIVERY_FEE = 500;
    private static final int FREE_DELIVERY_FROM = 10000;

    private static final Map<String, Integer> PRICES = Map.of(
            "burger", 2500,
            "steak", 6000,
            "fries", 900,
            "pizza", 4000);

    public int total(String dish, int quantity) {
        Integer price = PRICES.get(dish);
        if (price == null) {
            throw new CheckoutException("no price for " + dish);
        }
        int food = price * quantity;
        return food >= FREE_DELIVERY_FROM ? food : food + DELIVERY_FEE;
    }
}
