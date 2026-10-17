package kitchen.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvFileTest {

    @TempDir
    Path dir;

    @Test
    void missingFileReadsAsEmpty() {
        CsvFile file = new CsvFile(dir.resolve("none.csv"));

        assertFalse(file.exists());
        assertTrue(file.readRows().isEmpty());
    }

    @Test
    void writtenRowsCanBeReadBackAfterReopening() {
        new CsvFile(dir.resolve("rows.csv")).writeRows(List.<String[]>of(new String[]{"1", "burger", ""}));

        List<String[]> rows = new CsvFile(dir.resolve("rows.csv")).readRows();

        assertArrayEquals(new String[]{"1", "burger", ""}, rows.get(0));
    }

    @Test
    void separatorAndNewLinesInsideValuesDoNotBreakTheFile() {
        CsvFile file = new CsvFile(dir.resolve("rows.csv"));

        file.writeRows(List.<String[]>of(new String[]{"a;b", "line\nbreak"}));

        assertArrayEquals(new String[]{"a,b", "line break"}, file.readRows().get(0));
    }

    @Test
    void deleteRemovesTheFile() {
        CsvFile file = new CsvFile(dir.resolve("rows.csv"));
        file.writeRows(List.<String[]>of(new String[]{"x"}));

        file.delete();

        assertFalse(file.exists());
    }
}
