package com.mds.weather.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * weather.api.* 설정 바인딩 클래스
 *
 * <p>문제: @Value("${weather.api.cities}") + List&lt;String&gt; 조합은
 * YAML 리스트 형식(- Seoul / - Busan)을 바인딩할 수 없어
 * PlaceholderResolutionException 발생.
 *
 * <p>해결: @ConfigurationProperties 사용
 * → YAML 리스트를 List&lt;String&gt; 으로 자동 바인딩
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "weather.api")
public class WeatherApiProperties {

    /** OpenWeatherMap API 키 */
    private String key;

    /** API Base URL (기본값: https://api.openweathermap.org/data/2.5) */
    private String baseUrl = "https://api.openweathermap.org/data/2.5";

    /**
     * 수집 대상 도시 목록
     * application.yml:
     *   weather.api.cities:
     *     - Seoul
     *     - Busan
     */
    private List<String> cities;
}
