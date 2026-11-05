package com.katia.shop.order.dto;

import com.katia.shop.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, BigDecimal total, Instant createdAt,
                            OrderStatus status, List<OrderItemResponse> items) {}
