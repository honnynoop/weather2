package com.mds.weather.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * raw.products 테이블 매핑 엔티티
 *
 * [분리 이유] Java 규칙: public class 는 파일 하나당 하나만 허용.
 * 기존 Entities.java 에서 분리.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "products", schema = "raw")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    private String     name;
    private String     category;
    private String     subcategory;
    private String     brand;
    private BigDecimal price;
    private BigDecimal cost;

    @Column(name = "stock_qty")  private Integer       stockQty;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
