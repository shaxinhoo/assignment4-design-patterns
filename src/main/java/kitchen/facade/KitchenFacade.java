package kitchen.facade;

import kitchen.checkout.Bill;
import kitchen.checkout.BillingModule;
import kitchen.checkout.CheckoutException;
import kitchen.cooking.CookingException;
import kitchen.cooking.CookingLine;
import kitchen.cooking.CookingTicket;
import kitchen.intake.IntakeException;
import kitchen.intake.KitchenOrder;
import kitchen.intake.OrderQueue;

import java.util.List;

public class KitchenFacade {

    private final OrderQueue orders;
    private final CookingLine cooking;
    private final BillingModule billing;
    private final ChainStore chains;

    public KitchenFacade(OrderQueue orders, CookingLine cooking, BillingModule billing, ChainStore chains) {
        this.orders = orders;
        this.cooking = cooking;
        this.billing = billing;
        this.chains = chains;
    }

    public List<String> menu() {
        return orders.menu();
    }

    public ChainRecord placeOrder(String dish, int quantity, String zone) {
        ChainRecord chain = startOrder(dish, quantity, zone);
        return finishOrder(chain.getId());
    }

    public ChainRecord startOrder(String dish, int quantity, String zone) {
        if (!billing.deliversTo(zone)) {
            throw new OrderRejectedException("we do not deliver to " + zone);
        }
        try {
            KitchenOrder order = orders.place(dish, quantity, zone);
            return chains.create(order.getId());
        } catch (IntakeException e) {
            throw new OrderRejectedException(e.getMessage());
        }
    }

    public ChainRecord nextStep(int chainId) {
        ChainRecord chain = chains.find(chainId);
        if (!chain.isFinished()) {
            runStep(chain);
        }
        return chains.find(chainId);
    }

    public ChainRecord finishOrder(int chainId) {
        ChainRecord chain = chains.find(chainId);
        while (!chain.isFinished()) {
            runStep(chain);
            chain = chains.find(chainId);
        }
        return chain;
    }

    public ChainRecord findOrder(int chainId) {
        return chains.find(chainId);
    }

    private void runStep(ChainRecord chain) {
        switch (chain.getStatus()) {
            case PENDING_B -> cook(chain);
            case PENDING_C -> deliver(chain);
            case ROLLBACK_B -> undoCooking(chain);
            case ROLLBACK_A -> undoOrder(chain);
            default -> throw new IllegalStateException("order chain " + chain.getId() + " is already finished");
        }
    }

    private void cook(ChainRecord chain) {
        KitchenOrder order = orders.find(chain.getOrderId());
        try {
            CookingTicket ticket = cooking.cook(order.getId(), order.getDish(), order.getQuantity());
            chains.save(chain.withTicket(ticket.getId())
                    .moveTo(ChainRecord.Status.PENDING_C, "cooked on " + ticket.getStation()));
        } catch (CookingException e) {
            chains.save(chain.withReason(e.getMessage())
                    .moveTo(ChainRecord.Status.ROLLBACK_A, "cooking failed"));
        }
    }

    private void deliver(ChainRecord chain) {
        KitchenOrder order = orders.find(chain.getOrderId());
        try {
            Bill bill = billing.checkout(order.getId(), order.getDish(), order.getQuantity(), order.getZone());
            orders.markServed(order.getId());
            chains.save(chain.withBill(bill.getId())
                    .moveTo(ChainRecord.Status.COMPLETED, "sent with " + bill.getCourier()));
        } catch (CheckoutException e) {
            chains.save(chain.withReason(e.getMessage())
                    .moveTo(ChainRecord.Status.ROLLBACK_B, "delivery failed"));
        }
    }

    private void undoCooking(ChainRecord chain) {
        cooking.cancel(chain.getTicketId());
        chains.save(chain.moveTo(ChainRecord.Status.ROLLBACK_A, "cooking undone"));
    }

    private void undoOrder(ChainRecord chain) {
        orders.cancel(chain.getOrderId());
        chains.save(chain.moveTo(ChainRecord.Status.COMPENSATED, "order cancelled"));
    }
}
