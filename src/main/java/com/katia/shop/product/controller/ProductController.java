package com.katia.shop.product.controller;

import com.katia.shop.product.dto.ProductRequest;
import com.katia.shop.product.dto.ProductResponse;
import com.katia.shop.product.service.ProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;


@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;


    @GetMapping
    @SecurityRequirements
    public Page<ProductResponse> getProducts(@RequestParam(required = false) String keyword, Pageable pageable) {
        return  productService.getProducts(keyword,pageable);

    }

    @GetMapping("/{id}")
    @SecurityRequirements
    public ProductResponse getProductById(@PathVariable Long id) {
       return  productService.findProductById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public ResponseEntity<ProductResponse> createProduct(@RequestBody @Valid ProductRequest productRequest) {
        ProductResponse created = productService.createProduct(productRequest);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_EDIT')")
    public ProductResponse updateProduct(@PathVariable Long id ,@RequestBody @Valid ProductRequest productRequest) {
        return productService.updateProduct(id, productRequest);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }



}
