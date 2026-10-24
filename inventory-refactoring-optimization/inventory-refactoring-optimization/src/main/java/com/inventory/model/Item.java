package com.inventory.model;

import com.inventory.util.Validators;
import java.util.Locale;
import java.util.Objects;

/**
 * An immutable product in the inventory.
 *
 * REFACTORING NOTES
 * 1. Immutable: the Week 3 Item had public setters, so any caller holding a reference could change
 *    quantity or price behind the service's back. Now every change goes through withXxx(), which
 *    returns a NEW validated Item. This also makes the service's cached total value safe.
 * 2. DRY: validation is delegated to {@link Validators} instead of being repeated in each setter.
 * 3. Performance: the lower-case form of the name is computed once here, not on every search.
 */
public final class Item {
    private final String id;
    private final String name;
    private final String nameLower; // cached for fast case-insensitive search
    private final int quantity;
    private final double price;

    public Item(String id, String name, int quantity, double price) {
        this.id = Validators.requireNonBlank(id, "Item id");
        this.name = Validators.requireNonBlank(name, "Item name");
        this.nameLower = this.name.toLowerCase(Locale.ROOT); // ROOT: same result in every locale
        this.quantity = Validators.requireNonNegative(quantity, "Quantity");
        this.price = Validators.requireValidPrice(price);
    }

    public String id() { return id; }
    public String name() { return name; }
    public String nameLower() { return nameLower; }
    public int quantity() { return quantity; }
    public double price() { return price; }

    /** Total stock value of this item (quantity x price). */
    public double totalValue() {
        return quantity * price;
    }

    public Item withName(String newName) {
        return new Item(id, newName, quantity, price);
    }

    public Item withQuantity(int newQuantity) {
        return new Item(id, name, newQuantity, price);
    }

    public Item withPrice(double newPrice) {
        return new Item(id, name, quantity, newPrice);
    }

    /** Two items are the same product when their ids match. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        return id.equals(((Item) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Item{id='%s', name='%s', qty=%d, price=%.2f}", id, name, quantity, price);
    }
}
