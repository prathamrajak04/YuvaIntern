package com.inventory.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventory.model.Item;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CsvCodec")
class CsvCodecTest {

    @Test
    @DisplayName("encodes a simple item as one line")
    void encodesSimpleItem() {
        assertEquals("A1,Laptop,10,50000.0", CsvCodec.encode(new Item("A1", "Laptop", 10, 50000)));
    }

    @Test
    @DisplayName("round trip keeps every field")
    void roundTrip() {
        Item original = new Item("A1", "Laptop", 10, 49999.99);
        Item copy = CsvCodec.decode(CsvCodec.encode(original));
        assertEquals(original.id(), copy.id());
        assertEquals(original.name(), copy.name());
        assertEquals(original.quantity(), copy.quantity());
        assertEquals(original.price(), copy.price(), 0.0);
    }

    @Test
    @DisplayName("a comma in the name is quoted and survives a round trip")
    void commaInName() {
        Item item = new Item("A2", "Cable, USB-C", 25, 199.5);
        String line = CsvCodec.encode(item);
        assertEquals("A2,\"Cable, USB-C\",25,199.5", line);
        assertEquals("Cable, USB-C", CsvCodec.decode(line).name());
    }

    @Test
    @DisplayName("a double quote in the name is doubled and survives a round trip")
    void quoteInName() {
        Item item = new Item("A3", "15\" Monitor", 4, 8999);
        String line = CsvCodec.encode(item);
        assertEquals("A3,\"15\"\" Monitor\",4,8999.0", line);
        assertEquals("15\" Monitor", CsvCodec.decode(line).name());
    }

    @Test
    @DisplayName("name with both comma and quote survives a round trip")
    void commaAndQuote() {
        Item item = new Item("A4", "Rack, 19\" wide", 1, 10);
        assertEquals("Rack, 19\" wide", CsvCodec.decode(CsvCodec.encode(item)).name());
    }

    @Test
    @DisplayName("very small and very large prices round trip exactly")
    void extremePrices() {
        for (double price : new double[] {0.0, 0.0005, 1e-7, 123456789.123, 1e15}) {
            Item copy = CsvCodec.decode(CsvCodec.encode(new Item("P", "Thing", 1, price)));
            assertEquals(price, copy.price(), 0.0);
        }
    }

    @Test
    @DisplayName("wrong number of fields is rejected with a clear message")
    void wrongFieldCount() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CsvCodec.decode("A1,Laptop,10"));
        assertTrue(ex.getMessage().contains("Expected 4 fields but found 3"));
        assertThrows(IllegalArgumentException.class, () -> CsvCodec.decode("A1,Laptop,10,5,extra"));
    }

    @Test
    @DisplayName("non-numeric quantity or price is rejected")
    void badNumbers() {
        assertThrows(IllegalArgumentException.class, () -> CsvCodec.decode("A1,Laptop,ten,5"));
        assertThrows(IllegalArgumentException.class, () -> CsvCodec.decode("A1,Laptop,10,cheap"));
    }

    @Test
    @DisplayName("values that break the model rules are rejected (negative quantity, blank name)")
    void modelRulesStillApply() {
        assertThrows(IllegalArgumentException.class, () -> CsvCodec.decode("A1,Laptop,-1,5"));
        assertThrows(IllegalArgumentException.class, () -> CsvCodec.decode("A1,,1,5"));
        assertThrows(IllegalArgumentException.class, () -> CsvCodec.decode("A1,Laptop,1,NaN"));
    }

    @Test
    @DisplayName("unterminated quote is rejected")
    void unterminatedQuote() {
        assertThrows(IllegalArgumentException.class, () -> CsvCodec.decode("A1,\"Laptop,10,5"));
    }

    @Test
    @DisplayName("split keeps empty fields")
    void splitKeepsEmptyFields() {
        assertEquals(List.of("a", "", "c"), CsvCodec.split("a,,c"));
        assertEquals(List.of(""), CsvCodec.split(""));
    }
}
