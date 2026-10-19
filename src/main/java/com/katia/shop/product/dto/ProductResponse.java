package com.katia.shop.product.dto;
import java.math.BigDecimal;
import java.time.Instant;


public record ProductResponse(Long id, String name, String description,
                              BigDecimal price, int stock, Instant createdAt, boolean active) {}