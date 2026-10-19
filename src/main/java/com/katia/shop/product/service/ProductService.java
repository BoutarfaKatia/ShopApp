package com.katia.shop.product.service;

import com.katia.shop.product.exception.ProductAlreadyExistException;
import com.katia.shop.product.exception.ProductNotFoundException;
import com.katia.shop.product.mapper.ProductMapper;
import com.katia.shop.product.dto.ProductRequest;
import com.katia.shop.product.dto.ProductResponse;
import com.katia.shop.product.entity.Product;
import com.katia.shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;



    public Page<ProductResponse> getProducts(String keyword, Pageable pageable) {
        Page<Product> page = StringUtils.hasText(keyword)
                ? productRepository.search(keyword.trim(), pageable)
                : productRepository.findByActiveTrue(pageable);
        return page.map(ProductMapper::toResponse); }


    public ProductResponse findProductById(Long id) {
      return  productRepository.findById(id)
              .map(ProductMapper::toResponse)
              .orElseThrow(() -> new ProductNotFoundException(id));

    }


    public ProductResponse createProduct(ProductRequest request) {
        if(productRepository.existsByName(request.name()) ){
            throw new ProductAlreadyExistException(request.name());
        }
        return ProductMapper.toResponse(productRepository.save(ProductMapper.fromRequest(request)));
    }


    public ProductResponse updateProduct(Long id ,ProductRequest productRequest) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
          if(productRepository.existsByNameAndIdNot(productRequest.name(),id)) {
              throw new ProductAlreadyExistException(productRequest.name());
          }
        ProductMapper.updateEntity(product, productRequest);
        return ProductMapper.toResponse(product);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
       product.setActive(false);

    }
}

