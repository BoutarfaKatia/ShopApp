package com.katia.shop.product.repository;

import com.katia.shop.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {
@Query("""
      select p from Product p
       where p.active = true
      and lower(p.name) like lower(concat('%' , :keyword ,'%'))
       or lower(p.description) like lower(concat('%' , :keyword , '%'))
      
""")
    Page<Product> search(@Param("keyword") String keyword, Pageable pageable);
    Page<Product> findByActiveTrue(Pageable pageable);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name,Long id);
    Optional<Product> findByIdAndActiveTrue(Long id);
}

