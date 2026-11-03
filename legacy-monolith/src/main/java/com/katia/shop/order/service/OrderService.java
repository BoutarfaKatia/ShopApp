package com.katia.shop.order.service;

import com.katia.shop.order.exception.OrderNotFoundException;
import com.katia.shop.order.exception.InvalidOrderStateException;
import com.katia.shop.product.exception.ProductNotAvailableException;
import com.katia.shop.product.exception.ProductNotFoundException;
import com.katia.shop.user.exception.UserNotFoundException;
import com.katia.shop.order.mapper.OrderMapper;
import com.katia.shop.order.dto.OrderItemRequest;
import com.katia.shop.order.dto.OrderRequest;
import com.katia.shop.order.dto.OrderResponse;
import com.katia.shop.order.entity.OrderItem;
import com.katia.shop.order.entity.Order;
import com.katia.shop.order.entity.OrderStatus;
import com.katia.shop.order.repository.OrderRepository;
import com.katia.shop.product.entity.Product;
import com.katia.shop.product.repository.ProductRepository;
import com.katia.shop.user.entity.User;
import com.katia.shop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CacheManager cacheManager;

    @Transactional
    public OrderResponse createOrder(OrderRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));


        Map<Long, Integer> quantities = request.items().stream()
                .collect(Collectors.toMap(
                        OrderItemRequest::productId,
                        OrderItemRequest::quantity,
                        Integer::sum,
                        LinkedHashMap::new));

        Map<Long, Product> products = productRepository.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Order order = new Order();
        order.setUser(user);

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = products.get(entry.getKey());
            if (product == null) throw new ProductNotFoundException(entry.getKey());
            if (!product.isActive()) throw new ProductNotAvailableException(product.getId());

            int quantity = entry.getValue();
            product.decreaseStock(quantity);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            order.addItem(item);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        order.setTotal(total);

        Order saved = orderRepository.save(order);
        log.info("Order created. orderId={}, userId={}, total={}", saved.getId(), userId, total);
        evictProductsAfterCommit(quantities.keySet());
        return OrderMapper.toResponse(saved);
    }

    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return OrderMapper.toResponse(order);
    }

    public boolean isOwner(Long orderId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof User currentUser)) {
            return false;
        }
        return orderRepository.existsByIdAndUserId(orderId, currentUser.getId());
    }

    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(id, order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.getItems().forEach(i -> i.getProduct().increaseStock(i.getQuantity()));
        log.info("Order cancelled. orderId={}", id);
        evictProductsAfterCommit(order.getItems().stream().map(i -> i.getProduct().getId()).toList());
    }

    public Page<OrderResponse> getUserOrders(Long id, Pageable pageable) {

        return orderRepository.findByUserId(id, pageable)
                .map(OrderMapper::toResponse);
    }

    private void evictProductsAfterCommit(Collection<Long> productIds) {
        Runnable evict = () -> {
            Cache cache = cacheManager.getCache("products");
            if (cache != null) productIds.forEach(cache::evict);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { evict.run(); }
            });
        } else {
            evict.run();
        }
    }
}