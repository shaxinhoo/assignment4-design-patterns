package kitchen.checkout;

import kitchen.storage.CsvFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CheckoutStore {

    public record Courier(String name, String zone, boolean free) {
    }

    private static final List<Courier> DEFAULT_COURIERS = List.of(
            new Courier("aidar", "center", true),
            new Courier("dana", "north", true),
            new Courier("timur", "south", true));

    private final CsvFile billsFile;
    private final CsvFile couriersFile;

    public CheckoutStore(Path dataDir) {
        this.billsFile = new CsvFile(dataDir.resolve("bills.csv"));
        this.couriersFile = new CsvFile(dataDir.resolve("couriers.csv"));
    }

    public List<Bill> loadBills() {
        List<Bill> bills = new ArrayList<>();
        for (String[] row : billsFile.readRows()) {
            bills.add(new Bill(Integer.parseInt(row[0]), Integer.parseInt(row[1]), Integer.parseInt(row[2]), row[3],
                    row[4], Bill.Status.valueOf(row[5])));
        }
        return bills;
    }

    public Optional<Bill> findBill(int id) {
        return loadBills().stream().filter(bill -> bill.getId() == id).findFirst();
    }

    public Optional<Bill> findLiveBillForOrder(int orderId) {
        return loadBills().stream()
                .filter(bill -> bill.getOrderId() == orderId && bill.getStatus() != Bill.Status.VOIDED)
                .findFirst();
    }

    public int nextBillId() {
        return loadBills().stream().mapToInt(Bill::getId).max().orElse(0) + 1;
    }

    public void saveBill(Bill bill) {
        List<String[]> rows = new ArrayList<>();
        for (Bill saved : loadBills()) {
            if (saved.getId() != bill.getId()) {
                rows.add(toRow(saved));
            }
        }
        rows.add(toRow(bill));
        rows.sort((a, b) -> Integer.parseInt(a[0]) - Integer.parseInt(b[0]));
        billsFile.writeRows(rows);
    }

    public List<Courier> loadCouriers() {
        if (!couriersFile.exists()) {
            return new ArrayList<>(DEFAULT_COURIERS);
        }
        List<Courier> couriers = new ArrayList<>();
        for (String[] row : couriersFile.readRows()) {
            couriers.add(new Courier(row[0], row[1], Boolean.parseBoolean(row[2])));
        }
        return couriers;
    }

    public Optional<Courier> findCourier(String name) {
        return loadCouriers().stream().filter(courier -> courier.name().equals(name)).findFirst();
    }

    public List<String> freeCouriersIn(String zone) {
        List<String> names = new ArrayList<>();
        for (Courier courier : loadCouriers()) {
            if (courier.zone().equals(zone) && courier.free()) {
                names.add(courier.name());
            }
        }
        return names;
    }

    public void saveCourier(Courier courier) {
        List<String[]> rows = new ArrayList<>();
        for (Courier saved : loadCouriers()) {
            Courier row = saved.name().equals(courier.name()) ? courier : saved;
            rows.add(new String[]{row.name(), row.zone(), String.valueOf(row.free())});
        }
        couriersFile.writeRows(rows);
    }

    public void reset() {
        billsFile.delete();
        couriersFile.delete();
    }

    private String[] toRow(Bill bill) {
        return new String[]{String.valueOf(bill.getId()), String.valueOf(bill.getOrderId()),
                String.valueOf(bill.getAmount()), bill.getZone(), bill.getCourier(), bill.getStatus().name()};
    }
}
