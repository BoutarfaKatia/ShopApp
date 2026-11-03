package com.katia.shop.order.controller;

import com.katia.shop.order.dto.OrderRequest;
import com.katia.shop.order.dto.OrderResponse;
import com.katia.shop.order.service.OrderService;
import com.katia.shop.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('ORDER_WRITE')")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderRequest orderRequest, @AuthenticationPrincipal User user ) {
        OrderResponse orderResponse = orderService.createOrder(orderRequest, user.getId());
        return ResponseEntity.created(URI.create("/api/orders/" + orderResponse.id())).body(orderResponse);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_READ')")
    public Page<OrderResponse> getMyOrders(
            @AuthenticationPrincipal User user,
            Pageable pageable) {
       return orderService.getUserOrders(user.getId(), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @orderService.isOwner(#id,authentication) ")
    public OrderResponse getOrder(@PathVariable Long id) {
      return  orderService.getOrder(id);
    }

@PatchMapping("/{id}/cancel")
 @PreAuthorize("@orderService.isOwner(#id,authentication)")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
}



}
