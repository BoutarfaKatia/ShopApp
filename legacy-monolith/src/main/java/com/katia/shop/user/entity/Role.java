package com.katia.shop.user.entity;

import lombok.Getter;

import java.util.Set;
@Getter
public enum Role {
    USER(
            Set.of(Permission.ORDER_READ,
                    Permission.ORDER_WRITE,
                    Permission.ORDER_DELETE,
                    Permission.PRODUCT_READ)
    ),
    ADMIN(Set.of(Permission.PRODUCT_READ,
            Permission.PRODUCT_WRITE,
            Permission.PRODUCT_DELETE,Permission.PRODUCT_EDIT,
            Permission.ORDER_WRITE,Permission.ORDER_DELETE,Permission.ORDER_READ));



    private final Set<Permission> permissions;
    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
