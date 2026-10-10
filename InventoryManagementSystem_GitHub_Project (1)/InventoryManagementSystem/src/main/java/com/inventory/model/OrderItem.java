package com.inventory.model;

public class OrderItem {
    private int productId;
    private int quantity;
    private double price;

    public OrderItem(int productId, int quantity, double price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public int getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }

    public double getSubtotal() {
        return quantity * price;
    }
}
