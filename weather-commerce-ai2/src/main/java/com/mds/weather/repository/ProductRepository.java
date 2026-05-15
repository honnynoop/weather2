package com.mds.weather.repository;

import com.mds.weather.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * raw.products 테이블 Repository
 *
 * [분리 이유] Java 규칙: public interface 는 파일 하나당 하나만 허용.
 * 기존 Repositories.java 에서 분리.
 */
public interface ProductRepository
        extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);

    /** 상품명 또는 카테고리에 키워드 포함 */
    @Query("SELECT p FROM Product p WHERE p.name LIKE %:kw% OR p.category LIKE %:kw%")
    List<Product> searchByKeyword(@Param("kw") String keyword);
}
