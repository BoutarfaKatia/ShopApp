package com.katia.shop.order.repository;

import com.katia.shop.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);
    boolean existsByIdAndUserId(Long id, Long userId);


    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findWithItemsById(Long id);
}
