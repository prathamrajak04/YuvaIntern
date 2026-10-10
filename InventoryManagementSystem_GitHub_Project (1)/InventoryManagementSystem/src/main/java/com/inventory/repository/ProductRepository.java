package com.inventory.repository;

import com.inventory.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(int id);
    List<Product> findAll();
    List<Product> findByName(String name);
    void delete(int id);
}
