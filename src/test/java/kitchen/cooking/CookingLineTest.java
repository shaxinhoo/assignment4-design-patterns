package kitchen.cooking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookingLineTest {

    @TempDir
    Path dir;

    private CookingLine line;

    @BeforeEach
    void openLine() {
        line = new CookingLine(new CookingStore(dir));
    }

    @Test
    void burgerIsCookedOnTheGrill() {
        CookingTicket ticket = line.cook(1, "burger", 2);

        assertEquals("grill", ticket.getStation());
        assertEquals(CookingTicket.Status.COOKED, line.find(ticket.getId()).getStatus());
    }

    @Test
    void brokenStationFailsBeforeAnythingIsSaved() {
        line.breakStation("grill");

        assertThrows(CookingException.class, () -> line.cook(1, "burger", 1));
        assertTrue(line.tickets().isEmpty());
    }

    @Test
    void coldStationFailsTheQualityCheckAndSavesARejectedTicket() {
        line.setTemperature("grill", 55);

        CookingException e = assertThrows(CookingException.class, () -> line.cook(1, "burger", 1));

        assertEquals("quality check failed, burger reached 55 c but needs 70 c", e.getMessage());
        assertEquals(CookingTicket.Status.REJECTED, line.tickets().get(0).getStatus());
    }

    @Test
    void cookingTheSameOrderAgainReturnsTheSameTicket() {
        CookingTicket first = line.cook(7, "fries", 1);

        CookingTicket second = line.cook(7, "fries", 1);

        assertEquals(first.getId(), second.getId());
        assertEquals(1, line.tickets().size());
    }

    @Test
    void ticketLeftAssignedByACrashIsFinished() {
        CookingStore store = new CookingStore(dir);
        store.saveTicket(new CookingTicket(1, 3, "pizza", 1, "oven", CookingTicket.Status.ASSIGNED));

        CookingTicket ticket = line.cook(3, "pizza", 1);

        assertEquals(1, ticket.getId());
        assertEquals(CookingTicket.Status.COOKED, ticket.getStatus());
    }

    @Test
    void cancellingWorksOnceAndLeavesFinishedTicketsAlone() {
        CookingTicket ticket = line.cook(1, "steak", 1);

        line.cancel(ticket.getId());
        CookingTicket again = line.cancel(ticket.getId());

        assertEquals(CookingTicket.Status.CANCELLED, again.getStatus());
    }

    @Test
    void stationsCanBeRepairedAndChanged() {
        line.breakStation("oven");
        line.repairStation("oven");
        line.setTemperature("oven", 90);

        CookingStore.Station oven = new CookingStore(dir).findStation("oven").orElseThrow();
        assertTrue(oven.working());
        assertEquals(90, oven.temperature());
        assertEquals(3, line.stations().size());
    }

    @Test
    void unknownStationAndTicketAreReported() {
        assertThrows(CookingException.class, () -> line.breakStation("microwave"));
        assertThrows(CookingException.class, () -> line.find(42));
    }

    @Test
    void ticketsSurviveReopeningAndResetClearsThem() {
        line.cook(1, "burger", 1);
        line.breakStation("fryer");

        CookingLine reopened = new CookingLine(new CookingStore(dir));
        assertEquals(1, reopened.tickets().size());
        assertFalse(new CookingStore(dir).workingStations().contains("fryer"));

        reopened.reset();
        assertTrue(reopened.tickets().isEmpty());
        assertTrue(new CookingStore(dir).workingStations().contains("fryer"));
    }
}
