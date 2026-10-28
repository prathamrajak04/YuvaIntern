package com.inventory.cli;

import com.inventory.exception.DuplicateItemException;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.exception.RepositoryException;
import com.inventory.model.Item;
import com.inventory.service.InventoryService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Text user interface: the outermost layer. It talks ONLY to {@link InventoryService}; it never
 * touches the repository or files.
 *
 * <p>Input and output streams are injected, so the whole menu can be driven by a scripted string in
 * tests and by a pipe in the deployment smoke test, with no special test mode in the code.
 *
 * <p>Error policy: domain and storage exceptions are caught here and shown as a one-line
 * {@code Error: ...} message; the menu keeps running. Unexpected exceptions (real bugs) are NOT
 * caught, so they surface instead of being hidden.
 */
public class ConsoleMenu {

    private final InventoryService service;
    private final BufferedReader in;
    private final PrintStream out;
    private final int defaultLowStockThreshold;

    public ConsoleMenu(InventoryService service, InputStream in, PrintStream out, int defaultLowStockThreshold) {
        this.service = service;
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = out;
        this.defaultLowStockThreshold = defaultLowStockThreshold;
    }

    /** Runs until the user chooses 0 or the input ends. @return process exit code (always 0 here) */
    public int run() {
        out.println("=== Inventory Manager ===");
        try {
            while (true) {
                printMenu();
                String choice = prompt("Choose an option").trim();
                if (choice.equals("0")) {
                    out.println("Goodbye.");
                    return 0;
                }
                try {
                    dispatch(choice);
                } catch (IllegalArgumentException | ItemNotFoundException
                         | DuplicateItemException | RepositoryException e) {
                    out.println("Error: " + e.getMessage());
                }
            }
        } catch (EndOfInput e) {
            out.println();
            out.println("Input closed. Goodbye.");
            return 0;
        }
    }

    // ------------------------------------------------------------------ menu

    private void printMenu() {
        out.println();
        out.println(" 1) Add item          6) Update price");
        out.println(" 2) List all items    7) Rename item");
        out.println(" 3) Find by id        8) Delete item");
        out.println(" 4) Search by name    9) Low-stock report");
        out.println(" 5) Update quantity  10) Total inventory value");
        out.println(" 0) Exit");
    }

    private void dispatch(String choice) {
        switch (choice) {
            case "1" -> addItem();
            case "2" -> printTable(service.getAllItems());
            case "3" -> findById();
            case "4" -> search();
            case "5" -> updateQuantity();
            case "6" -> updatePrice();
            case "7" -> rename();
            case "8" -> delete();
            case "9" -> lowStock();
            case "10" -> out.println(String.format(Locale.ROOT, "Total inventory value: %.2f", service.getTotalInventoryValue()));
            default -> out.println("Unknown option '" + choice + "'. Please enter a number from the menu.");
        }
    }

    // ------------------------------------------------------------------ commands

    private void addItem() {
        String id = prompt("Item id");
        String name = prompt("Name");
        int quantity = readInt("Quantity");
        double price = readDouble("Price");
        Item item = new Item(id, name, quantity, price);
        service.addItem(item);
        out.println("Added: " + item);
    }

    private void findById() {
        String id = prompt("Item id");
        service.getItem(id).ifPresentOrElse(
                item -> out.println(item),
                () -> out.println("No item with id '" + id.trim() + "'."));
    }

    private void search() {
        printTable(service.searchByName(prompt("Search text")));
    }

    private void updateQuantity() {
        String id = prompt("Item id");
        service.updateQuantity(id, readInt("New quantity"));
        out.println("Updated: " + service.getItem(id).orElseThrow());
    }

    private void updatePrice() {
        String id = prompt("Item id");
        service.updatePrice(id, readDouble("New price"));
        out.println("Updated: " + service.getItem(id).orElseThrow());
    }

    private void rename() {
        String id = prompt("Item id");
        service.updateName(id, prompt("New name"));
        out.println("Updated: " + service.getItem(id).orElseThrow());
    }

    private void delete() {
        String id = prompt("Item id");
        service.deleteItem(id);
        out.println("Deleted item " + id.trim() + ".");
    }

    private void lowStock() {
        String raw = prompt("Threshold [" + defaultLowStockThreshold + "]").trim();
        int threshold = raw.isEmpty() ? defaultLowStockThreshold : parseInt(raw);
        out.println("Items with quantity below " + threshold + ":");
        printTable(service.getLowStockItems(threshold));
    }

    // ------------------------------------------------------------------ helpers

    private void printTable(List<Item> items) {
        if (items.isEmpty()) {
            out.println("(no items)");
            return;
        }
        out.println(String.format(Locale.ROOT, "%-10s %-24s %6s %12s %14s", "ID", "NAME", "QTY", "PRICE", "VALUE"));
        for (Item item : items) {
            out.println(String.format(Locale.ROOT, "%-10s %-24s %6d %12.2f %14.2f",
                    item.id(), item.name(), item.quantity(), item.price(), item.totalValue()));
        }
    }

    private String prompt(String label) {
        out.print(label + ": ");
        out.flush();
        try {
            String line = in.readLine();
            if (line == null) {
                throw new EndOfInput();
            }
            return line;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int readInt(String label) {
        return parseInt(prompt(label));
    }

    private static int parseInt(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + raw.trim() + "' is not a valid whole number");
        }
    }

    private double readDouble(String label) {
        String raw = prompt(label).trim();
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + raw + "' is not a valid number");
        }
    }

    /** Signals that standard input was closed (Ctrl+D / end of a piped script). */
    private static final class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;

        EndOfInput() {
            super(null, null, false, false); // no stack trace needed for control flow
        }
    }
}
