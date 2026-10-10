package com.inventory.model;

public class Product {
    private int productId;
    private String name;
    private String category;
    private double price;
    private int quantity;
    private int reorderLevel;

    public Product(int productId, String name, String category,
                   double price, int quantity, int reorderLevel) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
    }

    public int getProductId() { return productId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public int getReorderLevel() { return reorderLevel; }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return productId + " | " + name + " | " + category +
               " | ₹" + price + " | Stock: " + quantity;
    }
}
