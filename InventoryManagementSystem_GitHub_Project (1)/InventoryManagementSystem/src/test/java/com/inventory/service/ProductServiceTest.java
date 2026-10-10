package com.inventory.service;

import com.inventory.model.Product;
import com.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductServiceTest {

    @Test
    void shouldRejectNegativePrice() {
        ProductRepository repository = new FakeProductRepository();
        ProductService service = new ProductService(repository);

        Product product = new Product(
            1, "Keyboard", "Electronics", -100, 10, 2
        );

        assertThrows(IllegalArgumentException.class,
                     () -> service.addProduct(product));
    }

    static class FakeProductRepository implements ProductRepository {
        public Product save(Product p) { return p; }
        public java.util.Optional<Product> findById(int id) {
            return java.util.Optional.empty();
        }
        public java.util.List<Product> findAll() {
            return java.util.List.of();
        }
        public java.util.List<Product> findByName(String name) {
            return java.util.List.of();
        }
        public void delete(int id) {}
    }
}
