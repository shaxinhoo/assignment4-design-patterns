package kitchen.app;

import kitchen.facade.Kitchen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KitchenAppTest {

    @TempDir
    Path dir;

    @Test
    void userCanOrderBreakTheGrillAndSeeBothRollbacks() {
        String output = runApp(
                "1", "burger", "2", "center",
                "7", "grill",
                "1", "burger", "1", "south",
                "8", "grill",
                "10", "dana",
                "1", "steak", "1", "north",
                "6",
                "0");

        assertTrue(output.contains("#1 order 1, completed: order 1 reserved > cooked on grill > sent with aidar"));
        assertTrue(output.contains("#2 order 2, compensated (grill is out of order)"));
        assertTrue(output.contains("delivery failed > cooking undone > order cancelled"));
        assertTrue(output.contains("couriers: aidar (center) busy, dana (north) busy, timur (south) free"));
        assertTrue(output.contains("bye"));
    }

    @Test
    void orderStartedInOneRunIsFinishedInTheNext() {
        String first = runApp("2", "pizza", "1", "south", "0");
        String second = runApp("4", "4", "0");

        assertTrue(first.contains("#1 order 1, pending_b: order 1 reserved"));
        assertTrue(second.contains("#1 order 1, completed: order 1 reserved > cooked on oven > sent with timur"));
        assertTrue(second.contains("nothing to resume"));
    }

    @Test
    void otherMenuOptionsWork() {
        String output = runApp(
                "2", "fries", "1", "center",
                "3", "1",
                "5", "1",
                "1", "fries", "1", "center",
                "12", "1",
                "11", "aidar",
                "9", "oven", "90",
                "13", "dough", "5",
                "14",
                "6",
                "0");

        assertTrue(output.contains("#1 order 1, pending_c"));
        assertTrue(output.contains("#1 order 1, compensated (cancelled by manager)"));
        assertTrue(output.contains("stock: beef 10, bun 10, cheese 5, dough 5"));
    }

    @Test
    void mistakesArePrintedAndTheAppKeepsRunning() {
        String output = runApp("1", "sushi", "1", "center", "3", "abc", "99", "6", "5", "7");

        assertTrue(output.contains("error: sushi is not on the menu"));
        assertTrue(output.contains("error: please type a number"));
        assertTrue(output.contains("unknown option"));
        assertTrue(output.contains("error: order chain 7 not found"));
    }

    private String runApp(String... lines) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Scanner input = new Scanner(String.join("\n", lines) + "\n");
        new KitchenApp(Kitchen.open(dir), input, new PrintStream(buffer, true, StandardCharsets.UTF_8)).run();
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
