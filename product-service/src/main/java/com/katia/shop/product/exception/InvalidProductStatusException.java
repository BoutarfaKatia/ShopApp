package com.katia.shop.product.exception;

public class InvalidProductStatusException extends RuntimeException {
    public InvalidProductStatusException(Long id) {
        super("Invalid product status " + id);
    }
}
