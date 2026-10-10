package com.inventory.service;

import com.inventory.repository.StockRepository;

public class StockService {
    private final StockRepository repository;

    public StockService(StockRepository repository) {
        this.repository = repository;
    }

    public void addStock(int productId, int quantity) {
        validateQuantity(quantity);
        repository.increaseStock(productId, quantity);
    }

    public void reduceStock(int productId, int quantity) {
        validateQuantity(quantity);

        int available = repository.getQuantity(productId);
        if (available < quantity) {
            throw new IllegalStateException("Insufficient stock");
        }

        repository.decreaseStock(productId, quantity);
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
