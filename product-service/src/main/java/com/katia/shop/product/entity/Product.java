package com.katia.shop.product.entity;

import com.katia.shop.product.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;
    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false,length=2000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }
    public void decreaseStock(int quantity) {
        if (quantity > stock) {
            throw new InsufficientStockException(name, stock);
        }
        stock -= quantity;
    }
    public void increaseStock(int quantity) { stock += quantity; }


}
