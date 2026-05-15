package com.mds.weather.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
//  WeatherData  —  weather.weather_data
//  [수정] package-private → public (repository/service 패키지에서 참조)
// ============================================================
@Getter
@NoArgsConstructor
@Entity
@Table(name = "weather_data", schema = "weather")
public class WeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;
    private String country;
    private String description;

    @Column(name = "temperature_c")  private BigDecimal temperatureC;
    @Column(name = "feels_like_c")   private BigDecimal feelsLikeC;
    @Column(name = "temp_min_c")     private BigDecimal tempMinC;
    @Column(name = "temp_max_c")     private BigDecimal tempMaxC;
    @Column(name = "pressure_hpa")   private Integer    pressureHpa;
    @Column(name = "humidity_pct")   private Integer    humidityPct;
    @Column(name = "wind_speed_ms")  private BigDecimal windSpeedMs;
    @Column(name = "wind_deg")       private Integer    windDeg;
    @Column(name = "visibility_m")   private Integer    visibilityM;
    @Column(name = "cloudiness_pct") private Integer    cloudinessPct;
    @Column(name = "time_of_record") private LocalDateTime timeOfRecord;
    @Column(name = "sunrise_time")   private LocalDateTime sunriseTime;
    @Column(name = "sunset_time")    private LocalDateTime sunsetTime;
    @Column(name = "collected_at")   private LocalDateTime collectedAt;

    /** RAG 문서 생성 및 프롬프트 컨텍스트에 사용 */
    public String toText() {
        return String.format(
            "[날씨] 도시:%s | %s | 기온:%.1f°C (체감 %.1f°C) | 습도:%d%% | 풍속:%.1f m/s | 수집:%s",
            city, description, temperatureC, feelsLikeC, humidityPct, windSpeedMs, collectedAt
        );
    }
}
