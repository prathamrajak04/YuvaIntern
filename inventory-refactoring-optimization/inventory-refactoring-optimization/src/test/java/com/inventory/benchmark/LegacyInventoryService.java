package com.inventory.benchmark;

import com.inventory.exception.DuplicateItemException;
import com.inventory.exception.ItemNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * BEFORE: the Week 3 service (after its bug fixes), kept as the baseline for benchmarking.
 * Note the problems the refactoring removes:
 *  - storage and business rules are mixed in one class
 *  - "find item or throw" is repeated in every update method
 *  - searchByName lower-cases every item name on every call
 *  - getTotalInventoryValue loops over all items on every call (O(n))
 */
public class LegacyInventoryService {
    private final Map<String, LegacyItem> items = new LinkedHashMap<>();

    public void addItem(LegacyItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null");
        }
        if (items.containsKey(item.getId())) {
            throw new DuplicateItemException(item.getId());
        }
        items.put(item.getId(), item);
    }

    public Optional<LegacyItem> getItem(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public List<LegacyItem> searchByName(String keyword) {
        List<LegacyItem> result = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) {
            return result;
        }
        String needle = keyword.trim().toLowerCase();
        for (LegacyItem item : items.values()) {
            if (item.getName().toLowerCase().contains(needle)) { // lower-cased again on every call
                result.add(item);
            }
        }
        return result;
    }

    public List<LegacyItem> getLowStockItems(int threshold) {
        List<LegacyItem> result = new ArrayList<>();
        for (LegacyItem item : items.values()) {
            if (item.getQuantity() < threshold) {
                result.add(item);
            }
        }
        return result;
    }

    public double getTotalInventoryValue() {
        double total = 0;
        for (LegacyItem item : items.values()) { // full O(n) loop on every call
            total += item.getTotalValue();
        }
        return total;
    }

    public void updateQuantity(String id, int newQuantity) {
        requireItem(id).setQuantity(newQuantity);
    }

    public void updatePrice(String id, double newPrice) {
        requireItem(id).setPrice(newPrice);
    }

    public void updateName(String id, String newName) {
        requireItem(id).setName(newName);
    }

    public void deleteItem(String id) {
        requireItem(id);
        items.remove(id);
    }

    private LegacyItem requireItem(String id) {
        LegacyItem item = items.get(id);
        if (item == null) {
            throw new ItemNotFoundException(id);
        }
        return item;
    }
}
