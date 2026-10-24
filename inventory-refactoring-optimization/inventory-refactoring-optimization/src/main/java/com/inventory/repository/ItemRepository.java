package com.inventory.repository;

import com.inventory.model.Item;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Storage abstraction (Repository pattern).
 *
 * REFACTORING (modularization / design pattern): in Week 3 the service owned a HashMap directly,
 * so storage and business rules were tangled together. The service now depends only on this
 * interface, so storage can be swapped (file, database, test double) without touching business
 * logic - the Dependency Inversion principle.
 */
public interface ItemRepository {

    boolean existsById(String id);

    Optional<Item> findById(String id);

    /** Read-only snapshot of all items in insertion order. */
    List<Item> findAll();

    /** Lazy read-only view for filtering without copying. Do not modify the repository while iterating. */
    Stream<Item> stream();

    /** Inserts a new item or replaces the existing one with the same id. */
    void save(Item item);

    /** @return true if an item was removed, false if the id did not exist */
    boolean deleteById(String id);

    int count();

    /** Sum of quantity x price over all items, available in O(1). */
    double totalValue();
}
