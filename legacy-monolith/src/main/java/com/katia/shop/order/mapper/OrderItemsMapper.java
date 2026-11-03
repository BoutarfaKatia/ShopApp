package com.katia.shop.order.mapper;

import com.katia.shop.order.dto.OrderItemResponse;
import com.katia.shop.order.entity.OrderItem;

import java.util.List;

public class OrderItemsMapper {

    public static List<OrderItemResponse>  toResponse(List<OrderItem> orderItems) {
      List<OrderItemResponse> orderItemResponses =   orderItems.stream().map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();

      return orderItemResponses;
    }
}
