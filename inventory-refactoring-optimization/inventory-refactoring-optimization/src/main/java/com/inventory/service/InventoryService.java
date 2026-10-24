package com.inventory.service;

import com.inventory.exception.DuplicateItemException;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.model.Item;
import com.inventory.repository.InMemoryItemRepository;
import com.inventory.repository.ItemRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Business logic of the inventory system (CRUD + reports).
 *
 * REFACTORING SUMMARY
 * - Storage moved out into {@link ItemRepository}; this class only holds business rules.
 * - The three update methods used to repeat "look up item, throw if missing, mutate" three times.
 *   They now share one private {@link #update} helper (DRY).
 * - Loops that built result lists by hand are replaced with stream filters (readability).
 * - Search compares against the pre-computed Item.nameLower() instead of lower-casing every name on
 *   every call, and the total value is read from the repository's running total instead of looping.
 */
public class InventoryService {

    private final ItemRepository repository;

    public InventoryService() {
        this(new InMemoryItemRepository());
    }

    /** Constructor injection: lets tests or other storage back-ends supply their own repository. */
    public InventoryService(ItemRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    // ---------- CREATE ----------
    public void addItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null");
        }
        if (repository.existsById(item.id())) {
            throw new DuplicateItemException(item.id());
        }
        repository.save(item);
    }

    // ---------- READ ----------
    public Optional<Item> getItem(String id) {
        return repository.findById(id);
    }

    public List<Item> getAllItems() {
        return repository.findAll();
    }

    /** Case-insensitive partial match on the item name. Blank keyword returns an empty list. */
    public List<Item> searchByName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String needle = keyword.trim().toLowerCase(Locale.ROOT);
        return repository.stream()
                .filter(item -> item.nameLower().contains(needle))
                .toList();
    }

    /** Items whose quantity is strictly below the threshold. */
    public List<Item> getLowStockItems(int threshold) {
        return repository.stream()
                .filter(item -> item.quantity() < threshold)
                .toList();
    }

    /** O(1): read from the repository's running total. */
    public double getTotalInventoryValue() {
        return repository.totalValue();
    }

    public int size() {
        return repository.count();
    }

    // ---------- UPDATE ----------
    public void updateQuantity(String id, int newQuantity) {
        update(id, item -> item.withQuantity(newQuantity));
    }

    public void updatePrice(String id, double newPrice) {
        update(id, item -> item.withPrice(newPrice));
    }

    public void updateName(String id, String newName) {
        update(id, item -> item.withName(newName));
    }

    // ---------- DELETE ----------
    public void deleteItem(String id) {
        if (!repository.deleteById(id)) {
            throw new ItemNotFoundException(id);
        }
    }

    /**
     * The single place that implements "find, change, save". If the change is invalid the
     * withXxx() call throws before save(), so the stored item is never left half-updated.
     */
    private void update(String id, UnaryOperator<Item> change) {
        Item current = repository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
        repository.save(change.apply(current));
    }
}
