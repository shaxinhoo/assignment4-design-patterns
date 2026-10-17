package kitchen.facade;

import kitchen.checkout.BillingModule;
import kitchen.checkout.CheckoutStore;
import kitchen.cooking.CookingLine;
import kitchen.cooking.CookingStore;
import kitchen.intake.IntakeStore;
import kitchen.intake.OrderQueue;

import java.nio.file.Path;

public final class Kitchen {

    private final KitchenFacade facade;
    private final KitchenAdminFacade admin;

    private Kitchen(KitchenFacade facade, KitchenAdminFacade admin) {
        this.facade = facade;
        this.admin = admin;
    }

    public static Kitchen open(Path dataDir) {
        OrderQueue orders = new OrderQueue(new IntakeStore(dataDir));
        CookingLine cooking = new CookingLine(new CookingStore(dataDir));
        BillingModule billing = new BillingModule(new CheckoutStore(dataDir));
        ChainStore chains = new ChainStore(dataDir);
        KitchenFacade facade = new KitchenFacade(orders, cooking, billing, chains);
        return new Kitchen(facade, new KitchenAdminFacade(facade, orders, cooking, billing, chains));
    }

    public KitchenFacade facade() {
        return facade;
    }

    public KitchenAdminFacade admin() {
        return admin;
    }
}
