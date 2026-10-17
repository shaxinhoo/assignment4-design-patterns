# assignment 4. facade

domain 14, restaurant kitchen: a customer orders a dish for delivery, the kitchen reserves the ingredients (a), cooks the dish (b) and sends it with a courier (c)

## subsystems

- a, intake: OrderQueue, InventoryChecker, IntakeStore, KitchenOrder (reserved, served, cancelled)
- b, cooking: CookingLine, StationAllocator, QualityChecker, CookingStore, CookingTicket (assigned, cooked, rejected, cancelled)
- c, checkout: BillingModule, PriceCalculator, DeliveryDispatcher, CheckoutStore, Bill (open, dispatched, delivered, voided)

the subsystems do not know each other, they only meet in the facades. each one can be used alone, see OrderQueueTest, CookingLineTest and BillingModuleTest

## facades

KitchenFacade runs a > b > c for the customer. KitchenAdminFacade shows the saved state, resumes unfinished orders and cancels orders by hand

everything is saved in csv files in the data folder. the chain status (pending_b, pending_c, completed, rollback_b, rollback_a, compensated) is saved in chains.csv and read back before every step, so after a restart the facade knows what to finish or what to undo

- b fails after a (grill is broken or the dish fails the quality check): the order is cancelled
- c fails after a and b (no free courier): the cooking is cancelled, then the order
- wrong dish, quantity, zone or not enough ingredients: rejected before anything is saved

## run

```
./mvnw -q compile exec:java
```

to start from a clean state use menu option 14 or delete the data folder

## demo

1. option 1, burger, 2, center: completed
2. option 7, grill, then option 1, burger, 1, south: cooking failed > order cancelled
3. option 10, dana, then option 1, steak, 1, north: delivery failed > cooking undone > order cancelled
4. option 2, pizza, 1, south, exit, start again, option 4: the order is finished from the saved status

## least knowledge

before, the client knows every subsystem and does the rollback itself:

```java
KitchenOrder order = orderQueue.place("burger", 2, "center");
try {
    CookingTicket ticket = cookingLine.cook(order.getId(), order.getDish(), order.getQuantity());
    try {
        billingModule.checkout(order.getId(), order.getDish(), order.getQuantity(), order.getZone());
    } catch (CheckoutException e) {
        cookingLine.cancel(ticket.getId());
        orderQueue.cancel(order.getId());
    }
} catch (CookingException e) {
    orderQueue.cancel(order.getId());
}
```

after, the client has one friend:

```java
ChainRecord result = kitchen.placeOrder("burger", 2, "center");
```

KitchenApp only knows the two facades and ChainRecord, it does not import anything from intake, cooking or checkout

## facade vs adapter

an adapter changes the interface of one class so it fits what the client expects. here no class has a wrong interface, the problem is that one order needs three subsystems in the right order and a rollback when a later step fails. KitchenFacade gives one simple operation for this whole workflow, so the intent is to simplify and coordinate, not to convert

## tests

./mvnw test runs all tests, the coverage report is in target/site/jacoco/index.html (about 98 percent, ./mvnw verify fails below 80). every test uses its own temp folder. KitchenWorld is the test dsl, KitchenRestartTest reopens the kitchen in the middle of a chain

uml diagram: docs/uml.png
