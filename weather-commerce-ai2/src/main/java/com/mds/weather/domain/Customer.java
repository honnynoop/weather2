package com.mds.weather.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * raw.customers 테이블 매핑 엔티티
 *
 * [분리 이유] Java 규칙: public class 는 파일 하나당 하나만 허용.
 * 기존 Entities.java 에 Customer/Product/SalesOrder/WeatherSalesCorr
 * 4개의 public class 가 함께 있어 컴파일 에러 발생 → 각각 별도 파일로 분리.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "customers", schema = "raw")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    private String name;
    private String email;
    private String phone;
    private String city;
    private String region;

    @Column(name = "age_group")   private String        ageGroup;
    @Column(name = "gender")      private String        gender;
    @Column(name = "channel")     private String        channel;
    @Column(name = "signup_date") private LocalDate     signupDate;
    @Column(name = "created_at")  private LocalDateTime createdAt;
}
