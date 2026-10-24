package com.inventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Core CRUD logic of the inventory system. */
public class InventoryService {
    private final Map<String, Item> items = new LinkedHashMap<>();

    // ---------- CREATE ----------
    public void addItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null");
        }
        // FIX 1: reject duplicate ids instead of silently overwriting
        if (items.containsKey(item.getId())) {
            throw new DuplicateItemException(item.getId());
        }
        items.put(item.getId(), item);
    }

    // ---------- READ ----------
    public Optional<Item> getItem(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public List<Item> getAllItems() {
        return new ArrayList<>(items.values());
    }

    public List<Item> searchByName(String keyword) {
        List<Item> result = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) {
            return result;
        }
        String needle = keyword.trim().toLowerCase();
        for (Item item : items.values()) {
            // FIX 2: compare in lower case so the search is case-insensitive
            if (item.getName().toLowerCase().contains(needle)) {
                result.add(item);
            }
        }
        return result;
    }

    public List<Item> getLowStockItems(int threshold) {
        List<Item> result = new ArrayList<>();
        for (Item item : items.values()) {
            // FIX 3: "below threshold" means strictly less than
            if (item.getQuantity() < threshold) {
                result.add(item);
            }
        }
        return result;
    }

    public double getTotalInventoryValue() {
        double total = 0;
        for (Item item : items.values()) {
            total += item.getTotalValue();
        }
        return total;
    }

    public int size() {
        return items.size();
    }

    // ---------- UPDATE ----------
    public void updateQuantity(String id, int newQuantity) {
        Item item = requireItem(id);
        // FIX 4: go through the validating setter (unchecked setter removed from Item)
        item.setQuantity(newQuantity);
    }

    public void updatePrice(String id, double newPrice) {
        requireItem(id).setPrice(newPrice);
    }

    public void updateName(String id, String newName) {
        requireItem(id).setName(newName);
    }

    // ---------- DELETE ----------
    public void deleteItem(String id) {
        // FIX 5: fail loudly when the id does not exist
        requireItem(id);
        items.remove(id);
    }

    private Item requireItem(String id) {
        Item item = items.get(id);
        if (item == null) {
            throw new ItemNotFoundException(id);
        }
        return item;
    }
}
