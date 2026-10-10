package com.inventory.repository;

import com.inventory.model.Order;

public interface OrderRepository {
    Order save(Order order);
}
