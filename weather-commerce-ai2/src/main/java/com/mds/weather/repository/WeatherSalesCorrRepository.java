package com.mds.weather.repository;

import com.mds.weather.domain.WeatherSalesCorr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * analytics.weather_sales_corr 테이블 Repository
 *
 * [분리 이유] Java 규칙: public interface 는 파일 하나당 하나만 허용.
 * 기존 Repositories.java 에서 분리.
 */
public interface WeatherSalesCorrRepository
        extends JpaRepository<WeatherSalesCorr, Long> {

    List<WeatherSalesCorr> findByCityOrderByCalcDateDesc(String city);

    @Query("SELECT w FROM WeatherSalesCorr w WHERE w.calcDate BETWEEN :from AND :to ORDER BY w.calcDate DESC")
    List<WeatherSalesCorr> findBetween(@Param("from") LocalDate from,
                                       @Param("to")   LocalDate to);

    boolean existsByCityAndCalcDate(String city, LocalDate date);
}
