package com.inventory.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Item (immutable model)")
class ItemTest {

    @Test
    @DisplayName("valid item is created with trimmed id and name")
    void createsValidItemWithTrimmedFields() {
        Item item = new Item(" A1 ", "  Laptop ", 5, 999.99);
        assertEquals("A1", item.id());
        assertEquals("Laptop", item.name());
        assertEquals(5, item.quantity());
        assertEquals(999.99, item.price(), 0.0001);
    }

    @Test
    @DisplayName("blank or null id is rejected")
    void rejectsBlankOrNullId() {
        for (String badId : new String[] {null, "", "   ", "\t"}) {
            assertThrows(IllegalArgumentException.class, () -> new Item(badId, "Pen", 1, 1.0));
        }
    }

    @Test
    @DisplayName("blank or null name is rejected")
    void rejectsBlankOrNullName() {
        for (String badName : new String[] {null, "", "  "}) {
            assertThrows(IllegalArgumentException.class, () -> new Item("A1", badName, 1, 1.0));
        }
    }

    @Test
    @DisplayName("negative quantity is rejected")
    void rejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new Item("A1", "Pen", -1, 1.0));
    }

    @Test
    @DisplayName("negative, NaN and infinite prices are rejected")
    void rejectsInvalidPrice() {
        double[] badPrices = {-0.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double badPrice : badPrices) {
            assertThrows(IllegalArgumentException.class, () -> new Item("A1", "Pen", 1, badPrice));
        }
    }

    @Test
    @DisplayName("zero quantity and zero price are allowed (boundary)")
    void allowsZeroBoundary() {
        Item item = new Item("A1", "Freebie", 0, 0.0);
        assertEquals(0, item.totalValue(), 0.0001);
    }

    @Test
    @DisplayName("total value = quantity x price")
    void computesTotalValue() {
        assertEquals(30.0, new Item("A1", "Pen", 3, 10.0).totalValue(), 0.0001);
    }

    @Test
    @DisplayName("withQuantity returns a new item and leaves the original unchanged")
    void withQuantityDoesNotMutateOriginal() {
        Item original = new Item("A1", "Pen", 1, 2.0);
        Item updated = original.withQuantity(7);
        assertNotSame(original, updated);
        assertEquals(1, original.quantity());
        assertEquals(7, updated.quantity());
        assertEquals("A1", updated.id());
    }

    @Test
    @DisplayName("withXxx methods keep the other fields and still validate")
    void withMethodsValidateAndKeepOtherFields() {
        Item original = new Item("A1", "Pen", 4, 2.5);
        Item renamed = original.withName("Gel Pen");
        Item repriced = original.withPrice(3.0);
        assertEquals("Gel Pen", renamed.name());
        assertEquals(4, renamed.quantity());
        assertEquals(3.0, repriced.price(), 0.0001);
        assertEquals("Pen", repriced.name());
        assertThrows(IllegalArgumentException.class, () -> original.withQuantity(-1));
        assertThrows(IllegalArgumentException.class, () -> original.withPrice(-1));
        assertThrows(IllegalArgumentException.class, () -> original.withName(" "));
    }

    @Test
    @DisplayName("cached lower-case name follows the name")
    void cachesLowerCaseName() {
        Item item = new Item("A1", "  Gaming LAPTOP ", 1, 1.0);
        assertEquals("gaming laptop", item.nameLower());
        assertEquals("wireless mouse", item.withName("Wireless Mouse").nameLower());
    }

    @Test
    @DisplayName("items with the same id are equal")
    void equalityIsById() {
        assertEquals(new Item("A1", "Pen", 1, 1.0), new Item("A1", "Other", 9, 9.0));
        assertEquals(new Item("A1", "Pen", 1, 1.0).hashCode(), new Item("A1", "Other", 9, 9.0).hashCode());
        assertNotEquals(new Item("A1", "Pen", 1, 1.0), new Item("A2", "Pen", 1, 1.0));
    }

    @Test
    @DisplayName("id or name containing a line break is rejected")
    void rejectsLineBreaksInIdAndName() {
        assertThrows(IllegalArgumentException.class, () -> new Item("A\n1", "Pen", 1, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new Item("A1", "Pen\nCap", 1, 1.0));
    }
}
