package kitchen.facade;

import kitchen.storage.CsvFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ChainStore {

    private final CsvFile file;

    public ChainStore(Path dataDir) {
        this.file = new CsvFile(dataDir.resolve("chains.csv"));
    }

    public ChainRecord create(int orderId) {
        int id = loadAll().stream().mapToInt(ChainRecord::getId).max().orElse(0) + 1;
        ChainRecord chain = new ChainRecord(id, orderId, 0, 0, ChainRecord.Status.PENDING_B, "",
                "order " + orderId + " reserved");
        save(chain);
        return chain;
    }

    public ChainRecord find(int id) {
        return loadAll().stream()
                .filter(chain -> chain.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("order chain " + id + " not found"));
    }

    public List<ChainRecord> loadAll() {
        List<ChainRecord> chains = new ArrayList<>();
        for (String[] row : file.readRows()) {
            chains.add(new ChainRecord(Integer.parseInt(row[0]), Integer.parseInt(row[1]), Integer.parseInt(row[2]),
                    Integer.parseInt(row[3]), ChainRecord.Status.valueOf(row[4]), row[5], row[6]));
        }
        return chains;
    }

    public List<ChainRecord> unfinished() {
        List<ChainRecord> chains = new ArrayList<>();
        for (ChainRecord chain : loadAll()) {
            if (!chain.isFinished()) {
                chains.add(chain);
            }
        }
        return chains;
    }

    public void save(ChainRecord chain) {
        List<String[]> rows = new ArrayList<>();
        for (ChainRecord saved : loadAll()) {
            if (saved.getId() != chain.getId()) {
                rows.add(toRow(saved));
            }
        }
        rows.add(toRow(chain));
        rows.sort((a, b) -> Integer.parseInt(a[0]) - Integer.parseInt(b[0]));
        file.writeRows(rows);
    }

    public void reset() {
        file.delete();
    }

    private String[] toRow(ChainRecord chain) {
        return new String[]{String.valueOf(chain.getId()), String.valueOf(chain.getOrderId()),
                String.valueOf(chain.getTicketId()), String.valueOf(chain.getBillId()), chain.getStatus().name(),
                chain.getReason(), chain.getHistory()};
    }
}
