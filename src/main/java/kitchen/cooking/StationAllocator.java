package kitchen.cooking;

import java.util.Map;
import java.util.Set;

public class StationAllocator {

    private static final Map<String, String> STATION_FOR_DISH = Map.of(
            "burger", "grill",
            "steak", "grill",
            "fries", "fryer",
            "pizza", "oven");

    public String allocate(String dish, Set<String> workingStations) {
        String station = STATION_FOR_DISH.get(dish);
        if (station == null) {
            throw new CookingException("no station can cook " + dish);
        }
        if (!workingStations.contains(station)) {
            throw new CookingException(station + " is out of order");
        }
        return station;
    }
}
