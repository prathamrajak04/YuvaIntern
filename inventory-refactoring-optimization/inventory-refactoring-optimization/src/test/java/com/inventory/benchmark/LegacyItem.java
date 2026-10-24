package com.inventory.benchmark;

/**
 * BEFORE: the Week 3 Item, kept unchanged (apart from its name) as the baseline for benchmarking.
 * It is mutable, validates in every setter, and does not cache anything.
 */
public class LegacyItem {
    private final String id;
    private String name;
    private int quantity;
    private double price;

    public LegacyItem(String id, String name, int quantity, double price) {
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

    public double getTotalValue() {
        return quantity * price;
    }
}
