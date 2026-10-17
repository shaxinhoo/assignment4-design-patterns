package kitchen.checkout;

import java.util.List;
import java.util.Optional;

public class BillingModule {

    private final CheckoutStore store;
    private final PriceCalculator prices = new PriceCalculator();
    private final DeliveryDispatcher dispatcher = new DeliveryDispatcher();

    public BillingModule(CheckoutStore store) {
        this.store = store;
    }

    public boolean deliversTo(String zone) {
        return dispatcher.deliversTo(zone);
    }

    public Bill checkout(int orderId, String dish, int quantity, String zone) {
        Optional<Bill> existing = store.findLiveBillForOrder(orderId);
        if (existing.isPresent() && existing.get().getStatus() != Bill.Status.OPEN) {
            return existing.get();
        }
        Bill bill = existing.orElseGet(() -> openBill(orderId, dish, quantity, zone));
        String courier;
        try {
            courier = dispatcher.chooseCourier(zone, store.freeCouriersIn(zone));
        } catch (CheckoutException e) {
            store.saveBill(bill.withStatus(Bill.Status.VOIDED));
            throw e;
        }
        store.saveCourier(new CheckoutStore.Courier(courier, zone, false));
        Bill dispatched = bill.dispatchedBy(courier);
        store.saveBill(dispatched);
        return dispatched;
    }

    public Bill finishDelivery(int billId) {
        Bill bill = find(billId);
        Bill delivered = bill.withStatus(Bill.Status.DELIVERED);
        store.saveBill(delivered);
        setCourierFree(bill.getCourier(), true);
        return delivered;
    }

    public void setCourierFree(String name, boolean free) {
        CheckoutStore.Courier courier = store.findCourier(name)
                .orElseThrow(() -> new CheckoutException("unknown courier " + name));
        store.saveCourier(new CheckoutStore.Courier(name, courier.zone(), free));
    }

    public Bill find(int billId) {
        return store.findBill(billId).orElseThrow(() -> new CheckoutException("bill " + billId + " not found"));
    }

    public List<Bill> bills() {
        return store.loadBills();
    }

    public List<CheckoutStore.Courier> couriers() {
        return store.loadCouriers();
    }

    public void reset() {
        store.reset();
    }

    private Bill openBill(int orderId, String dish, int quantity, String zone) {
        Bill bill = new Bill(store.nextBillId(), orderId, prices.total(dish, quantity), zone, "", Bill.Status.OPEN);
        store.saveBill(bill);
        return bill;
    }
}
