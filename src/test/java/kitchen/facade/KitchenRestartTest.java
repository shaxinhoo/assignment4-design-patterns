package kitchen.facade;

import kitchen.checkout.Bill;
import kitchen.cooking.CookingTicket;
import kitchen.intake.KitchenOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitchenRestartTest {

    @TempDir
    Path dir;

    private KitchenWorld kitchen;

    @BeforeEach
    void openKitchen() {
        kitchen = KitchenWorld.aFreshKitchenIn(dir);
    }

    @Test
    void anOrderStartedBeforeARestartIsFinishedAfterIt() {
        ChainRecord chain = kitchen.aCustomerStartsAnOrder(1, "burger", "center");

        kitchen.theKitchenRestarts();
        kitchen.theSavedOrder(chain).endedAs(ChainRecord.Status.PENDING_B);
        List<ChainRecord> resumed = kitchen.theManagerResumesUnfinishedOrders();

        assertEquals(1, resumed.size());
        kitchen.theSavedOrder(chain)
                .endedAs(ChainRecord.Status.COMPLETED)
                .hasOrder(KitchenOrder.Status.SERVED)
                .hasTicket(CookingTicket.Status.COOKED)
                .hasBill(Bill.Status.DISPATCHED);
    }

    @Test
    void anOrderCookedBeforeARestartIsRolledBackWhenDeliveryFailsAfterIt() {
        ChainRecord chain = kitchen.aCustomerStartsAnOrder(1, "steak", "north");
        kitchen.theNextStepRunsFor(chain);

        kitchen.theKitchenRestarts().courierIsBusy("dana");
        kitchen.theSavedOrder(chain).endedAs(ChainRecord.Status.PENDING_C).hasTicket(CookingTicket.Status.COOKED);
        kitchen.theManagerResumesUnfinishedOrders();

        kitchen.theSavedOrder(chain)
                .endedAs(ChainRecord.Status.COMPENSATED)
                .hasTicket(CookingTicket.Status.CANCELLED)
                .hasOrder(KitchenOrder.Status.CANCELLED)
                .wentThrough("order 1 reserved", "cooked on grill", "delivery failed", "cooking undone",
                        "order cancelled");
        assertEquals(10, kitchen.stockOf("beef"));
    }

    @Test
    void aRollbackStoppedByARestartContinuesFromTheSavedStep() {
        ChainRecord chain = kitchen.aCustomerStartsAnOrder(1, "steak", "north");
        kitchen.theNextStepRunsFor(chain);
        kitchen.courierIsBusy("dana");
        kitchen.theNextStepRunsFor(chain);

        kitchen.theKitchenRestarts();
        kitchen.theSavedOrder(chain)
                .endedAs(ChainRecord.Status.ROLLBACK_B)
                .hasTicket(CookingTicket.Status.COOKED)
                .hasOrder(KitchenOrder.Status.RESERVED);
        kitchen.theManagerResumesUnfinishedOrders();

        kitchen.theSavedOrder(chain)
                .endedAs(ChainRecord.Status.COMPENSATED)
                .hasTicket(CookingTicket.Status.CANCELLED)
                .hasOrder(KitchenOrder.Status.CANCELLED);
    }

    @Test
    void aCookingFailureSavedBeforeARestartUndoesTheOrderAfterIt() {
        kitchen.theGrillIsBroken();
        ChainRecord chain = kitchen.aCustomerStartsAnOrder(2, "burger", "south");
        kitchen.theNextStepRunsFor(chain);

        kitchen.theKitchenRestarts();
        kitchen.theSavedOrder(chain).endedAs(ChainRecord.Status.ROLLBACK_A).hasOrder(KitchenOrder.Status.RESERVED);
        kitchen.theManagerResumesUnfinishedOrders();

        kitchen.theSavedOrder(chain).endedAs(ChainRecord.Status.COMPENSATED).hasOrder(KitchenOrder.Status.CANCELLED);
        assertEquals(10, kitchen.stockOf("bun"));
    }

    @Test
    void finishedOrdersAreStillThereAfterARestart() {
        ChainRecord chain = kitchen.aCustomerOrders(1, "pizza", "south");

        kitchen.theKitchenRestarts();

        assertTrue(kitchen.admin().unfinishedOrders().isEmpty());
        assertTrue(kitchen.admin().stateReport().contains("#1 1 x pizza to south, served"));
        kitchen.theSavedOrder(chain).endedAs(ChainRecord.Status.COMPLETED);
    }
}
