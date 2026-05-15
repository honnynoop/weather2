package com.mds.weather.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * analytics.weather_sales_corr 테이블 매핑 엔티티
 *
 * [분리 이유] Java 규칙: public class 는 파일 하나당 하나만 허용.
 * 기존 Entities.java 에서 분리.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "weather_sales_corr", schema = "analytics")
public class WeatherSalesCorr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(name = "weather_desc")  private String     weatherDesc;
    @Column(name = "avg_temp_c")    private BigDecimal avgTempC;
    @Column(name = "total_orders")  private Integer    totalOrders;
    @Column(name = "total_revenue") private BigDecimal totalRevenue;
    @Column(name = "top_category")  private String     topCategory;
    @Column(name = "calc_date")     private LocalDate  calcDate;
    @Column(name = "created_at")    private LocalDateTime createdAt;

    public static WeatherSalesCorr of(String city, String desc,
                                      BigDecimal temp, int orders,
                                      BigDecimal revenue, String category) {
        WeatherSalesCorr e = new WeatherSalesCorr();
        e.city         = city;
        e.weatherDesc  = desc;
        e.avgTempC     = temp;
        e.totalOrders  = orders;
        e.totalRevenue = revenue;
        e.topCategory  = category;
        e.calcDate     = LocalDate.now();
        e.createdAt    = LocalDateTime.now();
        return e;
    }

    /** 프롬프트 컨텍스트용 요약 */
    public String toText() {
        return String.format(
            "[분석] 날짜:%s | 도시:%s | 날씨:%s | 기온:%.1f°C | 주문:%d건 | 매출:%,.0f원 | 인기카테고리:%s",
            calcDate, city, weatherDesc, avgTempC, totalOrders, totalRevenue, topCategory
        );
    }
}
