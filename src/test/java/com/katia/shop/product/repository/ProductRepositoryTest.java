package com.katia.shop.product.repository;

import com.katia.shop.product.entity.Product;
import com.katia.shop.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void save_shouldPersistProductWithGeneratedFields() {
        Product saved = productRepository.saveAndFlush(product("Keyboard", "Mechanical keyboard", true));

        Product found = productRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(found.getId());
        assertNotNull(found.getCreatedAt());                       // @PrePersist
        assertEquals(0, new BigDecimal("2500.00").compareTo(found.getPrice()));
        assertEquals(0L, found.getVersion());                      // @Version
    }

    @Test
    void existsByName_shouldReflectExistence() {
        productRepository.saveAndFlush(product("Keyboard", "Mechanical keyboard", true));

        assertTrue(productRepository.existsByName("Keyboard"));
        assertFalse(productRepository.existsByName("Unknown"));
    }

    @Test
    void existsByNameAndIdNot_shouldIgnoreTheProductItself() {
        Product saved = productRepository.saveAndFlush(product("Keyboard", "Mechanical keyboard", true));

        assertFalse(productRepository.existsByNameAndIdNot("Keyboard", saved.getId()));
        assertTrue(productRepository.existsByNameAndIdNot("Keyboard", saved.getId() + 1));
    }

    @Test
    void search_shouldReturnOnlyActiveMatchingProductsInPages() {
        productRepository.saveAllAndFlush(List.of(
                product("Keyboard", "Mechanical keyboard", true),
                product("Mouse", "Keyboard device", true),
                product("Monitor", "Computer screen", true),
                product("Old Keyboard", "Discontinued", false)));

        Page<Product> page = productRepository.search("keyboard", PageRequest.of(0, 1));

        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void save_shouldFailOnDuplicateName() {
        productRepository.saveAndFlush(product("Keyboard", "Mechanical keyboard", true));

        assertThrows(DataIntegrityViolationException.class,
                () -> productRepository.saveAndFlush(product("Keyboard", "Another", true)));
    }

    @Test
    void save_shouldFailOnNegativeStock() {
        Product invalid = product("Broken", "Negative stock", true);
        invalid.setStock(-1);

        assertThrows(DataIntegrityViolationException.class, () -> productRepository.saveAndFlush(invalid));
    }

    private static Product product(String name, String description, boolean active) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setPrice(new BigDecimal("2500.00"));
        p.setStock(10);
        p.setActive(active);
        return p;
    }
}