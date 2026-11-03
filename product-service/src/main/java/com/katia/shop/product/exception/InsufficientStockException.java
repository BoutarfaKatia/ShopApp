package com.katia.shop.product.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productName, Integer stock) {
        super("Insufficient stock for product '" + productName + "': only " + stock + " left");
    }
}
