package com.inventory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("InventoryService CRUD operations")
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
    @Nested
    @DisplayName("Create")
    class CreateTests {

        @Test
        @DisplayName("adds a new item")
        void addsItem() {
            service.addItem(new Item("B1", "Monitor", 2, 12000));
            assertEquals(4, service.size());
            assertTrue(service.getItem("B1").isPresent());
        }

        @Test
        @DisplayName("null item is rejected")
        void rejectsNull() {
            assertThrows(IllegalArgumentException.class, () -> service.addItem(null));
        }

        @Test
        @DisplayName("duplicate id throws DuplicateItemException and keeps original")
        void rejectsDuplicateId() {
            assertThrows(DuplicateItemException.class,
                    () -> service.addItem(new Item("A1", "Fake Laptop", 1, 1)));
            assertEquals("Laptop", service.getItem("A1").get().getName());
            assertEquals(3, service.size());
        }
    }

    // ------------------------------------------------------------ READ
    @Nested
    @DisplayName("Read")
    class ReadTests {

        @Test
        @DisplayName("finds an existing item by id")
        void findsById() {
            assertEquals("Mouse", service.getItem("A2").orElseThrow().getName());
        }

        @Test
        @DisplayName("unknown id returns empty Optional")
        void unknownIdIsEmpty() {
            assertTrue(service.getItem("ZZZ").isEmpty());
            assertTrue(service.getItem(null).isEmpty());
        }

        @Test
        @DisplayName("getAllItems keeps insertion order")
        void listsAllInOrder() {
            List<Item> all = service.getAllItems();
            assertEquals(List.of("A1", "A2", "A3"), all.stream().map(Item::getId).toList());
        }

        @Test
        @DisplayName("returned list is a copy - modifying it does not affect the inventory")
        void getAllReturnsCopy() {
            service.getAllItems().clear();
            assertEquals(3, service.size());
        }

        @Test
        @DisplayName("empty inventory returns empty list")
        void emptyInventory() {
            assertTrue(new InventoryService().getAllItems().isEmpty());
        }

        @Test
        @DisplayName("search is case-insensitive and supports partial match")
        void searchIgnoresCase() {
            assertEquals(1, service.searchByName("laptop").size());
            assertEquals(1, service.searchByName("KEYB").size());
            assertEquals(1, service.searchByName("  mouse  ").size());
        }

        @Test
        @DisplayName("search with blank/null/no-match returns empty list")
        void searchEdgeCases() {
            assertTrue(service.searchByName(null).isEmpty());
            assertTrue(service.searchByName("   ").isEmpty());
            assertTrue(service.searchByName("tablet").isEmpty());
        }

        @Test
        @DisplayName("low stock means strictly below threshold (boundary)")
        void lowStockBoundary() {
            // quantities: Laptop 10, Mouse 3, Keyboard 5
            List<Item> low = service.getLowStockItems(5);
            assertEquals(1, low.size());
            assertEquals("A2", low.get(0).getId());
        }

        @Test
        @DisplayName("total inventory value sums quantity x price")
        void totalValue() {
            // 10*50000 + 3*500 + 5*1500 = 509000
            assertEquals(509000.0, service.getTotalInventoryValue(), 0.0001);
        }

        @Test
        @DisplayName("total value of empty inventory is zero")
        void totalValueEmpty() {
            assertEquals(0.0, new InventoryService().getTotalInventoryValue(), 0.0001);
        }
    }

    // ------------------------------------------------------------ UPDATE
    @Nested
    @DisplayName("Update")
    class UpdateTests {

        @Test
        @DisplayName("updates quantity")
        void updatesQuantity() {
            service.updateQuantity("A1", 25);
            assertEquals(25, service.getItem("A1").get().getQuantity());
        }

        @Test
        @DisplayName("quantity can be set to zero (boundary)")
        void quantityZeroAllowed() {
            service.updateQuantity("A1", 0);
            assertEquals(0, service.getItem("A1").get().getQuantity());
        }

        @Test
        @DisplayName("negative quantity is rejected and old value is kept")
        void rejectsNegativeQuantity() {
            assertThrows(IllegalArgumentException.class, () -> service.updateQuantity("A1", -1));
            assertEquals(10, service.getItem("A1").get().getQuantity());
        }

        @Test
        @DisplayName("updates price and name")
        void updatesPriceAndName() {
            service.updatePrice("A2", 650.5);
            service.updateName("A2", "Wireless Mouse");
            Item mouse = service.getItem("A2").get();
            assertEquals(650.5, mouse.getPrice(), 0.0001);
            assertEquals("Wireless Mouse", mouse.getName());
        }

        @Test
        @DisplayName("invalid price / name are rejected")
        void rejectsInvalidPriceAndName() {
            assertThrows(IllegalArgumentException.class, () -> service.updatePrice("A2", -10));
            assertThrows(IllegalArgumentException.class, () -> service.updateName("A2", "  "));
        }

        @Test
        @DisplayName("updating an unknown id throws ItemNotFoundException")
        void updateUnknownId() {
            assertThrows(ItemNotFoundException.class, () -> service.updateQuantity("NOPE", 1));
            assertThrows(ItemNotFoundException.class, () -> service.updatePrice("NOPE", 1));
            assertThrows(ItemNotFoundException.class, () -> service.updateName("NOPE", "x"));
        }
    }

    // ------------------------------------------------------------ DELETE
    @Nested
    @DisplayName("Delete")
    class DeleteTests {

        @Test
        @DisplayName("deletes an existing item")
        void deletesItem() {
            service.deleteItem("A2");
            assertEquals(2, service.size());
            assertTrue(service.getItem("A2").isEmpty());
        }

        @Test
        @DisplayName("deleting an unknown id throws ItemNotFoundException")
        void deleteUnknownId() {
            assertThrows(ItemNotFoundException.class, () -> service.deleteItem("NOPE"));
            assertEquals(3, service.size());
        }

        @Test
        @DisplayName("deleting twice fails the second time")
        void deleteTwice() {
            service.deleteItem("A1");
            assertThrows(ItemNotFoundException.class, () -> service.deleteItem("A1"));
        }

        @Test
        @DisplayName("deleted id can be added again")
        void reAddAfterDelete() {
            service.deleteItem("A1");
            assertDoesNotThrow(() -> service.addItem(new Item("A1", "New Laptop", 1, 1)));
        }
    }
}
