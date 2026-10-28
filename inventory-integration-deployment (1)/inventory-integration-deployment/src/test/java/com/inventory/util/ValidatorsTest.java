package com.inventory.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Validators")
class ValidatorsTest {

    @Test
    @DisplayName("requireNonBlank returns the trimmed value")
    void nonBlankTrims() {
        assertEquals("abc", Validators.requireNonBlank("  abc ", "Field"));
    }

    @Test
    @DisplayName("requireNonBlank rejects null/blank and names the field in the message")
    void nonBlankRejectsAndNamesField() {
        for (String bad : new String[] {null, "", "   "}) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Validators.requireNonBlank(bad, "Item name"));
            assertTrue(ex.getMessage().contains("Item name"));
        }
    }

    @Test
    @DisplayName("requireNonNegative accepts zero and rejects -1 (boundary)")
    void nonNegativeBoundary() {
        assertEquals(0, Validators.requireNonNegative(0, "Quantity"));
        assertEquals(5, Validators.requireNonNegative(5, "Quantity"));
        assertThrows(IllegalArgumentException.class, () -> Validators.requireNonNegative(-1, "Quantity"));
    }

    @Test
    @DisplayName("requireValidPrice accepts zero and normal prices, rejects bad ones")
    void priceRules() {
        assertEquals(0.0, Validators.requireValidPrice(0.0), 0.0);
        assertEquals(12.5, Validators.requireValidPrice(12.5), 0.0);
        double[] bad = {-0.01, Double.NaN, Double.POSITIVE_INFINITY};
        for (double price : bad) {
            assertThrows(IllegalArgumentException.class, () -> Validators.requireValidPrice(price));
        }
    }

    @Test
    @DisplayName("requireNonBlank rejects line breaks inside the value (would corrupt the CSV file)")
    void rejectsLineBreaks() {
        for (String bad : new String[] {"a\nb", "a\rb", "a\r\nb"}) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Validators.requireNonBlank(bad, "Item name"));
            assertTrue(ex.getMessage().contains("line breaks"));
        }
    }

    @Test
    @DisplayName("a trailing line break is trimmed away and therefore allowed")
    void trailingLineBreakIsTrimmed() {
        assertEquals("abc", Validators.requireNonBlank("abc\n", "Item name"));
    }
}
