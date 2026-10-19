package com.katia.shop.order.mapper;

import com.katia.shop.order.dto.OrderResponse;
import com.katia.shop.order.entity.Order;

public class OrderMapper {

    public static OrderResponse toResponse(Order order) {
        OrderResponse orderResponse = new OrderResponse(order.getId(), order.getTotal(),order.getCreatedAt(),order.getStatus(), OrderItemsMapper.toResponse(order.getItems()));

   return orderResponse;
    }
}
