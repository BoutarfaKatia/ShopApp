package com.katia.shop.product.mapper;

import com.katia.shop.product.dto.ProductRequest;
import com.katia.shop.product.dto.ProductResponse;
import com.katia.shop.product.entity.Product;

public class ProductMapper {

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStock(), product.getCreatedAt(), product.isActive());
    }
    public static Product fromRequest(ProductRequest productRequest) {
        Product product = new Product();
        updateEntity(product, productRequest);
        return product;
    }

    public static Product updateEntity(Product product, ProductRequest productRequest) {
        product.setName(productRequest.name().trim());
        product.setDescription(productRequest.description());
        product.setPrice(productRequest.price());
        product.setStock(productRequest.stock());
        return product;
    }
}
