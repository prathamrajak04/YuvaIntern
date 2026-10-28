package com.inventory.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventory.model.Item;
import com.inventory.repository.InMemoryItemRepository;
import com.inventory.service.InventoryService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Drives the menu with a scripted input string, exactly like a person typing (or a pipe). */
@DisplayName("ConsoleMenu (user interface)")
class ConsoleMenuTest {

    private InventoryService service;
    private int exitCode;

    @BeforeEach
    void setUp() {
        service = new InventoryService(new InMemoryItemRepository());
        service.addItem(new Item("A1", "Laptop", 10, 50000));
        service.addItem(new Item("A2", "Mouse", 3, 500));
        service.addItem(new Item("A3", "Keyboard", 5, 1500));
    }

    private String type(InventoryService target, String... lines) {
        String input = String.join("\n", lines) + "\n";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        exitCode = new ConsoleMenu(target, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), out, 5).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private String type(String... lines) {
        return type(service, lines);
    }

    @Test
    @DisplayName("0 exits immediately with a goodbye and exit code 0")
    void exitsOnZero() {
        String output = type("0");
        assertTrue(output.contains("Goodbye."));
        assertEquals(0, exitCode);
    }

    @Test
    @DisplayName("1: add item through the prompts")
    void addItem() {
        String output = type("1", "B1", "Monitor", "2", "12000.50", "0");
        assertTrue(output.contains("Added: Item{id='B1'"));
        assertEquals(4, service.size());
        assertEquals(2, service.getItem("B1").orElseThrow().quantity());
    }

    @Test
    @DisplayName("1: non-numeric quantity shows an error and the menu keeps running")
    void invalidQuantityKeepsRunning() {
        String output = type("1", "B1", "Cable", "abc", "0");
        assertTrue(output.contains("Error: 'abc' is not a valid whole number"));
        assertTrue(output.contains("Goodbye."));
        assertEquals(3, service.size());
    }

    @Test
    @DisplayName("1: non-numeric price shows an error")
    void invalidPrice() {
        String output = type("1", "B1", "Cable", "5", "free", "0");
        assertTrue(output.contains("Error: 'free' is not a valid number"));
        assertEquals(3, service.size());
    }

    @Test
    @DisplayName("1: duplicate id shows the domain error")
    void duplicateId() {
        String output = type("1", "A1", "Fake", "1", "1", "0");
        assertTrue(output.contains("Error: Item already exists: A1"));
        assertEquals("Laptop", service.getItem("A1").orElseThrow().name());
    }

    @Test
    @DisplayName("1: blank name shows the validation error")
    void blankName() {
        String output = type("1", "B1", "   ", "1", "1", "0");
        assertTrue(output.contains("Error: Item name must not be null or blank"));
    }

    @Test
    @DisplayName("2: lists all items as a table")
    void listsItems() {
        String output = type("2", "0");
        assertTrue(output.contains("Laptop"));
        assertTrue(output.contains("Mouse"));
        assertTrue(output.contains("Keyboard"));
        assertTrue(output.contains("500000.00")); // 10 x 50000 total value column
    }

    @Test
    @DisplayName("2: empty inventory prints (no items)")
    void emptyList() {
        String output = type(new InventoryService(new InMemoryItemRepository()), "2", "0");
        assertTrue(output.contains("(no items)"));
    }

    @Test
    @DisplayName("3: find by id, found and not found")
    void findById() {
        assertTrue(type("3", "A1", "0").contains("Laptop"));
        assertTrue(type("3", "ZZZ", "0").contains("No item with id 'ZZZ'."));
    }

    @Test
    @DisplayName("4: search is case-insensitive")
    void searchByName() {
        String output = type("4", "LAP", "0");
        assertTrue(output.contains("Laptop"));
        assertFalse(output.contains("Mouse"));
    }

    @Test
    @DisplayName("4: search with no match prints (no items)")
    void searchNoMatch() {
        assertTrue(type("4", "tablet", "0").contains("(no items)"));
    }

    @Test
    @DisplayName("5: update quantity")
    void updateQuantity() {
        String output = type("5", "A1", "25", "0");
        assertTrue(output.contains("Updated: Item{id='A1'"));
        assertEquals(25, service.getItem("A1").orElseThrow().quantity());
    }

    @Test
    @DisplayName("5: negative quantity is rejected and the old value is kept")
    void negativeQuantity() {
        String output = type("5", "A1", "-3", "0");
        assertTrue(output.contains("Error: Quantity must not be negative"));
        assertEquals(10, service.getItem("A1").orElseThrow().quantity());
    }

    @Test
    @DisplayName("6: update price")
    void updatePrice() {
        type("6", "A2", "650.5", "0");
        assertEquals(650.5, service.getItem("A2").orElseThrow().price(), 0.0001);
    }

    @Test
    @DisplayName("7: rename item")
    void rename() {
        String output = type("7", "A2", "Wireless Mouse", "0");
        assertTrue(output.contains("Wireless Mouse"));
        assertEquals("Wireless Mouse", service.getItem("A2").orElseThrow().name());
    }

    @Test
    @DisplayName("8: delete item")
    void delete() {
        String output = type("8", "A2", "0");
        assertTrue(output.contains("Deleted item A2."));
        assertEquals(2, service.size());
    }

    @Test
    @DisplayName("update and delete of an unknown id show Item not found")
    void unknownIds() {
        assertTrue(type("5", "NOPE", "1", "0").contains("Error: Item not found: NOPE"));
        assertTrue(type("6", "NOPE", "1", "0").contains("Error: Item not found: NOPE"));
        assertTrue(type("7", "NOPE", "x", "0").contains("Error: Item not found: NOPE"));
        assertTrue(type("8", "NOPE", "0").contains("Error: Item not found: NOPE"));
    }

    @Test
    @DisplayName("9: low stock with blank answer uses the default threshold (5)")
    void lowStockDefault() {
        String output = type("9", "", "0");
        assertTrue(output.contains("below 5"));
        assertTrue(output.contains("Mouse"));      // qty 3
        assertFalse(output.contains("Keyboard"));  // qty 5 is NOT below 5
        assertFalse(output.contains("Laptop"));
    }

    @Test
    @DisplayName("9: low stock with a custom threshold")
    void lowStockCustom() {
        String output = type("9", "11", "0");
        assertTrue(output.contains("Laptop"));     // 10 < 11
    }

    @Test
    @DisplayName("9: invalid threshold shows an error")
    void lowStockInvalid() {
        assertTrue(type("9", "many", "0").contains("Error: 'many' is not a valid whole number"));
    }

    @Test
    @DisplayName("10: total inventory value")
    void totalValue() {
        // 10*50000 + 3*500 + 5*1500 = 509000
        assertTrue(type("10", "0").contains("Total inventory value: 509000.00"));
    }

    @Test
    @DisplayName("unknown menu option is explained, not crashed on")
    void unknownOption() {
        String output = type("42", "hello", "", "0");
        assertTrue(output.contains("Unknown option '42'"));
        assertTrue(output.contains("Unknown option 'hello'"));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    @DisplayName("end of input without choosing 0 exits cleanly")
    void endOfInput() {
        String output = type("2");
        assertTrue(output.contains("Input closed. Goodbye."));
        assertEquals(0, exitCode);
    }

    @Test
    @DisplayName("end of input in the middle of a command exits cleanly")
    void endOfInputMidCommand() {
        String output = type("1", "B1");
        assertTrue(output.contains("Input closed."));
        assertEquals(3, service.size());
    }
}
