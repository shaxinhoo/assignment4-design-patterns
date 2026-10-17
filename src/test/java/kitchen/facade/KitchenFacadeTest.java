package kitchen.facade;

import kitchen.checkout.Bill;
import kitchen.cooking.CookingTicket;
import kitchen.intake.KitchenOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KitchenFacadeTest {

    @TempDir
    Path dir;

    private KitchenWorld kitchen;

    @BeforeEach
    void openKitchen() {
        kitchen = KitchenWorld.aFreshKitchenIn(dir);
    }

    @Test
    void aBurgerOrderGoesThroughAllThreeSteps() {
        ChainRecord chain = kitchen.aCustomerOrders(2, "burger", "center");

        kitchen.theSavedOrder(chain)
                .endedAs(ChainRecord.Status.COMPLETED)
                .hasOrder(KitchenOrder.Status.SERVED)
                .hasTicket(CookingTicket.Status.COOKED)
                .hasBill(Bill.Status.DISPATCHED)
                .wentThrough("order 1 reserved", "cooked on grill", "sent with aidar");
        assertEquals(8, kitchen.stockOf("beef"));
    }

    @Test
    void whenCookingFailsAfterTheOrderIsReservedOnlyTheOrderIsUndone() {
        kitchen.theGrillIsBroken();

        ChainRecord chain = kitchen.aCustomerOrders(2, "burger", "center");

        kitchen.theSavedOrder(chain)
                .endedAs(ChainRecord.Status.COMPENSATED)
                .becauseOf("grill is out of order")
                .hasOrder(KitchenOrder.Status.CANCELLED)
                .hasNoTicket()
                .hasNoBill()
                .wentThrough("order 1 reserved", "cooking failed", "order cancelled");
        assertEquals(10, kitchen.stockOf("beef"));
    }

    @Test
    void whenTheDishFailsTheQualityCheckOnlyTheOrderIsUndone() {
        kitchen.theGrillOnlyReaches(60);

        ChainRecord chain = kitchen.aCustomerOrders(1, "burger", "south");

        kitchen.theSavedOrder(chain)
                .endedAs(ChainRecord.Status.COMPENSATED)
                .becauseOf("quality check failed, burger reached 60 c but needs 70 c")
                .hasOrder(KitchenOrder.Status.CANCELLED)
                .hasTicket(CookingTicket.Status.REJECTED)
                .hasNoBill();
        assertEquals(10, kitchen.stockOf("bun"));
    }

    @Test
    void whenDeliveryFailsAfterCookingTheCookingIsUndoneAndThenTheOrder() {
        kitchen.courierIsBusy("dana");

        ChainRecord chain = kitchen.aCustomerOrders(2, "steak", "north");

        kitchen.theSavedOrder(chain)
                .endedAs(ChainRecord.Status.COMPENSATED)
                .becauseOf("no free courier in north")
                .hasOrder(KitchenOrder.Status.CANCELLED)
                .hasTicket(CookingTicket.Status.CANCELLED)
                .hasBill(Bill.Status.VOIDED)
                .wentThrough("order 1 reserved", "cooked on grill", "delivery failed", "cooking undone",
                        "order cancelled");
        assertEquals(10, kitchen.stockOf("beef"));
    }

    @Test
    void anOrderWithoutEnoughIngredientsIsRejectedBeforeAnythingIsSaved() {
        OrderRejectedException e = assertThrows(OrderRejectedException.class,
                () -> kitchen.aCustomerOrders(10, "steak", "center"));

        assertEquals("not enough beef for 10 steak", e.getMessage());
        assertEquals(0, kitchen.savedOrderChains());
        assertEquals(10, kitchen.stockOf("beef"));
    }

    @ParameterizedTest
    @CsvSource({
            "sushi, 1, center, sushi is not on the menu",
            "burger, 0, center, quantity must be from 1 to 10",
            "burger, 1, mars, we do not deliver to mars"})
    void badInputIsRejectedBeforeAnythingIsSaved(String dish, int quantity, String zone, String message) {
        OrderRejectedException e = assertThrows(OrderRejectedException.class,
                () -> kitchen.aCustomerOrders(quantity, dish, zone));

        assertEquals(message, e.getMessage());
        assertEquals(0, kitchen.savedOrderChains());
    }

    @Test
    void theChainCanBeRunOneStepAtATime() {
        ChainRecord chain = kitchen.aCustomerStartsAnOrder(1, "pizza", "south");
        assertEquals(ChainRecord.Status.PENDING_B, chain.getStatus());

        assertEquals(ChainRecord.Status.PENDING_C, kitchen.theNextStepRunsFor(chain).getStatus());
        assertEquals(ChainRecord.Status.COMPLETED, kitchen.theNextStepRunsFor(chain).getStatus());
        assertEquals(ChainRecord.Status.COMPLETED, kitchen.theNextStepRunsFor(chain).getStatus());
    }

    @Test
    void secondOrderToABusyZoneWaitsForTheFirstDelivery() {
        ChainRecord first = kitchen.aCustomerOrders(1, "fries", "center");
        ChainRecord second = kitchen.aCustomerOrders(1, "fries", "center");
        assertEquals(ChainRecord.Status.COMPENSATED, second.getStatus());

        kitchen.admin().finishDelivery(kitchen.theSavedOrder(first).hasBill(Bill.Status.DISPATCHED).billId());
        ChainRecord third = kitchen.aCustomerOrders(1, "fries", "center");

        assertEquals(ChainRecord.Status.COMPLETED, third.getStatus());
    }

    @Test
    void menuComesFromTheFacade() {
        assertEquals(List.of("burger", "fries", "pizza", "steak"), Kitchen.open(dir).facade().menu());
    }

    @Test
    void unknownChainIsReported() {
        assertThrows(IllegalArgumentException.class, () -> Kitchen.open(dir).facade().findOrder(5));
    }
}
