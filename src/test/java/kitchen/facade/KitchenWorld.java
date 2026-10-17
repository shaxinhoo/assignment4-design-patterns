package kitchen.facade;

import kitchen.checkout.Bill;
import kitchen.checkout.BillingModule;
import kitchen.checkout.CheckoutStore;
import kitchen.cooking.CookingLine;
import kitchen.cooking.CookingStore;
import kitchen.cooking.CookingTicket;
import kitchen.intake.IntakeStore;
import kitchen.intake.KitchenOrder;
import kitchen.intake.OrderQueue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitchenWorld {

    private final Path dataDir;
    private Kitchen kitchen;

    private KitchenWorld(Path dataDir) {
        this.dataDir = dataDir;
        this.kitchen = Kitchen.open(dataDir);
    }

    static KitchenWorld aFreshKitchenIn(Path dataDir) {
        return new KitchenWorld(dataDir);
    }

    KitchenWorld theGrillIsBroken() {
        kitchen.admin().breakStation("grill");
        return this;
    }

    KitchenWorld theGrillOnlyReaches(int temperature) {
        kitchen.admin().setStationTemperature("grill", temperature);
        return this;
    }

    KitchenWorld courierIsBusy(String courier) {
        kitchen.admin().setCourierFree(courier, false);
        return this;
    }

    KitchenWorld theKitchenRestarts() {
        kitchen = Kitchen.open(dataDir);
        return this;
    }

    ChainRecord aCustomerOrders(int quantity, String dish, String zone) {
        return kitchen.facade().placeOrder(dish, quantity, zone);
    }

    ChainRecord aCustomerStartsAnOrder(int quantity, String dish, String zone) {
        return kitchen.facade().startOrder(dish, quantity, zone);
    }

    ChainRecord theNextStepRunsFor(ChainRecord chain) {
        return kitchen.facade().nextStep(chain.getId());
    }

    List<ChainRecord> theManagerResumesUnfinishedOrders() {
        return kitchen.admin().resumeUnfinished();
    }

    ChainRecord theManagerCancels(ChainRecord chain) {
        return kitchen.admin().cancelOrder(chain.getId());
    }

    KitchenAdminFacade admin() {
        return kitchen.admin();
    }

    int stockOf(String ingredient) {
        return new OrderQueue(new IntakeStore(dataDir)).stock().get(ingredient);
    }

    int savedOrderChains() {
        return new ChainStore(dataDir).loadAll().size();
    }

    SavedChain theSavedOrder(ChainRecord chain) {
        return new SavedChain(new ChainStore(dataDir).find(chain.getId()));
    }

    class SavedChain {

        private final ChainRecord chain;

        private SavedChain(ChainRecord chain) {
            this.chain = chain;
        }

        SavedChain endedAs(ChainRecord.Status status) {
            assertEquals(status, chain.getStatus());
            return this;
        }

        SavedChain hasOrder(KitchenOrder.Status status) {
            assertEquals(status, new OrderQueue(new IntakeStore(dataDir)).find(chain.getOrderId()).getStatus());
            return this;
        }

        SavedChain hasTicket(CookingTicket.Status status) {
            assertEquals(status, ticket().orElseThrow().getStatus());
            return this;
        }

        SavedChain hasNoTicket() {
            assertTrue(ticket().isEmpty());
            return this;
        }

        SavedChain hasBill(Bill.Status status) {
            assertEquals(status, bill().orElseThrow().getStatus());
            return this;
        }

        SavedChain hasNoBill() {
            assertTrue(bill().isEmpty());
            return this;
        }

        SavedChain becauseOf(String reason) {
            assertEquals(reason, chain.getReason());
            return this;
        }

        SavedChain wentThrough(String... steps) {
            assertEquals(String.join(" > ", steps), chain.getHistory());
            return this;
        }

        int billId() {
            return chain.getBillId();
        }

        private Optional<CookingTicket> ticket() {
            return new CookingLine(new CookingStore(dataDir)).tickets().stream()
                    .filter(ticket -> ticket.getOrderId() == chain.getOrderId())
                    .reduce((first, second) -> second);
        }

        private Optional<Bill> bill() {
            return new BillingModule(new CheckoutStore(dataDir)).bills().stream()
                    .filter(bill -> bill.getOrderId() == chain.getOrderId())
                    .reduce((first, second) -> second);
        }
    }
}
