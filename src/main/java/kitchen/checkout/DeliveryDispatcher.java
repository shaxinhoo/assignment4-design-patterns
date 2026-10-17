package kitchen.checkout;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class DeliveryDispatcher {

    private static final Set<String> ZONES = Set.of("center", "north", "south");

    public boolean deliversTo(String zone) {
        return ZONES.contains(zone);
    }

    public String chooseCourier(String zone, List<String> freeCouriers) {
        if (!deliversTo(zone)) {
            throw new CheckoutException("we do not deliver to " + zone);
        }
        if (freeCouriers.isEmpty()) {
            throw new CheckoutException("no free courier in " + zone);
        }
        return new TreeSet<>(freeCouriers).first();
    }
}
