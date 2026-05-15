package com.mds.weather.repository;

import com.mds.weather.domain.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * weather.weather_data 테이블 Repository
 *
 * [분리 이유] Java 규칙: public interface 는 파일 하나당 하나만 허용.
 * 기존 Repositories.java 에 5개의 public interface 가 함께 있어
 * 컴파일 에러 발생 → 각각 별도 파일로 분리.
 */
public interface WeatherDataRepository
        extends JpaRepository<WeatherData, Long> {

    /** 특정 도시 최신 날씨 1건 */
    Optional<WeatherData> findTopByCityOrderByCollectedAtDesc(String city);

    /** 오늘 수집된 날씨 전체 */
    @Query("SELECT w FROM WeatherData w WHERE DATE(w.collectedAt) = :date")
    List<WeatherData> findByDate(@Param("date") LocalDate date);

    /** 아직 임베딩 처리되지 않은 날씨 (embedding_log 기준) */
    @Query(value = """
            SELECT w.* FROM weather.weather_data w
            WHERE NOT EXISTS (
                SELECT 1 FROM analytics.embedding_log e
                WHERE e.source_type = 'weather' AND e.source_id = w.id
            )
            ORDER BY w.collected_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<WeatherData> findNotEmbedded(@Param("limit") int limit);
}
