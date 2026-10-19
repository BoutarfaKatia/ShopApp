package com.katia.shop.product.exception;

public class ProductNotAvailableException extends RuntimeException {
    public ProductNotAvailableException(Long id) {
        super("Product " + id + " is not available");
    }
}
