package com.inventory.repository;

import com.inventory.model.Item;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * HashMap-backed repository. LinkedHashMap keeps insertion order and gives O(1) lookups.
 *
 * OPTIMIZATION: the Week 3 service recomputed the total inventory value with a full loop over
 * all items on every call (O(n)). This class keeps a running total that is adjusted by the
 * difference on each save/delete, so reading it is O(1).
 * This is safe only because Item is immutable - an item cannot change value without passing
 * through save().
 */
public class InMemoryItemRepository implements ItemRepository {

    private final Map<String, Item> items = new LinkedHashMap<>();
    private double totalValue = 0.0;

    @Override
    public boolean existsById(String id) {
        return items.containsKey(id);
    }

    @Override
    public Optional<Item> findById(String id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<Item> findAll() {
        return List.copyOf(items.values());
    }

    @Override
    public Stream<Item> stream() {
        return items.values().stream();
    }

    @Override
    public void save(Item item) {
        Objects.requireNonNull(item, "item");
        Item previous = items.put(item.id(), item);
        double previousValue = (previous == null) ? 0.0 : previous.totalValue();
        totalValue += item.totalValue() - previousValue;
    }

    @Override
    public boolean deleteById(String id) {
        Item removed = items.remove(id);
        if (removed == null) {
            return false;
        }
        totalValue -= removed.totalValue();
        if (items.isEmpty()) {
            totalValue = 0.0; // drop any floating-point drift once the inventory is empty
        }
        return true;
    }

    @Override
    public int count() {
        return items.size();
    }

    @Override
    public double totalValue() {
        return totalValue;
    }
}
