package com.inventory;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Item model validation")
class ItemTest {

    @Test
    @DisplayName("valid item is created with trimmed id and name")
    void createsValidItem() {
        Item item = new Item(" A1 ", "  Laptop ", 5, 999.99);
        assertEquals("A1", item.getId());
        assertEquals("Laptop", item.getName());
        assertEquals(5, item.getQuantity());
        assertEquals(999.99, item.getPrice(), 0.0001);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("blank or null id is rejected")
    void rejectsBlankId(String badId) {
        assertThrows(IllegalArgumentException.class, () -> new Item(badId, "Pen", 1, 1.0));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("blank or null name is rejected")
    void rejectsBlankName(String badName) {
        assertThrows(IllegalArgumentException.class, () -> new Item("A1", badName, 1, 1.0));
    }

    @Test
    @DisplayName("negative quantity is rejected")
    void rejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new Item("A1", "Pen", -1, 1.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("negative, NaN and infinite prices are rejected")
    void rejectsInvalidPrice(double badPrice) {
        assertThrows(IllegalArgumentException.class, () -> new Item("A1", "Pen", 1, badPrice));
    }

    @Test
    @DisplayName("zero quantity and zero price are allowed (boundary)")
    void allowsZeroBoundary() {
        Item item = new Item("A1", "Freebie", 0, 0.0);
        assertEquals(0, item.getTotalValue(), 0.0001);
    }

    @Test
    @DisplayName("total value = quantity x price")
    void computesTotalValue() {
        assertEquals(30.0, new Item("A1", "Pen", 3, 10.0).getTotalValue(), 0.0001);
    }

    @Test
    @DisplayName("setQuantity rejects negative values")
    void setQuantityRejectsNegative() {
        Item item = new Item("A1", "Pen", 1, 1.0);
        assertThrows(IllegalArgumentException.class, () -> item.setQuantity(-5));
        assertEquals(1, item.getQuantity());
    }

    @Test
    @DisplayName("items with the same id are equal")
    void equalityIsById() {
        assertEquals(new Item("A1", "Pen", 1, 1.0), new Item("A1", "Other", 9, 9.0));
        assertNotEquals(new Item("A1", "Pen", 1, 1.0), new Item("A2", "Pen", 1, 1.0));
    }
}
