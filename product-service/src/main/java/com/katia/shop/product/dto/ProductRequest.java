package com.katia.shop.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;


public record ProductRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal price,
        @NotNull @PositiveOrZero Integer stock) {}