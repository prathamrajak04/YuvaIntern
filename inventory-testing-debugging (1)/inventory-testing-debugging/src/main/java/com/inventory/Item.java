package com.inventory;

import java.util.Objects;

/** A single product tracked by the inventory system. */
public class Item {
    private final String id;
    private String name;
    private int quantity;
    private double price;

    public Item(String id, String name, int quantity, double price) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Item id must not be null or blank");
        }
        setName(name);
        setQuantity(quantity);
        setPrice(price);
        this.id = id.trim();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name must not be null or blank");
        }
        this.name = name.trim();
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must not be negative");
        }
        this.quantity = quantity;
    }

    public void setPrice(double price) {
        if (price < 0 || Double.isNaN(price) || Double.isInfinite(price)) {
            throw new IllegalArgumentException("Price must be a non-negative finite number");
        }
        this.price = price;
    }

    /** Total stock value of this item (quantity x price). */
    public double getTotalValue() {
        return quantity * price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        return id.equals(((Item) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Item{id='%s', name='%s', qty=%d, price=%.2f}", id, name, quantity, price);
    }
}
