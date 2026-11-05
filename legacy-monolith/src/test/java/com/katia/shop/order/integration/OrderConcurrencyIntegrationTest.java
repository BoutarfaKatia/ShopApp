package com.katia.shop.order.integration;

import com.katia.shop.order.dto.OrderItemRequest;
import com.katia.shop.order.dto.OrderRequest;
import com.katia.shop.order.service.OrderService;
import com.katia.shop.product.entity.Product;
import com.katia.shop.product.exception.InsufficientStockException;
import com.katia.shop.product.repository.ProductRepository;
import com.katia.shop.support.IntegrationTest;
import com.katia.shop.user.entity.Role;
import com.katia.shop.user.entity.User;
import com.katia.shop.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@IntegrationTest
class OrderConcurrencyIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void createOrder_shouldSellTheLastItemOnlyOnce() throws Exception {
        Product product = new Product();
        product.setName("Last item " + UUID.randomUUID());
        product.setPrice(new BigDecimal("10.00"));
        product.setStock(1);
        Long productId = productRepository.saveAndFlush(product).getId();

        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(productId, 1)));
        List<Long> buyers = List.of(createUser(), createUser());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier startTogether = new CyclicBarrier(2);
        List<Future<Boolean>> results = buyers.stream()
                .map(userId -> pool.submit(() -> {
                    startTogether.await();
                    try {
                        orderService.createOrder(request, userId);
                        return true;
                    } catch (ConcurrencyFailureException | InsufficientStockException e) {
                        return false;
                    }
                }))
                .toList();

        long successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get(15, TimeUnit.SECONDS)) successes++;
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(productRepository.findById(productId).orElseThrow().getStock()).isZero();
    }

    private Long createUser() {
        User user = new User();
        user.setEmail("buyer-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("unused");
        user.setFullName("Buyer");
        user.setRole(Role.USER);
        return userRepository.saveAndFlush(user).getId();
    }
}