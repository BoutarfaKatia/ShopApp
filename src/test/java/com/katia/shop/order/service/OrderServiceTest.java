
package com.katia.shop.order.service;

import com.katia.shop.order.entity.OrderItem;
import com.katia.shop.order.exception.InvalidOrderStateException;
import com.katia.shop.order.exception.OrderNotFoundException;
import com.katia.shop.product.exception.InsufficientStockException;
import com.katia.shop.product.exception.ProductNotAvailableException;
import com.katia.shop.product.exception.ProductNotFoundException;
import com.katia.shop.order.dto.OrderItemRequest;
import com.katia.shop.order.dto.OrderRequest;
import com.katia.shop.order.dto.OrderResponse;
import com.katia.shop.order.entity.Order;
import com.katia.shop.order.entity.OrderStatus;
import com.katia.shop.order.repository.OrderRepository;
import com.katia.shop.product.entity.Product;
import com.katia.shop.product.repository.ProductRepository;
import com.katia.shop.user.entity.User;
import com.katia.shop.user.exception.UserNotFoundException;
import com.katia.shop.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 1L;

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private OrderService orderService;

    @Test
    void createOrder_shouldCreateOrderAndDecreaseStock() {
        Product product = product(10L, "25.00", 10, true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new User()));
        when(productRepository.findAllById(anyIterable())).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });

        OrderResponse response = orderService.createOrder(request(item(10L, 2)), USER_ID);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.total()).isEqualByComparingTo("50.00");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.items()).hasSize(1);
        assertThat(product.getStock()).isEqualTo(8);
    }

    @Test
    void createOrder_shouldMergeDuplicateItemsIntoOne() {
        Product product = product(10L, "10.00", 10, true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new User()));
        when(productRepository.findAllById(anyIterable())).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createOrder(request(item(10L, 1), item(10L, 2)), USER_ID);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(3);
        assertThat(product.getStock()).isEqualTo(7);
    }

    @Test
    void createOrder_shouldThrowWhenStockIsInsufficient() {
        Product product = product(10L, "25.00", 10, true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new User()));
        when(productRepository.findAllById(anyIterable())).thenReturn(List.of(product));

        assertThrows(InsufficientStockException.class,
                () -> orderService.createOrder(request(item(10L, 11)), USER_ID));

        assertThat(product.getStock()).isEqualTo(10);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_shouldThrowWhenProductIsInactive() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new User()));
        when(productRepository.findAllById(anyIterable()))
                .thenReturn(List.of(product(10L, "25.00", 10, false)));

        assertThrows(ProductNotAvailableException.class,
                () -> orderService.createOrder(request(item(10L, 1)), USER_ID));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_shouldThrowWhenProductNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new User()));
        when(productRepository.findAllById(anyIterable())).thenReturn(List.of());

        assertThrows(ProductNotFoundException.class,
                () -> orderService.createOrder(request(item(10L, 1)), USER_ID));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> orderService.createOrder(request(item(10L, 1)), USER_ID));
        verifyNoInteractions(productRepository, orderRepository);
    }

    @Test
    void cancelOrder_shouldCancelPendingOrderAndRestoreStock() {
        Product product = product(10L, "25.00", 8, true);
        Order order = orderWith(OrderStatus.PENDING, product, 2);
        when(orderRepository.findWithItemsById(5L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(5L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStock()).isEqualTo(10);
    }

    @Test
    void cancelOrder_shouldThrowWhenOrderIsNotPending() {
        Product product = product(10L, "25.00", 8, true);
        Order order = orderWith(OrderStatus.CANCELLED, product, 2);
        when(orderRepository.findWithItemsById(5L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> orderService.cancelOrder(5L));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStock()).isEqualTo(8);
    }

    @Test
    void cancelOrder_shouldThrowWhenOrderNotFound() {
        when(orderRepository.findWithItemsById(5L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.cancelOrder(5L));
    }

    // ---------- Helping methods ----------

    private static Product product(Long id, String price, int stock, boolean active) {
        Product p = new Product();
        p.setId(id);
        p.setName("Product " + id);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        p.setActive(active);
        return p;
    }

    private static Order orderWith(OrderStatus status, Product product, int quantity) {
        Order order = new Order();
        order.setId(5L);
        order.setStatus(status);
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        order.addItem(item);
        return order;
    }

    private static OrderItemRequest item(Long productId, int quantity) {
        return new OrderItemRequest(productId, quantity);
    }

    private static OrderRequest request(OrderItemRequest... items) {
        return new OrderRequest(List.of(items));
    }
}

