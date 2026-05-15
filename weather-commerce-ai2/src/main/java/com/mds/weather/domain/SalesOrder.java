package com.mds.weather.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * raw.orders 테이블 매핑 엔티티
 * (Order 는 SQL 예약어이므로 SalesOrder 로 명명)
 *
 * [분리 이유] Java 규칙: public class 는 파일 하나당 하나만 허용.
 * 기존 Entities.java 에서 분리.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "orders", schema = "raw")
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer    quantity;

    @Column(name = "unit_price")    private BigDecimal    unitPrice;
    @Column(name = "discount_rate") private BigDecimal    discountRate;
    @Column(name = "order_date")    private LocalDate     orderDate;
    private String     status;
    private String     channel;
    @Column(name = "created_at")    private LocalDateTime createdAt;

    /** 실 매출액 = quantity × unitPrice × (1 − discountRate) */
    public BigDecimal getRevenue() {
        return unitPrice
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.ONE.subtract(
                        discountRate != null ? discountRate : BigDecimal.ZERO));
    }
}
