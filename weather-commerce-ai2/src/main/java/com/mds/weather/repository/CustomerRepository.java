package com.mds.weather.repository;

import com.mds.weather.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * raw.customers 테이블 Repository
 *
 * [분리 이유] Java 규칙: public interface 는 파일 하나당 하나만 허용.
 * 기존 Repositories.java 에서 분리.
 */
public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    /** 한국어 도시명 접두어로 검색 (예: '서울%') */
    @Query("SELECT c FROM Customer c WHERE c.city LIKE :prefix%")
    List<Customer> findByCityPrefix(@Param("prefix") String prefix);
}
