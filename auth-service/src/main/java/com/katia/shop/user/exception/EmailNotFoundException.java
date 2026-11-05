package com.katia.shop.user.exception;

public class EmailNotFoundException extends RuntimeException {
    public EmailNotFoundException(String email) {
        super("invalid email or password");
    }
}
