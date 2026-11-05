package com.katia.shop.product.exception;

public class ProductAlreadyExistException extends RuntimeException {
    public ProductAlreadyExistException(String name) {
        super("Product with name " + name + " already exists");
    }
}
