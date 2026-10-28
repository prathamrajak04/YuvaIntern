package com.inventory.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventory.exception.DuplicateItemException;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.model.Item;
import com.inventory.repository.InMemoryItemRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Behaviour must be identical to the Week 3 service; the refactoring only changed the internals. */
@DisplayName("InventoryService (refactored)")
class InventoryServiceTest {

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService();
        service.addItem(new Item("A1", "Laptop", 10, 50000));
        service.addItem(new Item("A2", "Mouse", 3, 500));
        service.addItem(new Item("A3", "Keyboard", 5, 1500));
    }

    // ------------------------------------------------------------ CREATE
    @Test
    @DisplayName("create: adds a new item")
    void createAddsItem() {
        service.addItem(new Item("B1", "Monitor", 2, 12000));
        assertEquals(4, service.size());
        assertTrue(service.getItem("B1").isPresent());
    }

    @Test
    @DisplayName("create: null item is rejected")
    void createRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> service.addItem(null));
    }

    @Test
    @DisplayName("create: duplicate id throws and keeps the original")
    void createRejectsDuplicateId() {
        assertThrows(DuplicateItemException.class,
                () -> service.addItem(new Item("A1", "Fake Laptop", 1, 1)));
        assertEquals("Laptop", service.getItem("A1").orElseThrow().name());
        assertEquals(3, service.size());
    }

    // ------------------------------------------------------------ READ
    @Test
    @DisplayName("read: finds an existing item by id")
    void readFindsById() {
        assertEquals("Mouse", service.getItem("A2").orElseThrow().name());
    }

    @Test
    @DisplayName("read: unknown or null id returns empty Optional")
    void readUnknownIdIsEmpty() {
        assertTrue(service.getItem("ZZZ").isEmpty());
        assertTrue(service.getItem(null).isEmpty());
    }

    @Test
    @DisplayName("read: getAllItems keeps insertion order")
    void readListsAllInOrder() {
        List<String> ids = service.getAllItems().stream().map(Item::id).toList();
        assertEquals(List.of("A1", "A2", "A3"), ids);
    }

    @Test
    @DisplayName("read: returned list is read-only and does not expose internal storage")
    void readReturnsReadOnlyCopy() {
        List<Item> all = service.getAllItems();
        assertThrows(UnsupportedOperationException.class, all::clear);
        assertEquals(3, service.size());
    }

    @Test
    @DisplayName("read: empty inventory returns empty list")
    void readEmptyInventory() {
        assertTrue(new InventoryService().getAllItems().isEmpty());
    }

    @Test
    @DisplayName("search: case-insensitive and partial")
    void searchIgnoresCase() {
        assertEquals(1, service.searchByName("laptop").size());
        assertEquals(1, service.searchByName("KEYB").size());
        assertEquals(1, service.searchByName("  mouse  ").size());
    }

    @Test
    @DisplayName("search: blank, null and no-match return an empty list")
    void searchEdgeCases() {
        assertTrue(service.searchByName(null).isEmpty());
        assertTrue(service.searchByName("   ").isEmpty());
        assertTrue(service.searchByName("tablet").isEmpty());
    }

    @Test
    @DisplayName("search: finds a renamed item by its NEW name (cached lower-case name is refreshed)")
    void searchSeesRenamedItem() {
        service.updateName("A2", "Wireless MOUSE");
        assertEquals(1, service.searchByName("wireless").size());
        assertTrue(service.searchByName("mouse").size() == 1);
    }

    @Test
    @DisplayName("low stock: strictly below the threshold (boundary)")
    void lowStockBoundary() {
        // quantities: Laptop 10, Mouse 3, Keyboard 5
        List<Item> low = service.getLowStockItems(5);
        assertEquals(1, low.size());
        assertEquals("A2", low.get(0).id());
    }

    @Test
    @DisplayName("total value: sums quantity x price")
    void totalValue() {
        // 10*50000 + 3*500 + 5*1500 = 509000
        assertEquals(509000.0, service.getTotalInventoryValue(), 0.0001);
    }

    @Test
    @DisplayName("total value: zero for an empty inventory")
    void totalValueEmpty() {
        assertEquals(0.0, new InventoryService().getTotalInventoryValue(), 0.0001);
    }

    // ------------------------------------------------------------ UPDATE
    @Test
    @DisplayName("update: changes quantity")
    void updateQuantity() {
        service.updateQuantity("A1", 25);
        assertEquals(25, service.getItem("A1").orElseThrow().quantity());
    }

    @Test
    @DisplayName("update: quantity can be set to zero (boundary)")
    void updateQuantityZeroAllowed() {
        service.updateQuantity("A1", 0);
        assertEquals(0, service.getItem("A1").orElseThrow().quantity());
    }

    @Test
    @DisplayName("update: negative quantity is rejected and the old value is kept")
    void updateRejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> service.updateQuantity("A1", -1));
        assertEquals(10, service.getItem("A1").orElseThrow().quantity());
        assertEquals(509000.0, service.getTotalInventoryValue(), 0.0001);
    }

    @Test
    @DisplayName("update: price and name")
    void updatePriceAndName() {
        service.updatePrice("A2", 650.5);
        service.updateName("A2", "Wireless Mouse");
        Item mouse = service.getItem("A2").orElseThrow();
        assertEquals(650.5, mouse.price(), 0.0001);
        assertEquals("Wireless Mouse", mouse.name());
        assertEquals(3, mouse.quantity());
    }

    @Test
    @DisplayName("update: invalid price or name is rejected")
    void updateRejectsInvalidPriceAndName() {
        assertThrows(IllegalArgumentException.class, () -> service.updatePrice("A2", -10));
        assertThrows(IllegalArgumentException.class, () -> service.updateName("A2", "  "));
        assertEquals("Mouse", service.getItem("A2").orElseThrow().name());
    }

    @Test
    @DisplayName("update: unknown id throws ItemNotFoundException")
    void updateUnknownId() {
        assertThrows(ItemNotFoundException.class, () -> service.updateQuantity("NOPE", 1));
        assertThrows(ItemNotFoundException.class, () -> service.updatePrice("NOPE", 1));
        assertThrows(ItemNotFoundException.class, () -> service.updateName("NOPE", "x"));
        assertThrows(ItemNotFoundException.class, () -> service.updateQuantity(null, 1));
    }

    // ------------------------------------------------------------ DELETE
    @Test
    @DisplayName("delete: removes an existing item")
    void deleteItem() {
        service.deleteItem("A2");
        assertEquals(2, service.size());
        assertTrue(service.getItem("A2").isEmpty());
    }

    @Test
    @DisplayName("delete: unknown id throws ItemNotFoundException")
    void deleteUnknownId() {
        assertThrows(ItemNotFoundException.class, () -> service.deleteItem("NOPE"));
        assertThrows(ItemNotFoundException.class, () -> service.deleteItem(null));
        assertEquals(3, service.size());
    }

    @Test
    @DisplayName("delete: deleting twice fails the second time")
    void deleteTwice() {
        service.deleteItem("A1");
        assertThrows(ItemNotFoundException.class, () -> service.deleteItem("A1"));
    }

    @Test
    @DisplayName("delete: a deleted id can be added again")
    void reAddAfterDelete() {
        service.deleteItem("A1");
        assertDoesNotThrow(() -> service.addItem(new Item("A1", "New Laptop", 1, 1)));
    }

    // ------------------------------------------------------------ REFACTORING-SPECIFIC
    @Test
    @DisplayName("running total equals a full recomputation after mixed operations")
    void runningTotalMatchesRecomputation() {
        service.addItem(new Item("B1", "Monitor", 2, 12000.25));
        service.updateQuantity("A1", 7);
        service.updatePrice("A2", 499.99);
        service.deleteItem("A3");
        service.updateQuantity("B1", 0);

        double recomputed = service.getAllItems().stream().mapToDouble(Item::totalValue).sum();
        assertEquals(recomputed, service.getTotalInventoryValue(), 0.0001);
    }

    @Test
    @DisplayName("an Item fetched earlier is not changed by later updates (immutability)")
    void fetchedItemIsASnapshot() {
        Item before = service.getItem("A1").orElseThrow();
        service.updateQuantity("A1", 99);
        assertEquals(10, before.quantity());
        assertEquals(99, service.getItem("A1").orElseThrow().quantity());
    }

    @Test
    @DisplayName("a custom repository can be injected")
    void acceptsInjectedRepository() {
        InMemoryItemRepository repository = new InMemoryItemRepository();
        InventoryService injected = new InventoryService(repository);
        injected.addItem(new Item("X1", "Injected", 1, 1));
        assertTrue(repository.existsById("X1"));
        assertThrows(NullPointerException.class, () -> new InventoryService(null));
    }
}
