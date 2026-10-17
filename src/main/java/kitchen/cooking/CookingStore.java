package kitchen.cooking;

import kitchen.storage.CsvFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class CookingStore {

    public record Station(String name, int temperature, boolean working) {
    }

    private static final List<Station> DEFAULT_STATIONS = List.of(
            new Station("fryer", 80, true),
            new Station("grill", 75, true),
            new Station("oven", 85, true));

    private final CsvFile ticketsFile;
    private final CsvFile stationsFile;

    public CookingStore(Path dataDir) {
        this.ticketsFile = new CsvFile(dataDir.resolve("tickets.csv"));
        this.stationsFile = new CsvFile(dataDir.resolve("stations.csv"));
    }

    public List<CookingTicket> loadTickets() {
        List<CookingTicket> tickets = new ArrayList<>();
        for (String[] row : ticketsFile.readRows()) {
            tickets.add(new CookingTicket(Integer.parseInt(row[0]), Integer.parseInt(row[1]), row[2],
                    Integer.parseInt(row[3]), row[4], CookingTicket.Status.valueOf(row[5])));
        }
        return tickets;
    }

    public Optional<CookingTicket> findTicket(int id) {
        return loadTickets().stream().filter(ticket -> ticket.getId() == id).findFirst();
    }

    public Optional<CookingTicket> findActiveTicketForOrder(int orderId) {
        return loadTickets().stream()
                .filter(ticket -> ticket.getOrderId() == orderId && ticket.isActive())
                .findFirst();
    }

    public int nextTicketId() {
        return loadTickets().stream().mapToInt(CookingTicket::getId).max().orElse(0) + 1;
    }

    public void saveTicket(CookingTicket ticket) {
        List<String[]> rows = new ArrayList<>();
        for (CookingTicket saved : loadTickets()) {
            if (saved.getId() != ticket.getId()) {
                rows.add(toRow(saved));
            }
        }
        rows.add(toRow(ticket));
        rows.sort((a, b) -> Integer.parseInt(a[0]) - Integer.parseInt(b[0]));
        ticketsFile.writeRows(rows);
    }

    public List<Station> loadStations() {
        if (!stationsFile.exists()) {
            return new ArrayList<>(DEFAULT_STATIONS);
        }
        List<Station> stations = new ArrayList<>();
        for (String[] row : stationsFile.readRows()) {
            stations.add(new Station(row[0], Integer.parseInt(row[1]), Boolean.parseBoolean(row[2])));
        }
        return stations;
    }

    public Optional<Station> findStation(String name) {
        return loadStations().stream().filter(station -> station.name().equals(name)).findFirst();
    }

    public Set<String> workingStations() {
        Set<String> names = new TreeSet<>();
        for (Station station : loadStations()) {
            if (station.working()) {
                names.add(station.name());
            }
        }
        return names;
    }

    public void saveStation(Station station) {
        List<String[]> rows = new ArrayList<>();
        for (Station saved : loadStations()) {
            Station row = saved.name().equals(station.name()) ? station : saved;
            rows.add(new String[]{row.name(), String.valueOf(row.temperature()), String.valueOf(row.working())});
        }
        stationsFile.writeRows(rows);
    }

    public void reset() {
        ticketsFile.delete();
        stationsFile.delete();
    }

    private String[] toRow(CookingTicket ticket) {
        return new String[]{String.valueOf(ticket.getId()), String.valueOf(ticket.getOrderId()), ticket.getDish(),
                String.valueOf(ticket.getQuantity()), ticket.getStation(), ticket.getStatus().name()};
    }
}
