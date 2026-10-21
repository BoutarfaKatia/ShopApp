package com.katia.shop.product.integration;

import com.katia.shop.product.entity.Product;
import com.katia.shop.product.repository.ProductRepository;
import com.katia.shop.product.service.ProductService;
import com.katia.shop.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@IntegrationTest

class ProductCacheIntegrationTest {

    @Autowired
    private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CacheManager cacheManager;

    @Test
    void findProductById_shouldBeCachedAndEvictedOnDelete() {
        Product p = new Product();
        p.setName("Cached " + UUID.randomUUID());
        p.setPrice(new BigDecimal("10.00"));
        p.setStock(5);
        Long id = productRepository.saveAndFlush(p).getId();

        productService.findProductById(id);
        System.out.println("Product ID: " + id);

        assertThat(cacheManager.getCache("products").get(id)).isNotNull();

        productService.deleteProduct(id);
        System.out.println("CacheManager: " + cacheManager.getClass().getName());
        System.out.println("Cache implementation: " +
                cacheManager.getCache("products").getClass().getName());
        assertThat(cacheManager.getCache("products").get(id)).isNull();
    }


}