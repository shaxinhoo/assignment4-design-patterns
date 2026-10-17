package kitchen.cooking;

import java.util.List;
import java.util.Optional;

public class CookingLine {

    private final CookingStore store;
    private final StationAllocator allocator = new StationAllocator();
    private final QualityChecker quality = new QualityChecker();

    public CookingLine(CookingStore store) {
        this.store = store;
    }

    public CookingTicket cook(int orderId, String dish, int quantity) {
        Optional<CookingTicket> existing = store.findActiveTicketForOrder(orderId);
        if (existing.isPresent() && existing.get().getStatus() == CookingTicket.Status.COOKED) {
            return existing.get();
        }
        CookingTicket ticket = existing.orElseGet(() -> assign(orderId, dish, quantity));
        return checkQuality(ticket);
    }

    public CookingTicket cancel(int ticketId) {
        CookingTicket ticket = find(ticketId);
        if (!ticket.isActive()) {
            return ticket;
        }
        CookingTicket cancelled = ticket.withStatus(CookingTicket.Status.CANCELLED);
        store.saveTicket(cancelled);
        return cancelled;
    }

    public void breakStation(String name) {
        CookingStore.Station station = station(name);
        store.saveStation(new CookingStore.Station(name, station.temperature(), false));
    }

    public void repairStation(String name) {
        CookingStore.Station station = station(name);
        store.saveStation(new CookingStore.Station(name, station.temperature(), true));
    }

    public void setTemperature(String name, int temperature) {
        CookingStore.Station station = station(name);
        store.saveStation(new CookingStore.Station(name, temperature, station.working()));
    }

    public CookingTicket find(int ticketId) {
        return store.findTicket(ticketId).orElseThrow(() -> new CookingException("ticket " + ticketId + " not found"));
    }

    public List<CookingTicket> tickets() {
        return store.loadTickets();
    }

    public List<CookingStore.Station> stations() {
        return store.loadStations();
    }

    public void reset() {
        store.reset();
    }

    private CookingTicket assign(int orderId, String dish, int quantity) {
        String station = allocator.allocate(dish, store.workingStations());
        CookingTicket ticket = new CookingTicket(store.nextTicketId(), orderId, dish, quantity, station,
                CookingTicket.Status.ASSIGNED);
        store.saveTicket(ticket);
        return ticket;
    }

    private CookingTicket checkQuality(CookingTicket ticket) {
        int temperature = station(ticket.getStation()).temperature();
        if (quality.passes(ticket.getDish(), temperature)) {
            CookingTicket cooked = ticket.withStatus(CookingTicket.Status.COOKED);
            store.saveTicket(cooked);
            return cooked;
        }
        store.saveTicket(ticket.withStatus(CookingTicket.Status.REJECTED));
        throw new CookingException("quality check failed, " + ticket.getDish() + " reached " + temperature
                + " c but needs " + quality.minimumFor(ticket.getDish()) + " c");
    }

    private CookingStore.Station station(String name) {
        return store.findStation(name).orElseThrow(() -> new CookingException("unknown station " + name));
    }
}
