package kitchen.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class CsvFile {

    private static final String SEPARATOR = ";";

    private final Path path;

    public CsvFile(Path path) {
        this.path = path;
    }

    public boolean exists() {
        return Files.exists(path);
    }

    public List<String[]> readRows() {
        if (!exists()) {
            return new ArrayList<>();
        }
        try {
            List<String[]> rows = new ArrayList<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    rows.add(line.split(SEPARATOR, -1));
                }
            }
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path, e);
        }
    }

    public void writeRows(List<String[]> rows) {
        List<String> lines = new ArrayList<>();
        for (String[] row : rows) {
            lines.add(String.join(SEPARATOR, clean(row)));
        }
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write " + path, e);
        }
    }

    public void delete() {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot delete " + path, e);
        }
    }

    private String[] clean(String[] row) {
        String[] cleaned = new String[row.length];
        for (int i = 0; i < row.length; i++) {
            cleaned[i] = row[i].replace(SEPARATOR, ",").replace("\n", " ").replace("\r", " ");
        }
        return cleaned;
    }
}
