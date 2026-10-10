package com.inventory.service;

import com.inventory.model.Order;
import com.inventory.model.OrderItem;
import com.inventory.repository.OrderRepository;
import com.inventory.repository.StockRepository;

public class OrderService {
    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;

    public OrderService(OrderRepository orderRepository,
                        StockRepository stockRepository) {
        this.orderRepository = orderRepository;
        this.stockRepository = stockRepository;
    }

    public Order createOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            int available = stockRepository.getQuantity(item.getProductId());

            if (available < item.getQuantity()) {
                throw new IllegalStateException(
                    "Insufficient stock for product " + item.getProductId()
                );
            }
        }

        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            stockRepository.decreaseStock(
                item.getProductId(), item.getQuantity()
            );
        }

        return savedOrder;
    }
}
