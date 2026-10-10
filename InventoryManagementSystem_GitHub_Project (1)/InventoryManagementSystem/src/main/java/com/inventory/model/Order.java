package com.inventory.model;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private int orderId;
    private List<OrderItem> items = new ArrayList<>();

    public Order(int orderId) {
        this.orderId = orderId;
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public int getOrderId() { return orderId; }
    public List<OrderItem> getItems() { return items; }

    public double getTotal() {
        return items.stream()
                .mapToDouble(OrderItem::getSubtotal)
                .sum();
    }
}
