package com.inventory.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventory.model.Item;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryItemRepository")
class InMemoryItemRepositoryTest {

    private InMemoryItemRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryItemRepository();
    }

    @Test
    @DisplayName("saved item can be found and exists")
    void savesAndFinds() {
        repository.save(new Item("A1", "Pen", 2, 10));
        assertTrue(repository.existsById("A1"));
        assertEquals("Pen", repository.findById("A1").orElseThrow().name());
        assertEquals(1, repository.count());
    }

    @Test
    @DisplayName("unknown id and null id are handled safely")
    void unknownIds() {
        assertFalse(repository.existsById("ZZZ"));
        assertTrue(repository.findById("ZZZ").isEmpty());
        assertTrue(repository.findById(null).isEmpty());
        assertFalse(repository.deleteById(null));
    }

    @Test
    @DisplayName("saving the same id replaces the item instead of adding a second one")
    void saveReplacesExisting() {
        repository.save(new Item("A1", "Pen", 2, 10));
        repository.save(new Item("A1", "Pen", 9, 10));
        assertEquals(1, repository.count());
        assertEquals(9, repository.findById("A1").orElseThrow().quantity());
    }

    @Test
    @DisplayName("deleteById returns true once, then false")
    void deleteReturnsBoolean() {
        repository.save(new Item("A1", "Pen", 2, 10));
        assertTrue(repository.deleteById("A1"));
        assertFalse(repository.deleteById("A1"));
        assertEquals(0, repository.count());
    }

    @Test
    @DisplayName("running total follows save, replace and delete")
    void totalValueTracksChanges() {
        repository.save(new Item("A", "Pen", 2, 10));    // 20
        repository.save(new Item("B", "Book", 3, 5));    // 15
        assertEquals(35.0, repository.totalValue(), 0.0001);

        repository.save(new Item("A", "Pen", 4, 10));    // replace: 40 + 15
        assertEquals(55.0, repository.totalValue(), 0.0001);

        repository.deleteById("B");                      // 40
        assertEquals(40.0, repository.totalValue(), 0.0001);

        repository.deleteById("A");                      // empty -> exactly 0
        assertEquals(0.0, repository.totalValue(), 0.0);
    }

    @Test
    @DisplayName("findAll keeps insertion order and is read-only")
    void findAllOrderAndReadOnly() {
        repository.save(new Item("C", "Third", 1, 1));
        repository.save(new Item("A", "First", 1, 1));
        List<Item> all = repository.findAll();
        assertEquals("C", all.get(0).id());
        assertEquals("A", all.get(1).id());
        assertThrows(UnsupportedOperationException.class, all::clear);
        assertEquals(2, repository.count());
    }

    @Test
    @DisplayName("stream exposes every stored item")
    void streamExposesItems() {
        repository.save(new Item("A", "Pen", 1, 1));
        repository.save(new Item("B", "Book", 1, 1));
        assertEquals(2, repository.stream().count());
    }

    @Test
    @DisplayName("saving null is rejected")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> repository.save(null));
    }
}
