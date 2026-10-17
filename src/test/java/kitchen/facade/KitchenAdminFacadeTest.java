package kitchen.facade;

import kitchen.cooking.CookingTicket;
import kitchen.intake.KitchenOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitchenAdminFacadeTest {

    @TempDir
    Path dir;

    private KitchenWorld kitchen;

    @BeforeEach
    void openKitchen() {
        kitchen = KitchenWorld.aFreshKitchenIn(dir);
    }

    @Test
    void managerCancelsAnOrderThatWasOnlyReserved() {
        ChainRecord chain = kitchen.aCustomerStartsAnOrder(1, "burger", "center");

        kitchen.theManagerCancels(chain);

        kitchen.theSavedOrder(chain)
                .endedAs(ChainRecord.Status.COMPENSATED)
                .becauseOf("cancelled by manager")
                .hasOrder(KitchenOrder.Status.CANCELLED)
                .wentThrough("order 1 reserved", "cancelled by manager", "order cancelled");
    }

    @Test
    void managerCancelsAnOrderThatWasAlreadyCooked() {
        ChainRecord chain = kitchen.aCustomerStartsAnOrder(1, "burger", "center");
        kitchen.theNextStepRunsFor(chain);

        kitchen.theManagerCancels(chain);

        kitchen.theSavedOrder(chain)
                .endedAs(ChainRecord.Status.COMPENSATED)
                .hasTicket(CookingTicket.Status.CANCELLED)
                .hasOrder(KitchenOrder.Status.CANCELLED)
                .wentThrough("order 1 reserved", "cooked on grill", "cancelled by manager", "cooking undone",
                        "order cancelled");
    }

    @Test
    void cancellingAnOrderThatIsAlreadyRollingBackJustFinishesIt() {
        ChainRecord chain = kitchen.aCustomerStartsAnOrder(1, "steak", "north");
        kitchen.theNextStepRunsFor(chain);
        kitchen.courierIsBusy("dana");
        kitchen.theNextStepRunsFor(chain);

        kitchen.theManagerCancels(chain);

        kitchen.theSavedOrder(chain).endedAs(ChainRecord.Status.COMPENSATED).becauseOf("no free courier in north");
    }

    @Test
    void finishedOrderCannotBeCancelled() {
        ChainRecord chain = kitchen.aCustomerOrders(1, "burger", "center");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> kitchen.theManagerCancels(chain));

        assertEquals("order chain 1 is already completed", e.getMessage());
    }

    @Test
    void repairedGrillCooksAgain() {
        kitchen.theGrillIsBroken();
        kitchen.admin().repairStation("grill");

        ChainRecord chain = kitchen.aCustomerOrders(1, "burger", "center");

        assertEquals(ChainRecord.Status.COMPLETED, chain.getStatus());
    }

    @Test
    void restockLetsABigOrderThrough() {
        kitchen.admin().restock("beef", 10);

        ChainRecord chain = kitchen.aCustomerOrders(10, "steak", "south");

        assertEquals(ChainRecord.Status.COMPLETED, chain.getStatus());
        assertEquals(0, kitchen.stockOf("beef"));
    }

    @Test
    void reportShowsEverySubsystem() {
        kitchen.aCustomerOrders(1, "burger", "center");
        kitchen.theGrillIsBroken();

        String report = kitchen.admin().stateReport();

        assertTrue(report.contains("stock: beef 9, bun 9"));
        assertTrue(report.contains("#1 1 x burger to center, served"));
        assertTrue(report.contains("#1 order 1, burger on grill, cooked"));
        assertTrue(report.contains("grill 75 c out of order"));
        assertTrue(report.contains("#1 order 1, 3000 tg, center, dispatched with aidar"));
        assertTrue(report.contains("aidar (center) busy"));
        assertTrue(report.contains("#1 order 1, completed: order 1 reserved > cooked on grill > sent with aidar"));
    }

    @Test
    void resetStartsFromCleanDefaults() {
        kitchen.aCustomerOrders(1, "burger", "center");
        kitchen.theGrillIsBroken();

        kitchen.admin().resetAllData();

        assertEquals(0, kitchen.savedOrderChains());
        assertEquals(10, kitchen.stockOf("beef"));
        assertEquals(ChainRecord.Status.COMPLETED, kitchen.aCustomerOrders(1, "burger", "center").getStatus());
    }
}
