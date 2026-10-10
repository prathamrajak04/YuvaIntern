package com.inventory.repository;

public interface StockRepository {
    void increaseStock(int productId, int quantity);
    void decreaseStock(int productId, int quantity);
    int getQuantity(int productId);
}
