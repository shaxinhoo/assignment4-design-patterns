package kitchen.app;

import kitchen.facade.ChainRecord;
import kitchen.facade.Kitchen;
import kitchen.facade.KitchenAdminFacade;
import kitchen.facade.KitchenFacade;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class KitchenApp {

    private static final String MENU = String.join("\n",
            "",
            " 1  place an order",
            " 2  start an order and stop after step a (simulate a crash)",
            " 3  run the next step of an order",
            " 4  resume unfinished orders",
            " 5  cancel an order (manual rollback)",
            " 6  show saved state",
            " 7  break a station",
            " 8  repair a station",
            " 9  set station temperature",
            "10  make a courier busy",
            "11  make a courier free",
            "12  finish a delivery",
            "13  restock an ingredient",
            "14  reset all data",
            " 0  exit");

    private final KitchenFacade kitchen;
    private final KitchenAdminFacade admin;
    private final Scanner in;
    private final PrintStream out;

    public KitchenApp(Kitchen kitchen, Scanner in, PrintStream out) {
        this.kitchen = kitchen.facade();
        this.admin = kitchen.admin();
        this.in = in;
        this.out = out;
    }

    public static void main(String[] args) {
        Path dataDir = Path.of(args.length > 0 ? args[0] : "data");
        System.out.println("restaurant kitchen, data folder: " + dataDir.toAbsolutePath());
        new KitchenApp(Kitchen.open(dataDir), new Scanner(System.in), System.out).run();
    }

    public void run() {
        while (true) {
            out.println(MENU);
            String choice = ask("choose");
            if (choice == null || choice.equals("0")) {
                out.println("bye");
                return;
            }
            try {
                handle(choice);
            } catch (NumberFormatException e) {
                out.println("error: please type a number");
            } catch (RuntimeException e) {
                out.println("error: " + e.getMessage());
            }
        }
    }

    private void handle(String choice) {
        switch (choice) {
            case "1" -> show(kitchen.placeOrder(askDish(), askNumber("quantity"), askZone()));
            case "2" -> startOnly();
            case "3" -> show(kitchen.nextStep(askNumber("order chain id")));
            case "4" -> resume();
            case "5" -> show(admin.cancelOrder(askNumber("order chain id")));
            case "6" -> out.println(admin.stateReport());
            case "7" -> admin.breakStation(ask("station (grill, fryer, oven)"));
            case "8" -> admin.repairStation(ask("station (grill, fryer, oven)"));
            case "9" -> admin.setStationTemperature(ask("station (grill, fryer, oven)"), askNumber("temperature"));
            case "10" -> admin.setCourierFree(ask("courier (aidar, dana, timur)"), false);
            case "11" -> admin.setCourierFree(ask("courier (aidar, dana, timur)"), true);
            case "12" -> admin.finishDelivery(askNumber("bill id"));
            case "13" -> admin.restock(ask("ingredient"), askNumber("amount"));
            case "14" -> admin.resetAllData();
            default -> out.println("unknown option");
        }
    }

    private void startOnly() {
        ChainRecord chain = kitchen.startOrder(askDish(), askNumber("quantity"), askZone());
        show(chain);
        out.println("step a is saved, close the app now and use option 4 after a restart");
    }

    private void resume() {
        List<ChainRecord> results = admin.resumeUnfinished();
        if (results.isEmpty()) {
            out.println("nothing to resume");
        }
        results.forEach(this::show);
    }

    private void show(ChainRecord chain) {
        out.println(KitchenAdminFacade.describe(chain));
    }

    private String askDish() {
        return ask("dish " + kitchen.menu());
    }

    private String askZone() {
        return ask("zone (center, north, south)");
    }

    private int askNumber(String question) {
        String answer = ask(question);
        if (answer == null) {
            throw new IllegalStateException("no input");
        }
        return Integer.parseInt(answer);
    }

    private String ask(String question) {
        out.print(question + ": ");
        if (!in.hasNextLine()) {
            return null;
        }
        return in.nextLine().trim();
    }
}
