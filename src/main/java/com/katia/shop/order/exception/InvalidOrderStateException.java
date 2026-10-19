package com.katia.shop.order.exception;

import com.katia.shop.order.entity.OrderStatus;

public class InvalidOrderStateException extends RuntimeException {
    public InvalidOrderStateException(Long id, OrderStatus status) {
        super("Order " + id + " is " + status + " and cannot be cancelled");
    }
}
